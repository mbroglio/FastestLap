package com.the_coffe_coders.fastestlap.data.source.constructor;


import static android.content.ContentValues.TAG;
import static com.the_coffe_coders.fastestlap.core.util.Constants.RETROFIT_ERROR;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.data.api.ConstructorStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.data.service.ErgastAPIService;
import com.the_coffe_coders.fastestlap.core.util.JSONParserUtils;
import com.the_coffe_coders.fastestlap.core.util.ServiceLocator;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConstructorStandingsRemoteDataSource extends BaseConstructorStandingsRemoteDataSource {

    private final ErgastAPIService ergastAPIService;

    public ConstructorStandingsRemoteDataSource(String apiKey) {
        this.ergastAPIService = ServiceLocator.getInstance().getConcreteErgastAPIService();
    }

    @Override
    public void getConstructorStandings() {
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
}
