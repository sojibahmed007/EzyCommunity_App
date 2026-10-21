# scripts/inspect_screen.ps1
# এমুলেটরের বর্তমান স্ক্রিনের UI এলিমেন্ট ও Appium লোকেটর ইনস্পেক্ট করার স্ক্রিপ্ট

param(
    [string]$FilePath = ""
)

# ফাইল পাথ না থাকলে এমুলেটর থেকে সরাসরি স্ক্রিন ডাম্প করবে
if ([string]::IsNullOrWhiteSpace($FilePath)) {
    $tmpFile = "/data/local/tmp/screen_inspect.xml"
    $FilePath = "./target/screen_inspect.xml"

    if (-not (Test-Path "./target")) { 
        New-Item -ItemType Directory -Path "./target" | Out-Null 
    }

    Write-Host ">>> এমুলেটর থেকে লাইভ স্ক্রিন ক্যাপচার করা হচ্ছে..." -ForegroundColor Cyan
    adb shell uiautomator dump $tmpFile | Out-Null
    adb pull $tmpFile $FilePath | Out-Null
}

if (-not (Test-Path $FilePath)) {
    Write-Host "ফাইল পাওয়া যায়নি: $FilePath" -ForegroundColor Red
    exit 1
}

# এক্সএমএল হায়ারার্কি পার্স করা
[xml]$xml = Get-Content -Path $FilePath -Raw
$nodes = $xml.SelectNodes("//node")
$interactiveNodes = @()

foreach ($node in $nodes) {
    $isClickable = $node.GetAttribute("clickable") -eq "true"
    $hasDesc = -not [string]::IsNullOrWhiteSpace($node.GetAttribute("content-desc"))
    $hasText = -not [string]::IsNullOrWhiteSpace($node.GetAttribute("text"))
    $className = $node.GetAttribute("class")
    $bounds = $node.GetAttribute("bounds")
    $hint = $node.GetAttribute("hint")
    $desc = $node.GetAttribute("content-desc")
    $txt = $node.GetAttribute("text")

    # ফুলস্ক্রিন খালি কন্টেইনার বাদ দেওয়া
    if ($bounds -eq "[0,0][1080,2400]" -and -not $hasDesc) { continue }

    # ক্লিকযোগ্য বা টেক্সটযুক্ত এলিমেন্ট ফিল্টার করা
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
Write-Host "  স্ক্রিনে পাওয়া এলিমেন্টসমূহ ($($interactiveNodes.Count) টি)" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green

# প্রতিটি এলিমেন্টের ক্লাস, কনটেন্ট ডেসক্রিপশন এবং প্রস্তাবিত জাভা লোকেটর প্রিন্ট করা
$count = 1
foreach ($item in $interactiveNodes) {
    $desc = $item.ContentDesc
    $txt = $item.Text
    $cls = $item.Class
    $bnd = $item.Bounds
    $hint = $item.Hint
    $isClickable = $item.Clickable

    # জাভা লোকেটর জেনারেশন
    $locator = ""
    if (-not [string]::IsNullOrWhiteSpace($desc)) {
        if ($desc.Contains("`n")) {
            $firstLine = ($desc -split "`r?`n")[0].Trim()
            $locator = "By.xpath(`"//*[contains(@content-desc, '$firstLine')]`")"
        } else {
            $locator = "AppiumBy.accessibilityId(`"$desc`")"
        }
    } elseif ($cls -eq "android.widget.EditText") {
        if (-not [string]::IsNullOrWhiteSpace($hint)) {
            $locator = "By.xpath(`"//android.widget.EditText[contains(@hint, '$hint')]`")"
        } else {
            $locator = "By.className(`"android.widget.EditText`")"
        }
    } else {
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
