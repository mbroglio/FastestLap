package com.the_coffe_coders.fastestlap;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface OpenF1 {
    @GET("intervals")
    Call<String> getIntervals(
            @Query("session_key") String sessionKey,
            @Query("date<") String endDate,
            @Query("date>") String startDate
    );
}
