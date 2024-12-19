package com.the_coffe_coders.fastestlap.data;


import com.the_coffe_coders.fastestlap.api.ErgastAPI;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;


import org.threeten.bp.Year;

import java.util.List;


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


