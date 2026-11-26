package com.saddy.mcptest.tests;

import com.saddy.mcptest.base.BaseMcpTest;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for three codegen gaps found while filming an AI-writes-Selenium demo
 * (seleniumboot-mcp 0.9.3):
 *   1. plain generators dropped every recorded assert_*, so a recorded login test passed
 *      even when the login failed;
 *   2. password fields were emitted as sendKeys("***"), so the test typed the wrong password;
 *   3. java ignored class_name and always produced RecordedFlowTest.
 *
 * Records: navigate → type username → type password → click Login → assert_text success.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Codegen — recorded assertions, secrets, class_name")
class RecordedAssertionsCodegenTest extends BaseMcpTest {

    private static final String SUCCESS = "Login successful!";

    @BeforeAll
    void recordSession() throws Exception {
        mcp.callToolText("start_browser",
            args("browser", "chrome", "headless", true, "window_size", "1280x800"));
        mcp.callToolText("navigate", args("url", BASE_URL));
        mcp.callToolText("type_text", args("selector", "#username", "text", "admin"));
        mcp.callToolText("type_text", args("selector", "#password", "text", "password"));
        mcp.callToolText("click", args("selector", "[data-testid='login-submit-btn']"));
        String check = mcp.callToolText("assert_text",
            args("selector", "#loginAlert", "expected", SUCCESS));
        assertContains(check, "PASS");
    }

    @AfterAll
    void closeBrowser() throws Exception {
        mcp.callToolText("close_browser", mcp.args());
    }

    @Test @Order(1)
    @DisplayName("java testng — replays the recorded assert_text and reads the password from TEST_PASSWORD")
    void javaTestNG() throws Exception {
        String code = mcp.callToolText("generate", args("language", "java", "framework", "testng"));
        assertContains(code, "String actual = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(\"#loginAlert\"))).getText();");
        assertContains(code, "Assert.assertTrue(actual.contains(\"" + SUCCESS + "\")");
        assertContains(code, "sendKeys(System.getenv(\"TEST_PASSWORD\"));");
        assertFalse(code.contains("\"***\""), "password emitted as a *** literal:\n" + code);
    }

    @Test @Order(2)
    @DisplayName("java junit5 — uses JUnit assertions for the recorded check")
    void javaJUnit5() throws Exception {
        String code = mcp.callToolText("generate", args("language", "java", "framework", "junit5"));
        assertContains(code, "assertTrue(actual.contains(\"" + SUCCESS + "\")");
        assertFalse(code.contains("Assert.assertTrue"), "TestNG assertion in a JUnit 5 test:\n" + code);
        assertContains(code, "System.getenv(\"TEST_PASSWORD\")");
    }

    @Test @Order(3)
    @DisplayName("python — assert on the recorded text, os.environ for the password")
    void python() throws Exception {
        String code = mcp.callToolText("generate", args("language", "python"));
        assertContains(code, "import os\n");
        assertContains(code, "assert '" + SUCCESS + "' in self.wait.until(");
        assertContains(code, "send_keys(os.environ[\"TEST_PASSWORD\"])");
        assertFalse(code.contains("\"***\""), "password emitted as a *** literal:\n" + code);
    }

    @Test @Order(4)
    @DisplayName("csharp — NUnit Assert.That for the recorded check, env var for the password")
    void csharp() throws Exception {
        String code = mcp.callToolText("generate", args("language", "csharp"));
        assertContains(code, "Does.Contain(\"" + SUCCESS + "\")");
        assertContains(code, "SendKeys(Environment.GetEnvironmentVariable(\"TEST_PASSWORD\"));");
    }

    @Test @Order(5)
    @DisplayName("java — class_name names the class when test_name is not given")
    void javaHonoursClassName() throws Exception {
        String code = mcp.callToolText("generate",
            args("language", "java", "framework", "testng", "class_name", "LoginTest"));
        assertContains(code, "public class LoginTest {");
        assertFalse(code.contains("class RecordedFlowTest"), "class_name ignored:\n" + code);
    }

    @Test @Order(6)
    @DisplayName("selenium_boot — keeps its assertion and also reads the password from TEST_PASSWORD")
    void seleniumBoot() throws Exception {
        String code = mcp.callToolText("generate", args("language", "java", "framework", "selenium_boot"));
        assertContains(code, "containsText(\"" + SUCCESS + "\")");
        assertContains(code, ".type(System.getenv(\"TEST_PASSWORD\"));");
        assertFalse(code.contains("\"***\""), "password emitted as a *** literal:\n" + code);
    }
}
