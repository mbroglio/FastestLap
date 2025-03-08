package com.the_coffe_coders.fastestlap.data.source.result;

import static com.the_coffe_coders.fastestlap.core.util.Constants.RETROFIT_ERROR;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.the_coffe_coders.fastestlap.data.api.RaceResultsAPIResponse;
import com.the_coffe_coders.fastestlap.data.service.ErgastAPIService;
import com.the_coffe_coders.fastestlap.core.util.JSONParserUtils;
import com.the_coffe_coders.fastestlap.core.util.ServiceLocator;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RaceResultRemoteDataSource extends BaseRaceResultRemoteDataSource {
    private static final String TAG = "RaceResultRemoteDataSource";
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 2000; // 2 seconds
    private final ErgastAPIService ergastAPIService;

    public RaceResultRemoteDataSource(String apiKey) {
        this.ergastAPIService = ServiceLocator.getInstance().getConcreteErgastAPIService();
    }

    @Override
    public void getRaceResults(int round) {
        Call<ResponseBody> responseCall = ergastAPIService.getRaceResults(round);
        Log.i(TAG, "getRaceResults from remote");
        responseCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseString = null;
                    try {
                        responseString = response.body().string();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    JsonObject jsonResponse = new Gson().fromJson(responseString, JsonObject.class);
                    JsonObject mrdata = jsonResponse.getAsJsonObject("MRData");
                    JSONParserUtils jsonParserUtils = new JSONParserUtils();
                    RaceResultsAPIResponse raceResultsAPIResponse = jsonParserUtils.parseRaceResults(mrdata);

                    raceResultCallback.onSuccessFromRemote(raceResultsAPIResponse, 0);
                } else {
                    raceResultCallback.onFailureFromRemote(new Exception());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                raceResultCallback.onFailureFromRemote(new Exception(RETROFIT_ERROR));
            }
        });

    }

    @Override
    public void getAllRaceResults() {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger retryCount = new AtomicInteger(0);
        final int TOTAL_RACES = 24;

        for (int i = 1; i <= TOTAL_RACES; i++) {
            final int raceNumber = i;
            fetchRaceResult(raceNumber, 0, successCount, failureCount, TOTAL_RACES);
        }
    }

    @Override
    public void getLastRaceResults() {
        Call<ResponseBody> responseCall = ergastAPIService.getLastRaceResults();
        responseCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call,
                                   @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseString = null;
                    try {
                        responseString = response.body().string();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    JsonObject jsonResponse = new Gson().fromJson(responseString, JsonObject.class);
                    JsonObject mrdata = jsonResponse.getAsJsonObject("MRData");
                    JSONParserUtils jsonParserUtils = new JSONParserUtils();
                    RaceResultsAPIResponse raceResultsAPIResponse = jsonParserUtils.parseRaceResults(mrdata);
                    Log.i(TAG, "LAST RACE RESULTS !!!!" + raceResultsAPIResponse.toString());
                    raceResultCallback.onSuccessFromRemote(raceResultsAPIResponse, 0);
                } else {
                    raceResultCallback.onFailureFromRemote(new Exception());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {

            }
        });
    }

    private void fetchRaceResult(int raceNumber, int currentRetry,
                                 AtomicInteger successCount, AtomicInteger failureCount,
                                 int totalRaces) {
        Call<ResponseBody> responseCall = ergastAPIService.getRaceResults(raceNumber);
        Log.i(TAG, String.format("Fetching results for race %d (attempt %d)",
                raceNumber, currentRetry + 1));

        responseCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call,
                                   @NonNull Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseString = response.body().string();
                        processRaceResult(responseString, raceNumber);

                        int completed = successCount.incrementAndGet();
                        checkAllRequestsCompleted(completed, failureCount.get(), totalRaces);
                    } else {
                        handleRetryOrFailure(raceNumber, currentRetry,
                                new Exception("API returned " + response.code()),
                                successCount, failureCount, totalRaces);
                    }
                } catch (IOException e) {
                    handleRetryOrFailure(raceNumber, currentRetry, e,
                            successCount, failureCount, totalRaces);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                handleRetryOrFailure(raceNumber, currentRetry,
                        new Exception(RETROFIT_ERROR + ": " + t.getMessage()),
                        successCount, failureCount, totalRaces);
            }
        });
    }

    private void handleRetryOrFailure(int raceNumber, int currentRetry, Exception error,
                                      AtomicInteger successCount, AtomicInteger failureCount,
                                      int totalRaces) {
        if (currentRetry < MAX_RETRIES) {
            Log.w(TAG, String.format("Retrying race %d after failure (attempt %d/%d): %s",
                    raceNumber, currentRetry + 1, MAX_RETRIES, error.getMessage()));

            // Delay before retry using Handler
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                fetchRaceResult(raceNumber, currentRetry + 1,
                        successCount, failureCount, totalRaces);
            }, RETRY_DELAY_MS);
        } else {
            handleFailure(raceNumber, error);
            checkAllRequestsCompleted(successCount.get(),
                    failureCount.incrementAndGet(), totalRaces);
        }
    }

    private void processRaceResult(String responseString, int raceNumber) {
        try {
            JsonObject jsonResponse = new Gson().fromJson(responseString, JsonObject.class);
            JsonObject mrdata = jsonResponse.getAsJsonObject("MRData");
            JSONParserUtils jsonParserUtils = new JSONParserUtils();
            RaceResultsAPIResponse raceResultsAPIResponse =
                    jsonParserUtils.parseRaceResults(mrdata);

            Log.d(TAG, "Successfully processed race " + raceNumber);
            raceResultCallback.onSuccessFromRemote(raceResultsAPIResponse, 1);
        } catch (JsonParseException e) {
            handleFailure(raceNumber,
                    new Exception("JSON parsing failed: " + e.getMessage()));
        }
    }

    private void handleFailure(int raceNumber, Exception e) {
        Log.e(TAG, "Failed to fetch/process race " + raceNumber + ": " +
                e.getMessage());
        raceResultCallback.onFailureFromRemote(e);
    }

    private void checkAllRequestsCompleted(int successCount, int failureCount,
                                           int totalRaces) {
        if (successCount + failureCount == totalRaces) {
            Log.i(TAG, String.format(
                    "All races processed. Success: %d, Failures: %d",
                    successCount, failureCount));
        }
    }
}