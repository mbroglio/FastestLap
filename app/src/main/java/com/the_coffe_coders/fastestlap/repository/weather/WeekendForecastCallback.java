package com.the_coffe_coders.fastestlap.repository.weather;

import com.the_coffe_coders.fastestlap.domain.weather.DailyForecast;

import java.util.List;

public interface WeekendForecastCallback {
    void onSuccess(List<DailyForecast> dailyForecasts);
    void onFailure(Exception e);
}
