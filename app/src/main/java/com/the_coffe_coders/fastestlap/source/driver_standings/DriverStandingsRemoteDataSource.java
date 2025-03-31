package com.the_coffe_coders.fastestlap.source.driver_standings;

import static com.the_coffe_coders.fastestlap.util.Constants.RETROFIT_ERROR;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.api.DriverStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandings;
import com.the_coffe_coders.fastestlap.mapper.DriverStandingsMapper;
import com.the_coffe_coders.fastestlap.repository.standings.driver.DriverStandingCallback;
import com.the_coffe_coders.fastestlap.service.ErgastAPIService;
import com.the_coffe_coders.fastestlap.util.JSONParserUtils;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverStandingsRemoteDataSource extends BaseDriverStandingsRemoteDataSource {
    private static final String TAG = "DriverRemoteDataSource";

    private final ErgastAPIService ergastAPIService;

    public DriverStandingsRemoteDataSource() {
        this.ergastAPIService = ServiceLocator.getInstance().getConcreteErgastAPIService();
    }

    @Override
    public void getDriversStandings(DriverStandingCallback driverCallback) {
        Log.d(TAG, "Fetching driver standings from remote API");
        Call<ResponseBody> responseCall = ergastAPIService.getDriverStandings();

        responseCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                ResponseBody body = response.body();
                if (body == null) {
                    Log.e(TAG, "API returned empty body");
                    driverCallback.onFailure(new Exception("Empty response body"));
                    return;
                }

                try {
                    String responseString = body.string();
                    JsonObject jsonResponse = new Gson().fromJson(responseString, JsonObject.class);

                    if (jsonResponse == null) {
                        Log.e(TAG, "Failed to parse JSON response");
                        driverCallback.onFailure(new Exception("Invalid JSON response"));
                        return;
                    }

                    JsonObject mrdata = jsonResponse.getAsJsonObject("MRData");
                    if (mrdata == null) {
                        Log.e(TAG, "MRData not found in response");
                        driverCallback.onFailure(new Exception("MRData not found in response"));
                        return;
                    }

                    JSONParserUtils jsonParserUtils = new JSONParserUtils();
                    DriverStandingsAPIResponse driverStandingsAPIResponse = jsonParserUtils.parseDriverStandings(mrdata);

                    Log.d(TAG, "Successfully parsed driver standings: " + driverStandingsAPIResponse);
                    driverCallback.onSuccess(DriverStandingsMapper.toDriverStandings(driverStandingsAPIResponse.getStandingsTable().getDriverStandingsDTOS().get(0)));
                } catch (IOException e) {
                    Log.e(TAG, "IOException while reading response", e);
                    driverCallback.onFailure(new Exception("Failed to read response: " + e.getMessage(), e));
                } catch (Exception e) {
                    Log.e(TAG, "Exception while processing response", e);
                    driverCallback.onFailure(new Exception("Failed to process response: " + e.getMessage(), e));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable throwable) {
                Log.e(TAG, "Failed to fetch driver standings", throwable);
                if (driverCallback != null) {
                    driverCallback.onFailure(new Exception(RETROFIT_ERROR, throwable));
                }
            }
        });
    }
}