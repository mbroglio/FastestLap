package com.the_coffe_coders.fastestlap.data;


import com.the_coffe_coders.fastestlap.domain.driver.Driver;

import com.the_coffe_coders.fastestlap.ui.ErgastAPI;


import org.threeten.bp.Year;

import java.util.List;


import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class RetrofitDriverRepository implements IDriverRepository{
    @Override
    public final List<Driver> findAll() {
        return null;
    }

    @Override
    public Driver find(String id) {

        //TODO REMOVE
        Year year = Year.now();
        String BASE_URL = "https://api.jolpi.ca/ergast/f1/" + year + "/";

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        ErgastAPI ergastApi = retrofit.create(ErgastAPI.class);

        return null;
    }
}


