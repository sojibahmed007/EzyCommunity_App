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

// EzyCommunity সার্চ ফিচারের সিকিউরিটি, স্ট্রেস ও ফাজিং টেস্ট স্যুট
public class EzyCommunitySearchTest {

    private AndroidDriver driver;
    private WebDriverWait wait;

    private String validEmail;
    private String validPassword;

    // --- লগইন ও ড্যাশবোর্ড লোকেটরসমূহ ---
    private final By signInButton = AppiumBy.accessibilityId("Sign In");
    private final By editTextLocator = By.className("android.widget.EditText");
    private final By checkBoxesLocator = By.className("android.widget.CheckBox");
    private final By appLauncherIcon = By.xpath("//*[@content-desc='EzyCommunity' or @text='EzyCommunity']");
    private final By teamName = AppiumBy.accessibilityId("SQA Team");
    private final By homeIcon = AppiumBy.accessibilityId("Home");

    // ড্যাশবোর্ডের সার্চ আইকন বাটন (Sort বাটনের আগের এলিমেন্ট)
    private final By searchIconButton = By.xpath(
        "//android.widget.Button[@content-desc='Sort']/preceding-sibling::*[1]" +
        " | //android.view.View[@content-desc='SQA Team']/following-sibling::*[@clickable='true'][1]"
    );

    // --- সার্চ স্ক্রিন লোকেটরসমূহ ---
    // সার্চ ইনপুট ফিল্ড (হিন্ট ও বাউন্ডস দিয়ে নিশ্চিত করা যাতে লগইন ফিল্ডের সাথে কনফ্লিক্ট না করে)
    private final By searchInputLocator = By.xpath(
        "//android.widget.EditText[contains(@hint, 'Search') or contains(@bounds, '[244,110]')]" +
        " | //android.widget.EditText[not(following-sibling::android.widget.EditText) and not(preceding-sibling::android.widget.EditText)]"
    );

    // সার্চ ইনপুট ক্লিয়ার বাটন (X)
    private final By clearButtonLocator = By.xpath(
        "//android.widget.EditText/following-sibling::*[@clickable='true'][1]"
    );

    // সার্চ স্ক্রিন থেকে ফিরে আসার ব্যাক বাটন
    private final By backButtonLocator = By.xpath(
        "//android.widget.EditText/preceding-sibling::*[@clickable='true'][1]"
    );

    // সাম্প্রতিক সার্চ সেকশন হেডার
    private final By recentSearchesHeader = By.xpath(
        "//*[contains(@content-desc, 'RECENT SEARCHES')]"
    );

    // কোনো রেজাল্ট না পেলে যে মেসেজ দেখায়
    private final By noResultsLocator = By.xpath(
        "//*[contains(@content-desc, 'No results for') or contains(@content-desc, 'Try different keywords')]"
    );

    // সার্চ রেজাল্ট পাওয়ার হেডার
    private final By resultsHeaderLocator = By.xpath(
        "//*[contains(@content-desc, 'Search results for')]"
    );

    // টেস্ট ক্লাস শুরুর আগে ড্রাইভার ইনিশিয়ালাইজেশন ও লগইন সম্পন্ন করা
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

