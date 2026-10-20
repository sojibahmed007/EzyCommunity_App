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

/**
 * ============================================================================
 * CLASS: EzyCommunityTest
 * ============================================================================
 * PURPOSE:
 * Comprehensive, Data-Driven Authentication & Session Test Suite for the 
 * Flutter-based EzyCommunity Android Mobile Application.
 *
 * WHAT THIS SUITE TESTS:
 * 1. Positive Login: Valid credentials login successfully to the Dashboard.
 * 2. Negative Login: Empty, invalid format, wrong password, and wrong email.
 * 3. Whitespace & Trimming: Validates that leading/trailing spaces in email 
 *    and password are auto-trimmed by the app without causing unexpected failures.
 * 4. Case Sensitivity: Uppercase email normalization.
 * 5. Two-Step Logout Flow: Opens Profile Drawer -> clicks 'Log out' -> confirms 
 *    in the confirmation modal dialog -> lands cleanly on Sign In screen.
 *
 * KEY AUTOMATION CONCEPTS FOR BEGINNERS:
 * - Appium UiAutomator2: The modern automation engine for Android apps.
 * - Flutter UI Automation: Flutter draws directly on a Canvas. Elements usually 
 *   do not have native Android resource-ids, so we rely on Accessibility IDs 
 *   (content-desc), bounds, and XPath axes (preceding-sibling / following-sibling).
 * - Explicit Waits: WebDriverWait is used instead of static Thread.sleep so tests 
 *   run as fast as possible while remaining 100% stable.
 * ============================================================================
 */
public class EzyCommunityTest {

    // ------------------------------------------------------------------------
    // CORE DRIVER & WAIT INSTANCES
    // ------------------------------------------------------------------------
    // 'driver': Manages communication between this test script and the Appium server.
    private AndroidDriver driver;

    // 'wait': Explicit wait instance used to wait dynamically for elements to appear.
    private WebDriverWait wait;

    // Test credentials loaded securely from environment variables.
    private String validEmail;
    private String validPassword;

    // ------------------------------------------------------------------------
    // ELEMENT LOCATORS (UiAutomator2 / XPath / Accessibility ID)
    // ------------------------------------------------------------------------

    /**
     * Profile Avatar on Dashboard:
     * Located to the left of the community name "SQA Team".
     * In Flutter, the avatar View has clickable="true" and precedes "SQA Team".
     * We use XPath axis 'preceding-sibling' to target it regardless of what photo is set.
     */
    private final By profileAvatar = By.xpath(
        "//android.view.View[@content-desc='SQA Team']/preceding-sibling::*[@clickable='true'][1]" +
        " | //android.view.View[@content-desc='SQA Team']/preceding-sibling::*[1]" +
        " | //android.view.View[@content-desc='SQA Team']/preceding-sibling::android.widget.ImageView[1]"
    );

    /**
     * Dashboard Community Header:
     * AppiumBy.accessibilityId targets the Android 'content-desc' attribute directly.
     * This is the fastest and most reliable locator strategy in mobile automation.
     */
    private final By teamName = AppiumBy.accessibilityId("SQA Team");

    // Home navigation icon at the bottom navigation bar.
    private final By homeIcon = AppiumBy.accessibilityId("Home");

    // Sign In button on the authentication screen.
    private final By signInButton = AppiumBy.accessibilityId("Sign In");

    /**
     * Step 1 Logout Menu Item (Inside the Profile Drawer):
     * Matches the 'Log out' row in the side drawer.
     */
    private final By logoutMenuItem = By.xpath(
        "//*[@content-desc='Log out' or @content-desc='Log Out']"
    );

    /**
     * Step 2 Logout Confirmation Button (Inside the Modal Dialog):
     * In Flutter, the confirmation dialog has both a title ("Log Out") and a button ("Log Out").
     * Specifying '//android.widget.Button' ensures we click the actionable button, not the title text.
     */
    private final By logoutConfirmButton = By.xpath(
        "//android.widget.Button[@content-desc='Log Out' or @content-desc='Log out']"
    );

