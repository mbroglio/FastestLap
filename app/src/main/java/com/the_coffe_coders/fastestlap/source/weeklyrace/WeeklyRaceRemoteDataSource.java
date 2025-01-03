package com.the_coffe_coders.fastestlap.source.weeklyrace;

import static com.the_coffe_coders.fastestlap.util.Constants.RETROFIT_ERROR;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.api.DriverStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.api.RaceAPIResponse;
import com.the_coffe_coders.fastestlap.api.ResultsAPIResponse;
import com.the_coffe_coders.fastestlap.repository.weeklyrace.OperationType;
import com.the_coffe_coders.fastestlap.service.ErgastAPIService;
import com.the_coffe_coders.fastestlap.util.JSONParserUtils;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeeklyRaceRemoteDataSource extends BaseWeeklyRaceRemoteDataSource {

    private final ErgastAPIService ergastAPIService;

    private static final String TAG = "WeeklyRaceRemoteDataSource";

    public WeeklyRaceRemoteDataSource(String apiKey) {
        this.ergastAPIService = ServiceLocator.getInstance().getWeeklyRaceAPIService();
    }

    @Override
    public void getWeeklyRaces() {
        Call<ResponseBody> responseCall = ergastAPIService.getRaces();
        Log.i(TAG, "getWeeklyRaces from remote");
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
                    RaceAPIResponse raceAPIsponse = jsonParserUtils.parseRace(mrdata);

                    raceCallback.onSuccessFromRemote(raceAPIsponse, OperationType.FETCH_WEEKLY_RACES);
                } else {
                    raceCallback.onFailureFromRemote(new Exception());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                raceCallback.onFailureFromRemote(new Exception(RETROFIT_ERROR));
            }
        });
    }

    @Override
    public void getNextRace() {
        Call<ResponseBody> responseCall = ergastAPIService.getNextRace();

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
                    RaceAPIResponse raceAPIResponse = jsonParserUtils.parseRace(mrdata);
                    System.out.println("CALLBACK");
                    raceCallback.onSuccessFromRemote(raceAPIResponse, OperationType.FETCH_NEXT_RACE);
                } else {
                    raceCallback.onFailureFromRemote(new Exception());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                raceCallback.onFailureFromRemote(new Exception(RETROFIT_ERROR));
            }
        });


    }

    @Override
    public void getLastRace() {
        Call<ResponseBody> responseCall = ergastAPIService.getLastRaceResults();

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
                    RaceAPIResponse raceAPIResponse = jsonParserUtils.parseRace(mrdata);
                    System.out.println("CALLBACK");
                    raceCallback.onSuccessFromRemote(raceAPIResponse, OperationType.FETCH_LAST_RACE);
                } else {
                    raceCallback.onFailureFromRemote(new Exception());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                raceCallback.onFailureFromRemote(new Exception(RETROFIT_ERROR));
            }
        });

    }
}
