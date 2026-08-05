package com.the_coffe_coders.fastestlap.source.f1.result.stint;

import static com.the_coffe_coders.fastestlap.util.Constants.RETROFIT_ERROR;
import static com.the_coffe_coders.fastestlap.util.ui.UIUtils.getIntOrDefault;
import static com.the_coffe_coders.fastestlap.util.ui.UIUtils.getNullableInt;
import static com.the_coffe_coders.fastestlap.util.ui.UIUtils.getStringOrNull;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.domain.f1.result.PitStopInfo;
import com.the_coffe_coders.fastestlap.domain.f1.result.Stint;
import com.the_coffe_coders.fastestlap.repository.f1.result.StintCallback;
import com.the_coffe_coders.fastestlap.service.OpenF1APIService;
import com.the_coffe_coders.fastestlap.util.service.ServiceLocator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Remote data source for fetching tyre stints from the OpenF1 API.
 * Performs mapping from event name and session name to meeting_key and session_key.
 */
public class OpenF1StintDataSource implements StintDataSource {

    private static final String TAG = "OpenF1StintDataSource";
    private static OpenF1StintDataSource instance;
    private final OpenF1APIService openF1APIService;

    public OpenF1StintDataSource() {
        this.openF1APIService = ServiceLocator.getInstance().getOpenF1APIService();
    }

    public static synchronized OpenF1StintDataSource getInstance() {
        if (instance == null) {
            instance = new OpenF1StintDataSource();
        }
        return instance;
    }

