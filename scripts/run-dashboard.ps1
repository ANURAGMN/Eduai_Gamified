# Generate EduAI metrics dashboard (HTML)
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$since = if ($args.Count -gt 0) { $args[0] } else { "2026-07-09" }
$out = Join-Path $root "reports\dashboard.html"
$snapshot = Join-Path $root "reports\firestore-snapshot.json"

Push-Location $root
$env:PYTHONIOENCODING = "utf-8"

if ($args -contains "--from-snapshot") {
    python scripts/metrics-dashboard-html.py $since --from-snapshot $snapshot --out $out
} else {
    python scripts/metrics-dashboard-html.py $since --out $out
}

$ok = $LASTEXITCODE -eq 0
Pop-Location

if ($ok) {
    Write-Host "Dashboard written: $out"
    Start-Process $out
} else {
    Write-Host "Failed. Try cached render: .\scripts\run-dashboard.ps1 --from-snapshot"
    exit 1
}
