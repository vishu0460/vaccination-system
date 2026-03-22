$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendPath = Join-Path $repoRoot "backend"

Write-Host "Checking for stale vaccination backend processes..."

$backendProcesses = Get-CimInstance Win32_Process |
    Where-Object {
        $_.Name -eq "java.exe" -and
        $_.CommandLine -like "*com.vaccine.VaccinationApplication*"
    }

if ($backendProcesses) {
    $backendProcesses | ForEach-Object {
        Write-Host ("Stopping stale backend PID {0}" -f $_.ProcessId)
        Stop-Process -Id $_.ProcessId -Force
    }

    Start-Sleep -Seconds 2
} else {
    Write-Host "No stale backend process found."
}

Push-Location $backendPath
try {
    Write-Host "Starting backend from $backendPath"
    mvn spring-boot:run
}
finally {
    Pop-Location
}
