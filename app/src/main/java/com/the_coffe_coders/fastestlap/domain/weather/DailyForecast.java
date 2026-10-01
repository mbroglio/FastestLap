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
public class DailyForecast {
    private String dayName;
    private String dateIso;
    private int weatherIconResId;
    private int tempMax;
    private int tempMin;
    private List<HourlyForecast> hourlyForecasts = new ArrayList<>();
}
