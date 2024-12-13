<<<<<<<< HEAD:app/src/main/java/com/the_coffe_coders/fastestlap/data/ErgastAPI.java
package com.the_coffe_coders.fastestlap.data;
========
package com.the_coffe_coders.fastestlap.api;
>>>>>>>> origin/DTO_definition:app/src/main/java/com/the_coffe_coders/fastestlap/api/ErgastAPI.java

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;

public interface ErgastAPI {
    @GET("constructorstandings")
    Call<ResponseBody> getConstructorStandings();

    @GET("driverstandings")
    Call<ResponseBody> getDriverStandings();

    @GET("races")
    Call<ResponseBody> getRaces();

    @GET("current/last/results.json")
    Call<ResponseBody> getLastRaceResults();

    @GET("results.json")
    Call<ResponseBody> getResults();

    @GET("current/next")
    Call<ResponseBody> getNextRace();

    @GET("circuits.json")
    Call<ResponseBody> getCircuits();
}