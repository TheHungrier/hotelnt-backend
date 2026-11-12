package com.hotelnt.dto;

import com.hotelnt.common.error.*;
import com.hotelnt.config.*;
import com.hotelnt.controller.*;
import com.hotelnt.domain.*;
import com.hotelnt.helper.*;
import com.hotelnt.repository.*;
import com.hotelnt.service.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hotelnt.common.error.AppException;
import com.hotelnt.common.error.ErrorCode;
import org.junit.jupiter.api.Test;

/** Test logic thuan, khong can Spring hay SQL Server. */
class MapBoundsTest {

    @Test void parseValidBounds() {
        MapBounds b = MapBounds.parse("12.45,109.3", "12.1, 109.1");
        assertEquals(12.45, b.neLat());
        assertEquals(109.3, b.neLng());
        assertEquals(12.1, b.swLat());
        assertEquals(109.1, b.swLng());
    }

    @Test void wrongFormatThrowsValidationError() {
        AppException e = assertThrows(AppException.class, () -> MapBounds.parse("abc", "12.1,109.1"));
        assertEquals(ErrorCode.VALIDATION_ERROR, e.getCode());
    }

    @Test void missingLongitudeThrowsValidationError() {
        AppException e = assertThrows(AppException.class, () -> MapBounds.parse("12.45,109.3", "12.1"));
        assertEquals(ErrorCode.VALIDATION_ERROR, e.getCode());
    }

    @Test void outOfRangeLatitudeThrowsValidationError() {
        AppException e = assertThrows(AppException.class, () -> MapBounds.parse("95,109.3", "12.1,109.1"));
        assertEquals(ErrorCode.VALIDATION_ERROR, e.getCode());
    }

    @Test void southWestLatGreaterThanNorthEastThrowsValidationError() {
        AppException e = assertThrows(AppException.class, () -> MapBounds.parse("12.1,109.3", "12.45,109.1"));
        assertEquals(ErrorCode.VALIDATION_ERROR, e.getCode());
    }
}