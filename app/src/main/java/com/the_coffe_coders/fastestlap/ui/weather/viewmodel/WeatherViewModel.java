package com.the_coffe_coders.fastestlap.ui.weather.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.repository.weather.WeatherRepository;

public class WeatherViewModel extends ViewModel {
    private final WeatherRepository weatherRepository;

    public WeatherViewModel() {
        this(WeatherRepository.getInstance());
    }

    public WeatherViewModel(WeatherRepository weatherRepository) {
        this.weatherRepository = weatherRepository != null ? weatherRepository : WeatherRepository.getInstance();
    }

    public MutableLiveData<Result> getCurrentWeather(String locality, double latitude, double longitude, String sessionKey, boolean isSessionInProgress) {
        return weatherRepository.getCurrentWeatherInfo(locality, latitude, longitude, sessionKey, isSessionInProgress);
    }

    public MutableLiveData<Result> getWeekendForecast(double latitude, double longitude, String startDate, String endDate) {
        return weatherRepository.getWeekendForecast(latitude, longitude, startDate, endDate);
    }
}
