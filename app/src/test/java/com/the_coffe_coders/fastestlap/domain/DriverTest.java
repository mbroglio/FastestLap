package com.the_coffe_coders.fastestlap.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.the_coffe_coders.fastestlap.domain.f1.driver.Driver;

import org.junit.Test;

public class DriverTest {

    @Test
    public void testFullNameAgeAndEquality() {
        Driver driver = new Driver();
        driver.setDriverId("max_verstappen");
        driver.setGivenName("Max");
        driver.setFamilyName("Verstappen");
        driver.setDateOfBirth("30/09/1997");

        assertEquals("Max Verstappen", driver.getFullName());
        assertTrue(driver.getDriverAge() >= 27);
        assertEquals(String.valueOf(driver.getDriverAge()), driver.getDriverAgeAsString());

        Driver same = new Driver();
        same.setDriverId("max_verstappen");
        same.setGivenName("Max");
        same.setFamilyName("Verstappen");
        same.setDateOfBirth("30/09/1997");

        Driver other = new Driver();
        other.setDriverId("leclerc");

        assertEquals(driver, same);
        assertEquals(driver.hashCode(), same.hashCode());
        assertNotEquals(driver, other);
    }
}
