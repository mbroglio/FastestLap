package com.the_coffe_coders.fastestlap.repository.constructor;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.api.ConstructorAPIResponse;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.mapper.ConstructorMapper;
import com.the_coffe_coders.fastestlap.util.JSONParserUtils;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class JolpicaConstructorRepository {
    private static final String TAG = "JolpicaConstructorRepository";

    // Cache for ongoing requests to avoid duplicate requests
    private final ConcurrentHashMap<String, CompletableFuture<Result>> pendingRequests = new ConcurrentHashMap<>();

    public JolpicaConstructorRepository() {
        // Constructor
    }

    public CompletableFuture<Result> getConstructor(String constructorId) {
        final String requestKey = "jolpica_" + constructorId;

        // Check if there's already an ongoing request
        CompletableFuture<Result> existingRequest = pendingRequests.get(requestKey);
        if (existingRequest != null && !existingRequest.isDone()) {
            Log.d(TAG, "Returning existing Jolpica request for constructor: " + constructorId);
            return existingRequest;
        }

        // Create a new CompletableFuture for this request
        CompletableFuture<Result> future = new CompletableFuture<>();
        pendingRequests.put(requestKey, future);

        Log.i(TAG, "Fetching constructor data from Jolpica: " + constructorId);
        Call<ResponseBody> responseCall = ServiceLocator.getInstance().getConcreteErgastAPIService().getConstructor(constructorId);

        responseCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseString = response.body().string();
                        JsonObject jsonResponse = new Gson().fromJson(responseString, JsonObject.class);
                        JsonObject mrdata = jsonResponse.getAsJsonObject("MRData");

                        JSONParserUtils jsonParserUtils = new JSONParserUtils();
                        ConstructorAPIResponse constructorAPIResponse = jsonParserUtils.parseConstructor(mrdata);
                        Log.i(TAG, "Constructor data successfully retrieved: " + constructorAPIResponse);

                        if (constructorAPIResponse != null &&
                                constructorAPIResponse.getConstructorTableDTO() != null &&
                                !constructorAPIResponse.getConstructorTableDTO().getConstructorDTOList().isEmpty()) {

                            Result result = new Result.ConstructorSuccess(
                                    ConstructorMapper.toConstructor(
                                            constructorAPIResponse.getConstructorTableDTO().getConstructorDTOList().get(0)
                                    )
                            );
                            future.complete(result);
                        } else {
                            Log.e(TAG, "Constructor data is empty or invalid");
                            future.complete(new Result.Error("Constructor data is empty or invalid"));
                        }
                    } else {
                        Log.e(TAG, "API response not successful: " + response.code());
                        future.complete(new Result.Error("API request failed with code: " + response.code()));
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error parsing response", e);
                    future.complete(new Result.Error("Error parsing response: " + e.getMessage()));
                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error", e);
                    future.complete(new Result.Error("Unexpected error: " + e.getMessage()));
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable throwable) {
                Log.e(TAG, "API call failed", throwable);
                future.complete(new Result.Error("API call failed: " +
                        (throwable != null ? throwable.getMessage() : "Unknown error")));
            }
        });

        // When the future is completed (success or failure), remove it from the pending requests map
        future.whenComplete((result, throwable) -> pendingRequests.remove(requestKey));

        return future;
    }
}