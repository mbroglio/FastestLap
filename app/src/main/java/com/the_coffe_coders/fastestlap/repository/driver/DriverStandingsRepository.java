package com.the_coffe_coders.fastestlap.repository.driver;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.api.DriverStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.service.ErgastAPIService;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandings;
import com.the_coffe_coders.fastestlap.dto.DriverStandingsDTO;
import com.the_coffe_coders.fastestlap.mapper.DriverStandingsMapper;
import com.the_coffe_coders.fastestlap.repository.JolpicaServer;
import com.the_coffe_coders.fastestlap.utils.JSONParserUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class DriverStandingsRepository implements JolpicaServer {

    String TAG = "JolpicaDriverStandingRepository";

    private static DriverStandingsRepository instance;

    public static synchronized DriverStandingsRepository getInstance() {
        if (instance == null) {
            instance = new DriverStandingsRepository();
        }
        return instance;
    }

    public CompletableFuture<DriverStandingsDTO> getDriverStandingsFromServer() {
        CompletableFuture<DriverStandingsDTO> future = new CompletableFuture<>();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(CURRENT_YEAR_BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        ErgastAPIService ergastApiService = retrofit.create(ErgastAPIService.class);

        ergastApiService.getDriverStandings().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseString = response.body().string();
                        JsonObject jsonResponse = new Gson().fromJson(responseString, JsonObject.class);
                        JsonObject mrdata = jsonResponse.getAsJsonObject("MRData");

                        JSONParserUtils jsonParserUtils = new JSONParserUtils();
                        DriverStandingsAPIResponse driverStandingsAPIResponse = jsonParserUtils.parseDriverStandings(mrdata);
                        DriverStandingsDTO driverStandingsDTO = driverStandingsAPIResponse.getStandingsTable().getStandingsLists().get(0);

                        future.complete(driverStandingsDTO); // Risolve il CompletableFuture
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


    public DriverStandingsDTO find() {
        return null;
    }

    public DriverStandings findDriverStandings() {

        try {
            return DriverStandingsMapper.toDriverStandings(getDriverStandingsFromServer().get());
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}


