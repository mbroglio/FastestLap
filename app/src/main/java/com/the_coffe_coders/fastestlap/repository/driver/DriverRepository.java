package com.the_coffe_coders.fastestlap.repository.driver;


import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.api.DriverStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.driver.DriverAPIResponse;
import com.the_coffe_coders.fastestlap.service.ErgastAPIService;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.dto.DriverDTO;
import com.the_coffe_coders.fastestlap.dto.DriverStandingsElementDTO;
import com.the_coffe_coders.fastestlap.mapper.DriverMapper;
import com.the_coffe_coders.fastestlap.repository.JolpicaServer;
import com.the_coffe_coders.fastestlap.source.driver.BaseDriverLocalDataSource;
import com.the_coffe_coders.fastestlap.source.driver.BaseDriverRemoteDataSource;
import com.the_coffe_coders.fastestlap.source.driver.DriverRemoteDataSource;
import com.the_coffe_coders.fastestlap.util.JSONParserUtils;

import com.the_coffe_coders.fastestlap.domain.Result;


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

public class DriverRepository implements IDriverRepository, JolpicaServer, DriverResponseCallback {

    String TAG = "DriverRepository";

    public static long FRESH_TIMEOUT = 60000;

    private final MutableLiveData<Result> allDriverMutableLiveData;//shuld be final
    private final MutableLiveData<Result> driverMutableLiveData;//shuld be final
    private final BaseDriverRemoteDataSource driverRemoteDataSource;//shuld be final
    private final BaseDriverLocalDataSource driverLocalDataSource;//shuld be final

    private static DriverRepository instance;

    public DriverRepository(BaseDriverRemoteDataSource driverRemoteDataSource, BaseDriverLocalDataSource driverLocalDataSource) {
        this.allDriverMutableLiveData = new MutableLiveData<>();
        this.driverMutableLiveData = new MutableLiveData<>();
        this.driverRemoteDataSource = driverRemoteDataSource;
        this.driverLocalDataSource = driverLocalDataSource;
        this.driverRemoteDataSource.setDriverCallback(this);
        this.driverLocalDataSource.setDriverCallback(this);
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

    public Driver find(String id) {

        //TODO REMOVE
        Year year = Year.now();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL + year + "/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        ErgastAPIService ergastApiService = retrofit.create(ErgastAPIService.class);

        return null;
    }

    public LiveData<Driver> fetchDriver(String driverId) {
        Driver driver = null;
        for (Driver current: findDrivers()) {
            if(current.getDriverId().equals(driverId))
                driver = current;
        }
        return new LiveData<Driver>() {
        };
    }

    public MutableLiveData<Result> fetchDrivers(long lastUpdate) {
        long currentTime = System.currentTimeMillis();
        System.out.println(currentTime);
        System.out.println(lastUpdate);
        System.out.println("FETCH DRIVER METHOD");
        Log.i(TAG, "FETCH DRIVER METHOD");
        if(currentTime - lastUpdate > FRESH_TIMEOUT) { //currentTime - lastUpdate > FRESH_TIMEOUT
            driverRemoteDataSource.getDrivers();
        } else {
          driverLocalDataSource.getDrivers();
        }

        return allDriverMutableLiveData;
    }

    public MutableLiveData<Result> fetchDriver(String driverId, long lastUpdate) {
        long currentTime = System.currentTimeMillis();

        if(true) { //TODO change in currentTime - lastUpdate > FRESH_TIMEOUT
            driverRemoteDataSource.getDrivers();
        } else {
            driverLocalDataSource.getDrivers();
        }

        return null;
    }

    public CompletableFuture<List<DriverDTO>> getDriversFromServer() {
        CompletableFuture<List<DriverDTO>> future = new CompletableFuture<>();

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

    @Override
    public void onSuccessFromRemote(DriverStandingsAPIResponse driverAPIResponse, long lastUpdate) {
        System.out.println(driverAPIResponse.getStandingsTable());
        List<Driver> drivers = new ArrayList<>();

        for (DriverStandingsElementDTO driver: driverAPIResponse.getStandingsTable().getStandingsLists().get(0).getDriverStandings()) {
            drivers.add(DriverMapper.toDriver(driver.getDriver()));
            Log.i("onSuccessFromRemoteDriver", "DRIVER: " + driver.getDriver());
        }
        driverLocalDataSource.insertDrivers(drivers);
    }

    @Override
    public void onFailureFromRemote(Exception exception) {

    }

    @Override
    public void onSuccessFromLocal(List<Driver> driverList) {
        for (Driver driver: driverList) {
            System.out.println("SuccessFromLocal" + driver);
        }
        Result.DriverSuccess result = new Result.DriverSuccess(new DriverAPIResponse(driverList));
        allDriverMutableLiveData.postValue(result);
    }

    @Override
    public void onFailureFromLocal(Exception exception) {

    }
}


