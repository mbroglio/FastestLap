package com.the_coffe_coders.fastestlap.source.standing.driver;

import static com.the_coffe_coders.fastestlap.util.Constants.RETROFIT_ERROR;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.api.DriverStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.mapper.DriverStandingsMapper;
import com.the_coffe_coders.fastestlap.repository.standing.driver.DriverStandingCallback;
import com.the_coffe_coders.fastestlap.service.ErgastAPIService;
import com.the_coffe_coders.fastestlap.util.JSONParserUtils;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class JolpicaDriverStandingsDataSource {
    private static final String TAG = "DriverRemoteDataSource";

    private final ErgastAPIService ergastAPIService;

    public JolpicaDriverStandingsDataSource() {
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
                    driverCallback.onError(new Exception("Empty response body"));
                    return;
                }

                try {
                    String responseString = body.string();
                    JsonObject jsonResponse = new Gson().fromJson(responseString, JsonObject.class);

                    if (jsonResponse == null) {
                        Log.e(TAG, "Failed to parse JSON response");
                        driverCallback.onError(new Exception("Invalid JSON response"));
                        return;
                    }

                    JsonObject mrdata = jsonResponse.getAsJsonObject("MRData");
                    if (mrdata == null) {
                        Log.e(TAG, "MRData not found in response");
                        driverCallback.onError(new Exception("MRData not found in response"));
                        return;
                    }

                    JSONParserUtils jsonParserUtils = new JSONParserUtils();
                    DriverStandingsAPIResponse driverStandingsAPIResponse = jsonParserUtils.parseDriverStandings(mrdata);

                    Log.d(TAG, "Successfully parsed driver standings: " + driverStandingsAPIResponse);
                    driverCallback.onDriverLoaded(DriverStandingsMapper.toDriverStandings(driverStandingsAPIResponse.getStandingsTable().getDriverStandingsDTOS().get(0)));
                } catch (IOException e) {
                    Log.e(TAG, "IOException while reading response", e);
                    driverCallback.onError(new Exception("Failed to read response: " + e.getMessage(), e));
                } catch (Exception e) {
                    Log.e(TAG, "Exception while processing response", e);
                    driverCallback.onError(new Exception("Failed to process response: " + e.getMessage(), e));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable throwable) {
                Log.e(TAG, "Failed to fetch driver standings", throwable);
                if (driverCallback != null) {
                    driverCallback.onError(new Exception(RETROFIT_ERROR, throwable));
                }
            }
        });
    }
}