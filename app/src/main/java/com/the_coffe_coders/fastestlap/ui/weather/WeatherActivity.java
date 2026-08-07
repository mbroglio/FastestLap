package com.the_coffe_coders.fastestlap.ui.weather;

import android.net.Uri;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.weather.DailyForecast;
import com.the_coffe_coders.fastestlap.domain.weather.HourlyForecast;
import com.the_coffe_coders.fastestlap.domain.weather.WeatherInfo;
import com.the_coffe_coders.fastestlap.ui.weather.viewmodel.WeatherViewModel;
import com.the_coffe_coders.fastestlap.util.ui.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ui.NavigationUtils;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class WeatherActivity extends AppCompatActivity {
    private static final String TAG = "WeatherActivity";

    private WeatherViewModel weatherViewModel;
    private VideoView videoView;
    private MaterialToolbar toolbar;
    private SwipeRefreshLayout weatherLayout;
    private View weatherMainContent;
    private LoadingScreen loadingScreen;
    private ViewGroup weekendForecastLayout;
    private TextView weatherNotAvailableText;

    private TextView todayDateText;
    private TextView weatherText;
    private ImageView weatherIcon;
    private TextView tempCurrentText;
    private TextView tempMinText;
    private TextView tempMaxText;
    private TextView trackTempText;
    private TextView humidityValueText;
    private TextView rainProbValueText;
    private TextView pressureValueText;
    private TextView windDirValueText;
    private TextView windSpeedValueText;
    private TextView windGustValueText;
    private TextView weekendForecastText;
    private String locality = "SILVERSTONE";
    private double latitude = 52.0786;
    private double longitude = -1.0169;
    private String sessionKey = "latest";
    private String startDate = null;
    private String endDate = null;
    private boolean isSessionInProgress = false;
    private int currentVideoResId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_weather);

        parseIntentExtras();
        start();
    }

    private void start() {
        weatherLayout = findViewById(R.id.weather_layout);
        weatherMainContent = findViewById(R.id.weather_main_content);

        loadingScreen = new LoadingScreen(getWindow().getDecorView(), this, weatherLayout, null);
        loadingScreen.showLoadingScreen(false);
        loadingScreen.updateProgress();

        initViews();
        setupToolbar();

        UIUtils.applyWindowInsets(weatherLayout);
        if (weatherLayout != null) {
            weatherLayout.setOnRefreshListener(() -> {
                start();
                weatherLayout.setRefreshing(false);
            });
        }

        weatherViewModel = new ViewModelProvider(this).get(WeatherViewModel.class);
        observeWeatherData();
    }

    private void parseIntentExtras() {
        if (getIntent() != null) {
            String loc = getIntent().getStringExtra("LOCALITY");
            if (loc != null && !loc.trim().isEmpty()) {
                locality = loc;
            }
            if (getIntent().hasExtra("LATITUDE")) {
                try {
                    latitude = Double.parseDouble(Objects.requireNonNull(getIntent().getStringExtra("LATITUDE")));
                } catch (Exception e) {
                    latitude = getIntent().getDoubleExtra("LATITUDE", 52.0786);
                }
            }
            if (getIntent().hasExtra("LONGITUDE")) {
                try {
                    longitude = Double.parseDouble(Objects.requireNonNull(getIntent().getStringExtra("LONGITUDE")));
                } catch (Exception e) {
                    longitude = getIntent().getDoubleExtra("LONGITUDE", -1.0169);
                }
            }
            String sKey = getIntent().getStringExtra("SESSION_KEY");
            if (sKey != null && !sKey.trim().isEmpty()) {
                sessionKey = sKey;
            }
            startDate = getIntent().getStringExtra("START_DATE");
            endDate = getIntent().getStringExtra("END_DATE");
            isSessionInProgress = getIntent().getBooleanExtra("IS_SESSION_IN_PROGRESS", false);
        }
    }

    private void initViews() {
        videoView = findViewById(R.id.video_view);
        toolbar = findViewById(R.id.topAppBar);
        weekendForecastLayout = findViewById(R.id.weekend_forecast_layout);
        weatherNotAvailableText = findViewById(R.id.weather_not_available_text);

        todayDateText = findViewById(R.id.today_date_text);
        weatherText = findViewById(R.id.weather_text);
        weatherIcon = findViewById(R.id.weather_icon);
        tempCurrentText = findViewById(R.id.temp_current_text);
        tempMinText = findViewById(R.id.temp_min_text);
        tempMaxText = findViewById(R.id.temp_max_text);
        trackTempText = findViewById(R.id.track_temp_text);
        humidityValueText = findViewById(R.id.humidity_value_text);
        rainProbValueText = findViewById(R.id.rain_prob_value_text);
        pressureValueText = findViewById(R.id.pressure_value_text);
        windDirValueText = findViewById(R.id.wind_dir_value_text);
        windSpeedValueText = findViewById(R.id.wind_speed_value_text);
        windGustValueText = findViewById(R.id.wind_gust_value_text);
        weekendForecastText = findViewById(R.id.weekend_forecast_text);
    }

    private void toggleRowExpansion(View container) {
        if (weekendForecastLayout != null) {
            TransitionManager.beginDelayedTransition(weekendForecastLayout);
        }
        if (container.getVisibility() == View.VISIBLE) {
            container.setVisibility(View.GONE);
        } else {
            container.setVisibility(View.VISIBLE);
        }
    }

    private void setupToolbar() {
        if (toolbar != null) {
            TextView titleTv = findViewById(R.id.topAppBarTitle);
            if (titleTv != null) {
                UIUtils.singleSetTextViewText(locality.toUpperCase(Locale.ROOT), titleTv);
            } else {
                toolbar.setTitle(locality.toUpperCase(Locale.ROOT));
            }
            toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

            UIUtils.applyWindowInsets(toolbar);

            UIUtils.applyWindowInsets(weatherLayout);

            toolbar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_open_google_weather) {
                    NavigationUtils.openGoogleWeather(this, locality);
                    return true;
                }
                return false;
            });
        }


    }

    private void observeWeatherData() {
        // CHIAMATA 1: Recupera il meteo corrente e popola gli elementi principali della pagina
        weatherViewModel.getCurrentWeather(locality, latitude, longitude, sessionKey, isSessionInProgress).observe(this, result -> {
            if (result instanceof Result.WeatherSuccess) {
                WeatherInfo info = ((Result.WeatherSuccess) result).getData();
                if (info != null) {
                    updateCurrentWeatherUI(info);
                }
            } else if (result instanceof Result.Error) {
                Log.e(TAG, "Error fetching current weather: " + result.getError());
                observeWeatherDataForecast();
            }
        });


    }

    private void updateCurrentWeatherUI(WeatherInfo info) {
        printWeatherInfoToConsole(info);

        UIUtils.multipleSetTextViewText(
                new String[]{
                        info.getDateString(),
                        info.getWeatherCondition(),
                        Math.round(info.getAirTempCurrent()) + "°",
                        "MIN: " + Math.round(info.getAirTempMin()) + "°",
                        "MAX: " + Math.round(info.getAirTempMax()) + "°",
                        info.getHumidity() + "%",
                        info.getRainProbability() + "%",
                        Math.round(info.getPressure()) + " hPa",
                        info.getWindDirection(),
                        Math.round(info.getWindSpeedKmH()) + " km/h",
                        Math.round(info.getWindGustKmH()) + " km/h",
                },
                new TextView[]{
                        todayDateText,
                        weatherText,
                        tempCurrentText,
                        tempMinText,
                        tempMaxText,
                        humidityValueText,
                        rainProbValueText,
                        pressureValueText,
                        windDirValueText,
                        windSpeedValueText,
                        windGustValueText,
                }
        );

        if(info.getTrackTemp() != null){
            UIUtils.singleSetTextViewText(Math.round(info.getTrackTemp()) + "°", trackTempText);
        }else{
            UIUtils.singleSetTextViewText((Math.round(info.getAirTempCurrent()) + 8) + "°", trackTempText);
        }

        if (weatherIcon != null && info.getWeatherIconResId() != 0) {
            weatherIcon.setImageResource(info.getWeatherIconResId());
        }

        playBackgroundVideo(info.getWeatherVideoResId() != 0 ? info.getWeatherVideoResId() : R.raw.sun_weather_video);

        observeWeatherDataForecast();
    }

    private void observeWeatherDataForecast() {
        // CHIAMATA 2: Recupera la tabella delle previsioni del weekend di gara
        weatherViewModel.getWeekendForecast(latitude, longitude, startDate, endDate).observe(this, result -> {
            if (result instanceof Result.WeekendForecastSuccess) {
                List<DailyForecast> forecasts = ((Result.WeekendForecastSuccess) result).getData();
                if (forecasts != null && !forecasts.isEmpty()) {
                    // Forecast disponibile: mostra la tabella
                    showWeekendForecast(forecasts);
                } else {
                    // Lista vuota: l'evento è già passato, nascondi la sezione forecast
                    hideWeather();
                    loadingScreen.hideLoadingScreen();
                }
            } else if (result instanceof Result.Error) {
                String errorMsg = result.getError();
                if ("FORECAST_NOT_AVAILABLE_YET".equals(errorMsg)) {
                    // 404: l'evento è troppo lontano nel futuro (oltre ~16 giorni)
                    Log.d(TAG, "Weekend forecast not yet available — event too far in the future.");
                } else {
                    // Errore di rete o server: mostra messaggio generico
                    Log.e(TAG, "Error fetching weekend forecast: " + errorMsg);
                }
                showWeatherNotAvailable();
                loadingScreen.hideLoadingScreen();
            }
        });
    }


    private void showWeatherNotAvailable() {
        if (weekendForecastLayout != null) weekendForecastLayout.setVisibility(View.GONE);
        if (weatherNotAvailableText != null) weatherNotAvailableText.setVisibility(View.VISIBLE);
        UIUtils.singleSetTextViewText(getString(R.string.separator_high_dash), trackTempText);
    }

    private void hideWeather() {
        if (weekendForecastLayout != null) weekendForecastLayout.setVisibility(View.GONE);
        if (weatherNotAvailableText != null) weatherNotAvailableText.setVisibility(View.GONE);
        if (weekendForecastText != null) weekendForecastText.setVisibility(View.GONE);
        UIUtils.singleSetTextViewText(getString(R.string.separator_high_dash), trackTempText);
    }

    private void showWeekendForecast(List<DailyForecast> forecasts) {
        if (weatherNotAvailableText != null) weatherNotAvailableText.setVisibility(View.GONE);
        if (weekendForecastLayout != null) weekendForecastLayout.setVisibility(View.VISIBLE);

        // Rimuovi eventuali container orari dinamici aggiunti in precedenza
        if (weekendForecastLayout != null) {
            for (int i = weekendForecastLayout.getChildCount() - 1; i >= 0; i--) {
                View child = weekendForecastLayout.getChildAt(i);
                if (child.getId() == R.id.day_hourly_container) {
                    weekendForecastLayout.removeViewAt(i);
                }
            }
        }

        int[] rowIds = new int[]{R.id.day_1_row, R.id.day_2_row, R.id.day_3_row};
        int[] textIds = new int[]{R.id.day_1_text, R.id.day_2_text, R.id.day_3_text};
        int[] dateIds = new int[]{R.id.day_1_date, R.id.day_2_date, R.id.day_3_date};
        int[] iconIds = new int[]{R.id.day_1_weather_icon, R.id.day_2_weather_icon, R.id.day_3_weather_icon};
        int[] maxIds = new int[]{R.id.day_1_temp_max, R.id.day_2_temp_max, R.id.day_3_temp_max};
        int[] minIds = new int[]{R.id.day_1_temp_min, R.id.day_2_temp_min, R.id.day_3_temp_min};

        for (int i = 0; i < Math.min(forecasts.size(), 3); i++) {
            DailyForecast df = forecasts.get(i);
            RelativeLayout dayRow = findViewById(rowIds[i]);
            TextView dayTv = findViewById(textIds[i]);
            TextView dateTv = findViewById(dateIds[i]);
            ImageView iconIv = findViewById(iconIds[i]);
            TextView maxTv = findViewById(maxIds[i]);
            TextView minTv = findViewById(minIds[i]);

            if (dayTv != null && df.getDayName() != null) {
                UIUtils.singleSetTextViewText(df.getDayName(), dayTv);
            }

            if (dateTv != null) {
                String dayNumber = "";
                if (df.getDateIso() != null && df.getDateIso().contains("-")) {
                    try {
                        String[] parts = df.getDateIso().split("-");
                        dayNumber = String.valueOf(Integer.parseInt(parts[2]));
                    } catch (Exception e) {
                        dayNumber = "";
                    }
                }
                UIUtils.singleSetTextViewText(dayNumber, dateTv);
            }

            if (iconIv != null && df.getWeatherIconResId() != 0) iconIv.setImageResource(df.getWeatherIconResId());
            if (maxTv != null) UIUtils.singleSetTextViewText(df.getTempMax() + "°", maxTv);
            if (minTv != null) UIUtils.singleSetTextViewText(df.getTempMin() + "°", minTv);

            if (dayRow != null && weekendForecastLayout != null) {
                View hourlyContainer = getLayoutInflater().inflate(R.layout.daily_weather_container, weekendForecastLayout, false);
                hourlyContainer.setVisibility(View.GONE);

                List<HourlyForecast> hourlyList = df.getHourlyForecasts();
                if (hourlyList != null && hourlyList.size() >= 3) {
                    bindHourlySlotDynamic(hourlyContainer, hourlyList.get(0), R.id.morning_icon, R.id.morning_temp, R.id.morning_rain);
                    bindHourlySlotDynamic(hourlyContainer, hourlyList.get(1), R.id.afternoon_icon, R.id.afternoon_temp, R.id.afternoon_rain);
                    bindHourlySlotDynamic(hourlyContainer, hourlyList.get(2), R.id.evening_icon, R.id.evening_temp, R.id.evening_rain);
                }

                int index = weekendForecastLayout.indexOfChild(dayRow);
                weekendForecastLayout.addView(hourlyContainer, index + 1);

                dayRow.setOnClickListener(v -> toggleRowExpansion(hourlyContainer));
            }
        }

        loadingScreen.hideLoadingScreen();
    }

    private void bindHourlySlotDynamic(View container, HourlyForecast hf, int iconId, int tempId, int rainId) {
        if (container == null || hf == null) return;
        ImageView iconIv = container.findViewById(iconId);
        TextView tempTv = container.findViewById(tempId);
        TextView rainTv = container.findViewById(rainId);

        if (iconIv != null && hf.getWeatherIconResId() != 0) {
            iconIv.setImageResource(hf.getWeatherIconResId());
        }
        if (tempTv != null) {
            UIUtils.singleSetTextViewText(hf.getTemperature() + "°", tempTv);
        }
        if (rainTv != null) {
            UIUtils.singleSetTextViewText(hf.getRainProbability() + "%", rainTv);
        }
    }

    private void playBackgroundVideo(int videoResId) {
        if (videoView == null) return;
        if (currentVideoResId == videoResId && videoView.isPlaying()) return;
        currentVideoResId = videoResId;

        String videoPath = "android.resource://" + getPackageName() + "/" + videoResId;
        Uri uri = Uri.parse(videoPath);
        videoView.setVideoURI(uri);
        videoView.setOnPreparedListener(mp -> {
            mp.setLooping(true);
            videoView.start();
        });
    }

    private void printWeatherInfoToConsole(WeatherInfo info) {
        if (info == null) return;
        Log.d(TAG, "========== CURRENT WEATHER RETRIEVED ==========");
        Log.d(TAG, "Locality: " + info.getLocality());
        Log.d(TAG, "Date: " + info.getDateString());
        Log.d(TAG, "Condition: " + info.getWeatherCondition());
        Log.d(TAG, "Current Air Temp: " + info.getAirTempCurrent() + "°C");
        Log.d(TAG, "Min Air Temp: " + info.getAirTempMin() + "°C");
        Log.d(TAG, "Max Air Temp: " + info.getAirTempMax() + "°C");
        Log.d(TAG, "Track Temp: " + info.getTrackTemp() + "°C");
        Log.d(TAG, "Humidity: " + info.getHumidity() + "%");
        Log.d(TAG, "Rain Probability: " + info.getRainProbability() + "%");
        Log.d(TAG, "Pressure: " + info.getPressure() + " hPa");
        Log.d(TAG, "Wind Direction: " + info.getWindDirection());
        Log.d(TAG, "Wind Speed: " + info.getWindSpeedKmH() + " km/h");
        Log.d(TAG, "Wind Gust: " + info.getWindGustKmH() + " km/h");
        Log.d(TAG, "===============================================");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (videoView != null && currentVideoResId != -1) {
            String videoPath = "android.resource://" + getPackageName() + "/" + currentVideoResId;
            Uri uri = Uri.parse(videoPath);
            videoView.setVideoURI(uri);
            videoView.setOnPreparedListener(mp -> {
                mp.setLooping(true);
                videoView.start();
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView != null && videoView.isPlaying()) {
            videoView.pause();
        }
    }
}