    // Launcher icon on the Android Home Screen (used to bring app to foreground if closed).
    private final By appLauncherIcon = By.xpath(
        "//*[@content-desc='EzyCommunity' or @text='EzyCommunity']"
    );

    // Checkboxes on Login Screen: "Remember me" and "I agree to Terms of Service".
    private final By checkBoxesLocator = By.className("android.widget.CheckBox");

    // Specific locator for 'Remember me' checkbox.
    private final By rememberMeCheckBox = By.xpath(
        "(//android.widget.CheckBox)[1] | //android.view.View[@content-desc='Remember me']/preceding-sibling::android.widget.CheckBox"
    );

    // Specific locator for 'Terms of Service' agreement checkbox.
    private final By termsCheckBox = By.xpath(
        "(//android.widget.CheckBox)[2] | //*[contains(@content-desc, 'I agree')]/preceding-sibling::android.widget.CheckBox"
    );

    // General EditText locator (used to locate input fields on any screen).
    private final By editTextLocator = By.className("android.widget.EditText");

    // Matches any logged-in profile avatar in the drawer (where content-desc contains the '@' username handle).
    private final By anyProfileAvatar = By.xpath(
        "//android.widget.ImageView[contains(@content-desc, '@')]"
    );

    // ========================================================================
    // SETUP & INITIALIZATION (@BeforeClass)
    // ========================================================================
    /**
     * setUp(): Runs ONCE before any test method in this class executes.
     * PURPOSE:
     * 1. Loads credentials from Environment Variables (Security Best Practice).
     * 2. Configures Appium Desired Capabilities (UiAutomator2Options).
     * 3. Initializes AndroidDriver and establishes connection with Appium server.
     */
    @BeforeClass
    public void setUp() throws Exception {
        // Step 1: Read credentials from system environment variables.
        // TIP: Never hardcode sensitive passwords directly in source code or Git repositories!
        validEmail = System.getenv("TEST_EMAIL");
        validPassword = System.getenv("TEST_PASSWORD");

        Assert.assertNotNull(validEmail, "TEST_EMAIL environment variable is missing.");
        Assert.assertNotNull(validPassword, "TEST_PASSWORD environment variable is missing.");
        Assert.assertFalse(validEmail.trim().isEmpty(), "TEST_EMAIL cannot be empty.");
        Assert.assertFalse(validPassword.isEmpty(), "TEST_PASSWORD cannot be empty.");

        // Step 2: Configure UiAutomator2 Options (Desired Capabilities)
        UiAutomator2Options options = new UiAutomator2Options();
        options.setPlatformName("Android");
        options.setAutomationName("UiAutomator2");             // Standard Android engine
        options.setDeviceName("Android Emulator");             // Device name
        options.setUdid("emulator-5554");                      // Target emulator identifier
        options.setAppPackage("com.ezycommunity.android");     // App Package name
        options.setAppActivity(".MainActivity");               // Entry point activity
        options.setNoReset(true);                              // Preserves app data & session between runs

        // Step 3: Start Appium Session on localhost port 4723
        driver = new AndroidDriver(
            new URL("http://127.0.0.1:4723"),
            options
        );

        // Step 4: Create a 20-second explicit wait instance for dynamic synchronization
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        System.out.println("EzyCommunityTest: Appium session started successfully.");
    }

    // ========================================================================
    // TEST PREPARATION LIFECYCLE (@BeforeMethod)
    // ========================================================================
    /**
     * prepareLoginScreen(): Runs BEFORE EACH individual test method.
     * PURPOSE:
     * Ensures test isolation. Every test starts from a clean, known state:
     * - The app is running in the foreground.
     * - The keyboard is hidden so elements are not obstructed.
     * - If the user is logged in from a previous test, it logs out.
     * - Waits until the "Sign In" button is visible and ready for interaction.
     */
    @BeforeMethod
    public void prepareLoginScreen() {
        try {
            ensureAppRunningAndOpen();

            Thread.sleep(1000);

            // Hide keyboard if it was left open from a previous screen
            try {
                driver.hideKeyboard();
            } catch (Exception ignored) {}

            // Ensure we are logged out so the current test can test login
            logoutIfLoggedIn();

            // Wait for Sign In button to be visible
            wait.until(ExpectedConditions.visibilityOfElementLocated(signInButton));

            System.out.println("Login screen is ready for test execution.");

        } catch (Exception e) {
            System.out.println("Failed to prepare login screen: " + e.getMessage());
            try {
                System.out.println(driver.getPageSource());
            } catch (Exception ignored) {}

            Assert.fail("Could not reach login screen: " + e.getMessage(), e);
        }
    }

