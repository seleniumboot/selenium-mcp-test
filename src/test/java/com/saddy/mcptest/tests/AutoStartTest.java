package com.saddy.mcptest.tests;

import com.saddy.mcptest.base.BaseMcpTest;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies auto-start behaviour: any tool that needs a browser
 * should spin up Chrome automatically without requiring an explicit
 * start_browser call first.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Auto-Start Browser")
class AutoStartTest extends BaseMcpTest {

    @AfterAll
    void closeBrowser() throws Exception {
        // close whatever was auto-started
        mcp.callToolText("close_browser", mcp.args());
    }

    @Test @Order(1)
    @DisplayName("navigate — auto-starts Chrome without explicit start_browser")
    void navigateAutoStartsBrowser() throws Exception {
        // No start_browser call — browser should launch automatically
        String text = mcp.callToolText("navigate", args("url", BASE_URL));

        log.info("navigate (auto-start) → {}", text);
        assertToolSuccess(text);
        assertContains(text, "Navigated");
    }

    @Test @Order(2)
    @DisplayName("get_page_title — browser is alive after auto-start")
    void pageTitleAfterAutoStart() throws Exception {
        String title = mcp.callToolText("get_page_title", mcp.args());

        log.info("get_page_title (auto-start) → {}", title);
        assertFalse(title.isBlank(), "Title should not be blank after auto-start");
    }

    @Test @Order(3)
    @DisplayName("get_session_log — auto-start is recorded as start_browser action")
    void autoStartIsRecorded() throws Exception {
        String sessionLog = mcp.callToolText("get_session_log", mcp.args());

        log.info("get_session_log → {}", sessionLog);
        assertContains(sessionLog, "start_browser");
    }

    @Test @Order(4)
    @DisplayName("start_browser — explicit call still works for configuration")
    void explicitStartBrowserStillWorks() throws Exception {
        // Even after auto-start, calling start_browser should restart with new config
        String text = mcp.callToolText("start_browser",
            args("browser", "chrome", "headless", true, "window_size", "1024x768"));

        log.info("start_browser (explicit) → {}", text);
        assertToolSuccess(text);
        assertContains(text, "Chrome");
    }
}
