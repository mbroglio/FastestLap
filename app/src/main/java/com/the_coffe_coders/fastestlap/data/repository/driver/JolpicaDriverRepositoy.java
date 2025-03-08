package com.the_coffe_coders.fastestlap.data.repository.driver;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.data.api.DriversAPIResponse;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.data.mapper.DriverMapper;
import com.the_coffe_coders.fastestlap.core.util.JSONParserUtils;
import com.the_coffe_coders.fastestlap.core.util.ServiceLocator;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class JolpicaDriverRepositoy {
    private final String TAG = "JolpicaDriverRepository";

    private final MutableLiveData<Result> driverMutableLiveData;

    public JolpicaDriverRepositoy() {
        driverMutableLiveData = new MutableLiveData<>();
    }

    public MutableLiveData<Result> getDriver(String driverId) {
        Log.i(TAG, "getDriver");
        Call<ResponseBody> responseCall = ServiceLocator.getInstance().getConcreteErgastAPIService().getDriver(driverId);
        responseCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.i(TAG, "onResponse");
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
                    DriversAPIResponse driversAPIResponse = jsonParserUtils.parseDrivers(mrdata);
                    Log.i("Drivers", driversAPIResponse.toString());

                    try {
                        driverMutableLiveData.setValue(new Result.DriverSuccess(DriverMapper.toDriver(driversAPIResponse.getStandingsTable().getDriverDTOList().get(0))));
                    } catch (Exception e) {
                        driverMutableLiveData.setValue(new Result.Error(e.getMessage()));
                    }
                } else {
                    Log.i(TAG, "onFailure");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable throwable) {
                Log.i(TAG, "onFailure");
            }
        });
        return driverMutableLiveData;
    }
}
