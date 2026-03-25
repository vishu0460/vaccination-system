param(
    [int]$Port = 8080,
    [string]$Profile = "local",
    [string]$DbHost = "localhost",
    [int]$DbPort = 3306
)

$ErrorActionPreference = "Stop"

Write-Host "Checking for listeners on port $Port..."
$listeners = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue

$staleBackendProcesses = Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
    Where-Object {
        $_.Name -eq "java.exe" -and (
            $_.CommandLine -like "*com.vaccine.VaccinationApplication*" -or
            $_.CommandLine -like "*vaccination-backend*" -or
            $_.CommandLine -like "*backend\\target\\classes*"
        )
    }

if ($staleBackendProcesses) {
    foreach ($process in $staleBackendProcesses) {
        Write-Host "Stopping stale backend PID $($process.ProcessId) ($($process.Name))"
        Stop-Process -Id $process.ProcessId -Force
    }

    Start-Sleep -Seconds 2
}

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

if ($Profile -in @("local-fixed", "prod")) {
    Write-Host "Checking MySQL availability at $DbHost:$DbPort for profile $Profile..."
    $dbReady = $false
    try {
        $dbReady = Test-NetConnection -ComputerName $DbHost -Port $DbPort -WarningAction SilentlyContinue | Select-Object -ExpandProperty TcpTestSucceeded
    } catch {
        $dbReady = $false
    }

    if (-not $dbReady) {
        $dockerAvailable = Get-Command docker -ErrorAction SilentlyContinue
        if ($dockerAvailable) {
            Write-Host "MySQL is not reachable. Attempting to start the docker-compose mysql service..."
            docker compose up -d mysql

            $deadline = (Get-Date).AddSeconds(60)
            while ((Get-Date) -lt $deadline) {
                Start-Sleep -Seconds 2
                try {
                    $dbReady = Test-NetConnection -ComputerName $DbHost -Port $DbPort -WarningAction SilentlyContinue | Select-Object -ExpandProperty TcpTestSucceeded
                } catch {
                    $dbReady = $false
                }

                if ($dbReady) {
                    break
                }
            }
        }
    }

    if (-not $dbReady) {
        throw "MySQL is not reachable at $DbHost:$DbPort. Start MySQL locally or run 'docker compose up -d mysql' from the project root, then retry."
    }
}

Write-Host "Starting Spring Boot on port $Port..."
$env:SPRING_PROFILES_ACTIVE = $Profile
mvn spring-boot:run
