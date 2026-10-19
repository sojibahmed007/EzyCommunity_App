
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

public class EzyCommunityTest {

    private AndroidDriver driver;
    private WebDriverWait wait;

    private String validEmail;
    private String validPassword;

    private final By profileAvatar = By.xpath(
        "//android.view.View[@content-desc='SQA Team']/preceding-sibling::*[@clickable='true'][1]" +
        " | //android.view.View[@content-desc='SQA Team']/preceding-sibling::*[1]" +
        " | //android.view.View[@content-desc='SQA Team']/preceding-sibling::android.widget.ImageView[1]"
    );

    private final By teamName =
        AppiumBy.accessibilityId("SQA Team");

    private final By homeIcon =
        AppiumBy.accessibilityId("Home");

    private final By signInButton =
        AppiumBy.accessibilityId("Sign In");

    // Profile menu logout option (content-desc="Log out" or "Log Out")
    private final By logoutMenuItem = By.xpath(
        "//*[@content-desc='Log out' or @content-desc='Log Out']"
    );

    // Confirmation modal logout button (Second click: Button with content-desc="Log Out")
    private final By logoutConfirmButton = By.xpath(
        "//android.widget.Button[@content-desc='Log Out' or @content-desc='Log out']"
    );

    // Android home screen / launcher app icon
    private final By appLauncherIcon = By.xpath(
        "//*[@content-desc='EzyCommunity' or @text='EzyCommunity']"
    );

    // Login screen checkboxes: Remember me & Terms of Service
    private final By checkBoxesLocator =
        By.className("android.widget.CheckBox");

    private final By rememberMeCheckBox = By.xpath(
        "(//android.widget.CheckBox)[1] | //android.view.View[@content-desc='Remember me']/preceding-sibling::android.widget.CheckBox"
    );

    private final By termsCheckBox = By.xpath(
        "(//android.widget.CheckBox)[2] | //*[contains(@content-desc, 'I agree')]/preceding-sibling::android.widget.CheckBox"
    );

    private final By editTextLocator =
        By.className("android.widget.EditText");

