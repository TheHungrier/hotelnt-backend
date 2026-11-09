package com.hotelnt.modules.chatbot;

import com.hotelnt.modules.chatbot.ChatbotDtos.ChatMessageRequest;
import com.hotelnt.modules.chatbot.ChatbotDtos.ChatMessageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chatbot")
@Tag(name = "chatbot-controller")
public class ChatbotController {

    private final ChatbotService service;

    public ChatbotController(ChatbotService service) {
        this.service = service;
    }

    @PostMapping("/messages")
    public ChatMessageResponse sendMessage(@Valid @RequestBody ChatMessageRequest body) {
        return service.handleMessage(body);
    }
}
