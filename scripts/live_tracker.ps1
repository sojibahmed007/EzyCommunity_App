# scripts/live_tracker.ps1
# ব্যাকগ্রাউন্ডে এমুলেটরের ব্যবহারকারীর অ্যাকশন ও স্ক্রিন ট্রানজিশন ট্র্যাকিং স্ক্রিপ্ট

$logFile = "./target/user_flow.log"
$stateDir = "./target/tracker_states"

if (-not (Test-Path $stateDir)) {
    New-Item -ItemType Directory -Path $stateDir -Force | Out-Null
}

"=== USER FLOW TRACKING SESSION STARTED $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') ===" | Out-File -FilePath $logFile -Encoding utf8

Write-Host ">>> লাইভ ট্র্যাকার চালু হয়েছে। প্রতি ২.৫ সেকেন্ড পর পর মনিটর করা হচ্ছে..." -ForegroundColor Cyan

$lastSignature = ""
$stepNumber = 1

# স্ক্রিন পরিবর্তন মনিটরিং লুপ
while ($true) {
    try {
        $tmpXml = "/data/local/tmp/tracker_dump.xml"
        $localXml = "$stateDir/step_$stepNumber.xml"

        $dumpResult = adb shell uiautomator dump $tmpXml 2>&1
        if ($dumpResult -like "*ERROR*") {
            Start-Sleep -Milliseconds 1500
            continue
        }

        adb pull $tmpXml $localXml 2>&1 | Out-Null

        if (Test-Path $localXml) {
            [xml]$xml = Get-Content -Path $localXml -Raw
            $nodes = $xml.SelectNodes("//node")

            $descs = @()
            $editTexts = @()
            $buttons = @()

            foreach ($node in $nodes) {
                $cls = $node.GetAttribute("class")
                $desc = $node.GetAttribute("content-desc")
                $txt = $node.GetAttribute("text")
                $bnd = $node.GetAttribute("bounds")
                $clickable = $node.GetAttribute("clickable") -eq "true"
                $focused = $node.GetAttribute("focused") -eq "true"

                if (-not [string]::IsNullOrWhiteSpace($desc)) {
                    $descs += $desc
                }
                if ($cls -eq "android.widget.EditText") {
                    $hint = $node.GetAttribute("hint")
                    $editTexts += [PSCustomObject]@{
                        Text = $txt
                        Hint = $hint
                        Bounds = $bnd
                        Focused = $focused
                    }
                }
                if ($clickable -and (-not [string]::IsNullOrWhiteSpace($desc) -or $cls -like "*Button*" -or $cls -like "*ImageView*")) {
                    $buttons += [PSCustomObject]@{
                        Class = $cls
                        Desc = $desc
                        Bounds = $bnd
                    }
                }
            }

            # স্ক্রিনের সিগনেচার তৈরি করে পরিবর্তন শনাক্ত করা
            $signature = ($descs[0..([Math]::Min(15, $descs.Count))] -join "|") + ";Edit:" + ($editTexts.Text -join ",")

            if ($signature -ne $lastSignature) {
                $timestamp = Get-Date -Format "HH:mm:ss"
                
                $summary = @()
                $summary += "`n------------------------------------------------------------"
                $summary += "[ধাপ $stepNumber] সময়: $timestamp"
                
                # প্রধান টাইটেল / হেডার
                if ($descs.Count -gt 0) {
                    $headerCandidates = $descs | Where-Object { $_.Length -lt 40 -and -not $_.Contains("`n") } | Select-Object -First 5
                    $summary += "শনাক্তকৃত স্ক্রিন / টাইটেল: " + ($headerCandidates -join " | ")
                }

                # ইনপুট ফিল্ড ও টেক্সট
                if ($editTexts.Count -gt 0) {
                    foreach ($et in $editTexts) {
                        $focusedStr = if ($et.Focused) { " [FOCUSED]" } else { "" }
                        $summary += "ইনপুট ফিল্ড: Text='$(($et.Text))' Hint='$(($et.Hint))'$focusedStr Bounds=$(($et.Bounds))"
                    }
                }

                # ক্লিকযোগ্য বাটন ও অ্যাকশন
                $keyActions = $buttons | Where-Object { -not [string]::IsNullOrWhiteSpace($_.Desc) } | Select-Object -First 8
                if ($keyActions) {
                    $summary += "ইন্টারঅ্যাক্টিভ বাটনসমূহ: " + (($keyActions.Desc | ForEach-Object { $_ -replace "`r?`n", " " }) -join "  •  ")
                }

                $summaryText = $summary -join "`n"
                Write-Host $summaryText -ForegroundColor Green
                $summaryText | Out-File -FilePath $logFile -Append -Encoding utf8

                $lastSignature = $signature
                $stepNumber++
            }
        }
    } catch {
        # ট্রানজিয়েন্ট এরর এড়িয়ে যাওয়া
    }

    Start-Sleep -Milliseconds 2500
}
