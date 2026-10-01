package com.the_coffe_coders.fastestlap.domain.weather;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class WeatherInfo {
    private String locality;
    private String dateString;
    private String weatherCondition;
    private int weatherIconResId;
    private int weatherVideoResId;
    private Double airTempCurrent;
    private Double airTempMin;
    private Double airTempMax;
    private Double trackTemp;
    private Double pressure;
    private Integer humidity;
    private Integer rainProbability;
    private String windDirection;
    private Double windSpeedKmH;
    private Double windGustKmH;
    private boolean weekendForecastAvailable = true;
    private List<DailyForecast> dailyForecasts = new ArrayList<>();
}
