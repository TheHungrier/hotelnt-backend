package com.hotelnt.helper;

import com.hotelnt.common.error.*;
import com.hotelnt.domain.*;
import com.hotelnt.dto.*;
import com.hotelnt.dto.AdminDtos.*;
import com.hotelnt.dto.ChatbotDtos.*;
import com.hotelnt.dto.ConfigDtos.*;
import com.hotelnt.dto.HostDtos.*;
import com.hotelnt.dto.ListingDtos.*;
import com.hotelnt.dto.MediaDtos.*;
import com.hotelnt.repository.*;
import com.hotelnt.service.*;
import java.security.Principal;

public record AdminPrincipal(Integer accountId, String email, String role) implements Principal {
    @Override
    public String getName() {
        return email != null ? email : String.valueOf(accountId);
    }
}
