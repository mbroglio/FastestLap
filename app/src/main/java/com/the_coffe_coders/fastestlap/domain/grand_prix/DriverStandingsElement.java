package com.the_coffe_coders.fastestlap.domain.grand_prix;

import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

//@Entity(tableName = "DriverStandingsElement")
@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DriverStandingsElement {
    private String position;
    private String positionText;
    private String points;
    private Driver driver;
    private String wins;
    private List<Constructor> constructors;

    public DriverStandingsElement(Driver driver) {
        position = "0";
        positionText = "0";
        points = "0";
        this.driver = driver;
    }


    public Driver getDriver() {
        return driver;
    }
}
