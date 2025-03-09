package com.the_coffe_coders.fastestlap.repository.standings;

import android.util.Log;

import com.the_coffe_coders.fastestlap.api.DriverStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandings;
import com.the_coffe_coders.fastestlap.mapper.DriverStandingsMapper;
import com.the_coffe_coders.fastestlap.source.driver.BaseDriverStandingsLocalDataSource;
import com.the_coffe_coders.fastestlap.source.driver.BaseDriverStandingsRemoteDataSource;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class DriverStandingsRepository implements IDriverStandingsRepository, DriverStandingsResponseCallback {
    private static final String TAG = "DriverStandingsRepository";
    public static final long FRESH_TIMEOUT = 60000; // 1 minuto

    private final BaseDriverStandingsRemoteDataSource driverRemoteDataSource;
    private final BaseDriverStandingsLocalDataSource driverLocalDataSource;

    // Cache per le richieste in corso per evitare richieste duplicate
    private final ConcurrentHashMap<String, CompletableFuture<Result>> pendingRequests = new ConcurrentHashMap<>();

    // Riferimento all'ultima richiesta di standings
    private final AtomicReference<CompletableFuture<Result>> currentDriverStandingsFuture = new AtomicReference<>();

    public DriverStandingsRepository(BaseDriverStandingsRemoteDataSource driverRemoteDataSource, BaseDriverStandingsLocalDataSource driverLocalDataSource) {
        this.driverRemoteDataSource = driverRemoteDataSource;
        this.driverLocalDataSource = driverLocalDataSource;
        this.driverRemoteDataSource.setDriverCallback(this);
        this.driverLocalDataSource.setDriverCallback(this);
    }

    @Override
    public CompletableFuture<Result> fetchDriversStandings(long lastUpdate) {
        final String requestKey = "driverStandings";

        // Controlla se esiste già una richiesta in corso
        CompletableFuture<Result> existingRequest = pendingRequests.get(requestKey);
        if (existingRequest != null && !existingRequest.isDone()) {
            Log.d(TAG, "Returning existing request for driver standings");
            return existingRequest;
        }

        // Crea un nuovo CompletableFuture per questa richiesta
        CompletableFuture<Result> future = new CompletableFuture<>();
        pendingRequests.put(requestKey, future);
        currentDriverStandingsFuture.set(future);

        // Imposta un timeout per il CompletableFuture
        setupFutureTimeout(future, 30000); // 30 secondi di timeout

        long currentTime = System.currentTimeMillis();
        boolean shouldFetchFromRemote = currentTime - lastUpdate > FRESH_TIMEOUT;

        if (shouldFetchFromRemote) {
            Log.i(TAG, "FETCHING FROM REMOTE SOURCE");
            // Tentativo di recupero remoto
            try {
                driverRemoteDataSource.getDriversStandings();
            } catch (Exception e) {
                Log.e(TAG, "Exception during remote fetch", e);
                // In caso di errore immediato, prova con la fonte locale
                fallbackToLocalSource();
            }
        } else {
            Log.i(TAG, "FETCHING FROM LOCAL SOURCE");
            try {
                driverLocalDataSource.getDriversStandings();
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
            driverLocalDataSource.getDriversStandings();
        } catch (Exception e) {
            Log.e(TAG, "Exception during local fallback", e);
            CompletableFuture<Result> future = currentDriverStandingsFuture.get();
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
    public void onSuccessFromRemote(DriverStandingsAPIResponse driverAPIResponse, long lastUpdate) {
        Log.i(TAG, "REMOTE DATA SUCCESS: " + driverAPIResponse);

        if (driverAPIResponse == null ||
                driverAPIResponse.getStandingsTable() == null ||
                driverAPIResponse.getStandingsTable().getStandingsLists().isEmpty()) {

            Log.i(TAG, "DRIVER API RESPONSE EMPTY");
            CompletableFuture<Result> future = currentDriverStandingsFuture.get();

            // Prova a recuperare dati locali come fallback
            fallbackToLocalSource();
            return;
        }

        try {
            DriverStandings driverStandings = DriverStandingsMapper.toDriverStandings(
                    driverAPIResponse.getStandingsTable().getStandingsLists().get(0));

            Log.i(TAG, "DRIVER STANDINGS MAPPED: " + driverStandings);

            // Salva nel database locale in modo asincrono
            driverLocalDataSource.insertDriversStandings(driverStandings);

            // Il callback onSuccessFromLocal verrà chiamato dopo l'inserimento nel DB
        } catch (Exception e) {
            Log.e(TAG, "Error mapping or saving driver standings", e);
            CompletableFuture<Result> future = currentDriverStandingsFuture.get();
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
    public void onSuccessFromLocal(List<Driver> driverList) {
        // Questo metodo non è utilizzato per gli standings, ma è richiesto dall'interfaccia
    }

    @Override
    public void onSuccessFromLocal(DriverStandings driverStandings) {
        Log.i(TAG, "LOCAL DATA SUCCESS: " + driverStandings);

        CompletableFuture<Result> future = currentDriverStandingsFuture.get();
        if (future != null && !future.isDone()) {
            if (driverStandings != null) {
                future.complete(new Result.DriverStandingsSuccess(driverStandings));
            } else {
                future.complete(new Result.Error("No driver standings data available"));
            }
        }
    }

    @Override
    public void onFailureFromLocal(Exception exception) {
        Log.e(TAG, "Local data fetch failed", exception);

        CompletableFuture<Result> future = currentDriverStandingsFuture.get();
        if (future != null && !future.isDone()) {
            future.complete(new Result.Error("Failed to get driver standings: " +
                    (exception != null ? exception.getMessage() : "Unknown error")));
        }
    }
}