# ============================================================================
# SCRIPT: inspect_screen.ps1
# ============================================================================
# PURPOSE:
# Real-Time UI Element Inspector & Appium Locator Generator for Android.
#
# HOW IT WORKS (FOR BEGINNERS):
# 1. Runs 'adb shell uiautomator dump' to capture the live accessibility node 
#    hierarchy of the Android Emulator screen into an XML file.
# 2. Pulls the XML file to the local machine using 'adb pull'.
# 3. Uses PowerShell's XML parser and XPath (//node) to scan every UI element.
# 4. Filters out non-interactive containers and extracts:
#    - Class name (e.g. android.widget.Button, android.widget.EditText)
#    - Content-Description (accessibilityId)
#    - Text & Hint
#    - Screen Bounds [x1, y1][x2, y2]
#    - Clickable status
# 5. Generates ready-to-copy Java Appium locators for each element!
#
# USAGE:
# - Inspect live emulator screen:
#   powershell -ExecutionPolicy Bypass -File .\scripts\inspect_screen.ps1
# - Inspect a previously captured XML file:
#   powershell -ExecutionPolicy Bypass -File .\scripts\inspect_screen.ps1 -FilePath .\target\tracker_states\step_1.xml
# ============================================================================

param(
    # Optional path to an existing XML file. If omitted, dumps live from emulator.
    [string]$FilePath = ""
)

# Step 1: If no file path provided, dump live UI hierarchy from the connected emulator
if ([string]::IsNullOrWhiteSpace($FilePath)) {
    $tmpFile = "/data/local/tmp/screen_inspect.xml"
    $FilePath = "./target/screen_inspect.xml"

    # Ensure output target directory exists
    if (-not (Test-Path "./target")) { 
        New-Item -ItemType Directory -Path "./target" | Out-Null 
    }

    Write-Host ">>> Capturing live screen from emulator via ADB..." -ForegroundColor Cyan
    # 'adb shell uiautomator dump' instructs Android OS to dump current view hierarchy
    adb shell uiautomator dump $tmpFile | Out-Null
    # 'adb pull' downloads the dumped XML from the emulator to local disk
    adb pull $tmpFile $FilePath | Out-Null
}

if (-not (Test-Path $FilePath)) {
    Write-Host "File not found: $FilePath" -ForegroundColor Red
    exit 1
}

# Step 2: Load and parse the XML document
[xml]$xml = Get-Content -Path $FilePath -Raw

# Select all <node> tags across the entire hierarchy using XPath
$nodes = $xml.SelectNodes("//node")
$interactiveNodes = @()

# Step 3: Iterate through nodes and filter for interactive / actionable elements
foreach ($node in $nodes) {
    $isClickable = $node.GetAttribute("clickable") -eq "true"
    $hasDesc = -not [string]::IsNullOrWhiteSpace($node.GetAttribute("content-desc"))
    $hasText = -not [string]::IsNullOrWhiteSpace($node.GetAttribute("text"))
    $className = $node.GetAttribute("class")
    $bounds = $node.GetAttribute("bounds")
    $hint = $node.GetAttribute("hint")
    $desc = $node.GetAttribute("content-desc")
    $txt = $node.GetAttribute("text")

    # Exclude full-screen outer container layouts (e.g. [0,0][1080,2400]) that have no description
    if ($bounds -eq "[0,0][1080,2400]" -and -not $hasDesc) { continue }

    # Keep element if it is clickable, an input field, or has meaningful text/description
    if ($isClickable -or $className -eq "android.widget.EditText" -or $hasDesc -or $hasText) {
        $interactiveNodes += [PSCustomObject]@{
            Class = $className
            ContentDesc = $desc
            Text = $txt
            Bounds = $bounds
            Clickable = $isClickable
            Hint = $hint
        }
    }
}

Write-Host "==========================================================" -ForegroundColor Green
Write-Host "  ELEMENTS IN $FilePath ($($interactiveNodes.Count) total)" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green

# Step 4: Display elements with recommended Java Appium locators
$count = 1
foreach ($item in $interactiveNodes) {
    $desc = $item.ContentDesc
    $txt = $item.Text
    $cls = $item.Class
    $bnd = $item.Bounds
    $hint = $item.Hint
    $isClickable = $item.Clickable

    # Heuristic rules to generate the best locator strategy:
    $locator = ""
    if (-not [string]::IsNullOrWhiteSpace($desc)) {
        if ($desc.Contains("`n")) {
            # Multi-line descriptions (common in Flutter feed cards) -> use XPath contains()
            $firstLine = ($desc -split "`r?`n")[0].Trim()
            $locator = "By.xpath(`"//*[contains(@content-desc, '$firstLine')]`")"
        } else {
            # Single-line clean descriptions -> use fastest AppiumBy.accessibilityId
            $locator = "AppiumBy.accessibilityId(`"$desc`")"
        }
    } elseif ($cls -eq "android.widget.EditText") {
        if (-not [string]::IsNullOrWhiteSpace($hint)) {
            $locator = "By.xpath(`"//android.widget.EditText[contains(@hint, '$hint')]`")"
        } else {
            $locator = "By.className(`"android.widget.EditText`")"
        }
    } else {
        # Fallback to class name with bounds
        $locator = "By.xpath(`"//$cls" + "[@bounds='$bnd']`")"
    }

    $clickLabel = if ($isClickable) { "[CLICKABLE]" } else { "" }
    Write-Host ("[{0:D2}] {1} {2}" -f $count, $cls, $clickLabel) -ForegroundColor Yellow
    if ($desc) { Write-Host ("     content-desc : {0}" -f ($desc -replace "`r?`n", " ↵ ")) -ForegroundColor White }
    if ($txt)  { Write-Host ("     text         : {0}" -f $txt) -ForegroundColor White }
    if ($hint) { Write-Host ("     hint         : {0}" -f $hint) -ForegroundColor Gray }
    Write-Host ("     bounds       : {0}" -f $bnd) -ForegroundColor DarkGray
    Write-Host ("     Java Locator : {0}" -f $locator) -ForegroundColor Cyan
    Write-Host "----------------------------------------------------------" -ForegroundColor DarkGray
    $count++
}
