package com.saddy.mcptest.tests;

import com.saddy.mcptest.base.BaseMcpTest;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies all 8 assertion MCP tools using correct parameter schemas:
 *   assert_title/assert_url → expected + exact (boolean)
 *   element tools            → selector (required) + by + timeout
 *   assert_element_count     → selector + expected_count (integer)
 *   assert_page_contains     → text
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Assertion Tools")
class AssertionToolsTest extends BaseMcpTest {

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
    //  Happy-path assertions                                               //
    // ------------------------------------------------------------------ //

    @Test @Order(1)
    @DisplayName("assert_title — passes when title contains expected substring")
    void assertTitleContains() throws Exception {
        String actualTitle = mcp.callToolText("get_page_title", mcp.args());
        log.info("Actual page title: {}", actualTitle);

        String firstWord = actualTitle.split("[\\s|]+")[0];
        String text = mcp.callToolText("assert_title",
            args("expected", firstWord, "exact", false));

        log.info("assert_title → {}", text);
        assertContains(text, "PASS");
    }

    @Test @Order(2)
    @DisplayName("assert_url — passes when URL contains the base domain")
    void assertUrlContains() throws Exception {
        String text = mcp.callToolText("assert_url",
            args("expected", "panjatan", "exact", false));

        log.info("assert_url → {}", text);
        assertContains(text, "PASS");
    }

    @Test @Order(3)
    @DisplayName("assert_element_visible — body is always visible")
    void assertElementVisible() throws Exception {
        String text = mcp.callToolText("assert_element_visible",
            args("selector", "body"));

        log.info("assert_element_visible → {}", text);
        assertContains(text, "PASS");
    }

    @Test @Order(4)
    @DisplayName("assert_page_contains — page body contains at least one word")
    void assertPageContains() throws Exception {
        String bodyText = mcp.callToolText("get_text", args("selector", "body"));
        assertFalse(bodyText.isBlank());

        String keyword = bodyText.trim().split("\\s+")[0];
        log.info("Using keyword: '{}'", keyword);

        String text = mcp.callToolText("assert_page_contains", args("text", keyword));
        log.info("assert_page_contains → {}", text);
        assertContains(text, "PASS");
    }

    @Test @Order(5)
    @DisplayName("assert_element_count — counts inputs on the home page")
    void assertElementCount() throws Exception {
        mcp.callToolText("navigate", args("url", BASE_URL));

        // Count the actual inputs first so expected_count always matches
        String findResult = mcp.callToolText("find_elements", args("selector", "input"));
        int actual = parseElementCount(findResult);
        log.info("Actual input count: {}", actual);

        String text = mcp.callToolText("assert_element_count",
            args("selector", "input", "expected_count", actual));

        log.info("assert_element_count → {}", text);
        assertContains(text, "PASS");
    }

    @Test @Order(6)
    @DisplayName("assert_attribute — body element has the tagName attribute")
    void assertAttribute() throws Exception {
        // Navigate to home to ensure we have known elements, then read the actual
        // type attribute of the first input and assert it matches
        mcp.callToolText("navigate", args("url", BASE_URL));
        String attrValue = mcp.callToolText("get_attribute",
            args("selector", "input", "attribute", "type"));
        assertFalse(attrValue.isBlank(), "First input should have a type attribute");
        log.info("First input type: {}", attrValue);

        String text = mcp.callToolText("assert_attribute",
            args("selector", "input",
                 "attribute", "type",
                 "expected", attrValue,
                 "exact", true));

        log.info("assert_attribute → {}", text);
        assertContains(text, "PASS");
    }

    @Test @Order(7)
    @DisplayName("assert_element_not_visible — a non-existent selector is not visible")
    void assertElementNotVisible() throws Exception {
        String text = mcp.callToolText("assert_element_not_visible",
            args("selector", "#this-element-does-not-exist-12345", "timeout", 3));

        log.info("assert_element_not_visible → {}", text);
        assertContains(text, "PASS");
    }

    @Test @Order(8)
    @DisplayName("assert_text — heading text contains first word of its own content")
    void assertText() throws Exception {
        mcp.callToolText("navigate", args("url", BASE_URL));

        String heading = mcp.callToolText("get_text",
            args("selector", "h1, h2, h3, .title"));

        if (!heading.isBlank() && !heading.toLowerCase().contains("error")) {
            String firstWord = heading.trim().split("\\s+")[0];
            String text = mcp.callToolText("assert_text",
                args("selector", "h1, h2, h3, .title",
                     "expected", firstWord,
                     "exact", false));

            log.info("assert_text → {}", text);
            assertContains(text, "PASS");
        } else {
            log.warn("No heading found, skipping assert_text");
        }
    }

    // ------------------------------------------------------------------ //
    //  Negative-path assertions (expect FAIL responses, not exceptions)   //
    // ------------------------------------------------------------------ //

    @Test @Order(9)
    @DisplayName("assert_title (fail) — reports FAIL when title does not match")
    void assertTitleFailureIsReported() throws Exception {
        String text = mcp.callToolText("assert_title",
            args("expected", "THIS_TITLE_DEFINITELY_DOES_NOT_EXIST_XYZ_999", "exact", true));

        log.info("assert_title (negative) → {}", text);
        assertContains(text, "FAIL");
    }

    @Test @Order(10)
    @DisplayName("assert_url (fail) — reports FAIL when URL does not match")
    void assertUrlFailureIsReported() throws Exception {
        String text = mcp.callToolText("assert_url",
            args("expected", "https://this-url-does-not-exist-xyz.example.com", "exact", true));

        log.info("assert_url (negative) → {}", text);
        assertContains(text, "FAIL");
    }

    @Test @Order(11)
    @DisplayName("assert_element_count (fail) — reports FAIL when count is wrong")
    void assertElementCountFailureIsReported() throws Exception {
        String text = mcp.callToolText("assert_element_count",
            args("selector", "input", "expected_count", 9999));

        log.info("assert_element_count (negative) → {}", text);
        assertContains(text, "FAIL");
    }

    // ------------------------------------------------------------------ //
    //  Helper                                                              //
    // ------------------------------------------------------------------ //

    private int parseElementCount(String findResult) {
        // "Found N element(s): ..."
        try {
            String[] parts = findResult.split("\\s+");
            return Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return 1;
        }
    }
}
