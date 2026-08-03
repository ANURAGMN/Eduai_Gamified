# Validates local AdMob + Firebase wiring before release.
# Usage: .\scripts\verify-admob-config.ps1

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$localProps = Join-Path $root "local.properties"
$googleServices = Join-Path $root "google-services.json"
$manifest = Join-Path $root "app\src\main\AndroidManifest.xml"
$expectedPackage = "com.ncert7.aitutorandlab"
$expectedFirebaseProject = "eduai-e090e"
$samplePublisher = "3940256099942544"
$issues = @()
$ok = @()

function Read-Prop($file, $key) {
    if (-not (Test-Path $file)) { return "" }
    foreach ($line in Get-Content $file) {
        if ($line -match "^\s*$key=(.+)$") { return $matches[1].Trim() }
    }
    return ""
}

Write-Host ""
Write-Host "=== EduAI AdMob / Firebase config check ==="
Write-Host ""

if (-not (Test-Path $localProps)) {
    $issues += "Missing local.properties (copy from local.properties.example)"
} else {
    $appId = Read-Prop $localProps "ADMOB_APP_ID"
    $bannerId = Read-Prop $localProps "BANNER_AD_UNIT_ID"
    $rewardedId = Read-Prop $localProps "REWARDED_AD_UNIT_ID"
    if ($appId -and -not $appId.Contains($samplePublisher)) {
        $ok += "ADMOB_APP_ID set (production publisher)"
    } elseif ($appId) {
        $issues += "ADMOB_APP_ID uses Google sample ID"
    } else {
        $issues += "ADMOB_APP_ID missing in local.properties"
    }
    if ($bannerId -and -not $bannerId.Contains($samplePublisher)) {
        $ok += "BANNER_AD_UNIT_ID set (production unit)"
    } elseif ($bannerId) {
        $issues += "BANNER_AD_UNIT_ID uses Google sample ID"
    } else {
        $issues += "BANNER_AD_UNIT_ID missing in local.properties"
    }
    if ($rewardedId -and -not $rewardedId.Contains($samplePublisher)) {
        $ok += "REWARDED_AD_UNIT_ID set (production unit: REWARDED_AD_1)"
    } elseif ($rewardedId) {
        $issues += "REWARDED_AD_UNIT_ID uses Google sample ID"
    } else {
        $issues += "REWARDED_AD_UNIT_ID missing in local.properties (release AAB falls back to test ID)"
    }
}

if (Test-Path $googleServices) {
    $gs = Get-Content $googleServices -Raw | ConvertFrom-Json
    $projectId = $gs.project_info.project_id
    $pkg = $gs.client[0].client_info.android_client_info.package_name
    if ($projectId -eq $expectedFirebaseProject) {
        $ok += "google-services.json Firebase project $expectedFirebaseProject"
    } else {
        $issues += "google-services.json project_id=$projectId (expected $expectedFirebaseProject)"
    }
    if ($pkg -eq $expectedPackage) {
        $ok += "google-services.json package $expectedPackage"
    } else {
        $issues += "google-services.json package=$pkg (expected $expectedPackage)"
    }
} else {
    $issues += "Missing google-services.json"
}

if (Test-Path $manifest) {
    $xml = Get-Content $manifest -Raw
    if ($xml -match "com\.google\.android\.gms\.ads\.APPLICATION_ID") {
        $ok += "AndroidManifest APPLICATION_ID meta-data present"
    } else {
        $issues += "AndroidManifest missing AdMob APPLICATION_ID"
    }
}

Write-Host "OK ($($ok.Count)):"
foreach ($item in $ok) { Write-Host "  [+] $item" }

if ($issues.Count -gt 0) {
    Write-Host ""
    Write-Host "Issues ($($issues.Count)):"
    foreach ($item in $issues) { Write-Host "  [!] $item" }
}

Write-Host ""
Write-Host "Console steps (manual):"
Write-Host "  1. Link AdMob app to Firebase eduai-e090e (Firebase Integrations or AdMob App settings)"
Write-Host "  2. AdMob Payments: payee profile, tax, bank account"
Write-Host "  3. Confirm banner + rewarded unit IDs match local.properties"
Write-Host "  Full checklist: scripts/admob-firebase-setup.md"
Write-Host ""

if ($issues.Count -gt 0) { exit 1 }
Write-Host "Local config looks good."
exit 0
