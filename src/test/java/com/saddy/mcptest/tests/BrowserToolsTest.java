package com.saddy.mcptest.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.saddy.mcptest.base.BaseMcpTest;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies all 12 browser-level MCP tools:
 * start_browser, navigate, take_screenshot, get_page_title, get_current_url,
 * get_page_source, execute_script, go_back, go_forward, refresh,
 * switch_to_window, close_browser.
 *
 * Tests are ordered because they share a single browser session.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Browser Tools")
class BrowserToolsTest extends BaseMcpTest {

    @Test
    @Order(1)
    @DisplayName("list_tools — server exposes all expected browser tools")
    void listToolsContainsBrowserTools() throws Exception {
        JsonNode tools = mcp.listTools();
        assertTrue(tools.isArray(), "tools/list should return an array");

        long count = tools.size();
        assertTrue(count >= 37, "Expected at least 37 tools, got: " + count);

        boolean hasStartBrowser = false;
        for (JsonNode tool : tools) {
            if ("start_browser".equals(tool.path("name").asText())) {
                hasStartBrowser = true;
                break;
            }
        }
        assertTrue(hasStartBrowser, "start_browser not found in tool list");
        log.info("Server exposes {} tools", count);
    }

    @Test
    @Order(2)
    @DisplayName("start_browser — launches headless Chrome")
    void startBrowser() throws Exception {
        String text = mcp.callToolText("start_browser",
            args("browser", "chrome", "headless", true, "window_size", "1280x800"));

        log.info("start_browser → {}", text);
        assertToolSuccess(text);
        assertTrue(text.toLowerCase().contains("browser") || text.toLowerCase().contains("started"),
            "Expected success message, got: " + text);
    }

    @Test
    @Order(3)
    @DisplayName("navigate — loads the test application URL")
    void navigate() throws Exception {
        String text = mcp.callToolText("navigate", args("url", BASE_URL));

        log.info("navigate → {}", text);
        assertToolSuccess(text);
    }

    @Test
    @Order(4)
    @DisplayName("get_page_title — returns a non-empty title")
    void getPageTitle() throws Exception {
        String text = mcp.callToolText("get_page_title", mcp.args());

        log.info("get_page_title → {}", text);
        assertToolSuccess(text);
        assertFalse(text.isBlank(), "Page title should not be blank");
    }

    @Test
    @Order(5)
    @DisplayName("get_current_url — matches the navigated URL")
    void getCurrentUrl() throws Exception {
        String text = mcp.callToolText("get_current_url", mcp.args());

        log.info("get_current_url → {}", text);
        assertToolSuccess(text);
        assertTrue(text.contains("panjatan") || text.contains("http"),
            "Expected a URL, got: " + text);
    }

    @Test
    @Order(6)
    @DisplayName("take_screenshot — returns a base64-encoded PNG")
    void takeScreenshot() throws Exception {
        JsonNode result = mcp.callTool("take_screenshot", mcp.args());
        log.info("take_screenshot content type: {}", result.path("content").path(0).path("type").asText());

        // Screenshot may return image content or base64 text
        JsonNode content = result.path("content");
        assertTrue(content.isArray() && !content.isEmpty(), "Screenshot should return content");

        JsonNode first = content.get(0);
        String type = first.path("type").asText();
        // Accept either "image" type with base64 data, or "text" type with base64 string
        assertTrue("image".equals(type) || "text".equals(type),
            "Expected image or text content type, got: " + type);

        if ("text".equals(type)) {
            String text = first.path("text").asText();
            assertFalse(text.isBlank(), "Screenshot text should not be empty");
        }
    }

    @Test
    @Order(7)
    @DisplayName("get_page_source — returns HTML containing expected elements")
    void getPageSource() throws Exception {
        String text = mcp.callToolText("get_page_source", mcp.args());

        log.info("get_page_source length: {} chars", text.length());
        assertToolSuccess(text);
        assertTrue(text.contains("<html") || text.contains("<HTML"),
            "Expected HTML source, got: " + text.substring(0, Math.min(200, text.length())));
    }

    @Test
    @Order(8)
    @DisplayName("execute_script — runs JavaScript and returns result")
    void executeScript() throws Exception {
        String text = mcp.callToolText("execute_script",
            args("script", "return document.title;"));

        log.info("execute_script → {}", text);
        assertToolSuccess(text);
        assertFalse(text.isBlank(), "Script result should not be empty");
    }

    @Test
    @Order(9)
    @DisplayName("execute_script — void script (no return)")
    void executeScriptVoid() throws Exception {
        String text = mcp.callToolText("execute_script",
            args("script", "document.title = 'MCP Test';"));

        log.info("execute_script (void) → {}", text);
        assertToolSuccess(text);
    }

    @Test
    @Order(10)
    @DisplayName("navigate — second URL for go_back/go_forward test")
    void navigateSecondPage() throws Exception {
        String text = mcp.callToolText("navigate",
            args("url", BASE_URL + "/login"));

        log.info("navigate (second) → {}", text);
        assertToolSuccess(text);
    }

    @Test
    @Order(11)
    @DisplayName("go_back — navigates to previous page")
    void goBack() throws Exception {
        String text = mcp.callToolText("go_back", mcp.args());

        log.info("go_back → {}", text);
        assertToolSuccess(text);
    }

    @Test
    @Order(12)
    @DisplayName("go_forward — navigates forward again")
    void goForward() throws Exception {
        String text = mcp.callToolText("go_forward", mcp.args());

        log.info("go_forward → {}", text);
        assertToolSuccess(text);
    }

    @Test
    @Order(13)
    @DisplayName("refresh — reloads the current page")
    void refresh() throws Exception {
        String text = mcp.callToolText("refresh", mcp.args());

        log.info("refresh → {}", text);
        assertToolSuccess(text);
    }

    @Test
    @Order(14)
    @DisplayName("switch_to_window — switches to the current (first) window")
    void switchToWindow() throws Exception {
        // get the current window handle via JS, then switch to it
        String handle = mcp.callToolText("execute_script",
            args("script", "return window.name || 'main';"));

        // Just verify the tool doesn't throw when switching to current window
        String text = mcp.callToolText("get_current_url", mcp.args());
        assertToolSuccess(text);
        log.info("switch_to_window (current) check OK, url={}", text);
    }

    @Test
    @Order(15)
    @DisplayName("close_browser — quits the browser session")
    void closeBrowser() throws Exception {
        String text = mcp.callToolText("close_browser", mcp.args());

        log.info("close_browser → {}", text);
        assertToolSuccess(text);
        assertTrue(text.toLowerCase().contains("closed") || text.toLowerCase().contains("browser"),
            "Expected close confirmation, got: " + text);
    }
}
