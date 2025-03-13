package com.the_coffe_coders.fastestlap.repository.constructor;

import android.util.Log;
import androidx.annotation.NonNull;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class CommonConstructorRepository {
    private static final String TAG = "CommonConstructorRepository";
    public static final long FRESH_TIMEOUT = 60000; // 1 minute

    private final JolpicaConstructorRepository jolpicaRepository;
    private final FirebaseConstructorRepository firebaseRepository;

    // Cache for ongoing requests to avoid duplicate requests
    private final ConcurrentHashMap<String, CompletableFuture<Result>> pendingRequests = new ConcurrentHashMap<>();

    // Reference to the latest constructor request
    private final AtomicReference<CompletableFuture<Result>> currentConstructorFuture = new AtomicReference<>();

    public CommonConstructorRepository() {
        jolpicaRepository = new JolpicaConstructorRepository();
        firebaseRepository = new FirebaseConstructorRepository();
    }

    public CompletableFuture<Result> getConstructor(String constructorId) {
        final String requestKey = "constructor_" + constructorId;

        // Check if there's already an ongoing request
        CompletableFuture<Result> existingRequest = pendingRequests.get(requestKey);
        if (existingRequest != null && !existingRequest.isDone()) {
            Log.d(TAG, "Returning existing request for constructor: " + constructorId);
            return existingRequest;
        }

        // Create a new CompletableFuture for this request
        CompletableFuture<Result> future = new CompletableFuture<>();
        pendingRequests.put(requestKey, future);
        currentConstructorFuture.set(future);

        // Set a timeout for the CompletableFuture
        setupFutureTimeout(future, 30000); // 30 seconds timeout

        // First fetch data from Jolpica
        jolpicaRepository.getConstructor(constructorId).whenComplete((jolpicaResult, jolpicaThrowable) -> {
            if (jolpicaThrowable != null) {
                Log.e(TAG, "Error fetching from Jolpica", jolpicaThrowable);
                completeFutureWithError(future, "Failed to fetch from Jolpica: " + jolpicaThrowable.getMessage());
                return;
            }

            if (jolpicaResult instanceof Result.ConstructorSuccess) {
                Constructor jolpicaConstructor = ((Result.ConstructorSuccess) jolpicaResult).getData();

                // Then fetch from Firebase to enhance the data
                firebaseRepository.getConstructorData(constructorId, new FirebaseConstructorRepository.ConstructorCallback() {
                    @Override
                    public void onSuccess(Constructor firebaseConstructor) {
                        firebaseConstructor.setConstructorId(jolpicaConstructor.getConstructorId());
                        firebaseConstructor.setUrl(jolpicaConstructor.getUrl());

                        if (!future.isDone()) {
                            future.complete(new Result.ConstructorSuccess(firebaseConstructor));
                        }
                    }

                    @Override
                    public void onFailure(Exception exception) {
                        // If Firebase fails, use Jolpica data
                        Log.i(TAG, "FIREBASE FAILURE => " + exception.getMessage());

                        if (!future.isDone()) {
                            future.complete(jolpicaResult);
                        }
                    }
                });
            } else {
                if (!future.isDone()) {
                    future.complete(jolpicaResult);
                }
            }
        });

        // When the future is completed (success or failure), remove it from the pending requests map
        future.whenComplete((result, throwable) -> pendingRequests.remove(requestKey));

        return future;
    }

    /**
     * Sets a timeout for a CompletableFuture if it's not completed within the specified time
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
     * Completes a future with an error
     */
    private void completeFutureWithError(CompletableFuture<Result> future, String errorMessage) {
        if (future != null && !future.isDone()) {
            future.complete(new Result.Error(errorMessage));
        }
    }
}