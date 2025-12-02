package com.mcp.server.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class McpServerSecurityTest {

    @Autowired
    private McpServerSecurity mcpServerSecurity;

    @Test
    void testSecurityConfiguration() throws Exception {
        assertNotNull(mcpServerSecurity);
        SecurityFilterChain filterChain = mcpServerSecurity.filterChain(null);
        assertNotNull(filterChain);
    }
}

