# Retry dashboard generation until Firestore quota clears
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$since = if ($args.Count -gt 0) { $args[0] } else { "2026-07-09" }
$out = Join-Path $root "reports\dashboard.html"
$attempt = 0
while ($true) {
    $attempt++
    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Attempt $attempt..."
    Push-Location $root
    $env:PYTHONIOENCODING = "utf-8"
    python scripts/metrics-dashboard-html.py $since --out $out
    $ok = $LASTEXITCODE -eq 0
    Pop-Location
    if ($ok) {
        Write-Host "Dashboard written: $out"
        Start-Process $out
        break
    }
    Write-Host "Failed (likely Firestore 429). Waiting 15 minutes..."
    Start-Sleep -Seconds 900
}
