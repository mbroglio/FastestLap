package com.the_coffe_coders.fastestlap.source.result;

import static com.the_coffe_coders.fastestlap.util.Constants.RETROFIT_ERROR;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.api.RaceResultsAPIResponse;
import com.the_coffe_coders.fastestlap.service.ErgastAPIService;
import com.the_coffe_coders.fastestlap.util.JSONParserUtils;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RaceResultRemoteDataSource extends BaseRaceResultRemoteDataSource {

    private static final String TAG = "RaceResultRemoteDataSource";
    private final ErgastAPIService ergastAPIService;

    public RaceResultRemoteDataSource(String apiKey) {
        this.ergastAPIService = ServiceLocator.getInstance().getConcreteErgastAPIService();
    }

    @Override
    public void getAllRaceResult() {

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

                    raceResultCallback.onSuccessFromRemote(raceResultsAPIResponse);
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
        Call<ResponseBody> responseCall = ergastAPIService.getResults();
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
                    Log.i(TAG, mrdata.getAsString());
                    //RaceResultsAPIResponse raceResultsAPIResponse = jsonParserUtils.parseRaceResults(mrdata);

                    //raceResultCallback.onSuccessFromRemote(raceResultsAPIResponse);
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
}
