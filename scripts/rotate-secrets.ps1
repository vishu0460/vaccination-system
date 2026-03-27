$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$backendDir = Join-Path $repoRoot "backend"
$classpathFile = Join-Path $backendDir ".tmp-security-classpath.txt"

Push-Location $backendDir
try {
    mvn -q -DskipTests dependency:build-classpath "-Dmdep.outputFile=$classpathFile" | Out-Null
    $dependencyClasspath = Get-Content $classpathFile -Raw
}
finally {
    Pop-Location
}

if (-not $dependencyClasspath) {
    throw "Unable to resolve backend dependency classpath for BCrypt generation."
}

$randomBytes = New-Object byte[] 64
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $rng.GetBytes($randomBytes)
}
finally {
    $rng.Dispose()
}
$jwtSecret = [Convert]::ToBase64String($randomBytes)

$alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%^&*()-_=+"
$passwordChars = for ($index = 0; $index -lt 20; $index++) {
    $alphabet[(Get-Random -Minimum 0 -Maximum $alphabet.Length)]
}
$adminPassword = -join $passwordChars

$jshellScript = @"
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
var encoder = new BCryptPasswordEncoder(12);
System.out.println(encoder.encode("$adminPassword"));
/exit
"@

$bcryptHash = $jshellScript | jshell --class-path $dependencyClasspath 2>$null |
    ForEach-Object { $_ -replace '^jshell>\s*', '' } |
    Where-Object { $_ -and $_ -notmatch "^\|" -and $_ -notmatch "^Welcome" -and $_ -notmatch "^Goodbye" -and $_ -match '^\$2[aby]\$.+' } |
    Select-Object -Last 1

if (-not $bcryptHash) {
    throw "Failed to generate BCrypt hash."
}

Write-Host "JWT_SECRET=$jwtSecret"
Write-Host "ADMIN_PASSWORD=$adminPassword"
Write-Host "ADMIN_PASSWORD_BCRYPT=$bcryptHash"

if (Test-Path $classpathFile) {
    Remove-Item $classpathFile -Force
}
