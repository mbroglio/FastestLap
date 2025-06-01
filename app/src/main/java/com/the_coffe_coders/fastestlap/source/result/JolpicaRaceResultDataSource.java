package com.the_coffe_coders.fastestlap.source.result;

import static com.the_coffe_coders.fastestlap.util.Constants.RETROFIT_ERROR;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.the_coffe_coders.fastestlap.api.RaceResultsAPIResponse;
import com.the_coffe_coders.fastestlap.mapper.RaceMapper;
import com.the_coffe_coders.fastestlap.repository.result.RaceResultCallback;
import com.the_coffe_coders.fastestlap.service.ErgastAPIService;
import com.the_coffe_coders.fastestlap.util.JSONParserUtils;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class JolpicaRaceResultDataSource implements RaceResultDataSource {
    private static final String TAG = "JolpicaRaceResultDataSource";
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 2000; // 2 seconds
    private static JolpicaRaceResultDataSource instance;
    private final ErgastAPIService ergastAPIService;

    public JolpicaRaceResultDataSource() {
        this.ergastAPIService = ServiceLocator.getInstance().getConcreteErgastAPIService();
    }

    public static synchronized JolpicaRaceResultDataSource getInstance() {
        if (instance == null) {
            instance = new JolpicaRaceResultDataSource();
        }
        return instance;
    }

    public void getRaceResults(int round, RaceResultCallback resultCallback) {
        Log.d(TAG, "Fetching race results from remote API for round: " + round);
        Call<ResponseBody> responseCall = ergastAPIService.getRaceResults(round);

        responseCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                ResponseBody body = response.body();
                if (body == null) {
                    Log.e(TAG, "API returned empty body");
                    resultCallback.onFailure(new Exception("Empty response body"));
                    return;
                }

                try {
                    String responseString = body.string();
                    JsonObject jsonResponse = new Gson().fromJson(responseString, JsonObject.class);

                    if (jsonResponse == null) {
                        Log.e(TAG, "Failed to parse JSON response");
                        resultCallback.onFailure(new Exception("Invalid JSON response"));
                        return;
                    }

                    JsonObject mrdata = jsonResponse.getAsJsonObject("MRData");
                    if (mrdata == null) {
                        Log.e(TAG, "MRData not found in response");
                        resultCallback.onFailure(new Exception("MRData not found in response"));
                        return;
                    }

                    JSONParserUtils jsonParserUtils = new JSONParserUtils();
                    RaceResultsAPIResponse raceResultsAPIResponse = jsonParserUtils.parseRaceResults(mrdata);

                    Log.d(TAG, "Successfully parsed race results: " + raceResultsAPIResponse);
                    if(raceResultsAPIResponse.getFinalRace() != null){
                        resultCallback.onSuccess(RaceMapper.toRace(raceResultsAPIResponse.getFinalRace()));
                    }
                } catch (IOException e) {
                    Log.e(TAG, "IOException while reading response", e);
                    resultCallback.onFailure(new Exception("Failed to read response: " + e.getMessage(), e));
                } catch (Exception e) {
                    Log.e(TAG, "Exception while processing response", e);
                    resultCallback.onFailure(new Exception("Failed to process response: " + e.getMessage(), e));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable throwable) {
                Log.e(TAG, "Failed to fetch race results", throwable);
                if (resultCallback != null) {
                    resultCallback.onFailure(new Exception(RETROFIT_ERROR, throwable));
                }
            }
        });
    }

    public void fetchRaceResult(int raceNumber, int currentRetry,
                                AtomicInteger successCount, AtomicInteger failureCount,
                                int totalRaces, RaceResultCallback callback, boolean isAPIRetry) {
        Log.d(TAG, String.format("Fetching results for race %d (attempt %d)",
                raceNumber, currentRetry + 1));
        Call<ResponseBody> responseCall;
        Log.i(TAG, "isAPIRetry: " + isAPIRetry);

        if(isAPIRetry){
            responseCall = ergastAPIService.getJustFinishedRace(raceNumber);
        }else{
            responseCall = ergastAPIService.getRaceResults(raceNumber);
        }

        responseCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                ResponseBody body = response.body();
                if (response.isSuccessful() && body != null) {
                    try {
                        String responseString = body.string();
                        processRaceResult(responseString, raceNumber, callback, currentRetry, successCount, failureCount, totalRaces);

                        int completed = successCount.incrementAndGet();
                        checkAllRequestsCompleted(completed, failureCount.get(), totalRaces);
                    } catch (IOException e) {
                        handleRetryOrFailure(raceNumber, currentRetry,
                                new Exception("Failed to read response: " + e.getMessage(), e),
                                successCount, failureCount, totalRaces, callback);
                    }
                } else {
                    handleRetryOrFailure(raceNumber, currentRetry,
                            new Exception("API returned " + response.code()),
                            successCount, failureCount, totalRaces, callback);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                handleRetryOrFailure(raceNumber, currentRetry,
                        new Exception(RETROFIT_ERROR + ": " + t.getMessage(), t),
                        successCount, failureCount, totalRaces, callback);
            }
        });
    }

    private void processRaceResult(String responseString, int raceNumber, RaceResultCallback callback, int currentRetry,
                                   AtomicInteger successCount, AtomicInteger failureCount, int totalRaces) {
        try {
            JsonObject jsonResponse = new Gson().fromJson(responseString, JsonObject.class);

            if (jsonResponse == null) {
                handleFailure(raceNumber, new Exception("Invalid JSON response"), callback);
                return;
            }

            JsonObject mrdata = jsonResponse.getAsJsonObject("MRData");

            if (mrdata == null) {
                handleFailure(raceNumber, new Exception("MRData not found in response"), callback);
                return;
            }

            JSONParserUtils jsonParserUtils = new JSONParserUtils();
            RaceResultsAPIResponse raceResultsAPIResponse = jsonParserUtils.parseRaceResults(mrdata);

            if(raceResultsAPIResponse.getFinalRace() == null){
                Log.w(TAG, "Current API returned null, trying backup API");
                fetchRaceResult(raceNumber, currentRetry, successCount, failureCount, totalRaces, callback, true);
                return;
            }

            Log.d(TAG, "Successfully processed race " + raceNumber);
            callback.onSuccess(RaceMapper.toRace(raceResultsAPIResponse.getFinalRace()));
        } catch (Exception e) {
            handleFailure(raceNumber, new Exception("Failed to process response: " + e.getMessage(), e), callback);
        }
    }

    private void handleRetryOrFailure(int raceNumber, int currentRetry, Exception error,
                                      AtomicInteger successCount, AtomicInteger failureCount,
                                      int totalRaces, RaceResultCallback callback) {
        if (currentRetry < MAX_RETRIES) {
            Log.w(TAG, String.format("Retrying race %d after failure (attempt %d/%d): %s",
                    raceNumber, currentRetry + 1, MAX_RETRIES, error.getMessage()));

            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() ->
                    fetchRaceResult(raceNumber, currentRetry + 1,
                            successCount, failureCount, totalRaces, callback, false), RETRY_DELAY_MS);
        } else {
            handleFailure(raceNumber, error, callback);
            checkAllRequestsCompleted(successCount.get(),
                    failureCount.incrementAndGet(), totalRaces);
        }
    }

    private void handleFailure(int raceNumber, Exception e, RaceResultCallback callback) {
        Log.e(TAG, "Failed to fetch/process race " + raceNumber + ": " + e.getMessage());
        callback.onFailure(e);
    }

    private void checkAllRequestsCompleted(int successCount, int failureCount, int totalRaces) {
        if (successCount + failureCount == totalRaces) {
            Log.i(TAG, String.format("All races processed. Success: %d, Failures: %d",
                    successCount, failureCount));
        }
    }
}