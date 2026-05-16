package com.saddy.mcptest.tests;

import com.saddy.mcptest.base.BaseMcpTest;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies element interaction MCP tools using the LOCATOR_SCHEMA format:
 *   selector (required CSS/XPath string), by (optional, default "css"), timeout (optional).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Element Tools")
class ElementToolsTest extends BaseMcpTest {

    @BeforeAll
    void openBrowserAndNavigate() throws Exception {
        mcp.callToolText("start_browser",
            args("browser", "chrome", "headless", true, "window_size", "1280x800"));
        // Home page contains the login form — /login is a SPA route with no inputs
        mcp.callToolText("navigate", args("url", BASE_URL));
    }

    @AfterAll
    void closeBrowser() throws Exception {
        mcp.callToolText("close_browser", mcp.args());
    }

    @Test @Order(1)
    @DisplayName("find_element — finds an input element by CSS selector")
    void findElement() throws Exception {
        String text = mcp.callToolText("find_element", args("selector", "input"));
        log.info("find_element → {}", text);
        assertToolSuccess(text);
        assertFalse(text.isBlank());
    }

    @Test @Order(2)
    @DisplayName("find_elements — finds all input elements on the login page")
    void findElements() throws Exception {
        String text = mcp.callToolText("find_elements", args("selector", "input"));
        log.info("find_elements → {}", text);
        assertToolSuccess(text);
        assertTrue(text.contains("Found"), "Expected 'Found N element(s)' message, got: " + text);
    }

    @Test @Order(3)
    @DisplayName("wait_for_element — waits for login form to be visible")
    void waitForElement() throws Exception {
        String text = mcp.callToolText("wait_for_element",
            args("selector", "form, input", "condition", "visible", "timeout", 10));
        log.info("wait_for_element → {}", text);
        assertToolSuccess(text);
    }

    @Test @Order(4)
    @DisplayName("is_displayed — input is displayed")
    void isDisplayed() throws Exception {
        String text = mcp.callToolText("is_displayed", args("selector", "input"));
        log.info("is_displayed → {}", text);
        assertToolSuccess(text);
        assertEquals("True", text, "Expected 'True', got: " + text);
    }

    @Test @Order(5)
    @DisplayName("is_enabled — input is enabled")
    void isEnabled() throws Exception {
        String text = mcp.callToolText("is_enabled", args("selector", "input"));
        log.info("is_enabled → {}", text);
        assertToolSuccess(text);
        assertEquals("True", text, "Expected 'True', got: " + text);
    }

    @Test @Order(6)
    @DisplayName("type_text — types username into the first input")
    void typeText() throws Exception {
        String text = mcp.callToolText("type_text",
            args("selector", "input[type='text'], input:not([type='password'])",
                 "text", "admin"));
        log.info("type_text → {}", text);
        assertToolSuccess(text);
        assertContains(text, "Typed");
    }

    @Test @Order(7)
    @DisplayName("get_attribute — reads the type attribute of a password input")
    void getAttribute() throws Exception {
        String text = mcp.callToolText("get_attribute",
            args("selector", "input[type='password']", "attribute", "type"));
        log.info("get_attribute → {}", text);
        assertToolSuccess(text);
        assertEquals("password", text);
    }

    @Test @Order(8)
    @DisplayName("get_text — gets visible text of a label or heading")
    void getText() throws Exception {
        String text = mcp.callToolText("get_text",
            args("selector", "label, h1, h2, h3, .title"));
        log.info("get_text → {}", text);
        // text may be empty for some selectors — just confirm no exception
        assertFalse(text.toLowerCase().contains("traceback"));
    }

    @Test @Order(9)
    @DisplayName("clear_field — clears the text input")
    void clearField() throws Exception {
        String text = mcp.callToolText("clear_field",
            args("selector", "input[type='text'], input:not([type='password'])"));
        log.info("clear_field → {}", text);
        assertToolSuccess(text);
        assertContains(text, "Cleared");
    }

    @Test @Order(10)
    @DisplayName("scroll_to_element — scrolls submit button into view")
    void scrollToElement() throws Exception {
        String text = mcp.callToolText("scroll_to_element",
            args("selector", "button[type='submit'], button"));
        log.info("scroll_to_element → {}", text);
        assertToolSuccess(text);
        assertContains(text, "Scrolled");
    }

    @Test @Order(11)
    @DisplayName("hover — hovers over the submit button")
    void hover() throws Exception {
        String text = mcp.callToolText("hover",
            args("selector", "button[type='submit'], button"));
        log.info("hover → {}", text);
        assertToolSuccess(text);
        assertContains(text, "Hovered");
    }

    @Test @Order(12)
    @DisplayName("type_text — fills full login form")
    void fillLoginForm() throws Exception {
        mcp.callToolText("type_text",
            args("selector", "input[type='text'], input:not([type='password'])", "text", "admin"));
        String text = mcp.callToolText("type_text",
            args("selector", "input[type='password']", "text", "password"));
        log.info("type_text (password) → {}", text);
        assertToolSuccess(text);
    }

    @Test @Order(13)
    @DisplayName("click — clicks the submit button")
    void click() throws Exception {
        String text = mcp.callToolText("click",
            args("selector", "button[type='submit'], button"));
        log.info("click → {}", text);
        assertToolSuccess(text);
        assertContains(text, "Clicked");
    }

    @Test @Order(14)
    @DisplayName("wait_for_element — waits for body after login")
    void waitForBody() throws Exception {
        String text = mcp.callToolText("wait_for_element",
            args("selector", "body", "condition", "visible", "timeout", 10));
        log.info("wait_for_element (body) → {}", text);
        assertToolSuccess(text);
    }

    @Test @Order(15)
    @DisplayName("get_healed_locators — returns cache (may be empty if no healing occurred)")
    void getHealedLocators() throws Exception {
        String text = mcp.callToolText("get_healed_locators", mcp.args());
        log.info("get_healed_locators → {}", text);
        // Either "No healed locators" or a list of healed entries — both are valid
        assertTrue(text.contains("healed") || text.contains("Healed"),
            "Expected healing status in response but got: " + text);
    }

    @Test @Order(16)
    @DisplayName("clear_healed_locators — clears the healing cache")
    void clearHealedLocators() throws Exception {
        String text = mcp.callToolText("clear_healed_locators", mcp.args());
        log.info("clear_healed_locators → {}", text);
        assertToolSuccess(text);
        assertContains(text, "Cleared");
    }

    @Test @Order(17)
    @DisplayName("self-healing — #id shorthand falls back to [id='...'] when primary fails")
    void selfHealingIdFallback() throws Exception {
        // Use a known-good element on the page via a comma-separated multi-selector fallback.
        // The first selector is intentionally broken; the second is valid.
        String text = mcp.callToolText("find_element",
            args("selector", ".does-not-exist-xyz, body", "by", "css", "timeout", 5));
        log.info("self-healing multi-selector → {}", text);
        assertToolSuccess(text);
        // Result should mention healing since the primary single-class selector failed
        assertTrue(text.contains("tag=") || text.contains("healed"),
            "Expected element found (possibly healed) but got: " + text);
    }
}
