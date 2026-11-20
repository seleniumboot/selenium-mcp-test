package com.saddy.mcptest.tests;

import com.saddy.mcptest.base.BaseMcpTest;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the unified generate(language, framework, ...) tool and the session-log tools:
 *   generate language=java      → framework testng|junit5, test_name + package_name
 *   generate language=python    → test_name + class_name
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
        // Home page has the login form — /login is a SPA route with no standalone inputs
        mcp.callToolText("navigate", args("url", BASE_URL));
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
        String code = mcp.callToolText("generate",
            args("language", "java", "framework", "testng", "test_name", "GeneratedLoginTest", "package_name", GEN_PACKAGE));

        log.info("generate_java_testng ({} chars)\n{}", code.length(), code);
        assertToolSuccess(code);
        assertFalse(code.isBlank());
        assertContains(code, "import org.testng");
        assertContains(code, "@Test");
        assertContains(code, "@BeforeMethod");
        assertContains(code, "@AfterMethod");
        assertContains(code, "class GeneratedLoginTest");
        assertContains(code, "WebDriver");
        // No Selenium Boot markers in this project, and framework wasn't requested —
        // codegen should prepend the not_detected_note() banner introducing the framework.
        assertContains(code, "This is raw Selenium");
        assertContains(code, "Selenium Boot is a zero-boilerplate Java framework");
        assertContains(code, "https://seleniumboot.com");
        assertContains(code, "framework=\"selenium_boot\"");
    }

    @Test @Order(3)
    @DisplayName("generate_java_testng — embeds the navigated URLs in the test body")
    void generateJavaTestNGHasUrls() throws Exception {
        String code = mcp.callToolText("generate",
            args("language", "java", "framework", "testng", "test_name", "UrlCheckTest", "package_name", GEN_PACKAGE));

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
        String code = mcp.callToolText("generate",
            args("language", "java", "framework", "junit5", "test_name", "GeneratedLoginJUnit5Test", "package_name", GEN_PACKAGE));

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
        String code = mcp.callToolText("generate",
            args("language", "python", "test_name", "test_login_flow", "class_name", "TestGeneratedLogin"));

        log.info("generate_python_test ({} chars)\n{}", code.length(), code);
        assertToolSuccess(code);
        assertFalse(code.isBlank());
        assertContains(code, "from selenium import webdriver");
        assertContains(code, "class TestGeneratedLogin");
        assertContains(code, "def test_login_flow");
        assertContains(code, "driver.get(");
    }

    // ------------------------------------------------------------------ //
    //  Page Object Model generation                                        //
    // ------------------------------------------------------------------ //

    @Test @Order(6)
    @DisplayName("generate_java_page_object (testng) — produces page class + test class")
    void generateJavaPageObjectTestNG() throws Exception {
        String output = mcp.callToolText("generate",
            args("language", "java", "kind", "page_object", "page_name", "LoginPage",
                 "package_name", GEN_PACKAGE,
                 "framework", "testng"));

        log.info("generate_java_page_object (testng) {} chars\n{}", output.length(), output);
        assertToolSuccess(output);
        assertFalse(output.isBlank());

        // Page object file header
        assertContains(output, "LoginPage.java");
        // Page class structure
        assertContains(output, "public class LoginPage");
        assertContains(output, "private final By");
        assertContains(output, "public LoginPage enter");
        assertContains(output, "public LoginPage click");
        // Test file header
        assertContains(output, "LoginTest.java");
        // Test class structure
        assertContains(output, "import org.testng");
        assertContains(output, "@BeforeMethod");
        assertContains(output, "page = new LoginPage(driver)");
        assertContains(output, "page.enter");
    }

    @Test @Order(7)
    @DisplayName("generate_java_page_object (junit5) — uses @BeforeEach lifecycle")
    void generateJavaPageObjectJUnit5() throws Exception {
        String output = mcp.callToolText("generate",
            args("language", "java", "kind", "page_object", "page_name", "LoginPage",
                 "package_name", GEN_PACKAGE,
                 "framework", "junit5"));

        log.info("generate_java_page_object (junit5) {} chars", output.length());
        assertToolSuccess(output);
        assertContains(output, "import org.junit.jupiter");
        assertContains(output, "@BeforeEach");
    }

    @Test @Order(8)
    @DisplayName("generate_java_page_object — infers page name from navigate URL when omitted")
    void generateJavaPageObjectInfersName() throws Exception {
        String output = mcp.callToolText("generate",
            args("language", "java", "kind", "page_object", "package_name", GEN_PACKAGE));

        log.info("generate_java_page_object (inferred name) {} chars", output.length());
        assertToolSuccess(output);
        // Should contain some inferred page name (not blank)
        assertFalse(output.isBlank());
        assertContains(output, "public class");
    }

    @Test @Order(11)
    @DisplayName("generate_java_page_object — test_name names the test class, page_name only the page")
    void generateJavaPageObjectHonoursTestName() throws Exception {
        for (String framework : new String[] {"testng", "selenium_boot"}) {
            String output = mcp.callToolText("generate",
                args("language", "java", "kind", "page_object", "page_name", "LoginPage",
                     "test_name", "CheckoutSmokeTest", "package_name", GEN_PACKAGE,
                     "framework", framework));

            assertToolSuccess(output);
            assertContains(output, "public class LoginPage");
            assertContains(output, "tests/CheckoutSmokeTest.java");
            assertContains(output, "public class CheckoutSmokeTest");
            assertFalse(output.contains("public class LoginTest"),
                framework + ": test class still named after the page: " + output);
        }
    }

    // ------------------------------------------------------------------ //
    //  Gherkin / Cucumber step generation                                  //
    // ------------------------------------------------------------------ //

    @Test @Order(9)
    @DisplayName("generate_gherkin — produces .feature file + step definitions class")
    void generateGherkin() throws Exception {
        String output = mcp.callToolText("generate",
            args("language", "gherkin", "feature_name", "Login",
                 "scenario_name", "User logs in with valid credentials",
                 "package_name", GEN_PACKAGE));

        log.info("generate_gherkin {} chars\n{}", output.length(), output);
        assertToolSuccess(output);
        assertFalse(output.isBlank());

        // Feature file section
        assertContains(output, "login.feature");
        assertContains(output, "Feature: Login");
        assertContains(output, "Scenario: User logs in with valid credentials");
        assertContains(output, "Given I navigate to");
        assertContains(output, "I enter");
        assertContains(output, "I click the");

        // Step definitions section
        assertContains(output, "LoginSteps.java");
        assertContains(output, "import io.cucumber.java");
        assertContains(output, "@Given");
        assertContains(output, "@And");
        assertContains(output, "@Before");
        assertContains(output, "@After");
        assertContains(output, "ExpectedConditions");
    }

    @Test @Order(10)
    @DisplayName("generate_gherkin — infers feature name from navigate URL when omitted")
    void generateGherkinInfersName() throws Exception {
        String output = mcp.callToolText("generate",
            args("language", "gherkin", "package_name", GEN_PACKAGE));

        log.info("generate_gherkin (inferred) {} chars", output.length());
        assertToolSuccess(output);
        assertContains(output, "Feature:");
        assertContains(output, "Scenario:");
    }

    // ------------------------------------------------------------------ //
    //  Session log management                                              //
    // ------------------------------------------------------------------ //

    @Test @Order(12)
    @DisplayName("clear_session_log — empties the log")
    void clearSessionLog() throws Exception {
        String clearResult = mcp.callToolText("clear_session_log", mcp.args());
        log.info("clear_session_log → {}", clearResult);
        assertContains(clearResult, "cleared");

        String logAfter = mcp.callToolText("get_session_log", mcp.args());
        log.info("get_session_log after clear → {}", logAfter);
        assertContains(logAfter, "empty");
    }

    @Test @Order(13)
    @DisplayName("generate_java_testng (empty session) — returns skeleton with no-op comment")
    void generateWithEmptySession() throws Exception {
        String code = mcp.callToolText("generate",
            args("language", "java", "framework", "testng", "test_name", "EmptySessionTest", "package_name", GEN_PACKAGE));

        log.info("generate_java_testng (empty) {} chars", code.length());
        assertFalse(code.isBlank());
        // With no recorded actions, codegen emits the "No actions recorded" comment
        assertContains(code, "No actions recorded");
    }

    @Test @Order(20)
    @DisplayName("generate — csharp, CI targets and the selenium_boot junit5 runner dispatch")
    void generateOtherTargets() throws Exception {
        assertContains(mcp.callToolText("generate", args("language", "csharp")), "NUnit");
        assertContains(mcp.callToolText("generate", args("language", "github_actions", "build", "python_pytest")), "pytest");
        assertFalse(mcp.callToolText("generate", args("language", "jenkins")).isBlank());
        assertFalse(mcp.callToolText("generate", args("language", "gitlab_ci", "build", "java_gradle")).isBlank());
        assertFalse(mcp.callToolText("generate", args("language", "playwright")).isBlank());
        assertContains(mcp.callToolText("generate",
            args("language", "java", "framework", "selenium_boot", "runner", "junit5")), "BaseJUnit5Test");
    }

    @Test @Order(21)
    @DisplayName("generate — unknown language returns a clear error")
    void generateUnknownLanguage() throws Exception {
        assertContains(mcp.callToolText("generate", args("language", "cobol")), "language must be one of");
    }
}
