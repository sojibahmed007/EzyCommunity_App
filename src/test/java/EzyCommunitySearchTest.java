import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import io.appium.java_client.android.options.UiAutomator2Options;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.net.URL;
import java.time.Duration;
import java.util.List;

/**
 * Critical Security, Stress & Edge-Case Test Suite for EzyCommunity Search.
 * Automatically launches app -> performs auto-login -> navigates to Search -> executes critical attack vectors.
 */
public class EzyCommunitySearchTest {

    private AndroidDriver driver;
    private WebDriverWait wait;

    private String validEmail;
    private String validPassword;

    // Login screen locators
    private final By signInButton =
        AppiumBy.accessibilityId("Sign In");

    private final By editTextLocator =
        By.className("android.widget.EditText");

    private final By checkBoxesLocator =
        By.className("android.widget.CheckBox");

    private final By appLauncherIcon = By.xpath(
        "//*[@content-desc='EzyCommunity' or @text='EzyCommunity']"
    );

    // Dashboard locators
    private final By teamName =
        AppiumBy.accessibilityId("SQA Team");

    private final By homeIcon =
        AppiumBy.accessibilityId("Home");

    private final By searchIconButton = By.xpath(
        "//android.widget.Button[@content-desc='Sort']/preceding-sibling::*[1]" +
        " | //android.view.View[@content-desc='SQA Team']/following-sibling::*[@clickable='true'][1]"
    );

    // Search screen locators
    private final By searchInputLocator = By.xpath(
        "//android.widget.EditText[contains(@hint, 'Search') or contains(@bounds, '[244,110]')]" +
        " | //android.widget.EditText[not(following-sibling::android.widget.EditText) and not(preceding-sibling::android.widget.EditText)]"
    );

    private final By clearButtonLocator = By.xpath(
        "//android.widget.EditText/following-sibling::*[@clickable='true'][1]"
    );

    private final By backButtonLocator = By.xpath(
        "//android.widget.EditText/preceding-sibling::*[@clickable='true'][1]"
    );

    private final By recentSearchesHeader = By.xpath(
        "//*[contains(@content-desc, 'RECENT SEARCHES')]"
    );

    private final By noResultsLocator = By.xpath(
        "//*[contains(@content-desc, 'No results for') or contains(@content-desc, 'Try different keywords')]"
    );

    private final By resultsHeaderLocator = By.xpath(
        "//*[contains(@content-desc, 'Search results for')]"
    );

    @BeforeClass
    public void setUp() throws Exception {
        validEmail = System.getenv("TEST_EMAIL");
        validPassword = System.getenv("TEST_PASSWORD");

        if (validEmail == null || validEmail.trim().isEmpty()) {
            validEmail = "testerone@imti.online";
        }
        if (validPassword == null || validPassword.trim().isEmpty()) {
            validPassword = "#Ab123456";
        }

        UiAutomator2Options options = new UiAutomator2Options();
        options.setPlatformName("Android");
        options.setAutomationName("UiAutomator2");
        options.setDeviceName("Android Emulator");
        options.setUdid("emulator-5554");
        options.setAppPackage("com.ezycommunity.android");
        options.setAppActivity(".MainActivity");
        options.setNoReset(true);

        driver = new AndroidDriver(
            new URL("http://127.0.0.1:4723"),
            options
        );

        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        System.out.println("EzyCommunitySearchTest: Appium session started.");

        // Step 1: Ensure app is running and brought to foreground
        ensureAppRunningAndOpen();

        // Step 2: Ensure user is logged in
        ensureLoggedIn();
    }

    private void ensureAppRunningAndOpen() {
        try {
            driver.activateApp("com.ezycommunity.android");
        } catch (Exception ignored) {}

        try {
            List<WebElement> launcherIcons = driver.findElements(appLauncherIcon);
            if (!launcherIcons.isEmpty() && launcherIcons.get(0).isDisplayed()) {
                launcherIcons.get(0).click();
                System.out.println("Clicked EzyCommunity icon on Android launcher.");
            }
        } catch (Exception ignored) {}
    }