    /**
     * ensureAppRunningAndOpen():
     * Verifies the app is in the foreground. If minimized or on launcher, activates it.
     */
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

    /**
     * logoutIfLoggedIn():
     * Handles the full 2-Step Logout workflow in Flutter:
     * Step 1: Tap Profile Avatar (opens side drawer)
     * Step 2: Click 'Log out' menu item (opens confirmation modal dialog)
     * Step 3: Click 'Log Out' button inside the confirmation dialog
     * Step 4: Wait for the Sign In screen to appear.
     */
    private void logoutIfLoggedIn() {
        // If already on login screen, no logout needed
        if (isLoginScreenVisible() || isVisible(editTextLocator)) {
            System.out.println("Already on login screen.");
            return;
        }

        // If not logged in, skip logout
        if (!isLoggedIn()) {
            System.out.println("Not logged in. Skipping logout.");
            return;
        }

        // If confirmation modal is already open from an earlier interaction, confirm directly
        if (isVisible(logoutConfirmButton)) {
            System.out.println("Confirmation modal is already open.");
            clickElement(logoutConfirmButton);
            wait.until(ExpectedConditions.visibilityOfElementLocated(signInButton));
            System.out.println("Logout completed. Login screen is ready.");
            return;
        }

        // Step 1: Open Profile Drawer if not already open
        if (!isVisible(logoutMenuItem)) {
            clickElement(profileAvatar);
            System.out.println("Profile avatar clicked.");
        }

        // Step 2: Click 'Log out' from the drawer menu
        WebElement menuLogoutButton = wait.until(
            ExpectedConditions.elementToBeClickable(logoutMenuItem)
        );
        menuLogoutButton.click();
        System.out.println("Profile menu Log out clicked.");

        // Step 3: Wait for confirmation modal to appear
        WebElement confirmButton = wait.until(
            ExpectedConditions.elementToBeClickable(logoutConfirmButton)
        );

        // Small pause to allow modal slide-in animation to settle
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        // Step 4: Click 'Log Out' on the modal
        confirmButton.click();
        System.out.println("Confirmation modal Log Out clicked.");

        // Step 5: Wait until Sign In screen is visible
        wait.until(ExpectedConditions.visibilityOfElementLocated(signInButton));
        System.out.println("Logout completed. Login screen is ready.");
    }

    /**
     * isLoginScreenVisible():
     * Returns true if either the 'Sign In' button or input fields are on screen.
     */
    private boolean isLoginScreenVisible() {
        return isVisible(signInButton) || isVisible(editTextLocator);
    }

    /**
     * isLoggedIn():
     * Checks multiple indicators to determine if an authenticated dashboard/session is active.
     */
    private boolean isLoggedIn() {
        return isVisible(profileAvatar)
            || isVisible(anyProfileAvatar)
            || isVisible(teamName)
            || isVisible(homeIcon)
            || isVisible(logoutMenuItem)
            || isVisible(logoutConfirmButton);
    }

