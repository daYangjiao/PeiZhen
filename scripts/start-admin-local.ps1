$ErrorActionPreference = 'Stop'

$javaHome = 'C:\Users\33613\.jdks\corretto-17.0.16'
$mavenBin = 'C:\Users\33613\tools\apache-maven-3.9.9\bin'
$nodeBin = 'C:\Users\33613\tools\node-v22.15.0-win-x64'
$projectRoot = Split-Path -Parent $PSScriptRoot
$adminRoot = Join-Path $projectRoot 'frontend\admin'
$logsDir = Join-Path $projectRoot 'logs'
$stdout = Join-Path $logsDir 'admin.out.log'
$stderr = Join-Path $logsDir 'admin.err.log'

New-Item -ItemType Directory -Force -Path $logsDir | Out-Null

$env:JAVA_HOME = $javaHome
$env:Path = "$($javaHome)\bin;$mavenBin;$nodeBin;" + [Environment]::GetEnvironmentVariable('Path', 'User') + ';' + [Environment]::GetEnvironmentVariable('Path', 'Machine')

$existing = Get-CimInstance Win32_Process | Where-Object {
  $_.Name -match 'node(.exe)?' -and $_.CommandLine -like '*vite*' -and $_.CommandLine -like "*$adminRoot*"
}
if ($existing) {
  Write-Host 'Admin frontend is already running.'
  exit 0
}

Start-Process -FilePath 'npm.cmd' `
  -ArgumentList 'run', 'dev', '--', '--host', '127.0.0.1', '--port', '5173' `
  -WorkingDirectory $adminRoot `
  -RedirectStandardOutput $stdout `
  -RedirectStandardError $stderr

Start-Sleep -Seconds 8

Write-Host 'STDOUT tail:'
if (Test-Path $stdout) {
  Get-Content $stdout -Tail 80
}

Write-Host 'STDERR tail:'
if (Test-Path $stderr) {
  Get-Content $stderr -Tail 80
}