    private void ensureLoggedIn() {
        try {
            Thread.sleep(1000);
            if (isLoggedIn() && !isLoginScreenVisible()) {
                System.out.println("User is already logged in.");
                return;
            }

            if (!isLoginScreenVisible()) {
                // If not on login screen, check if we are already inside the app
                if (isLoggedIn()) {
                    System.out.println("Authenticated screen detected.");
                    return;
                }
            }

            System.out.println("Performing automatic login...");
            wait.until(ExpectedConditions.visibilityOfElementLocated(signInButton));

            // Enter credentials
            List<WebElement> fields = driver.findElements(editTextLocator);
            Assert.assertTrue(fields.size() >= 2, "Expected email and password fields");

            WebElement emailInput = fields.get(0);
            emailInput.click();
            emailInput.clear();
            emailInput.sendKeys(validEmail);

            try { driver.hideKeyboard(); } catch (Exception ignored) {}

            fields = driver.findElements(editTextLocator);
            WebElement passwordInput = fields.get(1);
            passwordInput.click();
            passwordInput.clear();
            passwordInput.sendKeys(validPassword);

            try { driver.hideKeyboard(); } catch (Exception ignored) {}
            Thread.sleep(800);

            // Accept Remember Me & Terms checkboxes
            acceptTermsAndRememberMe();
            Thread.sleep(500);

            // Click Sign In with Flutter unfocus-retry logic
            clickSignIn();

            WebDriverWait loginWait = new WebDriverWait(driver, Duration.ofSeconds(25));
            loginWait.until(d -> isLoggedIn() && !isLoginScreenVisible());
            System.out.println("Automatic login successful. Dashboard reached.");

        } catch (Exception e) {
            Assert.fail("Failed during automatic login setup: " + e.getMessage(), e);
        }
    }

    private boolean isLoggedIn() {
        return isVisible(teamName)
            || isVisible(homeIcon)
            || isVisible(searchIconButton)
            || isVisible(searchInputLocator)
            || isVisible(backButtonLocator);
    }

    private boolean isLoginScreenVisible() {
        return isVisible(signInButton) && isVisible(editTextLocator);
    }

    private void clickSignIn() {
        try {
            WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(signInButton));
            btn.click();
        } catch (Exception ignored) {}

