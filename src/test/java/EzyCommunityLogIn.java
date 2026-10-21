import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
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

// EzyCommunity মোবাইল অ্যাপের ডাটা-ড্রিভেন লগইন ও সেশন অটোমেশন টেস্ট স্যুট
public class EzyCommunityLogIn {

    private AndroidDriver driver;
    private WebDriverWait wait;

    private String validEmail;
    private String validPassword;

    // --- এলিমেন্ট লোকেটরসমূহ ---

    // ড্যাশবোর্ডের প্রোফাইল অবতার (SQA Team-এর আগের ক্লিকযোগ্য ভিউ)
    private final By profileAvatar = By.xpath(
        "//android.view.View[@content-desc='SQA Team']/preceding-sibling::*[@clickable='true'][1]" +
        " | //android.view.View[@content-desc='SQA Team']/preceding-sibling::*[1]" +
        " | //android.view.View[@content-desc='SQA Team']/preceding-sibling::android.widget.ImageView[1]"
    );

    // ড্যাশবোর্ড হেডার ও হোম আইকন
    private final By teamName = AppiumBy.accessibilityId("SQA Team");
    private final By homeIcon = AppiumBy.accessibilityId("Home");

    // সাইন ইন বাটন
    private final By signInButton = AppiumBy.accessibilityId("Sign In");

    // প্রোফাইল ড্রয়ারের ভেতরের 'Log out' অপশন (ধাপ ১)
    private final By logoutMenuItem = By.xpath(
        "//*[@content-desc='Log out' or @content-desc='Log Out']"
    );

    // কনফার্মেশন মোডাল ডায়ালগের 'Log Out' বাটন (ধাপ ২)
    private final By logoutConfirmButton = By.xpath(
        "//android.widget.Button[@content-desc='Log Out' or @content-desc='Log out']"
    );

    // অ্যান্ড্রয়েড হোম স্ক্রিনের লঞ্চার আইকন
    private final By appLauncherIcon = By.xpath(
        "//*[@content-desc='EzyCommunity' or @text='EzyCommunity']"
    );

    // লগইন স্ক্রিনের চেকবক্স ও ইনপুট ফিল্ড লোকেটর
    private final By checkBoxesLocator = By.className("android.widget.CheckBox");
    private final By editTextLocator = By.className("android.widget.EditText");

    // ড্রয়ারের প্রোফাইল অবতার
    private final By anyProfileAvatar = By.xpath(
        "//android.widget.ImageView[contains(@content-desc, '@')]"
    );

