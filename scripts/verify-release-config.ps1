# Validates local.properties for a Play release build (secrets are not printed).
# Usage: .\scripts\verify-release-config.ps1

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$localProps = Join-Path $root "local.properties"
$issues = @()
$ok = @()
$warn = @()

function Read-Prop($file, $key) {
    if (-not (Test-Path $file)) { return "" }
    foreach ($line in Get-Content $file) {
        if ($line -match "^\s*$key=(.+)$") { return $matches[1].Trim() }
    }
    return ""
}

function Resolve-Keystore($pathFromRoot) {
    if ([string]::IsNullOrWhiteSpace($pathFromRoot)) { return $null }
    $candidates = @(
        (Join-Path $root $pathFromRoot),
        (Join-Path (Join-Path $root "app") $pathFromRoot)
    )
    foreach ($c in $candidates) {
        if (Test-Path $c) { return (Resolve-Path $c).Path }
    }
    return $null
}

Write-Host ""
Write-Host "=== EduAI release config check ==="
Write-Host ""

if (-not (Test-Path $localProps)) {
    $issues += "Missing local.properties"
} else {
    $required = @(
        "KEYSTORE_PATH", "KEYSTORE_PASSWORD", "KEY_ALIAS", "KEY_PASSWORD",
        "ADMOB_APP_ID", "BANNER_AD_UNIT_ID", "REWARDED_AD_UNIT_ID",
        "AUTH_KEY", "AGENTIC_AI_BASE_URL", "sdk.dir"
    )
    foreach ($key in $required) {
        $val = Read-Prop $localProps $key
        if ($val) { $ok += "$key set" } else { $issues += "$key missing" }
    }

    $gamified = Read-Prop $localProps "GAMIFIED_HOME_ENABLED"
    if ($gamified -eq "true") {
        $ok += "GAMIFIED_HOME_ENABLED=true (gamified home in release AAB)"
    } else {
        $warn += "GAMIFIED_HOME_ENABLED is not true - release will use classic home"
    }

    foreach ($key in @("GEMINI_API_KEY", "GROQ_API_KEY")) {
        $val = Read-Prop $localProps $key
        if ($val) { $ok += "$key set" } else {
            $warn += "$key missing - concept maps may fail; main tutor uses backend"
        }
    }

    $ksPath = Read-Prop $localProps "KEYSTORE_PATH"
    $resolved = Resolve-Keystore $ksPath
    if ($resolved) {
        $ok += "Keystore file found: $resolved"
    } else {
        $issues += "Keystore not found at KEYSTORE_PATH=$ksPath"
    }
}

Write-Host "OK ($($ok.Count)):"
foreach ($item in $ok) { Write-Host "  [+] $item" }

if ($warn.Count -gt 0) {
    Write-Host ""
    Write-Host "Warnings ($($warn.Count)):"
    foreach ($item in $warn) { Write-Host "  [~] $item" }
}

if ($issues.Count -gt 0) {
    Write-Host ""
    Write-Host "Issues ($($issues.Count)):"
    foreach ($item in $issues) { Write-Host "  [!] $item" }
}

Write-Host ""
Write-Host "Also run: .\scripts\verify-admob-config.ps1"
Write-Host "Backend:  curl http://13.48.59.144:8000/health"
Write-Host "Build:    .\gradlew.bat bundleRelease"
Write-Host ""

if ($issues.Count -gt 0) { exit 1 }
Write-Host "Release config looks good (review warnings)."
exit 0