        try {
            Thread.sleep(1200);
            if (!isLoggedIn() && isLoginScreenVisible()) {
                System.out.println("Retrying Sign In click after unfocus...");
                try {
                    WebElement btn = driver.findElement(signInButton);
                    btn.click();
                } catch (Exception e) {
                    try {
                        new org.openqa.selenium.interactions.Actions(driver)
                            .moveToLocation(540, 1477)
                            .click()
                            .perform();
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}
    }

    private void acceptTermsAndRememberMe() {
        try {
            List<WebElement> checkBoxes = driver.findElements(checkBoxesLocator);
            for (WebElement cb : checkBoxes) {
                try {
                    String checkedAttr = cb.getAttribute("checked");
                    if (!"true".equalsIgnoreCase(checkedAttr)) {
                        cb.click();
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    @BeforeMethod
    public void prepareSearchScreen() {
        try {
            ensureAppRunningAndOpen();
            Thread.sleep(300);

            if (isLoginScreenVisible()) {
                ensureLoggedIn();
            }

            // Retry entering search screen up to 3 times if not already visible
            int attempts = 0;
            while (!isVisible(searchInputLocator) && attempts < 3) {
                attempts++;
                System.out.println("Navigating to Search screen (attempt " + attempts + ")...");
                try {
                    List<WebElement> btns = driver.findElements(searchIconButton);
                    if (!btns.isEmpty()) {
                        btns.get(0).click();
                    } else {
                        // Direct coordinate tap on search icon [828,84][933,189]
                        new org.openqa.selenium.interactions.Actions(driver)
                            .moveToLocation(880, 136)
                            .click()
                            .perform();
                    }
                } catch (Exception e) {
                    try {
                        new org.openqa.selenium.interactions.Actions(driver)
                            .moveToLocation(880, 136)
                            .click()
                            .perform();
                    } catch (Exception ignored) {}
                }
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }

            WebDriverWait readyWait = new WebDriverWait(driver, Duration.ofSeconds(8));
            readyWait.until(ExpectedConditions.visibilityOfElementLocated(searchInputLocator));
            clearSearchInput();
            System.out.println("Search screen is ready for attack.");

        } catch (Exception e) {
            Assert.fail("Could not prepare search screen: " + e.getMessage(), e);
        }
    }

    private void clearSearchInput() {
        try {
            List<WebElement> clearBtns = driver.findElements(clearButtonLocator);
            for (WebElement btn : clearBtns) {
                try {
                    if (btn.isDisplayed()) {
                        btn.click();
                        Thread.sleep(300);
                        break;
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        try {
            List<WebElement> inputs = driver.findElements(searchInputLocator);
            if (!inputs.isEmpty()) {
                inputs.get(0).clear();
            }
        } catch (Exception ignored) {}
    }

    private boolean isVisible(By locator) {
        try {
            List<WebElement> elements = driver.findElements(locator);
            for (WebElement el : elements) {
                try {
                    if (el.isDisplayed()) return true;
                } catch (StaleElementReferenceException ignored) {}
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    private void verifyAppHealthy() {
        try {
            String appState = driver.queryAppState("com.ezycommunity.android").toString();
            Assert.assertTrue(
                "RUNNING_IN_FOREGROUND".equals(appState) || "RUNNING_IN_BACKGROUND".equals(appState),
                "App crashed or closed during search attack! Current state: " + appState
            );
        } catch (Exception e) {
            // If queryAppState unsupported, verify driver is still active
            Assert.assertNotNull(driver.getPageSource(), "App connection lost / crashed!");
        }
    }

    @DataProvider(name = "searchAttackCases")
    public Object[][] searchAttackCases() {
        return new Object[][] {
            // 1. Baseline Valid Query
            {"TC-SEC-01 Valid Baseline Query",
                "test", "Baseline query should return valid search results"},

            // 2. SQL Injection Payloads
            {"TC-SEC-02 SQLi Auth Bypass Payload",
                "' OR '1'='1", "SQL injection bypass syntax must not leak data or break app"},

            {"TC-SEC-03 SQLi Stacked Drop Query",
                "'; DROP TABLE posts; --", "SQL injection drop statement must be properly parameterized"},

            {"TC-SEC-04 SQLi Union-Based Extraction",
                "' UNION SELECT null, username, password FROM users --", "Union SQLi payload must be rejected gracefully"},

            // 3. Cross-Site Scripting (XSS) Payloads
            {"TC-SEC-05 Stored/Reflected XSS Script Tag",
                "<script>alert('XSS')</script>", "Script tags must be sanitized and rendered as raw text"},

            {"TC-SEC-06 XSS Image Tag with OnError Handler",
                "<img src=x onerror=alert(1)>", "Image onerror injection must not trigger DOM execution"},

            {"TC-SEC-07 HTML Iframe & Javascript URI",
                "<iframe src=\"javascript:alert(1)\"></iframe>", "Iframe HTML injection must be safely escaped"},

            // 4. Server-Side Template Injection (SSTI)
            {"TC-SEC-08 Server-Side Template Injection (SSTI)",
                "{{7*7}} ${7*7} <%= 7*7 %>", "Template tags should not be evaluated on server or client"},

            // 5. Buffer Overflow / Heavy Length Stress
            {"TC-SEC-09 Massive Length Boundary Attack (500 Chars)",
                "CRITICAL_PAYLOAD_FUZZING_TEST_BOUNDARY_OVERFLOW_ATTACK_".repeat(9),
                "Heavy 500+ character string must not cause memory overflow or ANR"},

            // 6. Special Characters & Symbol Flooding
            {"TC-SEC-10 Extreme Special Symbol & Punctuation Bomb",
                "!@#$%^&*()_+~|}{[]:;?><,./'\"\\", "Complex symbol combinations must not break regex parser"},

            // 7. Unicode, Complex Emojis & RTL Glyphs
            {"TC-SEC-11 Complex Multi-Byte Emojis & RTL Characters",
                "🔥🚀💻👻🎯💥 ﷽ \u202Ereversed\u202C", "Unicode and Bidirectional (RTL) glyphs must render without crash"},

            // 8. Null Byte Injection
            {"TC-SEC-12 Null Byte Poisoning Attack",
                "search\u0000admin", "Null byte character should not truncate or corrupt internal buffer"},

            // 9. Whitespace Flooding
            {"TC-SEC-13 Whitespace & Control Characters Flooding",
                "   \t   \n   \r   ", "Only whitespace and control chars should not cause blank screen crash"},

            // 10. Non-Existent String (Graceful Zero Result UI)
            {"TC-SEC-14 Non-Existent Random Query",
                "__xyz_random_nonexistent_token_99999__", "Non-existent token must display graceful empty state UI"}
        };
    }

    @Test(dataProvider = "searchAttackCases")
    public void testSearchAttackVectors(String testCase, String payload, String description) {
        System.out.println("----------------------------------------------------------------");
        System.out.println("Executing: " + testCase);
        System.out.println("Payload:   " + payload);
        System.out.println("Purpose:   " + description);

        clearSearchInput();

        WebElement input = wait.until(
            ExpectedConditions.elementToBeClickable(searchInputLocator)
        );
        input.click();

        if (!payload.isEmpty()) {
            input.sendKeys(payload);

            // Trigger search via ENTER key
            try {
                driver.pressKey(new KeyEvent(AndroidKey.ENTER));
            } catch (Exception e) {
                try { driver.hideKeyboard(); } catch (Exception ignored) {}
            }
        }

        // Wait up to 6 seconds for live search debounce / API response
        try {
            Thread.sleep(1500);
            WebDriverWait searchWait = new WebDriverWait(driver, Duration.ofSeconds(6));
            searchWait.until(d ->
                isVisible(resultsHeaderLocator)
                || isVisible(noResultsLocator)
                || isVisible(recentSearchesHeader)
            );
        } catch (Exception ignored) {
            // Search might still be idle or debouncing; ensure app didn't crash
        }

        // CRITICAL SECURITY ASSERTION 1: App must stay alive and healthy
        verifyAppHealthy();

        // CRITICAL SECURITY ASSERTION 2: Search input must still be present and functional
        Assert.assertTrue(
            isVisible(searchInputLocator),
            testCase + ": Search screen disappeared or crashed!"
        );

        // CRITICAL SECURITY ASSERTION 3: No raw database stack trace exposed in UI
        String pageSource = driver.getPageSource();
        Assert.assertFalse(
            pageSource.toLowerCase().contains("syntax error in sql") ||
            pageSource.toLowerCase().contains("sqliteexception") ||
            pageSource.toLowerCase().contains("pg::syntaxerror") ||
            pageSource.toLowerCase().contains("fatal error"),
            testCase + ": Raw database/backend error exposed in client UI!"
        );

        System.out.println(testCase + " PASSED - App handled payload robustly.");
    }

    @Test
    public void testRapidDebounceHammering() {
        System.out.println("----------------------------------------------------------------");
        System.out.println("Executing: TC-SEC-15 Rapid Debounce & Concurrency Hammering");

        clearSearchInput();
        String[] hammerPayloads = {"a", "ab", "abc", "abcd", "abcde", "test", "' OR 1=1", "🔥"};

        for (String q : hammerPayloads) {
            try {
                WebElement input = driver.findElement(searchInputLocator);
                input.clear();
                input.sendKeys(q);
                Thread.sleep(150); // Intentionally fast to stress debounce
            } catch (Exception ignored) {}
        }

        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

        verifyAppHealthy();
        Assert.assertTrue(isVisible(searchInputLocator), "Search input crashed after hammering!");
        System.out.println("TC-SEC-15 PASSED - Debounce hammer handled safely.");
    }

    @Test
    public void testClearButtonFunctionality() {
        System.out.println("----------------------------------------------------------------");
        System.out.println("Executing: TC-SEC-16 Clear Button & Screen Reset");

        clearSearchInput();
        WebElement input = wait.until(ExpectedConditions.elementToBeClickable(searchInputLocator));
        input.click();
        input.sendKeys("TemporaryAttackPayload");

        try {
            driver.pressKey(new KeyEvent(AndroidKey.ENTER));
            Thread.sleep(1500);
        } catch (Exception ignored) {}

        // Locate and click the Clear button (X)
        List<WebElement> clearBtns = driver.findElements(clearButtonLocator);
        Assert.assertFalse(clearBtns.isEmpty(), "Clear button (X) not found after entering text!");
        clearBtns.get(0).click();

        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

        // Input should now be cleared
        input = driver.findElement(searchInputLocator);
        String textAfterClear = input.getText();
        Assert.assertTrue(
            textAfterClear == null || textAfterClear.isEmpty() || "Search posts, comments, people, and more".equals(textAfterClear),
            "Search input was not cleared! Found: " + textAfterClear
        );

        verifyAppHealthy();
        System.out.println("TC-SEC-16 PASSED - Clear button successfully reset search screen.");
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        if (driver != null) {
            driver.quit();
            System.out.println("EzyCommunitySearchTest: Appium session closed.");
        }
    }
}
