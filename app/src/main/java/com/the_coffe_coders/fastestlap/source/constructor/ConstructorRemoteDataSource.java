package com.the_coffe_coders.fastestlap.source.constructor;


import static android.content.ContentValues.TAG;

import static com.the_coffe_coders.fastestlap.util.Constants.RETROFIT_ERROR;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.api.ConstructorStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.api.DriverStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.dto.ConstructorDTO;
import com.the_coffe_coders.fastestlap.dto.ConstructorStandingsElementDTO;
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

public class ConstructorRemoteDataSource extends BaseConstructorRemoteDataSource {

    private final ErgastAPIService ergastAPIService;

    public ConstructorRemoteDataSource(String apiKey) {
        this.ergastAPIService = ServiceLocator.getInstance().getConstructorAPIService();
    }

    @Override
    public void getConstructor() {
        Log.i(TAG, "getConstructor: ");
        Call<ResponseBody> newsResponseCall = ergastAPIService.getConstructorStandings();
        newsResponseCall.enqueue(new Callback<>() {
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
                    ConstructorStandingsAPIResponse constructorStandingsAPIResponse = jsonParserUtils.parseConstructorStandings(mrdata);
                    System.out.println("CALLBACK");
                    constructorCallback.onSuccessFromRemote(constructorStandingsAPIResponse, System.currentTimeMillis());
                } else {
                    constructorCallback.onFailureFromRemote(new Exception());
                }


            }

            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                constructorCallback.onFailureFromRemote(new Exception(RETROFIT_ERROR));
            }
        });
    }

    @Override
    public void getConstructorStandings() {

    }

    @Override
    public void getConstructor(String constructorId) {

    }
}
