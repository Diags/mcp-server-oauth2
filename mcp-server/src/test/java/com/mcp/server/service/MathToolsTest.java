package com.mcp.server.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MathToolsTest {

    @Autowired
    private MathTools mathTools;

    @Test
    void testAdd() {
        double result = mathTools.add(5.0, 3.0);
        assertEquals(8.0, result, 0.001);
    }

    @Test
    void testSubtract() {
        double result = mathTools.subtract(10.0, 4.0);
        assertEquals(6.0, result, 0.001);
    }

    @Test
    @WithMockUser(authorities = "SCOPE_mcp:write")
    void testMultiply() {
        double result = mathTools.multiply(5.0, 4.0);
        assertEquals(20.0, result, 0.001);
    }

    @Test
    @WithMockUser(authorities = "SCOPE_mcp:write")
    void testDivide() {
        double result = mathTools.divide(10.0, 2.0);
        assertEquals(5.0, result, 0.001);
    }

    @Test
    @WithMockUser(authorities = "SCOPE_mcp:write")
    void testDivideByZero() {
        assertThrows(IllegalArgumentException.class, () -> {
            mathTools.divide(10.0, 0.0);
        });
    }

    @Test
    @WithMockUser(authorities = "SCOPE_mcp:write")
    void testPower() {
        double result = mathTools.power(2.0, 3.0);
        assertEquals(8.0, result, 0.001);
    }
}

