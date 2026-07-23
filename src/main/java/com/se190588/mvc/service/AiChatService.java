package com.se190588.mvc.service;

import com.se190588.mvc.dto.AiChatDto;

public interface AiChatService {
    AiChatDto processUserMessage(String userMessage);
}