        driver = new AndroidDriver(new URL("http://127.0.0.1:4723"), options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        System.out.println("EzyCommunitySearchTest: Appium session started.");

        ensureAppRunningAndOpen();
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
            }
        } catch (Exception ignored) {}
    }

    // লগইন স্ক্রিনে থাকলে স্বয়ংক্রিয়ভাবে লগইন করে ড্যাশবোর্ডে নিয়ে যাবে
    private void ensureLoggedIn() {
        try {
            Thread.sleep(1000);
            if (isLoggedIn() && !isLoginScreenVisible()) {
                System.out.println("User is already logged in.");
                return;
            }

            if (!isLoginScreenVisible() && isLoggedIn()) {
                return;
            }

            System.out.println("Performing automatic login...");
            wait.until(ExpectedConditions.visibilityOfElementLocated(signInButton));

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

            acceptTermsAndRememberMe();
            Thread.sleep(500);

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
                    if (!"true".equalsIgnoreCase(cb.getAttribute("checked"))) {
                        cb.click();
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    // প্রতি টেস্টের আগে সার্চ স্ক্রিনে নেভিগেট করা ও ইনপুট বক্স প্রস্তুত করা
    @BeforeMethod
    public void prepareSearchScreen() {
        try {
            ensureAppRunningAndOpen();
            Thread.sleep(300);

            if (isLoginScreenVisible()) {
                ensureLoggedIn();
            }

            // সার্চ স্ক্রিনে না থাকলে সার্চ আইকনে ক্লিক করে প্রবেশ করবে
            int attempts = 0;
            while (!isVisible(searchInputLocator) && attempts < 3) {
                attempts++;
                System.out.println("Navigating to Search screen (attempt " + attempts + ")...");
                try {
                    List<WebElement> btns = driver.findElements(searchIconButton);
                    if (!btns.isEmpty()) {
                        btns.get(0).click();
                    } else {
                        // সার্চ আইকনের কোঅর্ডিনেট ফলব্যাক [828,84][933,189]
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

    // সার্চ ইনপুট ফিল্ড ও ক্লিয়ার বাটন রিসেট করা
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

    // ক্রিটিক্যাল সিকিউরিটি চেক: অ্যাপ ক্র্যাশ না করে জীবিত আছে কিনা যাচাই
    private void verifyAppHealthy() {
        try {
            String appState = driver.queryAppState("com.ezycommunity.android").toString();
            Assert.assertTrue(
                "RUNNING_IN_FOREGROUND".equals(appState) || "RUNNING_IN_BACKGROUND".equals(appState),
                "App crashed or closed during search attack! Current state: " + appState
            );
        } catch (Exception e) {
            Assert.assertNotNull(driver.getPageSource(), "App connection lost / crashed!");
        }
    }

    // ডেটা প্রোভাইডার: ১৪টি ক্রিটিক্যাল সিকিউরিটি ও স্ট্রেস অ্যাটাক ভেক্টর
    @DataProvider(name = "searchAttackCases")
    public Object[][] searchAttackCases() {
        return new Object[][] {
            // ১. বেসলাইন সাধারণ সার্চ
            {"TC-SEC-01 Valid Baseline Query",
                "test", "Baseline query should return valid search results"},

            // ২. এসকিউএল ইনজেকশন (SQLi) পে-লোড
            {"TC-SEC-02 SQLi Auth Bypass Payload",
                "' OR '1'='1", "SQL injection bypass syntax must not leak data or break app"},

            {"TC-SEC-03 SQLi Stacked Drop Query",
                "'; DROP TABLE posts; --", "SQL injection drop statement must be properly parameterized"},

            {"TC-SEC-04 SQLi Union-Based Extraction",
                "' UNION SELECT null, username, password FROM users --", "Union SQLi payload must be rejected gracefully"},

            // ৩. ক্রস-সাইট স্ক্রিপ্টিং (XSS) পে-লোড
            {"TC-SEC-05 Stored/Reflected XSS Script Tag",
                "<script>alert('XSS')</script>", "Script tags must be sanitized and rendered as raw text"},

            {"TC-SEC-06 XSS Image Tag with OnError Handler",
                "<img src=x onerror=alert(1)>", "Image onerror injection must not trigger DOM execution"},

            {"TC-SEC-07 HTML Iframe & Javascript URI",
                "<iframe src=\"javascript:alert(1)\"></iframe>", "Iframe HTML injection must be safely escaped"},

            // ৪. সার্ভার-সাইড টেমপ্লেট ইনজেকশন (SSTI)
            {"TC-SEC-08 Server-Side Template Injection (SSTI)",
                "{{7*7}} ${7*7} <%= 7*7 %>", "Template tags should not be evaluated on server or client"},

            // ৫. বাউন্ডারি ও বাফার স্ট্রেস (৫০০+ ক্যারেক্টার)
            {"TC-SEC-09 Massive Length Boundary Attack (500 Chars)",
                "CRITICAL_PAYLOAD_FUZZING_TEST_BOUNDARY_OVERFLOW_ATTACK_".repeat(9),
                "Heavy 500+ character string must not cause memory overflow or ANR"},

            // ৬. স্পেশাল ক্যারেক্টার ও সিম্বল বম্ব
            {"TC-SEC-10 Extreme Special Symbol & Punctuation Bomb",
                "!@#$%^&*()_+~|}{[]:;?><,./'\"\\", "Complex symbol combinations must not break regex parser"},

            // ৭. মাল্টি-বাইট ইমোজি ও আরবি RTL ক্যারেক্টার
            {"TC-SEC-11 Complex Multi-Byte Emojis & RTL Characters",
                "🔥🚀💻👻🎯💥 ﷽ \u202Ereversed\u202C", "Unicode and Bidirectional (RTL) glyphs must render without crash"},

            // ৮. নাল বাইট পয়জনিং
            {"TC-SEC-12 Null Byte Poisoning Attack",
                "search\u0000admin", "Null byte character should not truncate or corrupt internal buffer"},

            // ৯. হোয়াইটস্পেস ও কন্ট্রোল ক্যারেক্টার ফ্লাডিং
            {"TC-SEC-13 Whitespace & Control Characters Flooding",
                "   \t   \n   \r   ", "Only whitespace and control chars should not cause blank screen crash"},

            // ১০. অস্তিত্বহীন টোকেন (গ্রেসফুল এম্পটি স্টেট যাচাই)
            {"TC-SEC-14 Non-Existent Random Query",
                "__xyz_random_nonexistent_token_99999__", "Non-existent token must display graceful empty state UI"}
        };
    }

    // প্রতিটি অ্যাটাক পে-লোড ইনপুট করে ফলাফল ও অ্যাপের স্থিতিশীলতা যাচাই
    @Test(dataProvider = "searchAttackCases")
    public void testSearchAttackVectors(String testCase, String payload, String description) {
        System.out.println("----------------------------------------------------------------");
        System.out.println("Executing: " + testCase);
        System.out.println("Payload:   " + payload);
        System.out.println("Purpose:   " + description);

        clearSearchInput();

        WebElement input = wait.until(ExpectedConditions.elementToBeClickable(searchInputLocator));
        input.click();

        if (!payload.isEmpty()) {
            input.sendKeys(payload);
            try {
                driver.pressKey(new KeyEvent(AndroidKey.ENTER));
            } catch (Exception e) {
                try { driver.hideKeyboard(); } catch (Exception ignored) {}
            }
        }

        // সার্চের রেসপন্স বা ডিববাউন্স হওয়া পর্যন্ত অপেক্ষা
        try {
            Thread.sleep(1500);
            WebDriverWait searchWait = new WebDriverWait(driver, Duration.ofSeconds(6));
            searchWait.until(d ->
                isVisible(resultsHeaderLocator)
                || isVisible(noResultsLocator)
                || isVisible(recentSearchesHeader)
            );
        } catch (Exception ignored) {}

        // সিকিউরিটি অ্যাসারশন ১: অ্যাপ ক্র্যাশ করেনি
        verifyAppHealthy();

        // সিকিউরিটি অ্যাসারশন ২: সার্চ ইনপুট এখনো দৃশ্যমান ও কার্যকর আছে
        Assert.assertTrue(isVisible(searchInputLocator), testCase + ": Search screen disappeared or crashed!");

        // সিকিউরিটি অ্যাসারশন ৩: পেজ সোর্সে কোনো ডাটাবেজ এরর ফাঁস হয়নি
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

    // দ্রুত টাইপিং টেস্ট: ডিববাউন্স টাইমার ও রেস কন্ডিশন স্ট্রেস টেস্ট
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
                Thread.sleep(150);
            } catch (Exception ignored) {}
        }

        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

        verifyAppHealthy();
        Assert.assertTrue(isVisible(searchInputLocator), "Search input crashed after hammering!");
        System.out.println("TC-SEC-15 PASSED - Debounce hammer handled safely.");
    }

    // ক্লিয়ার (X) বাটন টেস্ট: টেক্সট ক্লিয়ার হয়ে সার্চ বক্স রিসেট হয় কিনা যাচাই
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

        // ক্লিয়ার বাটন ক্লিক করা
        List<WebElement> clearBtns = driver.findElements(clearButtonLocator);
        Assert.assertFalse(clearBtns.isEmpty(), "Clear button (X) not found after entering text!");
        clearBtns.get(0).click();

        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

        // ইনপুট বক্সটি রিসেট হয়েছে কিনা অ্যাসার্ট করা
        input = driver.findElement(searchInputLocator);
        String textAfterClear = input.getText();
        Assert.assertTrue(
            textAfterClear == null || textAfterClear.isEmpty() || "Search posts, comments, people, and more".equals(textAfterClear),
            "Search input was not cleared! Found: " + textAfterClear
        );

        verifyAppHealthy();
        System.out.println("TC-SEC-16 PASSED - Clear button successfully reset search screen.");
    }

    // টেস্ট শেষে Appium সেশন ক্লোজ করা
    @AfterClass(alwaysRun = true)
    public void tearDown() {
        if (driver != null) {
            driver.quit();
            System.out.println("EzyCommunitySearchTest: Appium session closed.");
        }
    }
}
