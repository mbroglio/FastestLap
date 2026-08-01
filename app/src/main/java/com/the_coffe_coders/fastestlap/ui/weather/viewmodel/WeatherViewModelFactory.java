package com.the_coffe_coders.fastestlap.ui.weather.viewmodel;

import android.app.Application;

import androidx.lifecycle.ViewModelProvider;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.repository.weather.WeatherRepository;

public class WeatherViewModelFactory implements ViewModelProvider.Factory {

    private final WeatherRepository weatherRepository;

    public WeatherViewModelFactory() {
        this.weatherRepository = WeatherRepository.getInstance();
    }

    @Override
    public <T extends androidx.lifecycle.ViewModel> T create(Class<T> modelClass) {
        if (modelClass.isAssignableFrom(WeatherViewModel.class)) {
            // Suppress unchecked cast warning
            @SuppressWarnings("unchecked")
            T viewModel = (T) new WeatherViewModel(weatherRepository);
            return viewModel;
        }else{
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}
