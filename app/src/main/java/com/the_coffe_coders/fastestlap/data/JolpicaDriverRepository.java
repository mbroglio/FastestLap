package com.the_coffe_coders.fastestlap.data;


import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.api.ErgastAPI;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;

import com.the_coffe_coders.fastestlap.api.DriverStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.dto.DriverDTO;
import com.the_coffe_coders.fastestlap.dto.DriverStandingsDTO;
import com.the_coffe_coders.fastestlap.utils.JSONParserUtils;


import org.threeten.bp.Year;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;


import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class JolpicaDriverRepository implements IDriverRepository, JolpicaServer {

    String TAG = "RetrofitDriverRepository";

    @Override
    public final List<Driver> findAll() {
        return null;
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

}


