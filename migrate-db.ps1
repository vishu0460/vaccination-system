$ErrorActionPreference = "Stop"

param(
    [switch]$StartBackend
)

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendPath = Join-Path $repoRoot "backend"
$envFile = Join-Path $repoRoot ".env"

if (-not (Test-Path $envFile)) {
    throw ".env file not found at $envFile"
}

$sourceConfig = @{}
Get-Content $envFile | ForEach-Object {
    if (-not ($_ -match '^\s*#' -or $_ -match '^\s*$')) {
        $parts = $_.Split('=', 2)
        if ($parts.Count -eq 2) {
            $sourceConfig[$parts[0].Trim()] = $parts[1]
        }
    }
}

if (-not $sourceConfig.ContainsKey('DB_URL')) {
    throw "Source .env does not contain DB_URL for the H2 database."
}

if (-not $sourceConfig['DB_URL'].StartsWith('jdbc:h2:', [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Source .env DB_URL is not an H2 URL. Refusing to run the H2 export step."
}

if ([string]::IsNullOrWhiteSpace($env:DB_URL) -or [string]::IsNullOrWhiteSpace($env:DB_USERNAME) -or $null -eq $env:DB_PASSWORD) {
    throw "Set target MySQL credentials in the current shell: DB_URL, DB_USERNAME, and DB_PASSWORD."
}

if (-not $env:DB_URL.StartsWith('jdbc:mysql://', [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "DB_URL must point to MySQL, for example jdbc:mysql://localhost:3306/vaccination_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
}

$env:SOURCE_DB_URL = $sourceConfig['DB_URL']
$env:SOURCE_DB_USERNAME = if ($sourceConfig.ContainsKey('DB_USERNAME')) { $sourceConfig['DB_USERNAME'] } else { 'sa' }
$env:SOURCE_DB_PASSWORD = if ($sourceConfig.ContainsKey('DB_PASSWORD')) { $sourceConfig['DB_PASSWORD'] } else { '' }

if (-not $env:DB_MIGRATION_OUTPUT_FILE) {
    $env:DB_MIGRATION_OUTPUT_FILE = (Join-Path $repoRoot "backups\db-migration\backup.sql")
}

if (-not $env:DB_MIGRATION_VALIDATION_FILE) {
    $env:DB_MIGRATION_VALIDATION_FILE = (Join-Path $repoRoot "backups\db-migration\validation-report.txt")
}

Push-Location $backendPath
try {
    Write-Host "Running H2 -> MySQL migration tool..."
    mvn -q -DskipTests exec:java "-Dexec.mainClass=com.vaccine.tools.DatabaseMigrationTool"

    if ($StartBackend) {
        Write-Host "Migration succeeded. Starting backend with SPRING_PROFILES_ACTIVE=prod in this session."
        $env:SPRING_PROFILES_ACTIVE = "prod"
        mvn spring-boot:run
    }
}
finally {
    Pop-Location
}
