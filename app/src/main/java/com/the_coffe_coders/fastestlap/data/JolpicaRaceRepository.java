package com.the_coffe_coders.fastestlap.data;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.api.ErgastAPI;
import com.the_coffe_coders.fastestlap.api.ResultsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;
import com.the_coffe_coders.fastestlap.api.RaceAPIResponse;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.dto.RaceDTO;
import com.the_coffe_coders.fastestlap.mapper.WeeklyRaceMapper;
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

    public List<WeeklyRace> getRacesFromServer() throws ExecutionException, InterruptedException {
        CompletableFuture<List<WeeklyRace>> weeklyRaces = new CompletableFuture<>();

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

                        List<RaceDTO> raceDTOList = raceResults.getRaceTable().getRaces();
                        List<WeeklyRace> weeklyRaceList = new ArrayList<>();
                        //System.out.println(raceResults.toString());
                        for (RaceDTO race: raceDTOList) {
                            weeklyRaceList.add(WeeklyRaceMapper.toWeeklyRace(race));
                        }
                        weeklyRaces.complete(weeklyRaceList);

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

        weeklyRaces.join();

        return weeklyRaces.get();
    }

    public WeeklyRace getNextRaceFromServer() throws ExecutionException, InterruptedException {
        CompletableFuture<RaceDTO> nextRace = new CompletableFuture<>();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(CURRENT_YEAR_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ergastAPI = retrofit.create(ErgastAPI.class);

        ergastAPI.getNextRace().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                //Log.i(TAG, "Response: " + response);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBody = response.body().string();
                        JsonObject jsonObject = new Gson().fromJson(responseBody, JsonObject.class);
                        JsonObject mrdata = jsonObject.getAsJsonObject("MRData");

                        JSONParserUtils parser = new JSONParserUtils();
                        RaceAPIResponse raceSchedule = parser.parseRace(mrdata);

                        List<RaceDTO> raceDTOList = raceSchedule.getRaceTable().getRaces();
                        for (RaceDTO race: raceDTOList){
                            System.out.println(race);
                        }

                        //System.out.println(mrdata);
                        if(!raceDTOList.isEmpty()) {
                            nextRace.complete(raceDTOList.get(0));
                        }else {
                            nextRace.complete(null);
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
        nextRace.join();
        return WeeklyRaceMapper.toWeeklyRace(nextRace.get());
    }

    public WeeklyRace getLastRaceResultFromServer() {
        CompletableFuture<WeeklyRace> lastWeeklyRace = new CompletableFuture<>();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(CURRENT_YEAR_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ergastAPI = retrofit.create(ErgastAPI.class);

        ergastAPI.getLastRaceResults().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                //Log.i(TAG, "Response: " + response);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBody = response.body().string();
                        JsonObject jsonObject = new Gson().fromJson(responseBody, JsonObject.class);
                        JsonObject mrdata = jsonObject.getAsJsonObject("MRData");

                        JSONParserUtils parser = new JSONParserUtils();
                        RaceAPIResponse raceSchedule = parser.parseRace(mrdata);
                        RaceDTO raceDTO = raceSchedule.getRaceTable().getRaces().get(0);
                        lastWeeklyRace.complete(WeeklyRaceMapper.toWeeklyRace(raceDTO));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
            }
        });
        WeeklyRace race;
        try {
            race = lastWeeklyRace.get();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return race;
    }
}
