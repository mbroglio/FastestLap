package com.the_coffe_coders.fastestlap.source.f1.livetiming;

import static com.the_coffe_coders.fastestlap.util.Constants.RETROFIT_ERROR;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.domain.f1.livetiming.RaceControlMessage;
import com.the_coffe_coders.fastestlap.domain.f1.livetiming.TeamRadioMessage;
import com.the_coffe_coders.fastestlap.repository.f1.livetiming.RaceControlCallback;
import com.the_coffe_coders.fastestlap.repository.f1.livetiming.TeamRadioCallback;
import com.the_coffe_coders.fastestlap.service.OpenF1APIService;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Remote data source that fetches live-timing data from the OpenF1 API.
 *
 * <p>This class follows the same structural pattern used by
 * {@code JolpicaWeeklyRaceDataSource} for the Ergast/Jolpica API:
 * <ul>
 *   <li>It holds a reference to the Retrofit-generated {@link OpenF1APIService}.</li>
 *   <li>Each public method enqueues an asynchronous HTTP call and delegates the
 *       response (or failure) back through the appropriate callback interface.</li>
 *   <li>JSON parsing is done manually via Gson on the raw {@link ResponseBody}
 *       so the response format is handled identically to the Ergast layer.</li>
 * </ul>
 * </p>
 */
public class OpenF1LiveTimingDataSource {

    private static final String TAG = "OpenF1LiveTimingDataSource";
    private static final String SESSION_KEY_LATEST = "latest";

    private static OpenF1LiveTimingDataSource instance;
    private final OpenF1APIService openF1APIService;

    public OpenF1LiveTimingDataSource() {
        this.openF1APIService = ServiceLocator.getInstance().getOpenF1APIService();
    }

    public static synchronized OpenF1LiveTimingDataSource getInstance() {
        if (instance == null) {
            instance = new OpenF1LiveTimingDataSource();
        }
        return instance;
    }

    // ─────────────────────────────────────────────────────────────
    // Race Control
    // ─────────────────────────────────────────────────────────────

