package com.the_coffe_coders.fastestlap.source.result;

import static com.the_coffe_coders.fastestlap.util.Constants.RETROFIT_ERROR;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.api.RaceResultsAPIResponse;
import com.the_coffe_coders.fastestlap.mapper.RaceMapper;
import com.the_coffe_coders.fastestlap.repository.result.RaceResultCallback;
import com.the_coffe_coders.fastestlap.service.ErgastAPIService;
import com.the_coffe_coders.fastestlap.util.JSONParserUtils;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class JolpicaRaceResultDataSource implements RaceResultDataSource {
    private static final String TAG = "JolpicaRaceResultDataSource";
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

    public void getQualifyingResults(int round, RaceResultCallback resultCallback) {
        Log.d(TAG, "Fetching qualifying results from remote API for round: " + round);
        Call<ResponseBody> responseCall = ergastAPIService.getQualifyingResults(round);

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

                    Log.d(TAG, "Successfully parsed qualifying results: " + raceResultsAPIResponse);
                    if (raceResultsAPIResponse.getFinalRace() != null) {
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
                Log.e(TAG, "Failed to fetch qualifying results", throwable);
                if (resultCallback != null) {
                    resultCallback.onFailure(new Exception(RETROFIT_ERROR, throwable));
                }
            }
        });
    }
}