package com.the_coffe_coders.fastestlap.data;

import com.the_coffe_coders.fastestlap.domain.driver.Driver;

import junit.framework.TestCase;

import java.util.List;

public class JolpicaDriverRepositoryTest extends TestCase {

    public void testFindAll() {
        JolpicaDriverRepository jolpicaDriverRepository = new JolpicaDriverRepository();
        List<Driver> drivers = jolpicaDriverRepository.findDrivers();
        System.out.println("Drivers:");
        for (Driver driver : drivers) {
            System.out.println(driver.toString());
        }
        assertNotNull(drivers);
        assertFalse(drivers.isEmpty());
    }
}