package com.the_coffe_coders.fastestlap.source.driver_standings;

import static com.the_coffe_coders.fastestlap.util.Constants.RETROFIT_ERROR;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.api.DriverStandingsAPIResponse;
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
    public void getDriversStandings() {
        Log.d(TAG, "Fetching driver standings from remote API");
        Call<ResponseBody> responseCall = ergastAPIService.getDriverStandings();

        responseCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                processDriversResponse(response);
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable throwable) {
                Log.e(TAG, "Failed to fetch driver standings", throwable);
                if (driverCallback != null) {
                    driverCallback.onFailureFromRemote(new Exception(RETROFIT_ERROR, throwable));
                }
            }
        });
    }

    private void processDriversResponse(Response<ResponseBody> response) {
        if (driverCallback == null) {
            Log.e(TAG, "Driver callback is null, cannot process response");
            return;
        }

        if (!response.isSuccessful()) {
            Log.e(TAG, "API call failed with code: " + response.code());
            driverCallback.onFailureFromRemote(
                    new Exception("API call failed with code: " + response.code())
            );
            return;
        }

        ResponseBody body = response.body();
        if (body == null) {
            Log.e(TAG, "API returned empty body");
            driverCallback.onFailureFromRemote(new Exception("Empty response body"));
            return;
        }

        try {
            String responseString = body.string();
            JsonObject jsonResponse = new Gson().fromJson(responseString, JsonObject.class);

            if (jsonResponse == null) {
                Log.e(TAG, "Failed to parse JSON response");
                driverCallback.onFailureFromRemote(new Exception("Invalid JSON response"));
                return;
            }

            JsonObject mrdata = jsonResponse.getAsJsonObject("MRData");
            if (mrdata == null) {
                Log.e(TAG, "MRData not found in response");
                driverCallback.onFailureFromRemote(new Exception("MRData not found in response"));
                return;
            }

            JSONParserUtils jsonParserUtils = new JSONParserUtils();
            DriverStandingsAPIResponse driverStandingsAPIResponse = jsonParserUtils.parseDriverStandings(mrdata);

            Log.d(TAG, "Successfully parsed driver standings: " + driverStandingsAPIResponse);
            driverCallback.onSuccessFromRemote(driverStandingsAPIResponse, System.currentTimeMillis());
        } catch (IOException e) {
            Log.e(TAG, "IOException while reading response", e);
            driverCallback.onFailureFromRemote(new Exception("Failed to read response: " + e.getMessage(), e));
        } catch (Exception e) {
            Log.e(TAG, "Exception while processing response", e);
            driverCallback.onFailureFromRemote(new Exception("Failed to process response: " + e.getMessage(), e));
        }
    }
}