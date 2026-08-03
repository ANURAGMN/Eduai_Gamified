# Release smoke test helper — monitors logcat for EduAI release validation.
# Usage: .\scripts\release-smoke-test.ps1 [-WatchSeconds 180]
param(
    [int]$WatchSeconds = 120,
    [string]$Serial = "adb-123249b7-RrRA4J._adb-tls-connect._tcp"
)

$ErrorActionPreference = "Continue"
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) { $adb = "C:\Users\anurag.mn\AppData\Local\Android\Sdk\platform-tools\adb.exe" }
$pkg = "com.ncert7.aitutorandlab"
$activity = "$pkg/.MainActivity"
$logFile = Join-Path $PSScriptRoot "..\build\release-smoke-logcat.txt"

Write-Host ""
Write-Host "=== EduAI release smoke test ==="
Write-Host "Device: $Serial"
Write-Host ""

function Adb { param([string[]]$Args) & $adb -s $Serial @Args }

# 1. Version check
Write-Host "[1] Installed version:"
$dumpsys = Adb @("shell", "dumpsys", "package", $pkg)
$dumpsys | Select-String "versionCode=|versionName=" | Select-Object -First 2 | ForEach-Object { Write-Host "    $_" }

# 2. Backend health
Write-Host ""
Write-Host "[2] Backend /health:"
try {
    $health = curl.exe -s --connect-timeout 8 "http://13.48.59.144:8000/health"
    if ($health -match "healthy") { Write-Host "    OK - backend healthy" } else { Write-Host "    WARN - $health" }
} catch { Write-Host "    FAIL - $($_.Exception.Message)" }

# 3. Launch app
Write-Host ""
Write-Host "[3] Launching MainActivity..."
Adb @("logcat", "-c") | Out-Null
Adb @("shell", "am", "force-stop", $pkg) | Out-Null
Start-Sleep -Seconds 1
Adb @("shell", "am", "start", "-n", $activity) | Out-Null
Write-Host '    App launched - complete Google Sign-In on device now.'
Write-Host "    Manual path: Login -> Home -> Plan -> Friends -> Simulation -> Quest ad"
Write-Host ""

# 4. Capture logcat
Write-Host "[4] Capturing logcat for ${WatchSeconds}s..."
$proc = Start-Process -FilePath $adb -ArgumentList @("-s", $Serial, "logcat", "-v", "time") -RedirectStandardOutput $logFile -PassThru -NoNewWindow
Start-Sleep -Seconds $WatchSeconds
Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 1

$lines = Get-Content $logFile -ErrorAction SilentlyContinue
$eduai = $lines | Where-Object { $_ -match "ncert7|FirebaseAuth|FirebaseRepository|Firestore|MobileAds|RewardedAd|PERMISSION_DENIED|auth_index|FATAL EXCEPTION" }

Write-Host ""
Write-Host "=== Log analysis ==="

$fatals = $eduai | Where-Object { $_ -match "FATAL EXCEPTION" }
if ($fatals) {
    Write-Host "FAIL: Crash detected"
    $fatals | Select-Object -First 3 | ForEach-Object { Write-Host ('  {0}' -f $_) }
} else {
    Write-Host "OK: No FATAL EXCEPTION in capture window"
}

$permDenied = $eduai | Where-Object { $_ -match "PERMISSION_DENIED" }
if ($permDenied) {
    Write-Host ('FAIL: Firestore PERMISSION_DENIED ({0} hits)' -f $permDenied.Count)
    $permDenied | Select-Object -First 5 | ForEach-Object { Write-Host ('  {0}' -f $_) }
} else {
    Write-Host "OK: No Firestore PERMISSION_DENIED"
}

$authOk = $eduai | Where-Object { $_ -match "Firebase Auth OK|auth_index updated" }
if ($authOk) {
    Write-Host "OK: Firebase Auth / auth_index activity detected"
    $authOk | Select-Object -First 3 | ForEach-Object { Write-Host ('  {0}' -f $_) }
} else {
    Write-Host 'PENDING: No Firebase Auth / auth_index logs - sign in during capture?'
}

$ads = $eduai | Where-Object { $_ -match "Mobile Ads initialized|Rewarded ad loaded|childDirected=true" }
if ($ads) {
    Write-Host "OK: AdMob initialization/ads detected"
    $ads | Select-Object -First 3 | ForEach-Object { Write-Host ('  {0}' -f $_) }
} else {
    Write-Host 'PENDING: No ad logs - open sim/quest flow to trigger ads'
}

Write-Host ""
Write-Host "Full log: $logFile"
Write-Host ""
