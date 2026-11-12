package com.hotelnt.helper;

import com.hotelnt.common.error.*;
import com.hotelnt.config.*;
import com.hotelnt.controller.*;
import com.hotelnt.domain.*;
import com.hotelnt.dto.*;
import com.hotelnt.repository.*;
import com.hotelnt.service.*;
import java.security.SecureRandom;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OtpCodeGeneratorTest {
    @Test
    void preservesLeadingZeroes() {
        SecureRandom random = mock(SecureRandom.class);
        when(random.nextInt(1_000_000)).thenReturn(7);

        assertEquals("000007", new OtpCodeGenerator(random).generate());
    }

    @Test
    void supportsUpperEndOfRange() {
        SecureRandom random = mock(SecureRandom.class);
        when(random.nextInt(1_000_000)).thenReturn(999_999);

        assertEquals("999999", new OtpCodeGenerator(random).generate());
    }
}