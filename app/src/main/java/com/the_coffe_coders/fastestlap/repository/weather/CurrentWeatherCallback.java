package com.the_coffe_coders.fastestlap.repository.weather;

import com.the_coffe_coders.fastestlap.domain.weather.WeatherInfo;

public interface CurrentWeatherCallback {
    void onSuccess(WeatherInfo weatherInfo);
    void onFailure(Exception e);
}
