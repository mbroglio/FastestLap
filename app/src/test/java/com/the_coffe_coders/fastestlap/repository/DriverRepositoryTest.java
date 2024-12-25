package com.the_coffe_coders.fastestlap.repository;

import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.repository.driver.DriverRepository;

import junit.framework.TestCase;

import java.util.List;

public class DriverRepositoryTest extends TestCase {

    public void testFindAll() {
        DriverRepository driverRepository = new DriverRepository();
        /*List<Driver> drivers = driverRepository.findDrivers();
        System.out.println("Drivers:");
        for (Driver driver : drivers) {
            System.out.println(driver.toString());
        }
        assertNotNull(drivers);
        assertFalse(drivers.isEmpty());*/
        driverRepository.fetchDrivers(System.currentTimeMillis());
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}