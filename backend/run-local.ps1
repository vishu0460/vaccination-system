param(
    [int]$Port = 8080
)

$ErrorActionPreference = "Stop"

Write-Host "Checking for listeners on port $Port..."
$listeners = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue

if ($listeners) {
    $pids = $listeners | Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($pid in $pids) {
        $process = Get-CimInstance Win32_Process -Filter "ProcessId = $pid" -ErrorAction SilentlyContinue
        if ($process) {
            Write-Host "Stopping PID $pid ($($process.Name)) using port $Port"
        } else {
            Write-Host "Stopping PID $pid using port $Port"
        }
        Stop-Process -Id $pid -Force
    }

    Start-Sleep -Seconds 2
}

$stillListening = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
if ($stillListening) {
    throw "Port $Port is still in use. Stop the listener manually and retry."
}

Write-Host "Starting Spring Boot on port $Port..."
mvn spring-boot:run
