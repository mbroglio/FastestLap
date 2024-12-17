package com.the_coffe_coders.fastestlap.data;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.api.ConstructorStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.api.DriverStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.api.ErgastAPI;
import com.the_coffe_coders.fastestlap.dto.ConstructorDTO;
import com.the_coffe_coders.fastestlap.dto.ConstructorStandingsDTO;
import com.the_coffe_coders.fastestlap.dto.DriverDTO;
import com.the_coffe_coders.fastestlap.dto.DriverStandingsDTO;
import com.the_coffe_coders.fastestlap.utils.JSONParserUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class JolpicaConstructorStandingRepository {

    String TAG = "JolpicaDriverStandingRepository";

    public CompletableFuture<List<ConstructorDTO>> getConstructorsFromServer() {
        CompletableFuture<List<ConstructorDTO>> future = new CompletableFuture<>();
        String BASE_URL = "https://api.jolpi.ca/ergast/f1/" + 2024 + "/";
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        ErgastAPI ergastApi = retrofit.create(ErgastAPI.class);

        ergastApi.getConstructorStandings().enqueue(new Callback<>() {
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
                            ConstructorStandingsDTO standingElement = constructorStandingsAPIResponse
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




    public static void main(String[] args) {
        CompletableFuture<List<ConstructorDTO>> future = new JolpicaConstructorStandingRepository().getConstructorsFromServer();

        try {
            List<ConstructorDTO> constructors = future.get();
            for (ConstructorDTO constructor : constructors) {
                System.out.println(constructor.toString());
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}