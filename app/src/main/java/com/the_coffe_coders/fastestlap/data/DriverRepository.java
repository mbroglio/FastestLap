package com.the_coffe_coders.fastestlap.data;

import com.the_coffe_coders.fastestlap.domain.driver.Driver;

import java.util.List;

public class DriverRepository implements IDriverRepository{
    @Override
    public final List<Driver> findAll() {//TODO
        return List.of(
                new Driver("Max Pera", "ds", "ITA", 90, 900, 200, "20", "Ferrari", null),
                new Driver("Max Sroll", "ds", "ITA", 90, 900, 200, "20", "Ferrari", null)
        );
    }
}
