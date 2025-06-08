package com.the_coffe_coders.fastestlap.source.weeklyrace;

import static com.the_coffe_coders.fastestlap.util.Constants.RETROFIT_ERROR;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.api.RaceAPIResponse;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.dto.RaceDTO;
import com.the_coffe_coders.fastestlap.mapper.WeeklyRaceMapper;
import com.the_coffe_coders.fastestlap.repository.weeklyrace.SingleWeeklyRaceCallback;
import com.the_coffe_coders.fastestlap.repository.weeklyrace.WeeklyRacesCallback;
import com.the_coffe_coders.fastestlap.service.ErgastAPIService;
import com.the_coffe_coders.fastestlap.util.JSONParserUtils;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class JolpicaWeeklyRaceDataSource{

    private static final String TAG = "JolpicaWeeklyRaceDataSource";
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 2000; // 2 seconds
    private static JolpicaWeeklyRaceDataSource instance;
    private final ErgastAPIService ergastAPIService;

    public JolpicaWeeklyRaceDataSource() {
        this.ergastAPIService = ServiceLocator.getInstance().getConcreteErgastAPIService();
    }

    public static synchronized JolpicaWeeklyRaceDataSource getInstance() {
        if (instance == null) {
            instance = new JolpicaWeeklyRaceDataSource();
        }
        return instance;
    }

    public void getWeeklyRaces(WeeklyRacesCallback weeklyRacesCallback) {
        Log.d(TAG, "Fetching weekly races from remote API");
        Call<ResponseBody> responseCall = ergastAPIService.getRaces();

        responseCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                ResponseBody body = response.body();
                if (body == null) {
                    Log.e(TAG, "API returned empty body for weekly races");
                    weeklyRacesCallback.onFailure(new Exception("Empty response body"));
                    return;
                }

                try {
                    String responseString = body.string();
                    processWeeklyRacesResponse(responseString, weeklyRacesCallback);
                } catch (IOException e) {
                    Log.e(TAG, "IOException while reading weekly races response", e);
                    weeklyRacesCallback.onFailure(new Exception("Failed to read response: " + e.getMessage(), e));
                } catch (Exception e) {
                    Log.e(TAG, "Exception while processing weekly races response", e);
                    weeklyRacesCallback.onFailure(new Exception("Failed to process response: " + e.getMessage(), e));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.e(TAG, "Failed to fetch weekly races", t);
                if (weeklyRacesCallback != null) {
                    weeklyRacesCallback.onFailure(new Exception(RETROFIT_ERROR, t));
                }
            }
        });
    }

    private void processWeeklyRacesResponse(String responseString, WeeklyRacesCallback weeklyRacesCallback) {
        try {
            JsonObject jsonResponse = new Gson().fromJson(responseString, JsonObject.class);

            if (jsonResponse == null) {
                Log.e(TAG, "Failed to parse JSON response for weekly races");
                weeklyRacesCallback.onFailure(new Exception("Invalid JSON response"));
                return;
            }

            JsonObject mrdata = jsonResponse.getAsJsonObject("MRData");
            if (mrdata == null) {
                Log.e(TAG, "MRData not found in weekly races response");
                weeklyRacesCallback.onFailure(new Exception("MRData not found in response"));
                return;
            }

            JSONParserUtils jsonParserUtils = new JSONParserUtils();
            RaceAPIResponse raceAPIResponse = jsonParserUtils.parseRace(mrdata);

            Log.d(TAG, "Successfully parsed weekly races: " + raceAPIResponse);
            weeklyRacesCallback.onSuccess(unpackWeeklyRaceList(raceAPIResponse));
        } catch (Exception e) {
            Log.e(TAG, "Exception while processing weekly races response", e);
            weeklyRacesCallback.onFailure(new Exception("Failed to process response: " + e.getMessage(), e));
        }
    }

    List<WeeklyRace> unpackWeeklyRaceList(RaceAPIResponse weeklyRaceAPIResponse) {
        List<RaceDTO> raceDTOS = weeklyRaceAPIResponse.getRaceTable().getRaces();
        List<WeeklyRace> weeklyRaceList = new ArrayList<>();
        for (RaceDTO raceDTO : raceDTOS) {
            weeklyRaceList.add(WeeklyRaceMapper.toWeeklyRace(raceDTO));
        }

        return weeklyRaceList;
    }

    public void getNextRace(SingleWeeklyRaceCallback weeklyRaceCallback) {
        getNextRaceWithRetry(weeklyRaceCallback, 0);
    }

    private void getNextRaceWithRetry(SingleWeeklyRaceCallback weeklyRaceCallback, int currentRetry) {
        Log.d(TAG, String.format("Fetching next race (attempt %d)", currentRetry + 1));
        Call<ResponseBody> responseCall = ergastAPIService.getNextRace();

        responseCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                ResponseBody body = response.body();
                if (body == null) {
                    Log.e(TAG, "API returned empty body for next race");
                    handleNextRaceRetryOrFailure(currentRetry, new Exception("Empty response body"), weeklyRaceCallback);
                    return;
                }

                try {
                    String responseString = body.string();
                    processNextRaceResponse(responseString, weeklyRaceCallback);
                } catch (IOException e) {
                    Log.e(TAG, "IOException while reading next race response", e);
                    handleNextRaceRetryOrFailure(currentRetry, new Exception("Failed to read response: " + e.getMessage(), e), weeklyRaceCallback);
                } catch (Exception e) {
                    Log.e(TAG, "Exception while processing next race response", e);
                    handleNextRaceRetryOrFailure(currentRetry, new Exception("Failed to process response: " + e.getMessage(), e), weeklyRaceCallback);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.e(TAG, "Failed to fetch next race", t);
                handleNextRaceRetryOrFailure(currentRetry, new Exception(RETROFIT_ERROR + ": " + t.getMessage(), t), weeklyRaceCallback);
            }
        });
    }

    private void processNextRaceResponse(String responseString, SingleWeeklyRaceCallback weeklyRaceCallback) {
        try {
            JsonObject jsonResponse = new Gson().fromJson(responseString, JsonObject.class);

            if (jsonResponse == null) {
                Log.e(TAG, "Failed to parse JSON response for next race");
                weeklyRaceCallback.onFailure(new Exception("Invalid JSON response"));
                return;
            }

            JsonObject mrdata = jsonResponse.getAsJsonObject("MRData");
            if (mrdata == null) {
                Log.e(TAG, "MRData not found in next race response");
                weeklyRaceCallback.onFailure(new Exception("MRData not found in response"));
                return;
            }

            JSONParserUtils jsonParserUtils = new JSONParserUtils();
            RaceAPIResponse raceAPIResponse = jsonParserUtils.parseRace(mrdata);

            Log.d(TAG, "Successfully parsed next race: " + raceAPIResponse.getRaceTable().getRace());
            weeklyRaceCallback.onSuccess(WeeklyRaceMapper.toWeeklyRace(raceAPIResponse.getRaceTable().getRaces().get(0)));
        } catch (Exception e) {
            Log.e(TAG, "Exception while processing next race response", e);
            weeklyRaceCallback.onFailure(new Exception("Failed to process response: " + e.getMessage(), e));
        }
    }

    private void handleNextRaceRetryOrFailure(int currentRetry, Exception error, SingleWeeklyRaceCallback weeklyRaceCallback) {
        if (currentRetry < MAX_RETRIES) {
            Log.w(TAG, String.format("Retrying next race after failure (attempt %d/%d): %s",
                    currentRetry + 1, MAX_RETRIES, error.getMessage()));

            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() ->
                    getNextRaceWithRetry(weeklyRaceCallback, currentRetry + 1), RETRY_DELAY_MS);
        } else {
            Log.e(TAG, "Failed to fetch next race after " + MAX_RETRIES + " attempts: " + error.getMessage());
            weeklyRaceCallback.onFailure(error);
        }
    }

    public void getLastRace(SingleWeeklyRaceCallback weeklyRaceCallback) {
        getLastRaceWithRetry(weeklyRaceCallback, 0);
    }

    private void getLastRaceWithRetry(SingleWeeklyRaceCallback weeklyRaceCallback, int currentRetry) {
        Log.d(TAG, String.format("Fetching last race (attempt %d)", currentRetry + 1));
        Call<ResponseBody> responseCall = ergastAPIService.getLastRace();

        responseCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                try {
                    ResponseBody body = response.body();
                    if (body == null) {
                        Log.e(TAG, "API returned empty body for last race");
                        handleLastRaceRetryOrFailure(currentRetry, new Exception("Empty response body"), weeklyRaceCallback);
                        return;
                    }

                    String responseString = body.string();
                    processLastRaceResponse(responseString, weeklyRaceCallback);
                } catch (IOException e) {
                    Log.e(TAG, "IOException while reading last race response", e);
                    handleLastRaceRetryOrFailure(currentRetry, new Exception("Failed to read response: " + e.getMessage(), e), weeklyRaceCallback);
                } catch (Exception e) {
                    Log.e(TAG, "Exception while processing last race response", e);
                    handleLastRaceRetryOrFailure(currentRetry, new Exception("Failed to process response: " + e.getMessage(), e), weeklyRaceCallback);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.e(TAG, "Failed to fetch last race", t);
                handleLastRaceRetryOrFailure(currentRetry, new Exception(RETROFIT_ERROR + ": " + t.getMessage(), t), weeklyRaceCallback);
            }
        });
    }

    private void processLastRaceResponse(String responseString, SingleWeeklyRaceCallback weeklyRaceCallback) {
        try {
            JsonObject jsonResponse = new Gson().fromJson(responseString, JsonObject.class);

            if (jsonResponse == null) {
                Log.e(TAG, "Failed to parse JSON response for last race");
                weeklyRaceCallback.onFailure(new Exception("Invalid JSON response"));
                return;
            }

            JsonObject mrdata = jsonResponse.getAsJsonObject("MRData");
            if (mrdata == null) {
                Log.e(TAG, "MRData not found in last race response");
                weeklyRaceCallback.onFailure(new Exception("MRData not found in response"));
                return;
            }

            JSONParserUtils jsonParserUtils = new JSONParserUtils();
            RaceAPIResponse raceAPIResponse = jsonParserUtils.parseRace(mrdata);

            Log.d(TAG, "Successfully parsed last race");
            weeklyRaceCallback.onSuccess(WeeklyRaceMapper.toWeeklyRace(raceAPIResponse.getRaceTable().getRaces().get(0)));
        } catch (Exception e) {
            Log.e(TAG, "Exception while processing last race response", e);
            weeklyRaceCallback.onFailure(new Exception("Failed to process response: " + e.getMessage(), e));
        }
    }

    private void handleLastRaceRetryOrFailure(int currentRetry, Exception error, SingleWeeklyRaceCallback weeklyRaceCallback) {
        if (currentRetry < MAX_RETRIES) {
            Log.w(TAG, String.format("Retrying last race after failure (attempt %d/%d): %s",
                    currentRetry + 1, MAX_RETRIES, error.getMessage()));

            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() ->
                    getLastRaceWithRetry(weeklyRaceCallback, currentRetry + 1), RETRY_DELAY_MS);
        } else {
            Log.e(TAG, "Failed to fetch last race after " + MAX_RETRIES + " attempts: " + error.getMessage());
            weeklyRaceCallback.onFailure(error);
        }
    }
}