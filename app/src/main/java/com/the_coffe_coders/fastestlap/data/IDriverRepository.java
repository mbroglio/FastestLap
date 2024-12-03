package com.the_coffe_coders.fastestlap.data;

import com.the_coffe_coders.fastestlap.domain.driver.Driver;

import java.util.List;

public interface IDriverRepository {
    List<Driver> findAll();

    Driver find(String id);
}
