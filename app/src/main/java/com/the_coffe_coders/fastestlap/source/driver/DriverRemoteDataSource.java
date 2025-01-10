package com.the_coffe_coders.fastestlap.source.driver;

import static com.the_coffe_coders.fastestlap.util.Constants.RETROFIT_ERROR;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.api.DriverStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.dto.DriverDTO;
import com.the_coffe_coders.fastestlap.dto.DriverStandingsElementDTO;
import com.the_coffe_coders.fastestlap.repository.driver.DriverResponseCallback;
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

public class DriverRemoteDataSource extends BaseDriverRemoteDataSource {
    private final ErgastAPIService ergastAPIService;

    public DriverRemoteDataSource(String apiKey) {
        this.ergastAPIService = ServiceLocator.getInstance().getConcreteErgastAPIService();
    }

    public void getDrivers() {

        Call<ResponseBody> responseCall = ergastAPIService.getDriverStandings();

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
                    DriverStandingsAPIResponse driverStandingsAPIResponse = jsonParserUtils.parseDriverStandings(mrdata);
                    System.out.println("CALLBACK");
                    driverCallback.onSuccessFromRemote(driverStandingsAPIResponse, System.currentTimeMillis());
                } else {
                    driverCallback.onFailureFromRemote(new Exception());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                driverCallback.onFailureFromRemote(new Exception(RETROFIT_ERROR));
            }
        });
    }

    @Override
    public void getDriversStandings() {
        Log.i("DriverRemoteDataSource", "getDriversStandings");
        Call<ResponseBody> responseCall = ergastAPIService.getDriverStandings();
        responseCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.i("DriverRemoteDataSource", "onResponse");
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
                    DriverStandingsAPIResponse driverStandingsAPIResponse = jsonParserUtils.parseDriverStandings(mrdata);
                    Log.i("DriverRemoteDataSource", driverStandingsAPIResponse.toString());
                    System.out.println("CALLBACK");
                    driverCallback.onSuccessFromRemote(driverStandingsAPIResponse, System.currentTimeMillis());
                } else {
                    Log.i("DriverRemoteDataSource", "onFailure");
                    driverCallback.onFailureFromRemote(new Exception());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable throwable) {

            }
        });

    }

    @Override
    public void getDriver(String driverId) {

    }
}