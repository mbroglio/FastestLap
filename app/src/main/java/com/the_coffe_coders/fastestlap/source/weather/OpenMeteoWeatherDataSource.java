package com.the_coffe_coders.fastestlap.source.weather;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.domain.weather.DailyForecast;
import com.the_coffe_coders.fastestlap.domain.weather.HourlyForecast;
import com.the_coffe_coders.fastestlap.domain.weather.WeatherInfo;
import com.the_coffe_coders.fastestlap.repository.weather.CurrentWeatherCallback;
import com.the_coffe_coders.fastestlap.repository.weather.WeekendForecastCallback;
import com.the_coffe_coders.fastestlap.service.OpenF1APIService;
import com.the_coffe_coders.fastestlap.service.OpenMeteoAPIService;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;
import com.the_coffe_coders.fastestlap.util.weather.WeatherUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Remote data source in the source package that executes actual HTTP API calls
 * to Open-Meteo and OpenF1 for weather telemetry.
 */
public class OpenMeteoWeatherDataSource implements WeatherDataSource {
    private static final String TAG = "OpenMeteoWeatherDataSource";
    private static OpenMeteoWeatherDataSource instance;

    private final OpenF1APIService openF1APIService;
    private final OpenMeteoAPIService openMeteoAPIService;

    public OpenMeteoWeatherDataSource() {
        this.openF1APIService = ServiceLocator.getInstance().getOpenF1APIService();
        this.openMeteoAPIService = ServiceLocator.getInstance().getOpenMeteoAPIService();
    }

    public static synchronized OpenMeteoWeatherDataSource getInstance() {
        if (instance == null) {
            instance = new OpenMeteoWeatherDataSource();
        }
        return instance;
    }

    @Override
    public void getCurrentWeather(String locality, double latitude, double longitude, String sessionKey, boolean isSessionInProgress, CurrentWeatherCallback callback) {
        WeatherInfo info = new WeatherInfo();
        info.setLocality(locality != null ? locality.toUpperCase(Locale.ROOT) : "CIRCUIT");

        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy", Locale.ENGLISH);
        info.setDateString(sdf.format(new Date()));

        openMeteoAPIService.getWeatherForecast(
                latitude,
                longitude,
                "temperature_2m,relative_humidity_2m,weather_code,surface_pressure,wind_speed_10m,wind_direction_10m,wind_gusts_10m",
                null,
                "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max",
                "best_match",
                "auto",
                null,
                null
        ).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                ResponseBody body = response.body();

                if (response.isSuccessful() && body != null) {
                    try {
                        String jsonStr = body.string();
                        JsonObject json = new Gson().fromJson(jsonStr, JsonObject.class);

                        if (json != null && !json.has("error")) {
                            if (json.has("current") && !json.get("current").isJsonNull()) {
                                JsonObject current = json.getAsJsonObject("current");
                                if (current.has("temperature_2m")) info.setAirTempCurrent(current.get("temperature_2m").getAsDouble());
                                if (current.has("relative_humidity_2m")) info.setHumidity(current.get("relative_humidity_2m").getAsInt());
                                if (current.has("surface_pressure")) info.setPressure(current.get("surface_pressure").getAsDouble());
                                if (current.has("wind_speed_10m")) info.setWindSpeedKmH(current.get("wind_speed_10m").getAsDouble());
                                if (current.has("wind_gusts_10m")) info.setWindGustKmH(current.get("wind_gusts_10m").getAsDouble());
                                if (current.has("wind_direction_10m")) {
                                    int deg = current.get("wind_direction_10m").getAsInt();
                                    info.setWindDirection(WeatherUtils.getCardinalDirection(deg));
                                }

                                int code = current.has("weather_code") ? current.get("weather_code").getAsInt() : 0;
                                info.setWeatherCondition(WeatherUtils.getWeatherText(code, false));
                                info.setWeatherIconResId(WeatherUtils.getWeatherIconResId(code, false));
                                info.setWeatherVideoResId(WeatherUtils.getWeatherVideoResId(code, false));
                            }

                            if (json.has("daily") && !json.get("daily").isJsonNull()) {
                                JsonObject daily = json.getAsJsonObject("daily");
                                JsonArray tempMaxs = daily.getAsJsonArray("temperature_2m_max");
                                JsonArray tempMins = daily.getAsJsonArray("temperature_2m_min");
                                JsonArray rainProbs = daily.getAsJsonArray("precipitation_probability_max");

                                if (tempMaxs != null && !tempMaxs.isEmpty()) info.setAirTempMax(tempMaxs.get(0).getAsDouble());
                                if (tempMins != null && !tempMins.isEmpty()) info.setAirTempMin(tempMins.get(0).getAsDouble());
                                if (rainProbs != null && !rainProbs.isEmpty()) info.setRainProbability(rainProbs.get(0).getAsInt());
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing Open-Meteo current weather response", e);
                    }
                }

                if (isSessionInProgress && sessionKey != null && !sessionKey.trim().isEmpty()) {
                    fetchOpenF1TrackTemperatureOnly(info, sessionKey, callback);
                } else {
                    callback.onSuccess(info);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.w(TAG, "Failed Open-Meteo current weather fetch: " + t.getMessage());
                if (isSessionInProgress && sessionKey != null && !sessionKey.trim().isEmpty()) {
                    fetchOpenF1TrackTemperatureOnly(info, sessionKey, callback);
                } else {
                    callback.onSuccess(info);
                }
            }
        });
    }

