# ============================================================================
# SCRIPT: live_tracker.ps1
# ============================================================================
# PURPOSE:
# Continuous Background User Action & Screen Transition Tracker.
#
# HOW IT WORKS (FOR BEGINNERS):
# 1. Runs as a background polling daemon while you manually use the mobile app.
# 2. Every 2.5 seconds, it queries Android via ADB to check if the UI changed.
# 3. Computes a "Screen Signature" (based on visible titles, inputs, and buttons).
# 4. If the screen changes (e.g. you tapped a button or entered text):
#    - Saves a full XML snapshot to './target/tracker_states/step_N.xml'.
#    - Logs the step number, timestamp, detected screen name, typed text, 
#      and active buttons to './target/user_flow.log'.
# 5. This allows the AI / test engineer to reconstruct your exact manual flow 
#    and generate 100% automated test code!
# ============================================================================

$logFile = "./target/user_flow.log"
$stateDir = "./target/tracker_states"

# Step 1: Ensure output directory exists
if (-not (Test-Path $stateDir)) {
    New-Item -ItemType Directory -Path $stateDir -Force | Out-Null
}

# Initialize session log file
"=== USER FLOW TRACKING SESSION STARTED $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') ===" | Out-File -FilePath $logFile -Encoding utf8

Write-Host ">>> LIVE TRACKER STARTED. Monitoring emulator every 2.5s..." -ForegroundColor Cyan

$lastSignature = ""
$stepNumber = 1

# Step 2: Continuous monitoring loop
while ($true) {
    try {
        $tmpXml = "/data/local/tmp/tracker_dump.xml"
        $localXml = "$stateDir/step_$stepNumber.xml"

        # Dump current UI hierarchy from Android
        $dumpResult = adb shell uiautomator dump $tmpXml 2>&1
        if ($dumpResult -like "*ERROR*") {
            # If screen was actively animating/scrolling, retry after 1.5s
            Start-Sleep -Milliseconds 1500
            continue
        }

        # Pull dumped XML file to local directory
        adb pull $tmpXml $localXml 2>&1 | Out-Null

        if (Test-Path $localXml) {
            [xml]$xml = Get-Content -Path $localXml -Raw
            $nodes = $xml.SelectNodes("//node")

            $descs = @()
            $editTexts = @()
            $buttons = @()
            $allElements = @()

            # Parse elements from current snapshot
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

            # Step 3: Compute screen signature to detect transitions
            $signature = ($descs[0..([Math]::Min(15, $descs.Count))] -join "|") + ";Edit:" + ($editTexts.Text -join ",")

            # If screen signature changed from previous state, log a new Step!
            if ($signature -ne $lastSignature) {
                $timestamp = Get-Date -Format "HH:mm:ss"
                
                $summary = @()
                $summary += "`n------------------------------------------------------------"
                $summary += "[STEP $stepNumber] Time: $timestamp"
                
                # Identify prominent headers / titles
                if ($descs.Count -gt 0) {
                    $headerCandidates = $descs | Where-Object { $_.Length -lt 40 -and -not $_.Contains("`n") } | Select-Object -First 5
                    $summary += "Detected Screen / Titles: " + ($headerCandidates -join " | ")
                }

                # Inputs if any
                if ($editTexts.Count -gt 0) {
                    foreach ($et in $editTexts) {
                        $focusedStr = if ($et.Focused) { " [FOCUSED]" } else { "" }
                        $summary += "Input Field: Text='$(($et.Text))' Hint='$(($et.Hint))'$focusedStr Bounds=$(($et.Bounds))"
                    }
                }

                # Key clickable buttons/actions
                $keyActions = $buttons | Where-Object { -not [string]::IsNullOrWhiteSpace($_.Desc) } | Select-Object -First 8
                if ($keyActions) {
                    $summary += "Interactive Actions: " + (($keyActions.Desc | ForEach-Object { $_ -replace "`r?`n", " " }) -join "  •  ")
                }

                $summaryText = $summary -join "`n"
                Write-Host $summaryText -ForegroundColor Green
                $summaryText | Out-File -FilePath $logFile -Append -Encoding utf8

                $lastSignature = $signature
                $stepNumber++
            }
        }
    } catch {
        # Ignore transient parsing exceptions during fast touch gestures
    }

    # Polling frequency: checks every 2.5 seconds
    Start-Sleep -Milliseconds 2500
}
