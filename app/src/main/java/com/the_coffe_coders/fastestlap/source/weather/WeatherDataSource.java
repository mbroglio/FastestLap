package com.the_coffe_coders.fastestlap.source.weather;

import com.the_coffe_coders.fastestlap.repository.weather.CurrentWeatherCallback;
import com.the_coffe_coders.fastestlap.repository.weather.WeekendForecastCallback;

public interface WeatherDataSource {
    void getCurrentWeather(String locality, double latitude, double longitude, String sessionKey, boolean isSessionInProgress, CurrentWeatherCallback callback);
    void getWeekendForecast(double latitude, double longitude, String startDate, String endDate, WeekendForecastCallback callback);
}
