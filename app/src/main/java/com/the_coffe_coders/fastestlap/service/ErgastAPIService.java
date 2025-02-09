package com.the_coffe_coders.fastestlap.service;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ErgastAPIService {
    @GET("constructorstandings")
    Call<ResponseBody> getConstructorStandings();

    @GET("driverstandings")
    Call<ResponseBody> getDriverStandings();

    @GET("races")
    Call<ResponseBody> getRaces();

    @GET("last")
    Call<ResponseBody> getLastRaceResults();

    @GET("{round}/results.json")
    Call<ResponseBody> getRaceResults(@Path("round") int round);

    @GET("results")
    Call<ResponseBody> getResults();

    @GET("next")
    Call<ResponseBody> getNextRace();

    @GET("circuits.json")
    Call<ResponseBody> getCircuits();

    @GET("drivers/{driverId}")
    Call<ResponseBody> getDriver(@Path("driverId") String driverId);

    @GET("constructors/{constructorId}")
    Call<ResponseBody> getConstructor(@Path("constructorId") String constructorId);
}