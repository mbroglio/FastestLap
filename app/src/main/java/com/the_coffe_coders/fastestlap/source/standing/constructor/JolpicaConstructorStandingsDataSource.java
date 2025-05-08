package com.the_coffe_coders.fastestlap.source.standing.constructor;

import static com.the_coffe_coders.fastestlap.util.Constants.RETROFIT_ERROR;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.api.ConstructorStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.mapper.ConstructorStandingsMapper;
import com.the_coffe_coders.fastestlap.repository.standing.constructor.ConstructorStandingCallback;
import com.the_coffe_coders.fastestlap.service.ErgastAPIService;
import com.the_coffe_coders.fastestlap.util.JSONParserUtils;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class JolpicaConstructorStandingsDataSource implements ConstructorStandingDataSource {
    private static JolpicaConstructorStandingsDataSource instance;
    private static final String TAG = "JolpicaConstructorStandingsDataSource";
    private final ErgastAPIService ergastAPIService;

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
                    } catch (IOException e) {
                        constructorCallback.onError(new Exception("Error reading response: " + e.getMessage()));
                        return;
                    }
                    JsonObject jsonResponse = new Gson().fromJson(responseString, JsonObject.class);
                    JsonObject mrdata = jsonResponse.getAsJsonObject("MRData");

                    JSONParserUtils jsonParserUtils = new JSONParserUtils();
                    ConstructorStandingsAPIResponse constructorStandingsAPIResponse = jsonParserUtils.parseConstructorStandings(mrdata);
                    constructorCallback.onConstructorLoaded(
                            ConstructorStandingsMapper.toConstructorStandings(
                                    constructorStandingsAPIResponse.getStandingsTable().getStandingsLists().get(0)
                            )
                    );
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
}