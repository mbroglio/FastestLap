package com.the_coffe_coders.fastestlap.repository.standings.constructor;

import android.util.Log;

import com.the_coffe_coders.fastestlap.api.ConstructorStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandings;
import com.the_coffe_coders.fastestlap.mapper.ConstructorStandingsMapper;
import com.the_coffe_coders.fastestlap.source.constructor_standings.BaseConstructorStandingsLocalDataSource;
import com.the_coffe_coders.fastestlap.source.constructor_standings.BaseConstructorStandingsRemoteDataSource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class ConstructorStandingsStandingsRepository implements IConstructorStandingsRepository, ConstructorStandingsResponseCallback {

    public static final long FRESH_TIMEOUT = 60000; // 1 minuto
    private static final String TAG = "ConstructorStandingsRepository";
    private final BaseConstructorStandingsRemoteDataSource constructorRemoteDataSource;
    private final BaseConstructorStandingsLocalDataSource constructorLocalDataSource;

    private final ConcurrentHashMap<String, CompletableFuture<Result>> pendingRequests = new ConcurrentHashMap<>();

    private final AtomicReference<CompletableFuture<Result>> currentConstructorStandingsFuture = new AtomicReference<>();

    public ConstructorStandingsStandingsRepository(BaseConstructorStandingsRemoteDataSource constructorRemoteDataSource, BaseConstructorStandingsLocalDataSource constructorLocalDataSource) {
        this.constructorRemoteDataSource = constructorRemoteDataSource;
        this.constructorLocalDataSource = constructorLocalDataSource;
        this.constructorRemoteDataSource.setConstructorCallback(this);
        this.constructorLocalDataSource.setConstructorCallback(this);
    }

    @Override
    public CompletableFuture<Result> fetchConstructorStandings(long lastUpdate) {
        final String requestKey = "constructorStandings";

        // Controlla se esiste già una richiesta in corso
        CompletableFuture<Result> existingRequest = pendingRequests.get(requestKey);
        if (existingRequest != null && !existingRequest.isDone()) {
            Log.d(TAG, "Returning existing request for constructor standings");
            return existingRequest;
        }

        // Crea un nuovo CompletableFuture per questa richiesta
        CompletableFuture<Result> future = new CompletableFuture<>();
        pendingRequests.put(requestKey, future);
        currentConstructorStandingsFuture.set(future);

        // Imposta un timeout per il CompletableFuture
        setupFutureTimeout(future, 30000); // 30 secondi di timeout

        long currentTime = System.currentTimeMillis();
        boolean shouldFetchFromRemote = currentTime - lastUpdate > FRESH_TIMEOUT;

        if (shouldFetchFromRemote) {
            Log.i(TAG, "FETCHING FROM REMOTE SOURCE");
            // Tentativo di recupero remoto
            try {
                constructorRemoteDataSource.getConstructorStandings();
            } catch (Exception e) {
                Log.e(TAG, "Exception during remote fetch", e);
                // In caso di errore immediato, prova con la fonte locale
                fallbackToLocalSource();
            }
        } else {
            Log.i(TAG, "FETCHING FROM LOCAL SOURCE");
            try {
                constructorLocalDataSource.getConstructorStandings();
            } catch (Exception e) {
                Log.e(TAG, "Exception during local fetch", e);
                completeFutureWithError(future, "Failed to fetch from local source: " + e.getMessage());
            }
        }

        // Quando il futuro è completato (con successo o fallimento), rimuovi dalla mappa delle richieste in corso
        future.whenComplete((result, throwable) -> pendingRequests.remove(requestKey));

        return future;
    }

    /**
     * Imposta un timeout per un CompletableFuture se non viene completato entro il tempo specificato
     */
    private void setupFutureTimeout(CompletableFuture<Result> future, long timeoutMs) {
        Thread timeoutThread = new Thread(() -> {
            try {
                Thread.sleep(timeoutMs);
                if (!future.isDone()) {
                    Log.w(TAG, "Request timed out after " + timeoutMs + "ms");
                    future.complete(new Result.Error("Request timed out"));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        timeoutThread.setDaemon(true);
        timeoutThread.start();
    }

    /**
     * Fallback alla fonte dati locale in caso di errore remoto
     */
    private void fallbackToLocalSource() {
        Log.i(TAG, "Falling back to local data source");
        try {
            constructorLocalDataSource.getConstructorStandings();
        } catch (Exception e) {
            Log.e(TAG, "Exception during local fallback", e);
            CompletableFuture<Result> future = currentConstructorStandingsFuture.get();
            if (future != null && !future.isDone()) {
                completeFutureWithError(future, "Failed to fetch from both remote and local sources");
            }
        }
    }

    /**
     * Completa un future con un errore
     */
    private void completeFutureWithError(CompletableFuture<Result> future, String errorMessage) {
        if (future != null && !future.isDone()) {
            future.complete(new Result.Error(errorMessage));
        }
    }

    @Override
    public void onSuccessFromRemote(ConstructorStandingsAPIResponse constructorStandingsAPIResponse, long lastUpdate) {
        Log.i(TAG, "REMOTE DATA SUCCESS: " + constructorStandingsAPIResponse);

        if (constructorStandingsAPIResponse == null ||
                constructorStandingsAPIResponse.getStandingsTable() == null ||
                constructorStandingsAPIResponse.getStandingsTable().getStandingsLists().isEmpty()) {

            Log.i(TAG, "CONSTRUCTOR API RESPONSE EMPTY");

            // Prova a recuperare dati locali come fallback
            fallbackToLocalSource();
            return;
        }

        try {
            ConstructorStandings constructorStandings = ConstructorStandingsMapper.toConstructorStandings(
                    constructorStandingsAPIResponse.getStandingsTable().getStandingsLists().get(0));

            Log.i(TAG, "CONSTRUCTOR STANDINGS MAPPED: " + constructorStandings);

            // Salva nel database locale in modo asincrono
            constructorLocalDataSource.insertConstructorsStandings(constructorStandings);

            // Il callback onSuccessFromLocal verrà chiamato dopo l'inserimento nel DB
        } catch (Exception e) {
            Log.e(TAG, "Error mapping or saving constructor standings", e);
            CompletableFuture<Result> future = currentConstructorStandingsFuture.get();
            if (future != null && !future.isDone()) {
                future.complete(new Result.Error("Error processing remote data: " + e.getMessage()));
            }
        }
    }

    @Override
    public void onFailureFromRemote(Exception exception) {
        Log.e(TAG, "Remote data fetch failed", exception);
        // Prova a recuperare i dati dal database locale come fallback
        fallbackToLocalSource();
    }

    @Override
    public void onSuccessFromLocal(ConstructorStandings constructorStandings) {
        Log.i(TAG, "LOCAL DATA SUCCESS: " + constructorStandings);

        CompletableFuture<Result> future = currentConstructorStandingsFuture.get();
        if (future != null && !future.isDone()) {
            if (constructorStandings != null) {
                future.complete(new Result.ConstructorStandingsSuccess(constructorStandings));
            } else {
                future.complete(new Result.Error("No constructor standings data available"));
            }
        }
    }

    @Override
    public void onFailureFromLocal(Exception exception) {
        Log.e(TAG, "Local data fetch failed", exception);

        CompletableFuture<Result> future = currentConstructorStandingsFuture.get();
        if (future != null && !future.isDone()) {
            future.complete(new Result.Error("Failed to get constructor standings: " +
                    (exception != null ? exception.getMessage() : "Unknown error")));
        }
    }
}