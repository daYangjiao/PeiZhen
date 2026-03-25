$ErrorActionPreference = 'Stop'

$javaHome = 'C:\Users\33613\.jdks\corretto-17.0.16'
$mavenBin = 'C:\Users\33613\tools\apache-maven-3.9.9\bin'
$nodeBin = 'C:\Users\33613\tools\node-v22.15.0-win-x64'
$projectRoot = Split-Path -Parent $PSScriptRoot
$logsDir = Join-Path $projectRoot 'logs'
$stdout = Join-Path $logsDir 'backend.out.log'
$stderr = Join-Path $logsDir 'backend.err.log'
$serverPort = '8081'
$jvmArgs = "-DSPRING_PROFILES_ACTIVE=dev -DSERVER_PORT=$serverPort"

New-Item -ItemType Directory -Force -Path $logsDir | Out-Null

$env:JAVA_HOME = $javaHome
$env:Path = "$($javaHome)\bin;$mavenBin;$nodeBin;" + [Environment]::GetEnvironmentVariable('Path', 'User') + ';' + [Environment]::GetEnvironmentVariable('Path', 'Machine')

& (Join-Path $projectRoot 'scripts\setup-local-mysql.ps1')

$existing = Get-CimInstance Win32_Process | Where-Object {
  $_.Name -match 'java(.exe)?' -and $_.CommandLine -like '*spring-boot:run*' -and $_.CommandLine -like "*$projectRoot*"
}
if ($existing) {
  Write-Host 'Backend is already running.'
  exit 0
}

Start-Process -FilePath 'mvn.cmd' `
  -ArgumentList "-Dspring-boot.run.jvmArguments=$jvmArgs", 'spring-boot:run' `
  -WorkingDirectory $projectRoot `
  -RedirectStandardOutput $stdout `
  -RedirectStandardError $stderr

Start-Sleep -Seconds 12

Write-Host 'STDOUT tail:'
if (Test-Path $stdout) {
  Get-Content $stdout -Tail 80
}

Write-Host 'STDERR tail:'
if (Test-Path $stderr) {
  Get-Content $stderr -Tail 80
}
