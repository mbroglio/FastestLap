package com.the_coffe_coders.fastestlap.repository.constructor;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.constructor.ConstructorAPIResponse;
import com.the_coffe_coders.fastestlap.mapper.ConstructorMapper;

import com.the_coffe_coders.fastestlap.util.JSONParserUtils;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class JolpicaConstructorRepository {
    private final String TAG = "JolpicaConstructorRepository";

    private final MutableLiveData<Result> constructorMutableLiveData;

    public JolpicaConstructorRepository() {
        constructorMutableLiveData = new MutableLiveData<>();
    }

    public MutableLiveData<Result> getConstructor(String constructorId) {
        Log.i(TAG, "getConstructor");
        Call<ResponseBody> responseCall = ServiceLocator.getInstance().getConcreteErgastAPIService().getConstructor(constructorId);
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
                    ConstructorAPIResponse constructorAPIResponse = jsonParserUtils.parseConstructor(mrdata);
                    Log.i("Constructor", constructorAPIResponse.toString());

                    constructorMutableLiveData.setValue(new Result.ConstructorSuccess(ConstructorMapper.toConstructor(constructorAPIResponse.getStandingsTable().getConstructorDTOList().get(0))));
                } else {
                    Log.i(TAG, "onFailure");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable throwable) {
                Log.i(TAG, "onFailure");
            }
        });
        return constructorMutableLiveData;
    }
}