    @Override
    public void getWeekendForecast(double latitude, double longitude, String startDate, String endDate, WeekendForecastCallback callback) {
        /*
        if (isEventDatePassed(endDate)) {
            Log.d(TAG, "Event date (" + endDate + ") has passed. Returning empty weekend forecast.");
            callback.onSuccess(new ArrayList<>());
            return;
        }

         */

        openMeteoAPIService.getWeatherForecast(
                latitude,
                longitude,
                null,
                "temperature_2m,precipitation_probability,weather_code",
                "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max",
                "best_match",
                "auto",
                startDate,
                endDate
        ).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                ResponseBody body = response.body();
                List<DailyForecast> dailyForecasts = new ArrayList<>();

                if (response.isSuccessful() && body != null) {
                    try {
                        String jsonStr = body.string();
                        JsonObject json = new Gson().fromJson(jsonStr, JsonObject.class);

                        if (json != null && !json.has("error")) {
                            Map<String, Map<String, HourlyForecast>> hourlyByDate = new HashMap<>();

                            if (json.has("hourly") && !json.get("hourly").isJsonNull()) {
                                JsonObject hourly = json.getAsJsonObject("hourly");
                                JsonArray hTime = hourly.getAsJsonArray("time");
                                JsonArray hTemp = hourly.getAsJsonArray("temperature_2m");
                                JsonArray hCode = hourly.getAsJsonArray("weather_code");
                                JsonArray hRain = hourly.getAsJsonArray("precipitation_probability");

                                if (hTime != null) {
                                    for (int i = 0; i < hTime.size(); i++) {
                                        String isoStr = hTime.get(i).getAsString();
                                        if (isoStr.contains("T")) {
                                            String[] parts = isoStr.split("T");
                                            String datePart = parts[0];
                                            String timePart = parts[1];

                                            if (!hourlyByDate.containsKey(datePart)) {
                                                hourlyByDate.put(datePart, new HashMap<>());
                                            }

                                            int tVal = (hTemp != null && i < hTemp.size()) ? (int) Math.round(hTemp.get(i).getAsDouble()) : 20;
                                            int cVal = (hCode != null && i < hCode.size()) ? hCode.get(i).getAsInt() : 0;
                                            int rVal = (hRain != null && i < hRain.size()) ? hRain.get(i).getAsInt() : 0;
                                            int iconRes = WeatherUtils.getWeatherIconResId(cVal, false);

                                            HourlyForecast hf = new HourlyForecast(timePart, iconRes, tVal, rVal);
                                            hourlyByDate.get(datePart).put(timePart, hf);
                                        }
                                    }
                                }
                            }

                            if (json.has("daily") && !json.get("daily").isJsonNull()) {
                                JsonObject daily = json.getAsJsonObject("daily");
                                JsonArray times = daily.getAsJsonArray("time");
                                JsonArray codes = daily.getAsJsonArray("weather_code");
                                JsonArray tempMaxs = daily.getAsJsonArray("temperature_2m_max");
                                JsonArray tempMins = daily.getAsJsonArray("temperature_2m_min");

                                SimpleDateFormat parseFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                                SimpleDateFormat dayNameFormat = new SimpleDateFormat("EEEE", Locale.ENGLISH);
                                String[] defaultDays = new String[]{"Friday", "Saturday", "Sunday"};

                                if (times != null && !times.isEmpty()) {
                                    for (int i = 0; i < Math.min(3, times.size()); i++) {
                                        String dateIso = times.get(i).getAsString();
                                        String dayName = defaultDays[i % 3];
                                        try {
                                            Date date = parseFormat.parse(dateIso);
                                            if (date != null) {
                                                dayName = dayNameFormat.format(date);
                                            }
                                        } catch (Exception ignored) {}

                                        int wCode = (codes != null && i < codes.size()) ? codes.get(i).getAsInt() : 0;
                                        int maxT = (tempMaxs != null && i < tempMaxs.size()) ? (int) Math.round(tempMaxs.get(i).getAsDouble()) : 25;
                                        int minT = (tempMins != null && i < tempMins.size()) ? (int) Math.round(tempMins.get(i).getAsDouble()) : 15;
                                        int iconRes = WeatherUtils.getWeatherIconResId(wCode, false);

                                        DailyForecast df = new DailyForecast();
                                        df.setDayName(dayName);
                                        df.setDateIso(dateIso);
                                        df.setWeatherIconResId(iconRes);
                                        df.setTempMax(maxT);
                                        df.setTempMin(minT);

                                        Map<String, HourlyForecast> dayHours = hourlyByDate.get(dateIso);
                                        if (dayHours != null) {
                                            HourlyForecast morning = dayHours.get("09:00");
                                            HourlyForecast afternoon = dayHours.get("14:00");
                                            HourlyForecast evening = dayHours.get("19:00");

                                            df.getHourlyForecasts().add(morning != null ? morning : new HourlyForecast("09:00", iconRes, minT + 2, 5));
                                            df.getHourlyForecasts().add(afternoon != null ? afternoon : new HourlyForecast("14:00", iconRes, maxT, 10));
                                            df.getHourlyForecasts().add(evening != null ? evening : new HourlyForecast("19:00", iconRes, minT + 4, 15));
                                        } else {
                                            df.getHourlyForecasts().add(new HourlyForecast("09:00", iconRes, minT + 2, 5));
                                            df.getHourlyForecasts().add(new HourlyForecast("14:00", iconRes, maxT, 10));
                                            df.getHourlyForecasts().add(new HourlyForecast("19:00", iconRes, minT + 4, 15));
                                        }

                                        dailyForecasts.add(df);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing weekend forecast response", e);
                    }
                }

                callback.onSuccess(dailyForecasts);
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.w(TAG, "Failed weekend forecast fetch: " + t.getMessage());
                callback.onFailure(new Exception(t));
            }
        });
    }

    private void fetchOpenF1TrackTemperatureOnly(WeatherInfo info, String sessionKey, CurrentWeatherCallback callback) {
        Call<ResponseBody> call;
        if (sessionKey != null && !"latest".equalsIgnoreCase(sessionKey)) {
            try {
                int sKeyInt = Integer.parseInt(sessionKey);
                call = openF1APIService.getWeather(sKeyInt);
            } catch (NumberFormatException e) {
                call = openF1APIService.getWeatherBySessionKey(sessionKey);
            }
        } else {
            call = openF1APIService.getWeatherBySessionKey("latest");
        }

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                ResponseBody body = response.body();
                if (body != null) {
                    try {
                        String jsonStr = body.string();
                        JsonArray array = new Gson().fromJson(jsonStr, JsonArray.class);
                        if (array != null && !array.isEmpty()) {
                            JsonObject lastSample = array.get(array.size() - 1).getAsJsonObject();
                            if (lastSample.has("track_temperature") && !lastSample.get("track_temperature").isJsonNull()) {
                                info.setTrackTemp(lastSample.get("track_temperature").getAsDouble());
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing OpenF1 track temperature", e);
                    }
                }
                callback.onSuccess(info);
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.w(TAG, "OpenF1 track temperature fetch failed: " + t.getMessage());
                callback.onSuccess(info);
            }
        });
    }

    private boolean isEventDatePassed(String endDate) {
        if (endDate == null || endDate.trim().isEmpty()) return false;
        try {
            SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            Date raceDate = sdfDate.parse(endDate);
            Date today = new Date();
            SimpleDateFormat cmpFormat = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
            if (raceDate != null && Integer.parseInt(cmpFormat.format(today)) > Integer.parseInt(cmpFormat.format(raceDate))) {
                return true;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error checking if event date passed", e);
        }
        return false;
    }
}