    /**
     * isVisible(locator):
     * Helper method to safely check if an element is currently displayed on screen 
     * without throwing an exception if it is absent.
     */
    private boolean isVisible(By locator) {
        try {
            List<WebElement> elements = driver.findElements(locator);
            for (WebElement element : elements) {
                try {
                    if (element.isDisplayed()) {
                        return true;
                    }
                } catch (StaleElementReferenceException ignored) {
                    // Flutter refreshed the DOM; element detached
                }
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    /**
     * clickElement(locator):
     * Waits until the element is clickable, then performs the click.
     */
    private void clickElement(By locator) {
        WebElement element = wait.until(
            ExpectedConditions.elementToBeClickable(locator)
        );
        element.click();
    }

    /**
     * clickSignIn():
     * FLUTTER SPECIFIC BEHAVIOR:
     * In Flutter apps, when an input field (EditText) is currently focused and the soft keyboard 
     * is open, tapping a button often only serves to unfocus the EditText and close the keyboard, 
     * rather than triggering the button's onPressed handler.
     * 
     * To solve this:
     * We click the Sign In button once. If after 1.2s the screen hasn't changed, we click again 
     * (with an Actions coordinate fallback at 540, 1477) to guarantee form submission.
     */
    private void clickSignIn() {
        try {
            WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(signInButton));
            btn.click();
        } catch (Exception ignored) {}

        try {
            Thread.sleep(1200);
            if (!isLoggedIn() && isLoginScreenVisible()) {
                System.out.println("Retrying Sign In click to ensure submission after unfocus...");
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

    /**
     * enterCredentials(email, password):
     * Fills the email and password fields sequentially.
     * Steps:
     * 1. Finds EditText[0] (Email), clicks, clears existing text, and enters new email.
     * 2. Hides keyboard so the password field is unobstructed.
     * 3. Finds EditText[1] (Password), clicks, clears, and enters password.
     * 4. Hides keyboard again.
     */
    private void enterCredentials(String email, String password) {
        wait.until(
            ExpectedConditions.numberOfElementsToBeMoreThan(editTextLocator, 1)
        );

        List<WebElement> fields = driver.findElements(editTextLocator);
        Assert.assertTrue(
            fields.size() >= 2,
            "Expected email and password fields; found " + fields.size()
        );

        // 1. Fill Email field
        WebElement emailInput = fields.get(0);
        emailInput.click();
        emailInput.clear();
        if (!email.isEmpty()) {
            emailInput.sendKeys(email);
        }

        // Hide keyboard to prevent layout shifts
        try { driver.hideKeyboard(); } catch (Exception ignored) {}

        // 2. Locate and fill Password field
        fields = driver.findElements(editTextLocator);
        WebElement passwordInput;
        if (fields.size() >= 2) {
            passwordInput = fields.get(1);
        } else {
            fields = wait.until(
                ExpectedConditions.numberOfElementsToBeMoreThan(editTextLocator, 1)
            );
            passwordInput = fields.get(1);
        }

        passwordInput.click();
        passwordInput.clear();
        if (!password.isEmpty()) {
            passwordInput.sendKeys(password);
        }

        // Hide keyboard after entering credentials
        try { driver.hideKeyboard(); } catch (Exception ignored) {}
    }

    /**
     * acceptTermsAndRememberMe():
     * Ensures both the "Remember me" and "I agree to Terms" checkboxes are checked.
     * Reads the 'checked' attribute so we do not uncheck an already-checked box!
     */
    private void acceptTermsAndRememberMe() {
        try {
            List<WebElement> checkBoxes = driver.findElements(checkBoxesLocator);
            for (WebElement cb : checkBoxes) {
                try {
                    String checkedAttr = cb.getAttribute("checked");
                    if (!"true".equalsIgnoreCase(checkedAttr)) {
                        cb.click();
                        System.out.println("Ticked checkbox because checked attribute was: " + checkedAttr);
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.out.println("Checkbox handling notice: " + e.getMessage());
        }
    }

    // ========================================================================
    // DATA-DRIVEN TEST MATRIX (@DataProvider)
    // ========================================================================
    /**
     * loginCases():
     * TestNG DataProvider supplying 16 comprehensive positive and negative scenarios:
     * Parameters provided per row:
     * 1. Test Description (String)
     * 2. Email payload (String)
     * 3. Password payload (String)
     * 4. Expected Outcome (boolean: true = login success, false = login failure)
     */
    @DataProvider(name = "loginCases")
    public Object[][] loginCases() {
        return new Object[][] {
            // Negative: Empty Fields
            {"TC-01 Empty email and password",
                "", "", false},

            // Negative: Invalid Email Syntax
            {"TC-02 Invalid email format",
                "invalid-email", "WrongPassword123!", false},

            // Negative: Valid email with incorrect password
            {"TC-03 Valid email with wrong password",
                validEmail, "WrongPassword123!", false},

            // Negative: Non-existent account
            {"TC-04 Unknown email",
                "unknownuser@example.com", "WrongPassword123!", false},

            // Negative: Invalid email and wrong password
            {"TC-05 Invalid email and wrong password",
                "wrong-email", "WrongPassword123!", false},

            // Negative: Missing email
            {"TC-06 Empty email with password",
                "", validPassword, false},

            // Negative: Missing password
            {"TC-07 Email with empty password",
                validEmail, "", false},

            // Positive: Leading spaces in email (app must auto-trim whitespace)
            {"TC-08 Leading spaces in email",
                "  " + validEmail, validPassword, true},

            // Positive: Trailing spaces in email (app must auto-trim whitespace)
            {"TC-09 Trailing spaces in email",
                validEmail + "  ", validPassword, true},

            // Positive: Uppercase email (emails are case-insensitive per RFC standards)
            {"TC-10 Uppercase email",
                validEmail.toUpperCase(), validPassword, true},

            // Positive: Leading space in password (app auto-trims whitespace)
            {"TC-11 Leading space in password",
                validEmail, " " + validPassword, true},

            // Positive: Trailing space in password (app auto-trims whitespace)
            {"TC-12 Trailing space in password",
                validEmail, validPassword + " ", true},

            // Negative: Wrong user with valid password
            {"TC-13 Wrong email with valid password",
                "wronguser@example.com", validPassword, false},

            // Negative: Valid user with incorrect password variation
            {"TC-14 Valid email with incorrect password",
                validEmail, "IncorrectPassword123!", false},

            // Negative: Only blank spaces in both fields
            {"TC-15 Spaces-only credentials",
                "   ", "   ", false},

            // Positive: Standard valid login baseline
            {"TC-16 Valid email and password",
                validEmail, validPassword, true}
        };
    }

    // ========================================================================
    // TEST METHOD EXECUTION (@Test)
    // ========================================================================
    /**
     * loginTest():
     * Executed 16 times automatically by TestNG using data from 'loginCases'.
     *
     * FLOW:
     * 1. Types credentials into Email & Password fields.
     * 2. Hides keyboard and ticks required agreement checkboxes.
     * 3. Clicks Sign In button (with Flutter unfocus retry).
     * 4. Evaluates outcome:
     *    - If expectedSuccess is true: waits up to 25s for dashboard to appear.
     *    - If expectedSuccess is false: waits 5s and asserts user remains on login screen.
     * 5. Uses Assert.assertEquals to validate actual vs expected outcome.
     */
    @Test(dataProvider = "loginCases")
    public void loginTest(
        String testCase,
        String email,
        String password,
        boolean expectedSuccess
    ) {
        System.out.println("----------------------------------------------------------------");
        System.out.println("Executing: " + testCase);

        enterCredentials(email, password);

        // Hide keyboard so checkboxes and sign in button are fully visible
        try {
            driver.hideKeyboard();
        } catch (Exception ignored) {}

        // Allow UI to stabilize after keyboard closes
        try { Thread.sleep(800); } catch (InterruptedException ignored) {}

        // Ensure "Remember me" and "I agree to Terms" checkboxes are checked
        acceptTermsAndRememberMe();

        // Small wait for checkbox state to register before tapping Sign In
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        clickSignIn();

        boolean actualSuccess = false;

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

        System.out.println(
            testCase
            + " | Expected: " + expectedSuccess
            + " | Actual: " + actualSuccess
        );

        // Assertion: Fails the test if actual result does not match expected result
        Assert.assertEquals(
            actualSuccess,
            expectedSuccess,
            "Unexpected login result for " + testCase
        );
    }

    // ========================================================================
    // TEARDOWN (@AfterClass)
    // ========================================================================
    /**
     * tearDown(): Runs ONCE after all tests in this class complete.
     * PURPOSE:
     * Closes the AndroidDriver and terminates the Appium session cleanly.
     * 'alwaysRun = true' ensures cleanup runs even if previous tests threw exceptions.
     */
    @AfterClass(alwaysRun = true)
    public void tearDown() {
        if (driver != null) {
            driver.quit();
            System.out.println("EzyCommunityTest: Appium session closed successfully.");
        }
    }
}