    // টেস্ট ক্লাস শুরুর আগে ড্রাইভার ও ক্রেডেনশিয়াল সেটআপ
    @BeforeClass
    public void setUp() throws Exception {
        // এনভায়রনমেন্ট ভেরিয়েবল থেকে ক্রেডেনশিয়াল লোড
        validEmail = System.getenv("TEST_EMAIL");
        validPassword = System.getenv("TEST_PASSWORD");

        Assert.assertNotNull(validEmail, "TEST_EMAIL environment variable is missing.");
        Assert.assertNotNull(validPassword, "TEST_PASSWORD environment variable is missing.");
        Assert.assertFalse(validEmail.trim().isEmpty(), "TEST_EMAIL cannot be empty.");
        Assert.assertFalse(validPassword.isEmpty(), "TEST_PASSWORD cannot be empty.");

        // Appium UiAutomator2 ক্যাপাবিলিটিস কনফিগারেশন
        UiAutomator2Options options = new UiAutomator2Options();
        options.setPlatformName("Android");
        options.setAutomationName("UiAutomator2");
        options.setDeviceName("Android Emulator");
        options.setUdid("emulator-5554");
        options.setAppPackage("com.ezycommunity.android");
        options.setAppActivity(".MainActivity");
        options.setNoReset(true); // পূর্বের অ্যাপ ডেটা অক্ষুণ্ণ রাখবে

        driver = new AndroidDriver(new URL("http://127.0.0.1:4723"), options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        System.out.println("EzyCommunityLogIn: Appium session started successfully.");
    }

    // প্রতি টেস্ট মেথডের আগে লগইন স্ক্রিন প্রস্তুত করা
    @BeforeMethod
    public void prepareLoginScreen() {
        try {
            ensureAppRunningAndOpen();
            Thread.sleep(1000);

            try {
                driver.hideKeyboard();
            } catch (Exception ignored) {}

            // আগের সেশন থাকলে লগআউট করে লগইন স্ক্রিনে আনবে
            logoutIfLoggedIn();

            wait.until(ExpectedConditions.visibilityOfElementLocated(signInButton));
            System.out.println("Login screen is ready for test execution.");

        } catch (Exception e) {
            System.out.println("Failed to prepare login screen: " + e.getMessage());
            Assert.fail("Could not reach login screen: " + e.getMessage(), e);
        }
    }

    // অ্যাপ মিনিমাইজ বা ব্যাকগ্রাউন্ডে থাকলে ফোরগ্রাউন্ডে আনা
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

    // ২-ধাপের লগআউট ফ্লো হ্যান্ডলার (ড্রয়ার -> কনফার্মেশন মোডাল -> সাইন ইন স্ক্রিন)
    private void logoutIfLoggedIn() {
        if (isLoginScreenVisible() || isVisible(editTextLocator)) {
            return;
        }

        if (!isLoggedIn()) {
            return;
        }

        // কনফার্মেশন মোডাল আগেই ওপেন থাকলে সরাসরি কনফার্ম করবে
        if (isVisible(logoutConfirmButton)) {
            clickElement(logoutConfirmButton);
            wait.until(ExpectedConditions.visibilityOfElementLocated(signInButton));
            return;
        }

        // ধাপ ১: প্রোফাইল ড্রয়ার ওপেন করা
        if (!isVisible(logoutMenuItem)) {
            clickElement(profileAvatar);
        }

        // ধাপ ২: ড্রয়ারের Log out মেনুতে ক্লিক
        WebElement menuLogoutButton = wait.until(
            ExpectedConditions.elementToBeClickable(logoutMenuItem)
        );
        menuLogoutButton.click();

        // ধাপ ৩: কনফার্মেশন মোডালে Log Out ক্লিক
        WebElement confirmButton = wait.until(
            ExpectedConditions.elementToBeClickable(logoutConfirmButton)
        );
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        confirmButton.click();

        // ধাপ ৪: সাইন ইন স্ক্রিন আসা পর্যন্ত অপেক্ষা
        wait.until(ExpectedConditions.visibilityOfElementLocated(signInButton));
    }

    private boolean isLoginScreenVisible() {
        return isVisible(signInButton) || isVisible(editTextLocator);
    }

    // ব্যবহারকারী ড্যাশবোর্ড বা লগইন অবস্থায় আছে কিনা যাচাই
    private boolean isLoggedIn() {
        return isVisible(profileAvatar)
            || isVisible(anyProfileAvatar)
            || isVisible(teamName)
            || isVisible(homeIcon)
            || isVisible(logoutMenuItem)
            || isVisible(logoutConfirmButton);
    }

    // স্ক্রিনে কোনো এলিমেন্ট দৃশ্যমান কিনা সেফলি চেক করা
    private boolean isVisible(By locator) {
        try {
            List<WebElement> elements = driver.findElements(locator);
            for (WebElement element : elements) {
                try {
                    if (element.isDisplayed()) return true;
                } catch (StaleElementReferenceException ignored) {}
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    private void clickElement(By locator) {
        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
        element.click();
    }

    // ফ্লাটারে কিবোর্ড ওপেন থাকলে প্রথম ক্লিকে আনফোকাস হয়, তাই সাবমিট নিশ্চিত করতে রিট্রাই রাখা হয়েছে
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

    // ইমেইল ও পাসওয়ার্ড ফিল্ডে ইনপুট দেওয়া এবং কিবোর্ড বন্ধ করা
    private void enterCredentials(String email, String password) {
        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(editTextLocator, 1));
        List<WebElement> fields = driver.findElements(editTextLocator);
        Assert.assertTrue(fields.size() >= 2, "Expected email and password fields");

        // ইমেইল ফিল্ড
        WebElement emailInput = fields.get(0);
        emailInput.click();
        emailInput.clear();
        if (!email.isEmpty()) {
            emailInput.sendKeys(email);
        }
        try { driver.hideKeyboard(); } catch (Exception ignored) {}

        // পাসওয়ার্ড ফিল্ড
        fields = driver.findElements(editTextLocator);
        WebElement passwordInput = (fields.size() >= 2) ? fields.get(1) :
            wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(editTextLocator, 1)).get(1);

        passwordInput.click();
        passwordInput.clear();
        if (!password.isEmpty()) {
            passwordInput.sendKeys(password);
        }
        try { driver.hideKeyboard(); } catch (Exception ignored) {}
    }

    // রিমেম্বার মি ও টার্মস অব সার্ভিস চেকবক্স টিক দেওয়া
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

    // টেস্টএনজি ডেটা প্রোভাইডার: ১৬টি পজিটিভ ও নেগেটিভ টেস্ট কেস
    @DataProvider(name = "loginCases")
    public Object[][] loginCases() {
        return new Object[][] {
            // খালি ফিল্ড
            {"TC-01 Empty email and password", "", "", false},
            // ইনভ্যালিড ইমেইল ফরম্যাট
            {"TC-02 Invalid email format", "invalid-email", "WrongPassword123!", false},
            // সঠিক ইমেইল কিন্তু ভুল পাসওয়ার্ড
            {"TC-03 Valid email with wrong password", validEmail, "WrongPassword123!", false},
            // অচেনা ইমেইল
            {"TC-04 Unknown email", "unknownuser@example.com", "WrongPassword123!", false},
            // ভুল ইমেইল ও ভুল পাসওয়ার্ড
            {"TC-05 Invalid email and wrong password", "wrong-email", "WrongPassword123!", false},
            // ইমেইল খালি, পাসওয়ার্ড দেওয়া
            {"TC-06 Empty email with password", "", validPassword, false},
            // পাসওয়ার্ড খালি
            {"TC-07 Email with empty password", validEmail, "", false},
            // ইমেইলে স্পেস (অ্যাপ স্বয়ংক্রিয়ভাবে ট্রিম করবে)
            {"TC-08 Leading spaces in email", "  " + validEmail, validPassword, true},
            {"TC-09 Trailing spaces in email", validEmail + "  ", validPassword, true},
            // বড় হাতের ইমেইল নরমালাইজেশন
            {"TC-10 Uppercase email", validEmail.toUpperCase(), validPassword, true},
            // পাসওয়ার্ডে স্পেস (অ্যাপ স্বয়ংক্রিয়ভাবে ট্রিম করবে)
            {"TC-11 Leading space in password", validEmail, " " + validPassword, true},
            {"TC-12 Trailing space in password", validEmail, validPassword + " ", true},
            // ভুল ইমেইল কিন্তু সঠিক পাসওয়ার্ড
            {"TC-13 Wrong email with valid password", "wronguser@example.com", validPassword, false},
            // সঠিক ইমেইল কিন্তু ভুল পাসওয়ার্ড
            {"TC-14 Valid email with incorrect password", validEmail, "IncorrectPassword123!", false},
            // শুধু স্পেস দেওয়া ক্রেডেনশিয়াল
            {"TC-15 Spaces-only credentials", "   ", "   ", false},
            // স্ট্যান্ডার্ড ভ্যালিড লগইন
            {"TC-16 Valid email and password", validEmail, validPassword, true}
        };
    }

    // ১৬টি টেস্ট কেস স্বয়ংক্রিয়ভাবে রান করবে এবং অ্যাসারশন দিয়ে ভ্যালিডেট করবে
    @Test(dataProvider = "loginCases")
    public void loginTest(String testCase, String email, String password, boolean expectedSuccess) {
        System.out.println("----------------------------------------------------------------");
        System.out.println("Executing: " + testCase);

        enterCredentials(email, password);

        try { driver.hideKeyboard(); } catch (Exception ignored) {}
        try { Thread.sleep(800); } catch (InterruptedException ignored) {}

        acceptTermsAndRememberMe();
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        clickSignIn();

        boolean actualSuccess = false;

        // প্রত্যাশিত ফলাফলের ভিত্তিতে ড্যাশবোর্ড উপস্থিতি যাচাই
        if (expectedSuccess) {
            try {
                WebDriverWait loginWait = new WebDriverWait(driver, Duration.ofSeconds(25));
                loginWait.until(d -> isLoggedIn());
                actualSuccess = true;
            } catch (TimeoutException e) {
                actualSuccess = false;
            }
        } else {
            try {
                WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
                shortWait.until(d -> isLoggedIn());
                actualSuccess = true;
            } catch (TimeoutException e) {
                actualSuccess = false;
            }
        }

        System.out.println(testCase + " | Expected: " + expectedSuccess + " | Actual: " + actualSuccess);

        // ফলাফল প্রত্যাশার সাথে মিলছে কিনা যাচাই
        Assert.assertEquals(actualSuccess, expectedSuccess, "Unexpected login result for " + testCase);
    }

    // টেস্ট শেষে Appium সেশন বন্ধ করা
    @AfterClass(alwaysRun = true)
    public void tearDown() {
        if (driver != null) {
            driver.quit();
            System.out.println("EzyCommunityLogIn: Appium session closed successfully.");
        }
    }
}
