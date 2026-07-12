package com.the_coffe_coders.fastestlap.source.f1.standing.driver;

import static com.the_coffe_coders.fastestlap.util.Constants.RETROFIT_ERROR;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.api.DriverStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.api.DriversAPIResponse;
import com.the_coffe_coders.fastestlap.repository.mapper.DriverStandingsMapper;
import com.the_coffe_coders.fastestlap.repository.f1.standing.driver.DriverStandingCallback;
import com.the_coffe_coders.fastestlap.service.ErgastAPIService;
import com.the_coffe_coders.fastestlap.util.JSONParserUtils;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;

import java.io.IOException;
import java.util.Calendar;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class JolpicaDriverStandingsDataSource implements DriverStandingDataSource {
    private static final String TAG = "DriverRemoteDataSource";
    private static JolpicaDriverStandingsDataSource instance;
    private final ErgastAPIService ergastAPIService;
    private final String currentYear = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));

    public JolpicaDriverStandingsDataSource() {
        this.ergastAPIService = ServiceLocator.getInstance().getConcreteErgastAPIService();
    }

    public static synchronized JolpicaDriverStandingsDataSource getInstance() {
        if (instance == null) {
            instance = new JolpicaDriverStandingsDataSource();
        }
        return instance;
    }

    @Override
    public void getDriverStandings(DriverStandingCallback driverCallback) {
        Log.d(TAG, "Fetching driver standings from remote API");
        Call<ResponseBody> responseCall = ergastAPIService.getDriverStandings();

        responseCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseString;

                    try {
                        responseString = response.body().string();
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
                        if (driverStandingsAPIResponse.getStandingsTable().getDriverStandingsDTOS() == null ||
                                driverStandingsAPIResponse.getStandingsTable().getDriverStandingsDTOS().isEmpty() ||
                                driverStandingsAPIResponse.getStandingsTable().getDriverStandingsDTOS().get(0) == null) {
                            driverCallback.onError(new Exception("No driver standings found"));
                            if (driverStandingsAPIResponse.getStandingsTable().getSeason().equals(currentYear)) {
                                Log.i(TAG, "Fetching drivers list");
                                getDriversList(driverCallback);
                            }
                        } else {
                            driverCallback.onDriverStandingsLoaded(
                                    DriverStandingsMapper.toDriverStandings(
                                            driverStandingsAPIResponse.getStandingsTable().getDriverStandingsDTOS().get(0)));
                        }

                    } catch (IOException e) {
                        Log.e(TAG, "IOException while reading response", e);
                        driverCallback.onError(new Exception("Failed to read response: " + e.getMessage(), e));
                    } catch (Exception e) {
                        Log.e(TAG, "Exception while processing response", e);
                        driverCallback.onError(new Exception("Failed to process response: " + e.getMessage(), e));
                    }
                } else {
                    driverCallback.onError(new Exception("Response unsuccessful"));
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

    @Override
    public void getDriversList(DriverStandingCallback driverCallback) {
        Log.d(TAG, "Fetching driver list from remote API");
        Call<ResponseBody> responseCall = ergastAPIService.getDrivers();

        responseCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseString;

                    try {
                        responseString = response.body().string();
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
                        DriversAPIResponse driversAPIResponse = jsonParserUtils.parseDrivers(mrdata);

                        Log.d(TAG, "Successfully parsed driver list: " + driversAPIResponse);
                        if (driversAPIResponse.getDriversTable().getDriverDTOList().isEmpty()) {
                            driverCallback.onError(new Exception("No driver standings found"));
                            return;
                        }
                        driverCallback.onDriverListLoaded(
                                DriverStandingsMapper.toDriverList(
                                        driversAPIResponse.getDriversTable()));
                    } catch (IOException e) {
                        Log.e(TAG, "IOException while reading response", e);
                        driverCallback.onError(new Exception("Failed to read response: " + e.getMessage(), e));
                    } catch (Exception e) {
                        Log.e(TAG, "Exception while processing response", e);
                        driverCallback.onError(new Exception("Failed to process response: " + e.getMessage(), e));
                    }
                } else {
                    driverCallback.onError(new Exception("Response unsuccessful"));
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