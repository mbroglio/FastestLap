package com.the_coffe_coders.fastestlap.data;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.domain.race_result.Race;
import com.the_coffe_coders.fastestlap.domain.race_result.ResultsAPIResponse;
import com.the_coffe_coders.fastestlap.ui.HomePageActivity;
import com.the_coffe_coders.fastestlap.utils.JSONParserUtils;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitRaceRepository {
    private static final String TAG = "RetrofitRaceRepository";
    private static RetrofitRaceRepository instance;

    private ErgastAPI ergastAPI;

    private RetrofitRaceRepository() {

    }

    public CompletableFuture<Race> getLastRaceFromServer() {
        CompletableFuture<Race> lastRace = new CompletableFuture<>();
        String BASE_URL = "https://ergast.com/api/f1/";
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ergastAPI = retrofit.create(ErgastAPI.class);

        ergastAPI.getLastRaceResults().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                Log.i(TAG, "Response: " + response);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBody = response.body().string();
                        JsonObject jsonObject = new Gson().fromJson(responseBody, JsonObject.class);
                        JsonObject mrdata = jsonObject.getAsJsonObject("MRData");

                        JSONParserUtils parser = new JSONParserUtils();
                        ResultsAPIResponse raceResults = parser.parseRaceResults(mrdata);
                        lastRace.complete(raceResults.getRaceTable().getRaces().get(0));

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
            }
        });
        return lastRace;
    }




    public static RetrofitRaceRepository getInstance() {
        if (instance == null) {
            instance = new RetrofitRaceRepository();
        }
        return instance;
    }
}
