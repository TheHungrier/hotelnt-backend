package com.hotelnt.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GeoUtilsTest {
    @Test
    void samePointIsZero() {
        assertEquals(0.0, GeoUtils.distanceKm(12.24, 109.19, 12.24, 109.19), 1e-9);
    }

    @Test
    void oneDegreeLatitudeIsAbout111Km() {
        assertEquals(111.19, GeoUtils.distanceKm(12.0, 109.0, 13.0, 109.0), 0.1);
    }
}