    @BeforeClass
    public void setUp() throws Exception {

        validEmail = System.getenv("TEST_EMAIL");
        validPassword = System.getenv("TEST_PASSWORD");

        Assert.assertNotNull(
            validEmail,
            "TEST_EMAIL environment variable is missing."
        );

        Assert.assertNotNull(
            validPassword,
            "TEST_PASSWORD environment variable is missing."
        );

        Assert.assertFalse(
            validEmail.trim().isEmpty(),
            "TEST_EMAIL cannot be empty."
        );

        Assert.assertFalse(
            validPassword.isEmpty(),
            "TEST_PASSWORD cannot be empty."
        );

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

        wait = new WebDriverWait(
            driver,
            Duration.ofSeconds(20)
        );

        System.out.println("Appium session started.");
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

    @BeforeMethod
    public void prepareLoginScreen() {

        try {
            ensureAppRunningAndOpen();

            Thread.sleep(1000);

            try {
                driver.hideKeyboard();
            } catch (Exception ignored) {}

            logoutIfLoggedIn();

            wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                    signInButton
                )
            );

            System.out.println("Login screen is ready.");

        } catch (Exception e) {

            System.out.println(
                "Failed to prepare login screen: " + e.getMessage()
            );

            try {
                System.out.println(driver.getPageSource());
            } catch (Exception ignored) {
                // Preserve the original exception.
            }

            Assert.fail(
                "Could not reach login screen: " + e.getMessage(),
                e
            );
        }
    }

    private void logoutIfLoggedIn() {

        // Do not attempt logout if already logged out or on login screen.
        if (isLoginScreenVisible() || isVisible(editTextLocator)) {
            System.out.println("Already on login screen.");
            return;
        }

        // Only attempt logout if confirmed logged in.
        if (!isLoggedIn()) {
            System.out.println("Not logged in. Skipping logout.");
            return;
        }

        // If confirmation modal is already open, click Log Out directly
        if (isVisible(logoutConfirmButton)) {
            System.out.println("Confirmation modal is already open.");
            clickElement(logoutConfirmButton);
            wait.until(ExpectedConditions.visibilityOfElementLocated(signInButton));
            System.out.println("Logout completed. Login screen is ready.");
            return;
        }

        // Step 1: Open the profile menu if not already open.
        if (!isVisible(logoutMenuItem)) {
            clickElement(profileAvatar);
            System.out.println("Profile avatar clicked.");
        }

        // Step 2: Click the profile menu Log out option once.
        WebElement menuLogoutButton = wait.until(
            ExpectedConditions.elementToBeClickable(
                logoutMenuItem
            )
        );

        menuLogoutButton.click();

        System.out.println(
            "Profile menu Log out clicked once."
        );

        // Step 3: Wait for the confirmation modal button to be clickable.
        WebElement confirmButton = wait.until(
            ExpectedConditions.elementToBeClickable(
                logoutConfirmButton
            )
        );

        // Allow modal animation to complete before clicking
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) {}

        // Step 4: Click confirmation modal Log Out button once.
        confirmButton.click();

        System.out.println(
            "Confirmation modal Log Out clicked once."
        );

        // Step 5: Wait for logout to finish.
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                signInButton
            )
        );

        System.out.println(
            "Logout completed. Login screen is ready."
        );
    }

    private boolean isLoginScreenVisible() {
        return isVisible(signInButton) || isVisible(editTextLocator);
    }

    // Matches any logged-in profile avatar (content-desc contains '@' handle)
    private final By anyProfileAvatar = By.xpath(
        "//android.widget.ImageView[contains(@content-desc, '@')]"
    );

    private boolean isLoggedIn() {
        return isVisible(profileAvatar)
            || isVisible(anyProfileAvatar)
            || isVisible(teamName)
            || isVisible(homeIcon)
            || isVisible(logoutMenuItem)
            || isVisible(logoutConfirmButton);
    }

    private boolean isVisible(By locator) {

        try {
            List<WebElement> elements =
                driver.findElements(locator);

            for (WebElement element : elements) {
                try {
                    if (element.isDisplayed()) {
                        return true;
                    }
                } catch (StaleElementReferenceException ignored) {
                    // UI changed; continue checking.
                }
            }

        } catch (Exception ignored) {
            return false;
        }

        return false;
    }

    private void clickElement(By locator) {

        WebElement element = wait.until(
            ExpectedConditions.elementToBeClickable(locator)
        );

        element.click();
    }

    private void clickSignIn() {
        try {
            WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(signInButton));
            btn.click();
        } catch (Exception ignored) {}

        // In Flutter, if an EditText had focus, the first tap unfocuses the field.
        // If still on login screen after 1.2s, tap again to ensure form submission.
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

    private void enterCredentials(
        String email,
        String password
    ) {

        wait.until(
            ExpectedConditions.numberOfElementsToBeMoreThan(
                editTextLocator,
                1
            )
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

        // Hide keyboard so password field remains accessible
        try {
            driver.hideKeyboard();
        } catch (Exception ignored) {}

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
        try {
            driver.hideKeyboard();
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
                        System.out.println("Ticked checkbox because checked was: " + checkedAttr);
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.out.println("Checkbox handling notice: " + e.getMessage());
        }
    }

    @DataProvider(name = "loginCases")
    public Object[][] loginCases() {

        return new Object[][] {

            {"TC-01 Empty email and password",
                "", "", false},

            {"TC-02 Invalid email format",
                "invalid-email", "WrongPassword123!", false},

            {"TC-03 Valid email with wrong password",
                validEmail, "WrongPassword123!", false},

            {"TC-04 Unknown email",
                "unknownuser@example.com",
                "WrongPassword123!", false},

            {"TC-05 Invalid email and wrong password",
                "wrong-email", "WrongPassword123!", false},

            {"TC-06 Empty email with password",
                "", validPassword, false},

            {"TC-07 Email with empty password",
                validEmail, "", false},

            {"TC-08 Leading spaces in email",
                "  " + validEmail, validPassword, true},

            {"TC-09 Trailing spaces in email",
                validEmail + "  ", validPassword, true},

            {"TC-10 Uppercase email",
                validEmail.toUpperCase(), validPassword, true},

            {"TC-11 Leading space in password",
                validEmail, " " + validPassword, true},

            {"TC-12 Trailing space in password",
                validEmail, validPassword + " ", true},

            {"TC-13 Wrong email with valid password",
                "wronguser@example.com", validPassword, false},

            {"TC-14 Valid email with incorrect password",
                validEmail, "IncorrectPassword123!", false},

            {"TC-15 Spaces-only credentials",
                "   ", "   ", false},

            {"TC-16 Valid email and password",
                validEmail, validPassword, true}
        };
    }

    @Test(dataProvider = "loginCases")
    public void loginTest(
        String testCase,
        String email,
        String password,
        boolean expectedSuccess
    ) {

        enterCredentials(email, password);

        // Hide the keyboard so checkboxes and sign in button are fully visible
        try {
            driver.hideKeyboard();
        } catch (Exception ignored) {}

        // Wait for UI to stabilize after keyboard dismissal
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

        Assert.assertEquals(
            actualSuccess,
            expectedSuccess,
            "Unexpected login result for " + testCase
        );
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {

        if (driver != null) {
            driver.quit();
            System.out.println("Appium session closed.");
        }
    }
}
