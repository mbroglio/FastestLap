package com.the_coffe_coders.fastestlap.domain.driver;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DriverHistory {
    private String podiums;
    private String points;
    private String position;
    private String team;
    private String wins;
    private String year;
}
