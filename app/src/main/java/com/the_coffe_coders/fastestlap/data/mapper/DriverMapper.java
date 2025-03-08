package com.the_coffe_coders.fastestlap.data.mapper;

import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.data.dto.DriverDTO;

public class DriverMapper {
    public static Driver toDriver(DriverDTO driverDTO) {
        Driver driver = new Driver();

        driver.setDriverId(driverDTO.getDriverId());
        driver.setCode(driverDTO.getCode());
        driver.setNationality(driverDTO.getNationality());
        driver.setUrl(driverDTO.getUrl());
        driver.setFamilyName(driverDTO.getFamilyName());
        driver.setDateOfBirth(driverDTO.getDateOfBirth());
        driver.setGivenName(driverDTO.getGivenName());
        driver.setPermanentNumber(driverDTO.getPermanentNumber());

        return driver;
    }
}
