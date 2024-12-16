package com.the_coffe_coders.fastestlap.data;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.api.ErgastAPI;
import com.the_coffe_coders.fastestlap.api.ResultsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;
import com.the_coffe_coders.fastestlap.api.RaceAPIResponse;
import com.the_coffe_coders.fastestlap.dto.RaceDTO;
import com.the_coffe_coders.fastestlap.utils.JSONParserUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class JolpicaRaceRepository implements JolpicaServer{
    private static final String TAG = "RetrofitRaceRepository";
    private static JolpicaRaceRepository instance;

    private ErgastAPI ergastAPI;

    public static synchronized JolpicaRaceRepository getInstance() {
        if (instance == null) {
            instance = new JolpicaRaceRepository();
        }
        return instance;
    }

    private JolpicaRaceRepository() {

    }

    public CompletableFuture<Race> getRacesFromServer() {
        CompletableFuture<RaceDTO> races = new CompletableFuture<>();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(CURRENT_YEAR_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ergastAPI = retrofit.create(ErgastAPI.class);
        ergastAPI.getRaces().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                System.out.println("Response: " + response);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBody = response.body().string();
                        JsonObject jsonObject = new Gson().fromJson(responseBody, JsonObject.class);
                        JsonObject mrdata = jsonObject.getAsJsonObject("MRData");
                        System.out.println(mrdata);
                        JSONParserUtils parser = new JSONParserUtils();
                        ResultsAPIResponse raceResults = parser.parseRaceResults(mrdata);

                        //System.out.println(raceResults.toString());
                        races.complete(raceResults.getRaceTable().getRaces().get(0));
                        List<RaceDTO> raceDTOList = raceResults.getRaceTable().getRaces();

                        for (RaceDTO race: raceDTOList){
                            System.out.println(race);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
            }
        });

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    public CompletableFuture<Race> getNextRaceFromServer() {
        CompletableFuture<Race> nextRace = new CompletableFuture<>();
        String BASE_URL = "https://ergast.com/api/f1/";
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ergastAPI = retrofit.create(ErgastAPI.class);

        ergastAPI.getNextRace().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                Log.i(TAG, "Response: " + response);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBody = response.body().string();
                        JsonObject jsonObject = new Gson().fromJson(responseBody, JsonObject.class);
                        JsonObject mrdata = jsonObject.getAsJsonObject("MRData");

                        JSONParserUtils parser = new JSONParserUtils();
                        RaceAPIResponse raceSchedule = parser.parseRace(mrdata);

                        //nextRace.complete(raceResults.getRaceTable().getRaces().get(0));

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
            }
        });
        return nextRace;
    }
}
