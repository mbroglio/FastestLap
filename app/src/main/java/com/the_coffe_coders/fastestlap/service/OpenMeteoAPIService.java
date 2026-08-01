package com.the_coffe_coders.fastestlap.service;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Interfaccia Retrofit per l'API open source del meteo Open-Meteo.
 * Base URL: https://api.open-meteo.com/v1/
 */
public interface OpenMeteoAPIService {

    @GET("forecast")
    Call<ResponseBody> getWeatherForecast(
            @Query("latitude") double latitude,
            @Query("longitude") double longitude,
            @Query("current") String currentFields,
            @Query("hourly") String hourlyFields,
            @Query("daily") String dailyFields,
            @Query("models") String models,
            @Query("timezone") String timezone,
            @Query("start_date") String startDate,
            @Query("end_date") String endDate
    );
}
