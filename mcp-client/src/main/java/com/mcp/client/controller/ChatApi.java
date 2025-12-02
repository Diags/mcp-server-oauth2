package com.mcp.client.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
public class ChatApi {
    
    private final ChatClient chatClient;

    // Injection via constructeur (Spring Boot 3.4+)
    public ChatApi(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/ask")
    public String ask(@RequestParam String question) {
        return chatClient.prompt()
                         .user(question)
                         .call()
                         .content();
    }
}