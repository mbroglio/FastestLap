package com.the_coffe_coders.fastestlap.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import com.the_coffe_coders.fastestlap.domain.f1.constructor.Constructor;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class ConstructorTest {

    @Test
    public void testDriverIdsAndEquality() {
        Constructor ferrari = new Constructor();
        ferrari.setConstructorId("ferrari");
        ferrari.setName("Ferrari");
        assertNull(ferrari.getDriverOneId());
        assertNull(ferrari.getDriverTwoId());

        ferrari.setDrivers(Collections.singletonList("leclerc"));
        assertEquals("leclerc", ferrari.getDriverOneId());
        assertNull(ferrari.getDriverTwoId());

        ferrari.setDrivers(Arrays.asList("leclerc", "hamilton"));
        assertEquals("leclerc", ferrari.getDriverOneId());
        assertEquals("hamilton", ferrari.getDriverTwoId());

        Constructor same = new Constructor();
        same.setConstructorId("ferrari");
        Constructor other = new Constructor();
        other.setConstructorId("mclaren");

        assertEquals(ferrari, same);
        assertEquals(ferrari.hashCode(), same.hashCode());
        assertNotEquals(ferrari, other);
    }
}
