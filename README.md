# EzyCommunity Mobile Automation 🚀

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Appium](https://img.shields.io/badge/Appium-9.5.0-blue.svg)](https://appium.io/)
[![Selenium](https://img.shields.io/badge/Selenium-4.34.0-green.svg)](https://www.selenium.dev/)
[![TestNG](https://img.shields.io/badge/TestNG-7.11.0-red.svg)](https://testng.org/)
[![Maven](https://img.shields.io/badge/Build-Maven-C71A36.svg)](https://maven.apache.org/)

An automated end-to-end mobile testing framework for the **EzyCommunity** Android application. Built using **Java**, **Appium (UiAutomator2)**, **Selenium WebDriver**, and **TestNG** with a data-driven testing approach.

---

## 📌 Overview

This automation suite tests the authentication and session management workflows of the Flutter-based **EzyCommunity** mobile application. It covers both positive and negative scenarios, credential validation, session persistence, and multi-step logout flows (Profile Drawer ➔ Confirmation Modal ➔ Sign In Screen).

---

## 🛠️ Tech Stack & Dependencies

| Tool / Technology | Version | Purpose |
| :--- | :--- | :--- |
| **Java** | 17+ | Programming Language |
| **Appium Java Client** | 9.5.0 | Mobile UI Automation Driver |
| **Selenium WebDriver** | 4.34.0 | Browser & Element Automation API |
| **TestNG** | 7.11.0 | Test Runner & DataProvider Framework |
| **UiAutomator2 Driver** | 3.8.0+ | Android Automation Engine |
| **Apache Maven** | 3.9+ | Build & Dependency Management |

---

## 📁 Project Structure

```text
EzyCommuntiyAutomation/
├── src/
│   └── test/
│       └── java/
│           ├── EzyCommunityTest.java           # Core Login Suite & Logout Locators
│           └── EzyCommunitySearchTest.java     # Critical Security & Search Attack Suite
├── target/                                     # Build outputs & Surefire reports
├── .gitignore                                  # Git exclusion rules
├── Pom.xml                                     # Maven dependencies & plugins
└── README.md                                   # Project documentation
```

---

## ⚙️ Prerequisites

Before executing the test suite, ensure your environment has:

1. **Java Development Kit (JDK 17+)**:
   ```powershell
   java -version
   ```
2. **Apache Maven**:
   ```powershell
   mvn -version
   ```
3. **Android SDK & Platform Tools (`adb`)**:
   Ensure `adb` is added to your system `PATH`:
   ```powershell
   adb devices
   ```
4. **Appium Server**:
   ```powershell
   npm install -g appium
   appium driver install uiautomator2
   appium
   ```
5. **Android Emulator or Physical Device**:
   - Device UDID: `emulator-5554` (or configured device)
   - App Package: `com.ezycommunity.android`
   - Main Activity: `.MainActivity`

---

## 🔑 Environment Variables Setup

The test framework retrieves credentials securely from environment variables:

### Windows (PowerShell):
```powershell
$env:TEST_EMAIL = "your-email@example.com"
$env:TEST_PASSWORD = "YourSecurePassword123!"
```

### Windows (CMD):
```cmd
set TEST_EMAIL=your-email@example.com
set TEST_PASSWORD=YourSecurePassword123!
```

### Linux / macOS (Bash / Zsh):
```bash
export TEST_EMAIL="your-email@example.com"
export TEST_PASSWORD="YourSecurePassword123!"
```

---

## 🚀 How to Run the Tests

### 1. Start Appium Server
Start Appium on the default port `4723`:
```bash
appium
```

### 2. Execute TestNG Suite via Maven

Run the entire test suite:
```powershell
mvn clean test
```

Run with environment variables inline (PowerShell):
```powershell
$env:TEST_EMAIL="testerone@imti.online"; $env:TEST_PASSWORD="#Ab123456"; mvn test
```

Run a specific test suite:
```powershell
# Run Authentication Test Suite (16 scenarios)
mvn test -Dtest=EzyCommunityTest

# Run Critical Search Security & Break Test Suite (16 scenarios)
mvn test -Dtest=EzyCommunitySearchTest
```

---

## 🧪 Test Case Coverage: Authentication Suite (`EzyCommunityTest`)

The authentication test suite executes 16 automated scenarios using TestNG `@DataProvider`:

| ID | Test Scenario | Input Data | Expected Result |
| :--- | :--- | :--- | :---: |
| **TC-01** | Empty email and password | `""`, `""` | ❌ Fail |
| **TC-02** | Invalid email format | `"invalid-email"`, `"WrongPassword123!"` | ❌ Fail |
| **TC-03** | Valid email with wrong password | `validEmail`, `"WrongPassword123!"` | ❌ Fail |
| **TC-04** | Unknown email address | `"unknownuser@example.com"`, `"WrongPassword123!"` | ❌ Fail |
| **TC-05** | Invalid email and wrong password | `"wrong-email"`, `"WrongPassword123!"` | ❌ Fail |
| **TC-06** | Empty email with password | `""`, `validPassword` | ❌ Fail |
| **TC-07** | Email with empty password | `validEmail`, `""` | ❌ Fail |
| **TC-08** | Leading spaces in email | `"  " + validEmail`, `validPassword` | ✅ Success |
| **TC-09** | Trailing spaces in email | `validEmail + "  "`, `validPassword` | ✅ Success |
| **TC-10** | Uppercase email normalization | `validEmail.toUpperCase()`, `validPassword` | ✅ Success |
| **TC-11** | Leading space in password (Auto-trimmed by app) | `validEmail`, `" " + validPassword` | ✅ Success |
| **TC-12** | Trailing space in password (Auto-trimmed by app) | `validEmail`, `validPassword + " "` | ✅ Success |
| **TC-13** | Wrong email with valid password | `"wronguser@example.com"`, `validPassword` | ❌ Fail |
| **TC-14** | Valid email with incorrect password | `validEmail`, `"IncorrectPassword123!"` | ❌ Fail |
| **TC-15** | Spaces-only credentials | `"   "`, `"   "` | ❌ Fail |
| **TC-16** | Valid email and password (Standard) | `validEmail`, `validPassword` | ✅ Success |

---

## 🛡️ Critical Community Search Attack & Break Test Suite (`EzyCommunitySearchTest`)

Automated end-to-end flow: **Launch App ➔ Auto-Login ➔ Navigate to Community Search ➔ Execute Security & Stress Attacks**.

| ID | Attack Vector / Scenario | Injected Payload | Expected Security & Stability Outcome | Status |
| :--- | :--- | :--- | :--- | :---: |
| **TC-SEC-01** | Baseline Search Sanity | `test` | Returns valid search results without error | ✅ PASSED |
| **TC-SEC-02** | SQLi Auth Bypass Injection | `' OR '1'='1` | Parameterized safely; no data leak or syntax error | ✅ PASSED |
| **TC-SEC-03** | SQLi Stacked Statement Drop | `'; DROP TABLE posts; --` | Parameterized safely; no query truncation | ✅ PASSED |
| **TC-SEC-04** | SQLi Union-Based Extraction | `' UNION SELECT null, username, password FROM users --` | Rejected cleanly; no database reflection | ✅ PASSED |
| **TC-SEC-05** | Reflected XSS Script Tag | `<script>alert('XSS')</script>` | Sanitized / rendered as raw text without execution | ✅ PASSED |
| **TC-SEC-06** | XSS Event Handler Injection | `<img src=x onerror=alert(1)>` | Escaped safely; DOM event handling prevented | ✅ PASSED |
| **TC-SEC-07** | HTML Iframe / JS Protocol | `<iframe src="javascript:alert(1)"></iframe>` | Escaped safely; iframe embedding blocked | ✅ PASSED |
| **TC-SEC-08** | Template Injection (SSTI) | `{{7*7}} ${7*7} <%= 7*7 %>` | Treated as literal text; no expression evaluation | ✅ PASSED |
| **TC-SEC-09** | Massive Buffer Stress (500+ Chars) | `500+ character repeating payload` | Memory safe; no ANR or buffer overflow | ✅ PASSED |
| **TC-SEC-10** | Special Symbol & Punctuation Bomb | `!@#$%^&*()_+~|}{[]:;?><,./'"` | Regex parser safe; no unhandled pattern error | ✅ PASSED |
| **TC-SEC-11** | Emojis, Unicode & RTL Characters | `🔥🚀💻 ﷽ \u202Ereversed\u202C` | UTF-8 / Bidirectional glyphs render without crash | ✅ PASSED |
| **TC-SEC-12** | Null Byte Poisoning Attempt | `search\u0000admin` | Null byte handled safely; no internal corruption | ✅ PASSED |
| **TC-SEC-13** | Whitespace Flooding | `   \t   \n   \r   ` | Gracefully treated as empty; no crash | ✅ PASSED |
| **TC-SEC-14** | Non-Existent String (Zero Results) | `__xyz_random_nonexistent_token_99999__` | Displays friendly "No results for..." message | ✅ PASSED |
| **TC-SEC-15** | Rapid Debounce Hammering | Rapid sequential queries | App remains responsive; handles race conditions | ✅ PASSED |
| **TC-SEC-16** | Clear Button Functionality | `TemporaryAttackPayload` ➔ Tap Clear (X) | Search input resets cleanly to initial state | ✅ PASSED |

---

## 🔄 Two-Step Logout Flow Implementation

In Flutter-based UI, logging out requires navigating through a drawer menu followed by an animated confirmation modal:

```text
[Dashboard / Home Screen]
        │
        ▼ (Click Profile Avatar)
[Profile Drawer Menu]
        │
        ▼ (Click 'Log out' ImageView)
[Confirmation Modal Dialog]
        │
        ▼ (Click 'Log Out' Button)
[Sign In Screen Ready]
```

### Locator Strategy:
- **Profile Avatar (Open Drawer):**
  ```java
  private final By profileAvatar = By.xpath(
      "//android.view.View[@content-desc='SQA Team']/preceding-sibling::*[@clickable='true'][1]" +
      " | //android.view.View[@content-desc='SQA Team']/preceding-sibling::*[1]" +
      " | //android.view.View[@content-desc='SQA Team']/preceding-sibling::android.widget.ImageView[1]"
  );
  ```
- **Menu Logout Item (Step 1):**
  ```java
  private final By logoutMenuItem = By.xpath(
      "//*[@content-desc='Log out' or @content-desc='Log Out']"
  );
  ```
- **Modal Confirmation Button (Step 2):**
  ```java
  private final By logoutConfirmButton = By.xpath(
      "//android.widget.Button[@content-desc='Log Out' or @content-desc='Log out']"
  );
  ```
> **Note:** Targeted `android.widget.Button` specifically avoids accidental clicks on the dialog title view which shares the same text.

---

## 📊 Test Reports

After test execution, comprehensive reports are generated at:
- **HTML Report:** `target/surefire-reports/index.html`
- **Emailable Report:** `target/surefire-reports/emailable-report.html`
- **XML Results:** `target/surefire-reports/testng-results.xml`

Open the HTML report in your browser:
```powershell
Start-Process .\target\surefire-reports\index.html
```

---

## 👤 Author & Contribution

Developed & maintained by **[sojibahmed007](https://github.com/sojibahmed007)**.
Feel free to open issues or submit pull requests to contribute!

