# Notification smoke test — run with a device connected via USB or wireless ADB.
# Usage: .\scripts\notification-smoke-test.ps1

$ErrorActionPreference = "Stop"
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$pkg = "com.ncert7.aitutorandlab"
$receiver = "$pkg/.notification.DailyReminderReceiver"
$action = "$pkg.notification.DAILY_REMINDER"

if (-not (Test-Path $adb)) {
    Write-Error "adb not found at $adb"
}

$devices = & $adb devices | Select-String "device$"
if (-not $devices) {
    Write-Error "No device connected. Enable wireless debugging or plug in USB, then retry."
}

Write-Host "=== Notification smoke test ===" -ForegroundColor Cyan
Write-Host "Device(s):" ($devices -join ", ")

Write-Host "`n[1] Notification permission state"
& $adb shell dumpsys notification --noredact $pkg 2>$null | Select-String -Pattern "permission|importance|Channel" | Select-Object -First 20

Write-Host "`n[2] Trigger daily reminder eval (WorkManager path)"
& $adb shell am broadcast -a $action -n $receiver

Write-Host "`n[3] Recent notification logs"
& $adb logcat -d -s NotificationScheduler NotificationOrchestrator NotificationEvalWorker DailyReminderReceiver | Select-Object -Last 15

Write-Host "`n[4] Active notifications in shade"
& $adb shell dumpsys notification --noredact $pkg 2>$null | Select-String -Pattern "NotificationRecord|title=" | Select-Object -First 10

Write-Host "`nDone. Manual checks:" -ForegroundColor Yellow
Write-Host "  - Profile > Notifications (settings screen)"
Write-Host "  - Profile > Developer > Fire * (debug) buttons for each type"
Write-Host "  - Tap notification body -> verify deep link"
Write-Host "  - Tap Cancel -> notification dismisses without opening app"
