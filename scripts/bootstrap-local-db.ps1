$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$mysqlExe = 'D:\mysql\bin\mysql.exe'
$sqlFile = Join-Path $projectRoot 'database\student.sql'

& (Join-Path $projectRoot 'scripts\setup-local-mysql.ps1')

function Invoke-MySqlWithPassword {
  param(
    [string[]]$Arguments,
    [switch]$SkipPassword,
    [switch]$IgnoreExitCode
  )

  $baseArgs = @('--protocol=TCP', '-h', '127.0.0.1', '-P', '3307', '-u', 'root')
  if ($SkipPassword) {
    $baseArgs += '--skip-password'
  } else {
    $baseArgs += '--password=123456'
  }

  & $mysqlExe @baseArgs @Arguments
  if (-not $IgnoreExitCode -and $LASTEXITCODE -ne 0) {
    throw "mysql exited with code $LASTEXITCODE"
  }
}

$hasPassword = $false
Invoke-MySqlWithPassword -Arguments @('-e', 'SELECT 1;') -IgnoreExitCode
if ($LASTEXITCODE -eq 0) {
  $hasPassword = $true
}

if (-not $hasPassword) {
  Invoke-MySqlWithPassword -SkipPassword -Arguments @(
    '-e',
    "ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '123456'; CREATE USER IF NOT EXISTS 'root'@'127.0.0.1' IDENTIFIED WITH mysql_native_password BY '123456'; GRANT ALL PRIVILEGES ON *.* TO 'root'@'127.0.0.1' WITH GRANT OPTION; FLUSH PRIVILEGES;"
  )
}

Invoke-MySqlWithPassword -Arguments @(
  '-e',
  'DROP DATABASE IF EXISTS student; CREATE DATABASE student CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;'
)

Get-Content $sqlFile | & $mysqlExe --protocol=TCP -h 127.0.0.1 -P 3307 -u root --password=123456 student
if ($LASTEXITCODE -ne 0) {
  throw "Import failed with exit code $LASTEXITCODE"
}

Write-Host 'Local database bootstrapped successfully.'
Write-Host 'Admin login: 13800000000 / admin123'
