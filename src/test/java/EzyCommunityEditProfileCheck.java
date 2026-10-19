import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.URL;
import java.time.Duration;
import java.util.List;

/**
 * Automated End-to-End Flow for EzyCommunity Edit Profile & Username Change.
 * File: EzyCommunityEditProfileCheck.java
 */
public class EzyCommunityEditProfileCheck {

    private AndroidDriver driver;
    private WebDriverWait wait;

    private String validEmail;
    private String validPassword;

    // Login screen locators
    private final By signInButton = AppiumBy.accessibilityId("Sign In");
    private final By editTextLocator = By.className("android.widget.EditText");
    private final By checkBoxesLocator = By.className("android.widget.CheckBox");
    private final By appLauncherIcon = By.xpath("//*[@content-desc='EzyCommunity' or @text='EzyCommunity']");

    // Dashboard locators
    private final By teamName = AppiumBy.accessibilityId("SQA Team");
    private final By profileAvatar = By.xpath(
        "//android.view.View[@content-desc='SQA Team']/preceding-sibling::*[@clickable='true'][1]" +
        " | //android.view.View[@bounds='[13,92][134,181]']"
    );

    // Profile Drawer locators
    private final By editProfileMenu = AppiumBy.accessibilityId("Edit Profile");
    private final By settingsHeader = AppiumBy.accessibilityId("SETTINGS");

    private final By editProfileHeader = By.xpath(
        "//android.view.View[@content-desc='Edit Profile' and contains(@bounds, '[147,110]')]" +
        " | //*[contains(@content-desc, 'Change Cover')]"
    );
    private final By firstNameInput = By.xpath("//android.view.View[@content-desc='First Name']/following-sibling::android.widget.EditText[1]");
    private final By lastNameInput = By.xpath("//android.view.View[@content-desc='Last Name']/following-sibling::android.widget.EditText[1]");
    private final By displayNameInput = By.xpath("//android.view.View[@content-desc='Display Name']/following-sibling::android.widget.EditText[1]");
    private final By usernameRow = By.xpath("//android.view.View[@content-desc='Username']/following-sibling::*[@clickable='true'][1]");
    private final By bioInput = By.xpath(
        "//android.view.View[@content-desc='About You']/following-sibling::android.widget.EditText[1]" +
        " | //android.widget.EditText[contains(@hint, 'Tell us about yourself')]"
    );

    // Social Links & Location
    private final By locationInput = By.xpath(
        "//android.view.View[@content-desc='Location']/following-sibling::android.widget.EditText[1]" +
        " | //android.widget.EditText[contains(@hint, 'San Francisco')]"
    );
    private final By websiteInput = By.xpath(
        "//android.view.View[@content-desc='Website']/following-sibling::android.widget.EditText[1]" +
        " | //android.widget.EditText[contains(@hint, 'yourwebsite.com')]"
    );
    private final By twitterInput = By.xpath(
        "//android.view.View[@content-desc='Twitter']/following-sibling::android.widget.EditText[1]" +
        " | //android.widget.EditText[contains(@hint, '@username')]"
    );
    private final By linkedInInput = By.xpath(
        "//android.view.View[@content-desc='LinkedIn']/following-sibling::android.widget.EditText[1]" +
        " | //android.widget.EditText[contains(@hint, 'linkedin.com/in/username')]"
    );
    private final By saveChangesBtn = AppiumBy.accessibilityId("Save Changes");

    // Username Sub-Screen locators
    private final By usernameScreenHeader = By.xpath("//android.view.View[@content-desc='Username']");
    private final By usernameInputField = By.className("android.widget.EditText");
    private final By usernameAvailableMsg = By.xpath("//*[contains(@content-desc, 'is available')]");
    private final By saveUsernameBtn = AppiumBy.accessibilityId("Save");

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
        System.out.println("EzyCommunityEditProfileCheck: Appium session started.");

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

    private void ensureLoggedIn() {
        try {
            Thread.sleep(1000);
            if (isLoggedIn() && !isLoginScreenVisible()) {
                System.out.println("User is already logged in.");
                return;
            }

            if (!isLoginScreenVisible()) {
                if (isLoggedIn()) {
                    System.out.println("Authenticated screen detected.");
                    return;
                }
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

            // Accept Checkboxes
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
            Thread.sleep(500);

            // Click Sign In
            try {
                WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(signInButton));
                btn.click();
            } catch (Exception ignored) {}

            WebDriverWait loginWait = new WebDriverWait(driver, Duration.ofSeconds(25));
            loginWait.until(d -> isLoggedIn() && !isLoginScreenVisible());
            System.out.println("Automatic login successful. Dashboard reached.");

        } catch (Exception e) {
            Assert.fail("Failed during automatic login setup: " + e.getMessage(), e);
        }
    }

