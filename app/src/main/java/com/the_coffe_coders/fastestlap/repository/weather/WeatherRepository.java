package com.the_coffe_coders.fastestlap.repository.weather;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.weather.DailyForecast;
import com.the_coffe_coders.fastestlap.domain.weather.WeatherInfo;
import com.the_coffe_coders.fastestlap.source.weather.OpenMeteoWeatherDataSource;
import com.the_coffe_coders.fastestlap.source.weather.WeatherDataSource;

import java.util.List;

public class WeatherRepository {
    private static final String TAG = "WeatherRepository";
    private static WeatherRepository instance;

    private final WeatherDataSource weatherDataSource;

    private WeatherRepository() {
        this.weatherDataSource = OpenMeteoWeatherDataSource.getInstance();
    }

    public WeatherRepository(WeatherDataSource weatherDataSource) {
        this.weatherDataSource = weatherDataSource != null ? weatherDataSource : OpenMeteoWeatherDataSource.getInstance();
    }

    public static synchronized WeatherRepository getInstance() {
        if (instance == null) {
            instance = new WeatherRepository();
        }
        return instance;
    }

    /**
     * Delegato alla DataSource per il recupero del meteo corrente.
     */
    public MutableLiveData<Result> getCurrentWeatherInfo(String locality, double latitude, double longitude, String sessionKey, boolean isSessionInProgress) {
        MutableLiveData<Result> liveData = new MutableLiveData<>();
        liveData.setValue(new Result.Loading("Fetching current weather data..."));

        weatherDataSource.getCurrentWeather(locality, latitude, longitude, sessionKey, isSessionInProgress, new CurrentWeatherCallback() {
            @Override
            public void onSuccess(WeatherInfo weatherInfo) {
                liveData.postValue(new Result.WeatherSuccess(weatherInfo));
            }

            @Override
            public void onFailure(Exception e) {
                liveData.postValue(new Result.Error(e != null ? e.getMessage() : "Failed to fetch current weather"));
            }
        });

        return liveData;
    }

    /**
     * Delegato alla DataSource per il recupero delle previsioni del weekend.
     */
    public MutableLiveData<Result> getWeekendForecast(double latitude, double longitude, String startDate, String endDate) {
        MutableLiveData<Result> liveData = new MutableLiveData<>();
        liveData.setValue(new Result.Loading("Fetching weekend forecast..."));

        weatherDataSource.getWeekendForecast(latitude, longitude, startDate, endDate, new WeekendForecastCallback() {
            @Override
            public void onSuccess(List<DailyForecast> dailyForecasts) {
                liveData.postValue(new Result.WeekendForecastSuccess(dailyForecasts));
            }

            @Override
            public void onFailure(Exception e) {
                liveData.postValue(new Result.Error(e != null ? e.getMessage() : "Failed to fetch weekend forecast"));
            }
        });

        return liveData;
    }
}
