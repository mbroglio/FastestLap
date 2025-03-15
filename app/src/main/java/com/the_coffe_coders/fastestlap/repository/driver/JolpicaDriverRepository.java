package com.the_coffe_coders.fastestlap.repository.driver;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.api.DriversAPIResponse;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.dto.DriverDTO;
import com.the_coffe_coders.fastestlap.mapper.DriverMapper;
import com.the_coffe_coders.fastestlap.util.JSONParserUtils;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class JolpicaDriverRepository {
    private static final String TAG = "JolpicaDriverRepository";

    public JolpicaDriverRepository() {
        // Empty constructor
    }

    public CompletableFuture<Result> getDriver(String driverId) {
        CompletableFuture<Result> future = new CompletableFuture<>();

        Log.i(TAG, "Fetching driver with ID: " + driverId);
        Call<ResponseBody> responseCall = ServiceLocator.getInstance()
                .getConcreteErgastAPIService()
                .getDriver(driverId);

        responseCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.i(TAG, "onResponse received for driver: " + driverId);
                if (response.isSuccessful() && response.body() != null) {
                    String responseString;
                    try {
                        responseString = response.body().string();
                        JsonObject jsonResponse = new Gson().fromJson(responseString, JsonObject.class);
                        JsonObject mrdata = jsonResponse.getAsJsonObject("MRData");

                        JSONParserUtils jsonParserUtils = new JSONParserUtils();
                        DriversAPIResponse driversAPIResponse = jsonParserUtils.parseDrivers(mrdata);
                        Log.i(TAG, "Parsed driver response: " + driversAPIResponse);

                        try {
                            Driver driver = DriverMapper.toDriver(
                                    driversAPIResponse.getStandingsTable().getDriverDTOList().get(0));
                            future.complete(new Result.DriverSuccess(driver));
                        } catch (Exception e) {
                            Log.e(TAG, "Error mapping driver data", e);
                            future.complete(new Result.Error("Error mapping driver data: " + e.getMessage()));
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading response body", e);
                        future.complete(new Result.Error("Error reading response: " + e.getMessage()));
                    }
                } else {
                    Log.i(TAG, "Request unsuccessful or empty response for driver: " + driverId);
                    future.complete(new Result.Error("Failed to fetch driver data, response code: " +
                            (response.code() + " " + response.message())));
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable throwable) {
                Log.e(TAG, "Request failed for driver: " + driverId, throwable);
                future.complete(new Result.Error("Network error: " +
                        (throwable != null ? throwable.getMessage() : "Unknown error")));
            }
        });

        // Set a timeout for the future
        setupFutureTimeout(future, 30000); // 30 seconds timeout

        return future;
    }

    public CompletableFuture<Result> getDrivers() {
        CompletableFuture<Result> future = new CompletableFuture<>();

        Log.i(TAG, "Fetching all drivers");
        Call<ResponseBody> responseCall = ServiceLocator.getInstance()
                .getConcreteErgastAPIService()
                .getDrivers();

        responseCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.i(TAG, "onResponse received for all drivers");
                if (response.isSuccessful() && response.body() != null) {
                    String responseString;
                    try {
                        responseString = response.body().string();
                        JsonObject jsonResponse = new Gson().fromJson(responseString, JsonObject.class);
                        JsonObject mrdata = jsonResponse.getAsJsonObject("MRData");

                        JSONParserUtils jsonParserUtils = new JSONParserUtils();
                        DriversAPIResponse driversAPIResponse = jsonParserUtils.parseDrivers(mrdata);
                        Log.i(TAG, "Parsed drivers response: " + driversAPIResponse);

                        try {
                            List<Driver> drivers = new ArrayList<>();
                            for (DriverDTO driverDTO : driversAPIResponse.getStandingsTable().getDriverDTOList()) {
                                drivers.add(DriverMapper.toDriver(driverDTO));
                            }
                            future.complete(new Result.DriversSuccess(drivers));
                        } catch (Exception e) {
                            Log.e(TAG, "Error mapping drivers data", e);
                            future.complete(new Result.Error("Error mapping drivers data: " + e.getMessage()));
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading response body", e);
                        future.complete(new Result.Error("Error reading response: " + e.getMessage()));
                    }
                } else {
                    Log.i(TAG, "Request unsuccessful or empty response for all drivers");
                    future.complete(new Result.Error("Failed to fetch drivers data, response code: " +
                            (response.code() + " " + response.message())));
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable throwable) {
                Log.e(TAG, "Request failed for all drivers", throwable);
                future.complete(new Result.Error("Network error: " +
                        (throwable != null ? throwable.getMessage() : "Unknown error")));
            }
        });

        // Set a timeout for the future
        setupFutureTimeout(future, 30000); // 30 seconds timeout

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