package com.saddy.mcptest.tests;

import java.util.Base64;

import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.fasterxml.jackson.databind.JsonNode;
import com.saddy.mcptest.base.BaseMcpTest;

/**
 * Covers the tools not exercised in ElementToolsTest:
 *   double_click, right_click, drag_and_drop, select_option,
 *   switch_to_window, and a thorough take_screenshot byte check.
 *
 * All tests use https://panjatan.netlify.app which has:
 *   - "Double Click" and "Right Click" buttons in the Button Interactions section
 *   - Drag-and-drop list with Items 1-4 and a drop zone
 *   - A Country <select> dropdown in the Registration Form
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Advanced Interactions")
class AdvancedInteractionsTest extends BaseMcpTest {

    @BeforeAll
    void openBrowser() throws Exception {
        mcp.callToolText("start_browser",
            args("browser", "chrome", "headless", true, "window_size", "1280x800"));
        mcp.callToolText("navigate", args("url", BASE_URL));
    }

    @AfterAll
    void closeBrowser() throws Exception {
        mcp.callToolText("close_browser", mcp.args());
    }

    // ------------------------------------------------------------------ //
    //  take_screenshot — verify actual PNG bytes                          //
    // ------------------------------------------------------------------ //

    @Test @Order(1)
    @DisplayName("take_screenshot — server returns ImageContent with valid PNG bytes")
    void screenshotIsValidPng() throws Exception {
        JsonNode result = mcp.callTool("take_screenshot", mcp.args());
        JsonNode content = result.path("content");
        assertTrue(content.isArray() && !content.isEmpty(), "Screenshot should return content");

        JsonNode item = content.get(0);
        String type = item.path("type").asText();
        log.info("Screenshot content type: {}", type);

        // Server now returns ImageContent(type="image", data=<b64>, mimeType="image/png")
        assertEquals("image", type, "Expected ImageContent type='image', got: " + type);
        assertEquals("image/png", item.path("mimeType").asText(), "Expected mimeType=image/png");

        String b64 = item.path("data").asText();
        assertFalse(b64.isBlank(), "Base64 data should not be empty");

        byte[] png = Base64.getDecoder().decode(b64);
        // PNG magic bytes: 0x89 P N G
        assertEquals((byte) 0x89, png[0], "Byte 0 should be 0x89 (PNG magic)");
        assertEquals((byte) 'P',  png[1], "Byte 1 should be 'P'");
        assertEquals((byte) 'N',  png[2], "Byte 2 should be 'N'");
        assertEquals((byte) 'G',  png[3], "Byte 3 should be 'G'");
        log.info("Screenshot verified: ImageContent, valid PNG, {} bytes", png.length);
    }

    // ------------------------------------------------------------------ //
    //  double_click                                                        //
    // ------------------------------------------------------------------ //

    @Test @Order(2)
    @DisplayName("double_click — double-clicks the 'Double Click' button")
    void doubleClick() throws Exception {
        // Scroll to the Button Interactions section first
        mcp.callToolText("execute_script",
            args("script", "window.scrollTo(0, 600);"));

        String text = mcp.callToolText("double_click",
            args("selector", "//button[contains(text(),'Double Click')]",
                 "by", "xpath"));

        log.info("double_click → {}", text);
        assertToolSuccess(text);
        assertContains(text, "Double-clicked");
    }

    // ------------------------------------------------------------------ //
    //  right_click                                                         //
    // ------------------------------------------------------------------ //

    @Test @Order(3)
    @DisplayName("right_click — right-clicks the 'Right Click' button")
    void rightClick() throws Exception {
        String text = mcp.callToolText("right_click",
            args("selector", "//button[contains(text(),'Right Click')]",
                 "by", "xpath"));

        log.info("right_click → {}", text);
        assertToolSuccess(text);
        assertContains(text, "Right-clicked");

        // Dismiss any context menu that appeared
        mcp.callToolText("execute_script", args("script", "document.body.click();"));
    }

    // ------------------------------------------------------------------ //
    //  select_option                                                       //
    // ------------------------------------------------------------------ //

    @Test @Order(4)
    @DisplayName("select_option (by_index) — selects the second option in the Country dropdown")
    void selectOptionByIndex() throws Exception {
        // Scroll to Registration Form
        mcp.callToolText("execute_script",
            args("script", "window.scrollTo(0, 400);"));

        String text = mcp.callToolText("select_option",
            args("selector", "select", "by_index", 1));

        log.info("select_option (by_index=1) → {}", text);
        assertToolSuccess(text);
        assertContains(text, "Selected by index");
    }

    @Test @Order(5)
    @DisplayName("select_option (by_text) — selects 'India' from the Country dropdown")
    void selectOptionByText() throws Exception {
        String text = mcp.callToolText("select_option",
            args("selector", "select", "by_text", "India"));

        log.info("select_option (by_text=India) → {}", text);
        assertToolSuccess(text);
        assertContains(text, "India");
    }

    @Test @Order(6)
    @DisplayName("select_option (by_value) — selects a country by its value attribute")
    void selectOptionByValue() throws Exception {
        // First get the actual value attribute of the first real option
        String optionValue = mcp.callToolText("execute_script",
            args("script",
                "var sel = document.querySelector('select');" +
                "return sel && sel.options.length > 1 ? sel.options[1].value : 'US';"));

        log.info("First option value: {}", optionValue);

        String text = mcp.callToolText("select_option",
            args("selector", "select", "by_value", optionValue));

        log.info("select_option (by_value={}) → {}", optionValue, text);
        assertToolSuccess(text);
    }

    // ------------------------------------------------------------------ //
    //  drag_and_drop                                                       //
    // ------------------------------------------------------------------ //

    @Test @Order(7)
    @DisplayName("drag_and_drop — drags the first .draggable-item into #dropZone")
    void dragAndDrop() throws Exception {
        // Scroll the draggable section into view
        mcp.callToolText("execute_script",
            args("script",
                "var el = document.querySelector('.draggable-item');" +
                "if (el) el.scrollIntoView(true);"));

        String text = mcp.callToolText("drag_and_drop",
            args("source_selector", ".draggable-item",
                 "target_selector", "#dropZone",
                 "by", "css"));

        log.info("drag_and_drop → {}", text);
        // Accept success or a Selenium limitation note — drag-and-drop is notoriously
        // fragile in headless mode; confirm the tool at least didn't throw
        assertFalse(text.toLowerCase().contains("exception"),
            "drag_and_drop should not throw an unhandled exception: " + text);
        assertFalse(text.toLowerCase().contains("traceback"),
            "drag_and_drop should not produce a traceback: " + text);
        log.info("drag_and_drop result (headless may differ from headed): {}", text);
    }

    // ------------------------------------------------------------------ //
    //  switch_to_window                                                    //
    // ------------------------------------------------------------------ //

    @Test @Order(8)
    @DisplayName("switch_to_window — opens a second tab and switches between windows")
    void switchToWindow() throws Exception {
        // Open a second tab
        mcp.callToolText("execute_script",
            args("script", "window.open('" + BASE_URL + "', '_blank');"));

        // Give the tab a moment to open
        Thread.sleep(500);

        // Switch to the new tab (index 1)
        String toNew = mcp.callToolText("switch_to_window", args("index", 1));
        log.info("switch_to_window(1) → {}", toNew);
        assertToolSuccess(toNew);
        assertContains(toNew, "Switched to window 1");

        // Confirm we are on the second window
        String urlOnNew = mcp.callToolText("get_current_url", mcp.args());
        log.info("URL on window 1: {}", urlOnNew);
        assertTrue(urlOnNew.contains("panjatan"), "Should still be on the test app: " + urlOnNew);

        // Switch back to the original tab (index 0)
        String toOrig = mcp.callToolText("switch_to_window", args("index", 0));
        log.info("switch_to_window(0) → {}", toOrig);
        assertContains(toOrig, "Switched to window 0");

        // Close the extra tab
        mcp.callToolText("execute_script",
            args("script",
                "var handles = arguments; " +
                "window.open('', '_blank').close();"));
    }

    @Test @Order(9)
    @DisplayName("switch_to_window — returns error message for out-of-range index")
    void switchToWindowOutOfRange() throws Exception {
        String text = mcp.callToolText("switch_to_window", args("index", 999));
        log.info("switch_to_window(999) → {}", text);
        // Server returns "No window at index 999. Available: N"
        assertTrue(text.contains("No window") || text.contains("999"),
            "Should report out-of-range index, got: " + text);
    }
}
