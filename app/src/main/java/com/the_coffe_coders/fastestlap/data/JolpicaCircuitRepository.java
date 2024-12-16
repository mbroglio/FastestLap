package com.the_coffe_coders.fastestlap.data;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.api.DriverStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.api.ErgastAPI;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Circuit;

import com.the_coffe_coders.fastestlap.utils.JSONParserUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class JolpicaCircuitRepository implements ICircuitRepository, JolpicaServer {

    String TAG = "JolpicaCircuitRepository";

    @Override
    public List<Circuit> findAll() {
        return getCircuitsFromServer().join();
    }

    @Override
    public Circuit find(String circuitId) {
        return null;
    }

    public CompletableFuture<List<Circuit>> getCircuitsFromServer() {
        CompletableFuture<List<Circuit>> future = new CompletableFuture<>();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(CURRENT_YEAR_BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        ErgastAPI ergastApi = retrofit.create(ErgastAPI.class);

        ergastApi.getCircuits().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                System.out.println(response.toString());
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseString = response.body().string();
                        JsonObject jsonResponse = new Gson().fromJson(responseString, JsonObject.class);
                        JsonObject mrdata = jsonResponse.getAsJsonObject("MRData");
                        System.out.println(mrdata);
                        JSONParserUtils jsonParserUtils = new JSONParserUtils();
                        //CircuitAPIResponse circuitAPIResponse = jsonParserUtils.parseCircuit(mrdata);

                        List<Circuit> circuits = new ArrayList<>();

                        //circuits.addAll(circuitAPIResponse.getCircuitTable().getCircuits());
                        //System.out.println(circuitAPIResponse.getCircuitTable());
                       future.complete(circuits); // Risolve il CompletableFuture
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse JSON response", e);
                        future.completeExceptionally(e);
                    }
                } else {
                    Log.e(TAG, "Response not successful");
                    future.completeExceptionally(new Exception("Response not successful"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.e(TAG, "Network request failed", t);
                future.completeExceptionally(t);
            }
        });

        return future;
    }
}
