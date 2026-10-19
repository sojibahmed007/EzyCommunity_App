
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
        "//android.view.View[@content-desc='SQA Team']" +
        "/preceding-sibling::android.widget.ImageView[1]"
    );

    private final By teamName =
        AppiumBy.accessibilityId("SQA Team");

    private final By homeIcon =
        AppiumBy.accessibilityId("Home");

    private final By signInButton =
        AppiumBy.accessibilityId("Sign In");

    // Profile menu logout option (First click: ImageView with content-desc="Log out")
    private final By logoutMenuItem = By.xpath(
        "//android.widget.ImageView[@content-desc='Log out' or @content-desc='Log Out']"
    );

    // Confirmation modal logout button (Second click: Button with content-desc="Log Out")
    private final By logoutConfirmButton = By.xpath(
        "//android.widget.Button[@content-desc='Log Out' or @content-desc='Log out']"
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

    @BeforeMethod
    public void prepareLoginScreen() {

        try {
            driver.activateApp("com.ezycommunity.android");

            Thread.sleep(1000);

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

        // Do not attempt logout if already logged out.
        if (isLoginScreenVisible()) {
            System.out.println("Already on login screen.");
            return;
        }

        // Ensure the authenticated screen is ready.
        if (!isLoggedIn()) {
            System.out.println(
                "Login state is not confirmed. Checking UI..."
            );
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
        return isVisible(signInButton);
    }

    private boolean isLoggedIn() {
        return isVisible(profileAvatar)
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

        List<WebElement> fields =
            driver.findElements(editTextLocator);

        Assert.assertTrue(
            fields.size() >= 2,
            "Expected email and password fields; found "
                + fields.size()
        );

        WebElement emailInput = fields.get(0);
        WebElement passwordInput = fields.get(1);

        emailInput.click();
        emailInput.clear();

        if (!email.isEmpty()) {
            emailInput.sendKeys(email);
        }

        passwordInput.click();
        passwordInput.clear();

        if (!password.isEmpty()) {
            passwordInput.sendKeys(password);
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
                validEmail, " " + validPassword, false},

            {"TC-12 Trailing space in password",
                validEmail, validPassword + " ", false},

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

        // Hide the keyboard if open.
        try {
            driver.hideKeyboard();
        } catch (Exception ignored) {
            // Keyboard may already be hidden.
        }

        clickElement(signInButton);

        boolean actualSuccess;

        try {
            wait.until(
                d -> isLoggedIn() || isLoginScreenVisible()
            );

            actualSuccess = isLoggedIn();

        } catch (TimeoutException e) {
            actualSuccess = false;
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
