package com.the_coffe_coders.fastestlap.source.f1.standing.constructor;

import static com.the_coffe_coders.fastestlap.util.Constants.RETROFIT_ERROR;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.api.ConstructorAPIResponse;
import com.the_coffe_coders.fastestlap.api.ConstructorStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.repository.f1.standing.constructor.ConstructorStandingCallback;
import com.the_coffe_coders.fastestlap.repository.mapper.ConstructorStandingsMapper;
import com.the_coffe_coders.fastestlap.service.ErgastAPIService;
import com.the_coffe_coders.fastestlap.util.JSONParserUtils;
import com.the_coffe_coders.fastestlap.util.service.ServiceLocator;

import java.io.IOException;
import java.util.Calendar;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class JolpicaConstructorStandingsDataSource implements ConstructorStandingDataSource {
    private static final String TAG = "JolpicaConstructorStandingsDataSource";
    private static JolpicaConstructorStandingsDataSource instance;
    private final ErgastAPIService ergastAPIService;

    private final String currentYear = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));

    private JolpicaConstructorStandingsDataSource() {
        this.ergastAPIService = ServiceLocator.getInstance().getConcreteErgastAPIService();
    }

    public static synchronized JolpicaConstructorStandingsDataSource getInstance() {
        if (instance == null) {
            instance = new JolpicaConstructorStandingsDataSource();
        }
        return instance;
    }

    @Override
    public void getConstructorStandings(ConstructorStandingCallback constructorCallback) {
        Log.d(TAG, "Fetching constructor standings");
        Call<ResponseBody> newsResponseCall = ergastAPIService.getConstructorStandings();
        newsResponseCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseString;

                    try {
                        responseString = response.body().string();
                        JsonObject jsonResponse = new Gson().fromJson(responseString, JsonObject.class);

                        if (jsonResponse == null) {
                            Log.e(TAG, "Failed to parse JSON response");
                            constructorCallback.onError(new Exception("Invalid JSON response"));
                            return;
                        }

                        JsonObject mrdata = jsonResponse.getAsJsonObject("MRData");
                        if (mrdata == null) {
                            Log.e(TAG, "MRData not found in response");
                            constructorCallback.onError(new Exception("MRData not found in response"));
                            return;
                        }

                        JSONParserUtils jsonParserUtils = new JSONParserUtils();
                        ConstructorStandingsAPIResponse constructorStandingsAPIResponse = jsonParserUtils.parseConstructorStandings(mrdata);

                        Log.d(TAG, "Successfully parsed constructor standings: " + constructorStandingsAPIResponse);
                        if (constructorStandingsAPIResponse.getStandingsTable().getConstructorStandingsDTOS() == null ||
                                constructorStandingsAPIResponse.getStandingsTable().getConstructorStandingsDTOS().isEmpty() ||
                                constructorStandingsAPIResponse.getStandingsTable().getConstructorStandingsDTOS().get(0) == null) {
                            constructorCallback.onError(new Exception("No constructor standings found"));
                            if (constructorStandingsAPIResponse.getStandingsTable().getSeason().equals(currentYear)) {
                                Log.i(TAG, "Fetching constructor list");
                                getConstructorList(constructorCallback);
                            }
                        } else {
                            constructorCallback.onConstructorLoaded(
                                    ConstructorStandingsMapper.toConstructorStandings(
                                            constructorStandingsAPIResponse.getStandingsTable().getConstructorStandingsDTOS().get(0)));

                        }
                    } catch (IOException e) {
                        Log.e(TAG, "IOException while reading response", e);
                        constructorCallback.onError(new Exception("Error reading response: " + e.getMessage()));
                    } catch (Exception e) {
                        Log.e(TAG, "Exception while processing response", e);
                        constructorCallback.onError(new Exception("Error processing response: " + e.getMessage()));
                    }
                } else {
                    constructorCallback.onError(new Exception("Response unsuccessful"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                constructorCallback.onError(new Exception(RETROFIT_ERROR));
            }
        });
    }

    private void getConstructorList(ConstructorStandingCallback constructorCallback) {
        Log.d(TAG, "Fetching constructor list from remote API");
        Call<ResponseBody> responseCall = ergastAPIService.getConstructors();

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
                            constructorCallback.onError(new Exception("Invalid JSON response"));
                            return;
                        }

                        JsonObject mrdata = jsonResponse.getAsJsonObject("MRData");
                        if (mrdata == null) {
                            Log.e(TAG, "MRData not found in response");
                            constructorCallback.onError(new Exception("MRData not found in response"));
                            return;
                        }

                        JSONParserUtils jsonParserUtils = new JSONParserUtils();
                        ConstructorAPIResponse constructorAPIResponse = jsonParserUtils.parseConstructor(mrdata);

                        Log.d(TAG, "Successfully parsed constructor list: " + constructorAPIResponse);
                        if (constructorAPIResponse.getConstructorTable().getConstructorDTOList().isEmpty()) {
                            constructorCallback.onError(new Exception("No constructor standings found"));
                            return;
                        }
                        constructorCallback.onConstructorListLoaded(
                                ConstructorStandingsMapper.toConstructorList(
                                        constructorAPIResponse.getConstructorTable()));
                    } catch (IOException e) {
                        Log.e(TAG, "IOException while reading response", e);
                        constructorCallback.onError(new Exception("Error reading response: " + e.getMessage()));
                    } catch (Exception e) {
                        Log.e(TAG, "Exception while processing response", e);
                        constructorCallback.onError(new Exception("Error processing response: " + e.getMessage()));
                    }

                } else {
                    constructorCallback.onError(new Exception("Response unsuccessful"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.e(TAG, "failed to fetch constructor list", t);
                if (constructorCallback != null) {
                    constructorCallback.onError(new Exception(RETROFIT_ERROR));
                }
            }
        });


    }
}