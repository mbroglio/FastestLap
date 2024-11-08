package com.the_coffe_coders.fastestlap;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;

public interface ErgastAPI {
    @GET("constructorstandings")
    Call<ResponseBody> getConstructorStandings();
}