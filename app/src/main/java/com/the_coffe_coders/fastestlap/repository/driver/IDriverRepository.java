package com.the_coffe_coders.fastestlap.repository.driver;

import com.the_coffe_coders.fastestlap.domain.driver.Driver;

import java.util.List;

public interface IDriverRepository {
    List<Driver> findDrivers();
}
