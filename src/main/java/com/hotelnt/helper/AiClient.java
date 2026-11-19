package com.hotelnt.helper;

import com.hotelnt.dto.ChatbotDtos.SuggestedListing;
import java.util.List;
import java.util.Optional;

public interface AiClient {

    Optional<String> generateReply(String userMessage, List<SuggestedListing> suggested);
}
