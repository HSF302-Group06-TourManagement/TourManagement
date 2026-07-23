package com.se190588.mvc.controller;

import com.se190588.mvc.dto.AiChatDto;
import com.se190588.mvc.service.AiChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    @Autowired
    private AiChatService aiChatService;

    @PostMapping("/chat")
    public ResponseEntity<AiChatDto> chat(@RequestBody AiChatDto request) {
        String userMessage = request != null ? request.getMessage() : "";
        AiChatDto response = aiChatService.processUserMessage(userMessage);
        return ResponseEntity.ok(response);
    }
}
