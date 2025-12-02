package com.mcp.gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Contrôleur de fallback pour le Circuit Breaker
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/client")
    public Mono<String> clientFallback() {
        return Mono.just("MCP Client is currently unavailable. Please try again later.");
    }

    @GetMapping("/server")
    public Mono<String> serverFallback() {
        return Mono.just("MCP Server is currently unavailable. Please try again later.");
    }
}

