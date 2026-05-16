package com.saddy.mcptest.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.saddy.mcptest.client.McpStdioClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.saddy.mcptest.client.McpStdioClient.MAPPER;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Base class for all MCP tool tests.
 * Each concrete subclass gets its own MCP server process (isolated sessions).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseMcpTest {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected static final String BASE_URL = "https://panjatan.netlify.app";

    protected McpStdioClient mcp;

    @BeforeAll
    void startMcpServer() throws Exception {
        log.info("Starting MCP server: python -m selenium_mcp.server");
        mcp = new McpStdioClient("python", "-m", "selenium_mcp.server");
    }

    @AfterAll
    void stopMcpServer() {
        if (mcp != null) {
            log.info("Stopping MCP server");
            mcp.close();
        }
    }

    // ------------------------------------------------------------------ //
    //  Helpers                                                             //
    // ------------------------------------------------------------------ //

    /**
     * Build an ObjectNode from alternating key-value pairs.
     * Supported value types: String, Integer, Long, Boolean.
     */
    protected ObjectNode args(Object... keyValues) {
        ObjectNode node = MAPPER.createObjectNode();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            String key = (String) keyValues[i];
            Object val = keyValues[i + 1];
            if (val instanceof String s)       node.put(key, s);
            else if (val instanceof Integer n) node.put(key, n);
            else if (val instanceof Long l)    node.put(key, l);
            else if (val instanceof Boolean b) node.put(key, b);
            else                               node.put(key, val.toString());
        }
        return node;
    }

    /** Extract text from the first content item in a tool result. */
    protected String textOf(JsonNode result) {
        JsonNode content = result.path("content");
        if (content.isArray() && !content.isEmpty()) {
            return content.get(0).path("text").asText();
        }
        return result.asText();
    }

    /** Assert that a tool result text does not contain an error indicator. */
    protected void assertToolSuccess(String text) {
        String lower = text.toLowerCase();
        assertFalse(lower.contains("error:"), "Tool returned an error: " + text);
        assertFalse(lower.contains("exception"), "Tool threw an exception: " + text);
        assertFalse(lower.contains("traceback"), "Tool produced a traceback: " + text);
    }

    /** Assert that a tool result text contains the expected substring. */
    protected void assertContains(String text, String expected) {
        assertTrue(text.contains(expected),
            "Expected '" + expected + "' in tool response but got: " + text);
    }
}
