package com.mcp.server.service;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class MathTools {

    @org.springaicommunity.mcp.annotation.McpTool(description = "Add two numbers")
    public double add(@org.springaicommunity.mcp.annotation.McpToolParam double a, @org.springaicommunity.mcp.annotation.McpToolParam double b) {
        return a + b;
    }

    @org.springaicommunity.mcp.annotation.McpTool(description = "Multiply two numbers")
    @PreAuthorize("hasAuthority('SCOPE_mcp:write')")
    public double multiply(@org.springaicommunity.mcp.annotation.McpToolParam double a, @org.springaicommunity.mcp.annotation.McpToolParam double b) {
        return a * b;
    }

    @org.springaicommunity.mcp.annotation.McpTool(description = "Subtract two numbers")
    public double subtract(@org.springaicommunity.mcp.annotation.McpToolParam double a, @org.springaicommunity.mcp.annotation.McpToolParam double b) {
        return a - b;
    }

    @org.springaicommunity.mcp.annotation.McpTool(description = "Divide two numbers")
    @PreAuthorize("hasAuthority('SCOPE_mcp:write')")
    public double divide(@org.springaicommunity.mcp.annotation.McpToolParam double a, @org.springaicommunity.mcp.annotation.McpToolParam double b) {
        if (b == 0) {
            throw new IllegalArgumentException("Division by zero is not allowed");
        }
        return a / b;
    }

    @org.springaicommunity.mcp.annotation.McpTool(description = "Calculate the power of a number")
    @PreAuthorize("hasAuthority('SCOPE_mcp:write')")
    public double power(@org.springaicommunity.mcp.annotation.McpToolParam double base, @org.springaicommunity.mcp.annotation.McpToolParam double exponent) {
        return Math.pow(base, exponent);
    }
}

