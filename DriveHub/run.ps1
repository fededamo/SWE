# Script di avvio per DriveHub
$ErrorActionPreference = "Stop"

# Verifica Java e Maven: usa quelli di sistema se presenti, altrimenti il percorso locale
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    $fallbackJdk = "$env:LOCALAPPDATA\Programs\jdk-21"
    if (Test-Path $fallbackJdk) {
        $env:JAVA_HOME = $fallbackJdk
        $env:Path = "$fallbackJdk\bin;$env:Path"
    }
}

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    $fallbackMaven = "$env:LOCALAPPDATA\Programs\apache-maven-3.9.9"
    if (Test-Path $fallbackMaven) {
        $env:MAVEN_HOME = $fallbackMaven
        $env:Path = "$fallbackMaven\bin;$env:Path"
    }
}

# Carica variabili dal file .env se presente
$envFile = Join-Path $PSScriptRoot ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
            $parts = $line.Split("=", 2)
            $varName = $parts[0].Trim()
            $varValue = $parts[1].Trim()
            [System.Environment]::SetEnvironmentVariable($varName, $varValue, "Process")
        }
    }
}

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "   Avvio DriveHub (JavaFX + Maven)        " -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "JAVA_HOME: $env:JAVA_HOME"
Write-Host "DB URL:    $env:DRIVEHUB_DB_URL"
Write-Host "DB USER:   $env:DRIVEHUB_DB_USER"
Write-Host "SEED DEMO: $env:DRIVEHUB_DB_SEED_DEMO"
Write-Host "------------------------------------------"

mvn javafx:run
