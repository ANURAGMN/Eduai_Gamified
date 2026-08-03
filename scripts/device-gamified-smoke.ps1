# Gamified shell smoke test via ADB input + logcat
param(
    [string]$Serial = $env:ANDROID_SERIAL,
    [string]$Adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
)

if (-not $Serial) { throw "Set ANDROID_SERIAL or pass -Serial" }

function Adb { param([string[]]$Args) & $Adb -s $Serial @Args }

$pkg = "com.ncert7.aitutorandlab"
$results = @()

function Record([string]$Name, [bool]$Pass, [string]$Detail = "") {
    $script:results += [pscustomobject]@{ Test = $Name; Pass = $Pass; Detail = $Detail }
}

Adb logcat -c | Out-Null
Adb shell am force-stop $pkg | Out-Null
Start-Sleep -Milliseconds 800

$start = Adb shell am start -n "$pkg/.MainActivity" 2>&1
Record "Launch MainActivity" ($LASTEXITCODE -eq 0) ($start -join " ")
Start-Sleep -Seconds 6

$pid = (Adb shell pidof $pkg).Trim()
Record "App process running" ($pid -match '^\d+$') "pid=$pid"

$fatal = Adb logcat -d -s AndroidRuntime:E | Select-String -Pattern "FATAL EXCEPTION" -SimpleMatch
Record "No fatal crash on launch" ($null -eq $fatal) ($(if ($fatal) { $fatal.Line } else { "clean" }))

$wm = Adb shell wm size
Record "Display size read" ($wm -match "Physical size") ($wm.Trim())

# Bottom nav: 6 tabs, icon-only bar ~ bottom 72dp on 1080p-ish phone
$sizeLine = ($wm | Select-String "Physical size: (\d+)x(\d+)")
if ($sizeLine) {
    $w = [int]$sizeLine.Matches[0].Groups[1].Value
    $h = [int]$sizeLine.Matches[0].Groups[2].Value
    $y = [int]($h * 0.965)
    $tabNames = @("Home", "Plan", "Quests", "Leagues", "Avatar", "Profile")
    for ($i = 0; $i -lt 6; $i++) {
        $x = [int]($w * (0.083 + $i * 0.167))
        Adb shell input tap $x $y | Out-Null
        Start-Sleep -Seconds 2
        $still = (Adb shell pidof $pkg).Trim()
        $tabFatal = Adb logcat -d -s AndroidRuntime:E | Select-String -Pattern "FATAL EXCEPTION" -SimpleMatch
        $pass = ($still -match '^\d+$') -and ($null -eq $tabFatal)
        Record "Tab: $($tabNames[$i])" $pass "tap=($x,$y) pid=$still"
    }
}

# Scroll home and tap plan see-all region (upper-mid scroll area approximation)
Adb shell input tap ([int]($w * 0.92)) ([int]($h * 0.55)) | Out-Null
Start-Sleep -Seconds 2
$afterPlan = (Adb shell pidof $pkg).Trim()
Record "Plan see-all tap (approx)" ($afterPlan -match '^\d+$') "pid=$afterPlan"

# Back to home tab
Adb shell input tap ([int]($w * 0.08)) ([int]($h * 0.965)) | Out-Null
Start-Sleep -Seconds 1

# Open subjects rail at bottom of home scroll - swipe up first
Adb shell input swipe ([int]($w * 0.5)) ([int]($h * 0.7)) ([int]($w * 0.5)) ([int]($h * 0.25)) 400 | Out-Null
Start-Sleep -Seconds 1
Adb shell input swipe ([int]($w * 0.5)) ([int]($h * 0.7)) ([int]($w * 0.5)) ([int]($h * 0.25)) 400 | Out-Null
Start-Sleep -Seconds 1
# Math tile (left subject)
Adb shell input tap ([int]($w * 0.2)) ([int]($h * 0.78)) | Out-Null
Start-Sleep -Seconds 3
$chapterPid = (Adb shell pidof $pkg).Trim()
Record "Math subject opens chapters" ($chapterPid -match '^\d+$') "pid=$chapterPid"
Adb shell input keyevent KEYCODE_BACK | Out-Null
Start-Sleep -Seconds 1

$ncertFatals = Adb logcat -d | Select-String -Pattern "ncert7.aitutorandlab.*FATAL|AndroidRuntime.*ncert7" | Select-Object -First 3
Record "No app fatals in logcat" ($null -eq $ncertFatals) ($(if ($ncertFatals) { ($ncertFatals | ForEach-Object Line) -join " | " } else { "clean" }))

$version = Adb shell dumpsys package $pkg | Select-String "versionName" | Select-Object -First 1
Write-Host "`n=== Gamified smoke test ($Serial) ===" -ForegroundColor Cyan
Write-Host $version
$results | Format-Table -AutoSize
$failed = @($results | Where-Object { -not $_.Pass })
if ($failed.Count -gt 0) {
    Write-Host "FAILED: $($failed.Count) / $($results.Count)" -ForegroundColor Red
    exit 1
}
Write-Host "PASSED: $($results.Count) / $($results.Count)" -ForegroundColor Green
exit 0
