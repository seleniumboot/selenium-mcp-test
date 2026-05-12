package com.saddy.mcptest.tests;

import com.saddy.mcptest.base.BaseMcpTest;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the 5 code-generation MCP tools using correct parameter names:
 *   generate_java_testng/junit5 → test_name + package_name
 *   generate_python_test        → test_name + class_name
 *   get_session_log / clear_session_log → no parameters
 *
 * Records a short login flow before generating code.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Codegen Tools")
class CodegenToolsTest extends BaseMcpTest {

    private static final String GEN_PACKAGE = "com.saddy.generated";

    @BeforeAll
    void recordSession() throws Exception {
        mcp.callToolText("start_browser",
            args("browser", "chrome", "headless", true, "window_size", "1280x800"));
        mcp.callToolText("navigate", args("url", BASE_URL));
        mcp.callToolText("navigate", args("url", BASE_URL + "/login"));
        mcp.callToolText("type_text",
            args("selector", "input[type='text'], input:not([type='password'])", "text", "admin"));
        mcp.callToolText("type_text",
            args("selector", "input[type='password']", "text", "password"));
        mcp.callToolText("click",
            args("selector", "button[type='submit'], button"));
    }

    @AfterAll
    void closeBrowser() throws Exception {
        mcp.callToolText("close_browser", mcp.args());
    }

    // ------------------------------------------------------------------ //
    //  Session log                                                         //
    // ------------------------------------------------------------------ //

    @Test @Order(1)
    @DisplayName("get_session_log — returns recorded actions")
    void getSessionLog() throws Exception {
        String text = mcp.callToolText("get_session_log", mcp.args());
        log.info("get_session_log → {}", text);
        assertToolSuccess(text);
        assertFalse(text.isBlank());
        // navigate, type_text x2, and click were recorded
        assertContains(text, "navigate");
    }

    // ------------------------------------------------------------------ //
    //  Java TestNG generation                                              //
    // ------------------------------------------------------------------ //

    @Test @Order(2)
    @DisplayName("generate_java_testng — produces a TestNG skeleton with @Test and @BeforeMethod")
    void generateJavaTestNG() throws Exception {
        String code = mcp.callToolText("generate_java_testng",
            args("test_name", "GeneratedLoginTest", "package_name", GEN_PACKAGE));

        log.info("generate_java_testng ({} chars)\n{}", code.length(), code);
        assertToolSuccess(code);
        assertFalse(code.isBlank());
        assertContains(code, "import org.testng");
        assertContains(code, "@Test");
        assertContains(code, "@BeforeMethod");
        assertContains(code, "@AfterMethod");
        assertContains(code, "class GeneratedLoginTest");
        assertContains(code, "WebDriver");
    }

    @Test @Order(3)
    @DisplayName("generate_java_testng — embeds the navigated URLs in the test body")
    void generateJavaTestNGHasUrls() throws Exception {
        String code = mcp.callToolText("generate_java_testng",
            args("test_name", "UrlCheckTest", "package_name", GEN_PACKAGE));

        log.info("generate_java_testng (url check) {} chars", code.length());
        assertContains(code, "driver.get(");
        assertContains(code, "panjatan");
    }

    // ------------------------------------------------------------------ //
    //  Java JUnit 5 generation                                             //
    // ------------------------------------------------------------------ //

    @Test @Order(4)
    @DisplayName("generate_java_junit5 — produces a JUnit 5 skeleton with @BeforeEach")
    void generateJavaJUnit5() throws Exception {
        String code = mcp.callToolText("generate_java_junit5",
            args("test_name", "GeneratedLoginJUnit5Test", "package_name", GEN_PACKAGE));

        log.info("generate_java_junit5 ({} chars)\n{}", code.length(), code);
        assertToolSuccess(code);
        assertFalse(code.isBlank());
        assertContains(code, "import org.junit.jupiter.api");
        assertContains(code, "@Test");
        assertContains(code, "@BeforeEach");
        assertContains(code, "@AfterEach");
        assertContains(code, "class GeneratedLoginJUnit5Test");
        assertContains(code, "WebDriver");
    }

    // ------------------------------------------------------------------ //
    //  Python pytest generation                                            //
    // ------------------------------------------------------------------ //

    @Test @Order(5)
    @DisplayName("generate_python_test — produces a pytest skeleton")
    void generatePythonTest() throws Exception {
        String code = mcp.callToolText("generate_python_test",
            args("test_name", "test_login_flow", "class_name", "TestGeneratedLogin"));

        log.info("generate_python_test ({} chars)\n{}", code.length(), code);
        assertToolSuccess(code);
        assertFalse(code.isBlank());
        assertContains(code, "from selenium import webdriver");
        assertContains(code, "class TestGeneratedLogin");
        assertContains(code, "def test_login_flow");
        assertContains(code, "driver.get(");
    }

    // ------------------------------------------------------------------ //
    //  Session log management                                              //
    // ------------------------------------------------------------------ //

    @Test @Order(6)
    @DisplayName("clear_session_log — empties the log")
    void clearSessionLog() throws Exception {
        String clearResult = mcp.callToolText("clear_session_log", mcp.args());
        log.info("clear_session_log → {}", clearResult);
        assertContains(clearResult, "cleared");

        String logAfter = mcp.callToolText("get_session_log", mcp.args());
        log.info("get_session_log after clear → {}", logAfter);
        assertContains(logAfter, "empty");
    }

    @Test @Order(7)
    @DisplayName("generate_java_testng (empty session) — returns skeleton with no-op comment")
    void generateWithEmptySession() throws Exception {
        String code = mcp.callToolText("generate_java_testng",
            args("test_name", "EmptySessionTest", "package_name", GEN_PACKAGE));

        log.info("generate_java_testng (empty) {} chars", code.length());
        assertFalse(code.isBlank());
        // With no recorded actions, codegen emits the "No actions recorded" comment
        assertContains(code, "No actions recorded");
    }
}
