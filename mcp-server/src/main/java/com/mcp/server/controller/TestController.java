package com.mcp.server.controller;

import com.mcp.server.service.MathTools;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final MathTools mathTools;

    @GetMapping("/multiply")
    @PreAuthorize("hasAuthority('SCOPE_mcp:write')")
    public Double multiply(@RequestParam double a, @RequestParam double b) {
        return mathTools.multiply(a, b);
    }

    @GetMapping("/add")
    public Double add(@RequestParam double a, @RequestParam double b) {
        return mathTools.add(a, b);
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}

