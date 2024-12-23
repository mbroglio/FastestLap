package com.the_coffe_coders.fastestlap.data;


import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.api.DriverStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.api.ErgastAPI;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.dto.DriverDTO;
import com.the_coffe_coders.fastestlap.dto.DriverStandingsElementDTO;
import com.the_coffe_coders.fastestlap.utils.JSONParserUtils;
import com.the_coffe_coders.fastestlap.mapper.DriverMapper;



import org.threeten.bp.Year;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class JolpicaDriverRepository implements IDriverRepository, JolpicaServer {

    String TAG = "RetrofitDriverRepository";

    private static JolpicaDriverRepository instance;

    public static synchronized JolpicaDriverRepository getInstance() {
        if (instance == null) {
            instance = new JolpicaDriverRepository();
        }
        return instance;
    }

    @Override
    public final List<Driver> findDrivers() {
        List<Driver> drivers = new ArrayList<>();

        try {
            for (DriverDTO driver: getDriversFromServer().get()) {
                drivers.add(DriverMapper.toDriver(driver));
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        return drivers;
    }

    @Override
    public Driver find(String id) {

        //TODO REMOVE
        Year year = Year.now();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL + year + "/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        ErgastAPI ergastApi = retrofit.create(ErgastAPI.class);

        return null;
    }

    public CompletableFuture<List<DriverDTO>> getDriversFromServer() {
        CompletableFuture<List<DriverDTO>> future = new CompletableFuture<>();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(CURRENT_YEAR_BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        ErgastAPI ergastApi = retrofit.create(ErgastAPI.class);

        ergastApi.getDriverStandings().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseString = response.body().string();
                        JsonObject jsonResponse = new Gson().fromJson(responseString, JsonObject.class);
                        JsonObject mrdata = jsonResponse.getAsJsonObject("MRData");

                        JSONParserUtils jsonParserUtils = new JSONParserUtils();
                        DriverStandingsAPIResponse driverStandingsAPIResponse = jsonParserUtils.parseDriverStandings(mrdata);

                        List<DriverDTO> drivers = new ArrayList<>();
                        int total = Integer.parseInt(driverStandingsAPIResponse.getTotal());
                        for (int i = 0; i < total; i++) {
                            DriverStandingsElementDTO standingElement = driverStandingsAPIResponse
                                    .getStandingsTable()
                                    .getStandingsLists().get(0)
                                    .getDriverStandings().get(i);
                            drivers.add(standingElement.getDriver());
                        }

                        future.complete(drivers); // Risolve il CompletableFuture
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

}


