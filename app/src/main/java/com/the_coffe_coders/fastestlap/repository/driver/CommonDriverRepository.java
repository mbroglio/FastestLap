package com.the_coffe_coders.fastestlap.repository.driver;

import android.util.Log;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class CommonDriverRepository {
    public static final long FRESH_TIMEOUT = 60000; // 1 minute
    private static final String TAG = "CommonDriverRepository";
    private final JolpicaDriverRepository jolpicaDriverRepository;
    private final FirebaseDriverRepository firebaseDriverRepository;

    // Cache for ongoing requests to avoid duplicate requests
    private final ConcurrentHashMap<String, CompletableFuture<Result>> pendingRequests = new ConcurrentHashMap<>();

    // Reference to the current driver request
    private final AtomicReference<CompletableFuture<Result>> currentDriverFuture = new AtomicReference<>();

    public CommonDriverRepository() {
        jolpicaDriverRepository = new JolpicaDriverRepository();
        firebaseDriverRepository = new FirebaseDriverRepository();
    }

    public CompletableFuture<Result> getDriver(String driverId) {
        final String requestKey = "driver_" + driverId;

        // Check if a request is already in progress
        CompletableFuture<Result> existingRequest = pendingRequests.get(requestKey);
        if (existingRequest != null && !existingRequest.isDone()) {
            Log.d(TAG, "Returning existing request for driver: " + driverId);
            return existingRequest;
        }

        // Create a new CompletableFuture for this request
        CompletableFuture<Result> future = new CompletableFuture<>();
        pendingRequests.put(requestKey, future);
        currentDriverFuture.set(future);

        // Set a timeout for the CompletableFuture
        setupFutureTimeout(future, 60000); // 60 seconds timeout

        Log.i(TAG, "Getting driver with ID: " + driverId);

        // First get driver data from Jolpica API
        jolpicaDriverRepository.getDriver(driverId)
                .thenCompose(jolpicaResult -> {
                    if (jolpicaResult instanceof Result.DriverSuccess) {
                        // If Jolpica succeeds, enrich the data with Firebase data
                        Driver jolpicaDriver = ((Result.DriverSuccess) jolpicaResult).getData();
                        Log.i(TAG, "Successfully got driver from Jolpica: " + jolpicaDriver);

                        return firebaseDriverRepository.getDriver(driverId)
                                .thenApply(firebaseResult -> {
                                    if (firebaseResult instanceof Result.DriverSuccess) {
                                        // If Firebase succeeds, merge the data
                                        Driver firebaseDriver = ((Result.DriverSuccess) firebaseResult).getData();
                                        Log.i(TAG, "Successfully got driver from Firebase: " + firebaseDriver);

                                        // Merge data, prioritizing Firebase data but keeping essential Jolpica data
                                        firebaseDriver.setDriverId(jolpicaDriver.getDriverId());
                                        firebaseDriver.setPermanentNumber(jolpicaDriver.getPermanentNumber());
                                        firebaseDriver.setCode(jolpicaDriver.getCode());
                                        firebaseDriver.setUrl(jolpicaDriver.getUrl());

                                        return new Result.DriverSuccess(firebaseDriver);
                                    } else {
                                        // If Firebase fails, use Jolpica data only
                                        Log.i(TAG, "Failed to get driver from Firebase, using Jolpica data only");
                                        return jolpicaResult;
                                    }
                                });
                    } else {
                        // If Jolpica fails, return the error
                        Log.e(TAG, "Failed to get driver from Jolpica");
                        return CompletableFuture.completedFuture(jolpicaResult);
                    }
                })
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        Log.e(TAG, "Exception occurred while fetching driver", throwable);
                        future.complete(new Result.Error("Exception: " + throwable.getMessage()));
                    } else {
                        future.complete(result);
                    }

                    // When the future is completed (successfully or with failure), remove it from the pending requests map
                    pendingRequests.remove(requestKey);
                });

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
}