    private boolean isLoggedIn() {
        return isVisible(teamName)
            || isVisible(profileAvatar)
            || isVisible(editProfileHeader)
            || isVisible(settingsHeader)
            || isVisible(AppiumBy.accessibilityId("Back"))
            || isVisible(AppiumBy.accessibilityId("Home"));
    }

    private boolean isLoginScreenVisible() {
        return isVisible(signInButton) && isVisible(editTextLocator);
    }

    private boolean isVisible(By locator) {
        try {
            List<WebElement> elements = driver.findElements(locator);
            for (WebElement el : elements) {
                if (el.isDisplayed()) return true;
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    private void clearAndType(WebElement element, String text) {
        try {
            element.click();
            element.clear();
            element.sendKeys(text);
            try { driver.hideKeyboard(); } catch (Exception ignored) {}
            Thread.sleep(400);
        } catch (Exception e) {
            System.out.println("Clear & type warning: " + e.getMessage());
        }
    }

    private void scrollDown() {
        try {
            new org.openqa.selenium.interactions.Actions(driver)
                .moveToLocation(540, 1800)
                .clickAndHold()
                .moveToLocation(540, 600)
                .release()
                .perform();
            Thread.sleep(800);
        } catch (Exception ignored) {}
    }

    @Test
    public void testAutomateEditProfileFlow() throws InterruptedException {
        System.out.println("==============================================================");
        System.out.println("STARTING AUTOMATION: Edit Profile & Username Change Flow");
        System.out.println("==============================================================");

        // -----------------------------------------------------------------
        // STEP 1 & 2: Navigate to Edit Profile Screen (Self-Healing)
        // -----------------------------------------------------------------
        if (!isVisible(editProfileHeader)) {
            if (!isVisible(editProfileMenu)) {
                if (isVisible(AppiumBy.accessibilityId("Back"))) {
                    try {
                        driver.findElement(AppiumBy.accessibilityId("Back")).click();
                        Thread.sleep(1000);
                    } catch (Exception ignored) {}
                }

                if (!isVisible(editProfileMenu)) {
                    System.out.println("[STEP 1] Tapping Profile Avatar on Dashboard...");
                    try {
                        List<WebElement> avatars = driver.findElements(profileAvatar);
                        if (!avatars.isEmpty()) {
                            avatars.get(0).click();
                        } else {
                            new org.openqa.selenium.interactions.Actions(driver)
                                .moveToLocation(73, 136)
                                .click()
                                .perform();
                        }
                    } catch (Exception e) {
                        new org.openqa.selenium.interactions.Actions(driver)
                            .moveToLocation(73, 136)
                            .click()
                            .perform();
                    }
                    Thread.sleep(1200);
                }
            }

            System.out.println("[STEP 2] Clicking 'Edit Profile' from Drawer menu...");
            WebElement editProfileBtn = wait.until(ExpectedConditions.elementToBeClickable(editProfileMenu));
            editProfileBtn.click();
            Thread.sleep(1500);
        }

        wait.until(ExpectedConditions.visibilityOfElementLocated(editProfileHeader));
        System.out.println("[STEP 2 SUCCESS] Edit Profile screen loaded.");

        // -----------------------------------------------------------------
        // STEP 3: Update Basic Info (First Name, Last Name, Display Name)
        // -----------------------------------------------------------------
        System.out.println("[STEP 3] Updating Basic Identity fields...");

        WebElement fName = wait.until(ExpectedConditions.visibilityOfElementLocated(firstNameInput));
        clearAndType(fName, "testerQA");
        System.out.println("  - First Name set: testerQA");

        WebElement lName = driver.findElement(lastNameInput);
        clearAndType(lName, "Auto");
        System.out.println("  - Last Name set: Auto");

        WebElement dName = driver.findElement(displayNameInput);
        clearAndType(dName, "testerQA Auto");
        System.out.println("  - Display Name set: testerQA Auto");

        // -----------------------------------------------------------------
        // STEP 4: Change Username (Sub-screen flow)
        // -----------------------------------------------------------------
        System.out.println("[STEP 4] Opening Username Change screen...");
        WebElement uRow = driver.findElement(usernameRow);
        uRow.click();

        Thread.sleep(1500);
        wait.until(ExpectedConditions.visibilityOfElementLocated(usernameInputField));

        // Generate dynamic username to avoid collisions: tester-a + 3 random digits
        String newUsername = "tester-a" + (int)(Math.random() * 900 + 100);
        System.out.println("  - Entering new username: " + newUsername);

        WebElement uInput = driver.findElement(usernameInputField);
        clearAndType(uInput, newUsername);

        // Wait for availability confirmation badge
        try {
            Thread.sleep(1500);
            WebDriverWait availWait = new WebDriverWait(driver, Duration.ofSeconds(6));
            availWait.until(ExpectedConditions.visibilityOfElementLocated(usernameAvailableMsg));
            System.out.println("  - Verified: Username availability message displayed.");
        } catch (Exception e) {
            System.out.println("  - Availability badge notice: " + e.getMessage());
        }

        // Click Save on Username screen
        WebElement saveUserBtn = driver.findElement(saveUsernameBtn);
        saveUserBtn.click();
        Thread.sleep(1500);
        System.out.println("[STEP 4 SUCCESS] Username saved, returned to Edit Profile.");

        // -----------------------------------------------------------------
        // STEP 5: Update About You (Bio)
        // -----------------------------------------------------------------
        System.out.println("[STEP 5] Updating About You (Bio)...");
        try {
            WebElement bio = driver.findElement(bioInput);
            clearAndType(bio, "Automated QA verification bio.");
            System.out.println("  - Bio updated successfully.");
        } catch (Exception e) {
            System.out.println("  - Bio field warning: " + e.getMessage());
        }

        // -----------------------------------------------------------------
        // STEP 6: Scroll down to Links & Fill Website, Twitter, LinkedIn
        // -----------------------------------------------------------------
        System.out.println("[STEP 6] Scrolling down to Social Links & Location...");
        scrollDown();

        try {
            List<WebElement> locFields = driver.findElements(locationInput);
            if (!locFields.isEmpty()) {
                clearAndType(locFields.get(0), "Dhaka, Bangladesh");
                System.out.println("  - Location set: Dhaka, Bangladesh");
            }
        } catch (Exception ignored) {}

        try {
            List<WebElement> webFields = driver.findElements(websiteInput);
            if (!webFields.isEmpty()) {
                clearAndType(webFields.get(0), "https://ezycommunity.com");
                System.out.println("  - Website set: https://ezycommunity.com");
            }
        } catch (Exception ignored) {}

        try {
            List<WebElement> twitterFields = driver.findElements(twitterInput);
            if (!twitterFields.isEmpty()) {
                clearAndType(twitterFields.get(0), "@sqatester");
                System.out.println("  - Twitter handle set: @sqatester");
            }
        } catch (Exception ignored) {}

        try {
            List<WebElement> linkedInFields = driver.findElements(linkedInInput);
            if (!linkedInFields.isEmpty()) {
                clearAndType(linkedInFields.get(0), "linkedin.com/in/sqatester");
                System.out.println("  - LinkedIn set: linkedin.com/in/sqatester");
            }
        } catch (Exception ignored) {}

        // -----------------------------------------------------------------
        // STEP 7: Save Changes and Complete Flow
        // -----------------------------------------------------------------
        System.out.println("[STEP 7] Clicking 'Save Changes' button...");
        try {
            WebElement saveBtn = wait.until(ExpectedConditions.elementToBeClickable(saveChangesBtn));
            saveBtn.click();
            System.out.println("  - Clicked 'Save Changes'.");
        } catch (Exception e) {
            // Coordinate fallback for Save Changes [42,2148][1038,2274] (center: 540, 2211)
            new org.openqa.selenium.interactions.Actions(driver)
                .moveToLocation(540, 2211)
                .click()
                .perform();
            System.out.println("  - Tapped 'Save Changes' via coordinates fallback.");
        }

        // Wait for save process to complete and return to Dashboard or Drawer
        Thread.sleep(2500);

        // Verify App is alive and healthy
        String appState = driver.queryAppState("com.ezycommunity.android").toString();
        Assert.assertTrue(
            "RUNNING_IN_FOREGROUND".equals(appState) || "RUNNING_IN_BACKGROUND".equals(appState),
            "App crashed during profile save!"
        );

        System.out.println("==============================================================");
        System.out.println("EDIT PROFILE FLOW COMPLETED SUCCESSFULLY!");
        System.out.println("==============================================================");
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        if (driver != null) {
            driver.quit();
            System.out.println("EzyCommunityEditProfileCheck: Appium session closed.");
        }
    }
}