    /**
     * Fetches tyre stints for a specific event and session.
     * Maps eventName -> meeting_key and sessionName -> session_key via OpenF1 endpoints.
     */
    @Override
    public void getStints(String eventName, String sessionName, StintCallback callback) {
        String year = ServiceLocator.currentYear;
        Log.d(TAG, "Fetching stints for eventName: " + eventName + ", sessionName: " + sessionName + ", year: " + year);

        if (eventName == null || eventName.trim().isEmpty()) {
            // Fallback: fetch latest session stints directly if eventName is missing
            fetchStintsBySessionKey("latest", callback);
            return;
        }

        openF1APIService.getMeetings(year).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                ResponseBody body = response.body();
                if (body == null) {
                    Log.e(TAG, "OpenF1 meetings returned empty body");
                    fetchStintsBySessionKey("latest", callback);
                    return;
                }
                try {
                    String json = body.string();
                    Integer meetingKey = findMeetingKey(json, eventName);
                    if (meetingKey != null) {
                        Log.d(TAG, "Matched meeting_key: " + meetingKey + " for event: " + eventName);
                        fetchSessionKeyAndStints(meetingKey, sessionName, callback);
                    } else {
                        Log.w(TAG, "No matching meeting_key found for event: " + eventName + ", falling back to latest");
                        fetchStintsBySessionKey("latest", callback);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing meetings response", e);
                    callback.onFailure(new Exception("Failed to process meetings response: " + e.getMessage(), e));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.e(TAG, "Network failure fetching meetings", t);
                callback.onFailure(new Exception(RETROFIT_ERROR + ": " + t.getMessage(), t));
            }
        });
    }

    private Integer findMeetingKey(String meetingsJson, String targetEventName) {
        JsonArray jsonArray = new Gson().fromJson(meetingsJson, JsonArray.class);
        if (jsonArray == null) return null;

        String targetNorm = targetEventName.toLowerCase(java.util.Locale.ROOT).trim();

        for (JsonElement element : jsonArray) {
            if (!element.isJsonObject()) continue;
            JsonObject obj = element.getAsJsonObject();

            String meetingName = getStringOrNull(obj, "meeting_name");
            String meetingOfficialName = getStringOrNull(obj, "meeting_official_name");
            String location = getStringOrNull(obj, "location");
            String countryName = getStringOrNull(obj, "country_name");
            String circuitShort = getStringOrNull(obj, "circuit_short_name");

            if (matches(meetingName, targetNorm)
                    || matches(meetingOfficialName, targetNorm)
                    || matches(location, targetNorm)
                    || matches(countryName, targetNorm)
                    || matches(circuitShort, targetNorm)) {
                return getIntOrDefault(obj, "meeting_key", -1);
            }
        }
        return null;
    }

    private boolean matches(String candidate, String targetNorm) {
        if (candidate == null) return false;
        String candNorm = candidate.toLowerCase(java.util.Locale.ROOT).trim();
        return candNorm.contains(targetNorm) || targetNorm.contains(candNorm);
    }

    private void fetchSessionKeyAndStints(int meetingKey, String sessionName, StintCallback callback) {
        String targetSession = (sessionName != null && !sessionName.trim().isEmpty()) ? sessionName : "Race";

        openF1APIService.getSessionsByMeetingKey(meetingKey).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                ResponseBody body = response.body();
                if (body == null) {
                    Log.e(TAG, "OpenF1 sessions returned empty body");
                    callback.onFailure(new Exception("Empty response body from sessions endpoint"));
                    return;
                }
                try {
                    String json = body.string();
                    Integer sessionKey = findSessionKey(json, targetSession);
                    if (sessionKey != null) {
                        Log.d(TAG, "Matched session_key: " + sessionKey + " for session: " + targetSession);
                        fetchStintsBySessionKey(String.valueOf(sessionKey), callback);
                    } else {
                        Log.w(TAG, "No matching session_key found for sessionName: " + targetSession);
                        fetchStintsBySessionKey("latest", callback);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing sessions response", e);
                    callback.onFailure(new Exception("Failed to process sessions response: " + e.getMessage(), e));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.e(TAG, "Network failure fetching sessions", t);
                callback.onFailure(new Exception(RETROFIT_ERROR + ": " + t.getMessage(), t));
            }
        });
    }

    private Integer findSessionKey(String sessionsJson, String targetSessionName) {
        JsonArray jsonArray = new Gson().fromJson(sessionsJson, JsonArray.class);
        if (jsonArray == null || jsonArray.isEmpty()) return null;

        String targetNorm = targetSessionName.toLowerCase(java.util.Locale.ROOT).trim();

        // 1° Pass: Match ESATTO con equalsIgnoreCase su session_name
        for (JsonElement element : jsonArray) {
            if (!element.isJsonObject()) continue;
            JsonObject obj = element.getAsJsonObject();

            String sName = getStringOrNull(obj, "session_name");
            if (sName != null && sName.trim().equalsIgnoreCase(targetNorm)) {
                return getIntOrDefault(obj, "session_key", -1);
            }
        }

        // 2° Pass: Fallback partial match se non trovato match esatto
        for (JsonElement element : jsonArray) {
            if (!element.isJsonObject()) continue;
            JsonObject obj = element.getAsJsonObject();

            String sName = getStringOrNull(obj, "session_name");
            if (matches(sName, targetNorm)) {
                return getIntOrDefault(obj, "session_key", -1);
            }
        }

        // Fallback: primo session_key disponibile se nessun nome corrisponde
        JsonObject first = jsonArray.get(0).getAsJsonObject();
        return getIntOrDefault(first, "session_key", -1);
    }

    private void fetchStintsBySessionKey(String sessionKey, StintCallback callback) {
        Log.d(TAG, "Fetching stints for session_key: " + sessionKey);
        Call<ResponseBody> call;
        if ("latest".equals(sessionKey)) {
            call = openF1APIService.getStintsBySessionKey("latest");
        } else {
            try {
                int sKeyInt = Integer.parseInt(sessionKey);
                call = openF1APIService.getStints(sKeyInt);
            } catch (NumberFormatException e) {
                call = openF1APIService.getStintsBySessionKey(sessionKey);
            }
        }

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                ResponseBody body = response.body();
                if (body == null) {
                    Log.e(TAG, "OpenF1 stints returned empty body");
                    callback.onFailure(new Exception("Empty response body from stints endpoint"));
                    return;
                }
                try {
                    String json = body.string();
                    processStintsResponse(json, sessionKey, callback);
                } catch (IOException e) {
                    Log.e(TAG, "IOException while reading stints response", e);
                    callback.onFailure(new Exception("Failed to read stints response: " + e.getMessage(), e));
                } catch (Exception e) {
                    Log.e(TAG, "Exception while processing stints response", e);
                    callback.onFailure(new Exception("Failed to process stints response: " + e.getMessage(), e));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.e(TAG, "Network failure fetching stints", t);
                callback.onFailure(new Exception(RETROFIT_ERROR + ": " + t.getMessage(), t));
            }
        });
    }

    private void processStintsResponse(String json, String sessionKey, StintCallback callback) {
        try {
            JsonArray jsonArray = new Gson().fromJson(json, JsonArray.class);
            if (jsonArray == null) {
                Log.e(TAG, "Failed to parse JSON array for stints");
                callback.onFailure(new Exception("Invalid JSON response from stints endpoint"));
                return;
            }

            List<Stint> stints = new ArrayList<>();
            for (JsonElement element : jsonArray) {
                if (!element.isJsonObject()) continue;
                JsonObject obj = element.getAsJsonObject();

                int meetingKey    = getIntOrDefault(obj, "meeting_key", 0);
                int sKey          = getIntOrDefault(obj, "session_key", 0);
                int stintNumber   = getIntOrDefault(obj, "stint_number", 0);
                int driverNumber  = getIntOrDefault(obj, "driver_number", 0);
                Integer lapStart  = getNullableInt(obj, "lap_start");
                Integer lapEnd    = getNullableInt(obj, "lap_end");
                String compound   = getStringOrNull(obj, "compound");
                Integer tyreAge   = getNullableInt(obj, "tyre_age_at_start");

                stints.add(new Stint(meetingKey, sKey, stintNumber, driverNumber, lapStart, lapEnd, compound, tyreAge));
            }

            // Ordina prima per driverNumber crescente, poi per lapStart crescente
            stints.sort((s1, s2) -> {
                int driverCompare = Integer.compare(s1.getDriverNumber(), s2.getDriverNumber());
                if (driverCompare != 0) {
                    return driverCompare;
                }
                Integer l1 = s1.getLapStart();
                Integer l2 = s2.getLapStart();
                if (l1 == null && l2 == null) return 0;
                if (l1 == null) return -1;
                if (l2 == null) return 1;
                return Integer.compare(l1, l2);
            });

            Log.d(TAG, "Successfully parsed and sorted " + stints.size() + " stints. Fetching pit stop data...");
            fetchPitStopsAndEnrichStints(sessionKey, stints, callback);

        } catch (Exception e) {
            Log.e(TAG, "Exception while parsing stints JSON", e);
            callback.onFailure(new Exception("Failed to parse stints response: " + e.getMessage(), e));
        }
    }

    private void fetchPitStopsAndEnrichStints(String sessionKey, List<Stint> stints, StintCallback callback) {
        Call<ResponseBody> pitCall;
        try {
            int sKeyInt = Integer.parseInt(sessionKey);
            pitCall = openF1APIService.getPitStops(sKeyInt);
        } catch (NumberFormatException e) {
            pitCall = openF1APIService.getPitStopsBySessionKey(sessionKey);
        }

        pitCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                ResponseBody body = response.body();
                if (body != null) {
                    try {
                        String json = body.string();
                        JsonArray jsonArray = new Gson().fromJson(json, JsonArray.class);
                        if (jsonArray != null) {
                            Map<Integer, List<PitStopInfo>> driverPitMap = new HashMap<>();
                            for (JsonElement elem : jsonArray) {
                                Log.d(TAG, "Pit stop: " + elem.toString());
                                if (!elem.isJsonObject()) continue;
                                JsonObject obj = elem.getAsJsonObject();
                                int driverNum = getIntOrDefault(obj, "driver_number", -1);
                                int lapNum = getIntOrDefault(obj, "lap_number", -1);

                                Double pitDur = null;
                                if (obj.has("stop_duration") && !obj.get("stop_duration").isJsonNull()) {
                                    try {
                                        pitDur = obj.get("stop_duration").getAsDouble();
                                    } catch (Exception ignored) {}
                                }else{
                                    if (obj.has("lane_duration") && !obj.get("lane_duration").isJsonNull()) {
                                        try {
                                            pitDur = obj.get("lane_duration").getAsDouble();
                                        } catch (Exception ignored) {}
                                    }
                                }

                                if (driverNum != -1 && pitDur != null && pitDur > 0) {
                                    driverPitMap.computeIfAbsent(driverNum, k -> new ArrayList<>())
                                                .add(new PitStopInfo(lapNum, pitDur));
                                }
                            }

                            for (List<PitStopInfo> list : driverPitMap.values()) {
                                list.sort((p1, p2) -> Integer.compare(p1.getLapNumber(), p2.getLapNumber()));
                            }

                            Map<Integer, List<Stint>> driverStintsMap = new LinkedHashMap<>();
                            for (Stint stint : stints) {
                                driverStintsMap.computeIfAbsent(stint.getDriverNumber(), k -> new ArrayList<>())
                                               .add(stint);
                            }

                            for (Map.Entry<Integer, List<Stint>> entry : driverStintsMap.entrySet()) {
                                int driverNum = entry.getKey();
                                List<Stint> driverStints = entry.getValue();
                                List<PitStopInfo> driverPits = driverPitMap.get(driverNum);

                                if (driverPits != null && !driverPits.isEmpty()) {
                                    for (int i = 0; i < driverStints.size(); i++) {
                                        Stint stint = driverStints.get(i);
                                        if (i > 0 && (i - 1) < driverPits.size()) {
                                            stint.setPitDuration(driverPits.get(i - 1).getDuration());
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Error parsing pit stop data: " + e.getMessage());
                    }
                }
                callback.onSuccess(stints);
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.w(TAG, "Failed to fetch pit stops, returning stints without pit duration: " + t.getMessage());
                callback.onSuccess(stints);
            }
        });
    }
}
