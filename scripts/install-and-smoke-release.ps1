# Install release APK and run smoke test on CPH2661 (wireless ADB).
# Prerequisite: phone connected — run `adb devices` first.
param(
    [string]$Serial = "",
    [int]$WatchSeconds = 150
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) { $adb = "C:\Users\anurag.mn\AppData\Local\Android\Sdk\platform-tools\adb.exe" }
$apk = Join-Path $root "app\build\outputs\apk\release\app-release.apk"
$pkg = "com.ncert7.aitutorandlab"

if (-not (Test-Path $apk)) {
    Write-Host "Missing release APK. Run: .\gradlew.bat assembleRelease"
    exit 1
}

$devices = & $adb devices | Select-String "device$" | ForEach-Object { ($_ -split "\s+")[0] } | Where-Object { $_ -ne "List" }
if (-not $devices) {
    Write-Host "No device connected. Enable wireless debugging and pair in Android Studio, then retry."
    exit 1
}
if (-not $Serial) { $Serial = $devices[0] }
Write-Host "Using device: $Serial"

Write-Host "Installing release APK..."
& $adb -s $Serial install -r $apk
if ($LASTEXITCODE -ne 0) {
    Write-Host "Install failed (signature conflict?). Trying uninstall first..."
    & $adb -s $Serial uninstall $pkg
    & $adb -s $Serial install $apk
}

Write-Host ""
& (Join-Path $root "scripts\release-smoke-test.ps1") -Serial $Serial -WatchSeconds $WatchSeconds
