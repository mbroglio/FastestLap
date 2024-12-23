package com.the_coffe_coders.fastestlap.repository.constructor;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.api.ConstructorStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.service.ErgastAPIService;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandings;
import com.the_coffe_coders.fastestlap.dto.ConstructorStandingsDTO;
import com.the_coffe_coders.fastestlap.mapper.ConstructorStandingsMapper;
import com.the_coffe_coders.fastestlap.repository.JolpicaServer;
import com.the_coffe_coders.fastestlap.util.JSONParserUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class ConstructorStandingsRepository implements JolpicaServer, IConstructorStandingsRepository, ConstructorStandingsResponseCallback {

    String TAG = "JolpicaDriverStandingRepository";

    private static ConstructorStandingsRepository instance;


    public static synchronized ConstructorStandingsRepository getInstance() {
        if (instance == null) {
            instance = new ConstructorStandingsRepository();
        }
        return instance;
    }

    public CompletableFuture<ConstructorStandingsDTO> getConstructorStandingsFromServer() {
        CompletableFuture<ConstructorStandingsDTO> future = new CompletableFuture<>();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(CURRENT_YEAR_BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        ErgastAPIService ergastApiService = retrofit.create(ErgastAPIService.class);

        ergastApiService.getConstructorStandings().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseString = response.body().string();
                        JsonObject jsonResponse = new Gson().fromJson(responseString, JsonObject.class);
                        JsonObject mrdata = jsonResponse.getAsJsonObject("MRData");

                        JSONParserUtils jsonParserUtils = new JSONParserUtils();
                        ConstructorStandingsAPIResponse constructorStandingsAPIResponse = jsonParserUtils.parseConstructorStandings(mrdata);

                        ConstructorStandingsDTO constructorStandingsDTO = constructorStandingsAPIResponse.getStandingsTable().getStandingsLists().get(0);

                        future.complete(constructorStandingsDTO); // Risolve il CompletableFuture
                    } catch (Exception e) {
                        //Log.e(TAG, "Failed to parse JSON response", e);
                        future.completeExceptionally(e);
                    }
                } else {
                    Log.e(TAG, "Response not successful");
                    future.completeExceptionally(new Exception("Response not successful"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.e(TAG, "Network request failed", t);
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    @Override
    public ConstructorStandings findConstructorStandings() {
        try {
            return ConstructorStandingsMapper.toConstructorStandings(getConstructorStandingsFromServer().get());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onSuccessFromRemote(ConstructorStandingsAPIResponse constructorStandingsAPIResponse, long lastUpdate) {

    }

    @Override
    public void onFailureFromRemote(Exception e) {

    }

    @Override
    public void onSuccessFromLocal(List<ConstructorStandings> constructorsStandings) {

    }

    @Override
    public void onFailureFromLocal(Exception e) {

    }
}