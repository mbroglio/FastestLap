package com.the_coffe_coders.fastestlap.data;

public class RetrofitRaceRepository {
    private static final String TAG = "RetrofitRaceRepository";
    private static RetrofitRaceRepository instance;


    private RetrofitRaceRepository() {

    }

    public static RetrofitRaceRepository getInstance() {
        if (instance == null) {
            instance = new RetrofitRaceRepository();
        }
        return instance;
    }
}