    /**
     * Fetches all Race Control messages for the latest (or currently live) session.
     *
     * @param callback delivers the parsed list of {@link RaceControlMessage} objects,
     *                 or an error if the request or parsing fails
     */
    public void getRaceControlMessages(RaceControlCallback callback) {
        Log.d(TAG, "Fetching race control messages from OpenF1 API");
        Call<ResponseBody> call = openF1APIService.getRaceControlMessages(SESSION_KEY_LATEST);

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call,
                                   @NonNull Response<ResponseBody> response) {
                ResponseBody body = response.body();
                if (body == null) {
                    Log.e(TAG, "OpenF1 race_control returned an empty body");
                    callback.onFailure(new Exception("Empty response body from race_control endpoint"));
                    return;
                }
                try {
                    String json = body.string();
                    processRaceControlResponse(json, callback);
                } catch (IOException e) {
                    Log.e(TAG, "IOException while reading race_control response", e);
                    callback.onFailure(new Exception("Failed to read race_control response: " + e.getMessage(), e));
                } catch (Exception e) {
                    Log.e(TAG, "Exception while processing race_control response", e);
                    callback.onFailure(new Exception("Failed to process race_control response: " + e.getMessage(), e));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.e(TAG, "Network failure fetching race_control messages", t);
                callback.onFailure(new Exception(RETROFIT_ERROR + ": " + t.getMessage(), t));
            }
        });
    }

    /**
     * Parses the JSON array returned by the {@code race_control} endpoint and
     * maps each entry to a {@link RaceControlMessage} domain object.
     */
    private void processRaceControlResponse(String json, RaceControlCallback callback) {
        try {
            JsonArray jsonArray = new Gson().fromJson(json, JsonArray.class);
            if (jsonArray == null) {
                Log.e(TAG, "Failed to parse JSON array for race_control");
                callback.onFailure(new Exception("Invalid JSON response from race_control endpoint"));
                return;
            }

            List<RaceControlMessage> messages = new ArrayList<>();
            for (JsonElement element : jsonArray) {
                JsonObject obj = element.getAsJsonObject();

                int meetingKey    = getIntOrDefault(obj, "meeting_key", 0);
                int sessionKey    = getIntOrDefault(obj, "session_key", 0);
                String date       = getStringOrNull(obj, "date");
                Integer driverNum = getNullableInt(obj, "driver_number");
                Integer lapNum    = getNullableInt(obj, "lap_number");
                String category   = getStringOrNull(obj, "category");
                String flag       = getStringOrNull(obj, "flag");
                String scope      = getStringOrNull(obj, "scope");
                Integer sector    = getNullableInt(obj, "sector");
                String qualPhase  = getStringOrNull(obj, "qualifying_phase");
                String message    = getStringOrNull(obj, "message");

                messages.add(new RaceControlMessage(
                        meetingKey, sessionKey, date, driverNum,
                        lapNum, category, flag, scope, sector, qualPhase, message));
            }

            Log.d(TAG, "Successfully parsed " + messages.size() + " race control messages");
            callback.onSuccess(messages);

        } catch (Exception e) {
            Log.e(TAG, "Exception while parsing race_control JSON", e);
            callback.onFailure(new Exception("Failed to parse race_control response: " + e.getMessage(), e));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Team Radio
    // ─────────────────────────────────────────────────────────────

    /**
     * Fetches all Team Radio recordings for the latest (or currently live) session.
     *
     * @param callback delivers the parsed list of {@link TeamRadioMessage} objects,
     *                 or an error if the request or parsing fails
     */
    public void getTeamRadioMessages(TeamRadioCallback callback) {
        Log.d(TAG, "Fetching team radio messages from OpenF1 API");
        Call<ResponseBody> call = openF1APIService.getTeamRadioMessages(SESSION_KEY_LATEST);

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call,
                                   @NonNull Response<ResponseBody> response) {
                ResponseBody body = response.body();
                if (body == null) {
                    Log.e(TAG, "OpenF1 team_radio returned an empty body");
                    callback.onFailure(new Exception("Empty response body from team_radio endpoint"));
                    return;
                }
                try {
                    String json = body.string();
                    processTeamRadioResponse(json, callback);
                } catch (IOException e) {
                    Log.e(TAG, "IOException while reading team_radio response", e);
                    callback.onFailure(new Exception("Failed to read team_radio response: " + e.getMessage(), e));
                } catch (Exception e) {
                    Log.e(TAG, "Exception while processing team_radio response", e);
                    callback.onFailure(new Exception("Failed to process team_radio response: " + e.getMessage(), e));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.e(TAG, "Network failure fetching team_radio messages", t);
                callback.onFailure(new Exception(RETROFIT_ERROR + ": " + t.getMessage(), t));
            }
        });
    }

    /**
     * Parses the JSON array returned by the {@code team_radio} endpoint and
     * maps each entry to a {@link TeamRadioMessage} domain object.
     */
    private void processTeamRadioResponse(String json, TeamRadioCallback callback) {
        try {
            JsonArray jsonArray = new Gson().fromJson(json, JsonArray.class);
            if (jsonArray == null) {
                Log.e(TAG, "Failed to parse JSON array for team_radio");
                callback.onFailure(new Exception("Invalid JSON response from team_radio endpoint"));
                return;
            }

            List<TeamRadioMessage> messages = new ArrayList<>();
            for (JsonElement element : jsonArray) {
                JsonObject obj = element.getAsJsonObject();

                int meetingKey  = getIntOrDefault(obj, "meeting_key", 0);
                int sessionKey  = getIntOrDefault(obj, "session_key", 0);
                int driverNum   = getIntOrDefault(obj, "driver_number", 0);
                String date     = getStringOrNull(obj, "date");
                String recordingUrl = getStringOrNull(obj, "recording_url");

                messages.add(new TeamRadioMessage(meetingKey, sessionKey, driverNum, date, recordingUrl));
            }

            Log.d(TAG, "Successfully parsed " + messages.size() + " team radio messages");
            callback.onSuccess(messages);

        } catch (Exception e) {
            Log.e(TAG, "Exception while parsing team_radio JSON", e);
            callback.onFailure(new Exception("Failed to parse team_radio response: " + e.getMessage(), e));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // JSON helper utilities
    // ─────────────────────────────────────────────────────────────

    private String getStringOrNull(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return null;
        return obj.get(key).getAsString();
    }

    private int getIntOrDefault(JsonObject obj, String key, int defaultValue) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return defaultValue;
        return obj.get(key).getAsInt();
    }

    private Integer getNullableInt(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return null;
        return obj.get(key).getAsInt();
    }
}
