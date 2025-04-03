package com.the_coffe_coders.fastestlap.source.weeklyrace;

import static com.the_coffe_coders.fastestlap.util.Constants.RETROFIT_ERROR;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.api.RaceAPIResponse;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.dto.RaceDTO;
import com.the_coffe_coders.fastestlap.mapper.WeeklyRaceMapper;
import com.the_coffe_coders.fastestlap.repository.weeklyrace.SingleWeeklyRaceCallback;
import com.the_coffe_coders.fastestlap.repository.weeklyrace.WeeklyRacesCallback;
import com.the_coffe_coders.fastestlap.service.ErgastAPIService;
import com.the_coffe_coders.fastestlap.util.JSONParserUtils;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeeklyRaceRemoteDataSource extends BaseWeeklyRaceRemoteDataSource {

    private static final String TAG = "WeeklyRaceRemoteDataSource";
    private final ErgastAPIService ergastAPIService;

    public WeeklyRaceRemoteDataSource() {
        this.ergastAPIService = ServiceLocator.getInstance().getConcreteErgastAPIService();
    }

    @Override
    public void getWeeklyRaces(WeeklyRacesCallback weeklyRacesCallback) {
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
                    weeklyRacesCallback.onSuccess(unpackWeeklyRaceList(raceAPIsponse));
                } else {
                    weeklyRacesCallback.onFailure(new Exception());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                weeklyRacesCallback.onFailure(new Exception(RETROFIT_ERROR));
            }
        });
    }

    List<WeeklyRace> unpackWeeklyRaceList(RaceAPIResponse weeklyRaceAPIResponse) {
        List<RaceDTO> raceDTOS = weeklyRaceAPIResponse.getRaceTable().getRaces();
        List<WeeklyRace> weeklyRaceList = new ArrayList<>();
        for (RaceDTO raceDTO : raceDTOS) {
            weeklyRaceList.add(WeeklyRaceMapper.toWeeklyRace(raceDTO));
        }

        return weeklyRaceList;
    }

    @Override
    public void getNextRace(SingleWeeklyRaceCallback weeklyRaceCallback) {
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
                    Log.i(TAG, "FETCH NEXT RACE ==> " + raceAPIResponse.getRaceTable().getRace());
                    weeklyRaceCallback.onSuccess(WeeklyRaceMapper.toWeeklyRace(raceAPIResponse.getRaceTable().getRaces().get(0)));
                } else {
                    weeklyRaceCallback.onFailure(new Exception());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                weeklyRaceCallback.onFailure(new Exception(RETROFIT_ERROR));
            }
        });
    }

    @Override
    public void getLastRace(SingleWeeklyRaceCallback weeklyRaceCallback) {
        Call<ResponseBody> responseCall = ergastAPIService.getLastRace();

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
                    System.out.println("LAST RACE RESULTS" + mrdata);
                    weeklyRaceCallback.onSuccess(WeeklyRaceMapper.toWeeklyRace(raceAPIResponse.getRaceTable().getRaces().get(0)));
                } else {
                    weeklyRaceCallback.onFailure(new Exception());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                weeklyRaceCallback.onFailure(new Exception(RETROFIT_ERROR));
            }
        });
    }
}
