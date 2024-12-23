package com.the_coffe_coders.fastestlap.repository.constructor;


import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.api.ConstructorStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.service.ErgastAPIService;
import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;

import com.the_coffe_coders.fastestlap.dto.ConstructorDTO;
import com.the_coffe_coders.fastestlap.dto.ConstructorStandingsElementDTO;
import com.the_coffe_coders.fastestlap.mapper.ConstructorMapper;
import com.the_coffe_coders.fastestlap.repository.JolpicaServer;
import com.the_coffe_coders.fastestlap.util.JSONParserUtils;


import org.threeten.bp.Year;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class ConstructorRepository implements IConstructorRepository, JolpicaServer, ConstructorResponseCallback {

    String TAG = "JolpicaConstructorRepository";

    private static ConstructorRepository instance;

    public static synchronized ConstructorRepository getInstance() {
        if (instance == null) {
            instance = new ConstructorRepository();
        }
        return instance;
    }

    @Override
    public final List<Constructor> findConstructors() {
     List<Constructor> constructors = new ArrayList<>();
        try {
            for (ConstructorDTO constructor: getConstructorsFromServer().get()) {
                constructors.add(ConstructorMapper.toConstructor(constructor));
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return constructors;
    }

    public CompletableFuture<List<ConstructorDTO>> getConstructorsFromServer() {
        CompletableFuture<List<ConstructorDTO>> future = new CompletableFuture<>();

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

                        List<ConstructorDTO> constructors = new ArrayList<>();
                        int total = Integer.parseInt(constructorStandingsAPIResponse.getTotal());
                        for (int i = 0; i < total; i++) {
                            ConstructorStandingsElementDTO standingElement = constructorStandingsAPIResponse
                                    .getStandingsTable()
                                    .getStandingsLists().get(0)
                                    .getConstructorStandings().get(i);
                            constructors.add(standingElement.getConstructor());
                        }

                        future.complete(constructors); // Risolve il CompletableFuture
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
    public Constructor find(String id) {

        //TODO REMOVE
        Year year = Year.now();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL + year + "/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        ErgastAPIService ergastApiService = retrofit.create(ErgastAPIService.class);

        return null;
    }

    @Override
    public void onSuccessFromRemote(ConstructorStandingsAPIResponse constructorStandingsAPIResponse, long lastUpdate) {

    }

    @Override
    public void onFailureFromRemote(Exception e) {

    }

    @Override
    public void onSuccessFromLocal(List<Constructor> constructors) {

    }

    @Override
    public void onFailureFromLocal(Exception e) {

    }
}