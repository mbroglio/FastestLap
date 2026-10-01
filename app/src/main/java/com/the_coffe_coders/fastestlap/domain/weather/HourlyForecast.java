package com.the_coffe_coders.fastestlap.domain.weather;

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
public class HourlyForecast {
    private String timeLabel; // "09:00", "14:00", "19:00"
    private int weatherIconResId;
    private int temperature;
    private int rainProbability;
}
