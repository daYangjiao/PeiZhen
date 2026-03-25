$ErrorActionPreference = 'Stop'

$baseDir = 'D:\mysql'
$rootDir = Join-Path $env:USERPROFILE 'mysql-local'
$dataDir = Join-Path $rootDir 'data'
$logsDir = Join-Path $rootDir 'logs'
$mysqlExe = Join-Path $baseDir 'bin\mysql.exe'
$mysqldExe = Join-Path $baseDir 'bin\mysqld.exe'
$myIni = Join-Path $rootDir 'my.ini'

New-Item -ItemType Directory -Force -Path $rootDir, $logsDir | Out-Null

if (-not (Test-Path $mysqldExe)) {
  throw "mysqld.exe not found at $mysqldExe"
}

if (-not (Test-Path $dataDir) -or -not (Get-ChildItem $dataDir -Force -ErrorAction SilentlyContinue)) {
  Remove-Item $dataDir -Recurse -Force -ErrorAction SilentlyContinue
  New-Item -ItemType Directory -Force -Path $dataDir | Out-Null
  & $mysqldExe --initialize-insecure --basedir=$baseDir --datadir=$dataDir --console
  if ($LASTEXITCODE -ne 0) {
    throw "mysqld initialize failed with exit code $LASTEXITCODE"
  }
}

@" 
[client]
port=3307
default-character-set=utf8mb4

[mysqld]
port=3307
bind-address=127.0.0.1
basedir=$baseDir
datadir=$dataDir
character-set-server=utf8mb4
collation-server=utf8mb4_0900_ai_ci
default-storage-engine=INNODB
lower_case_table_names=1
secure-file-priv=
mysqlx=0
log-error=$logsDir\mysqld.err
pid-file=$rootDir\mysqld.pid
"@ | Set-Content -Path $myIni -Encoding ASCII

$existing = Get-CimInstance Win32_Process | Where-Object {
  $_.Name -eq 'mysqld.exe' -and $_.CommandLine -like "*$rootDir*"
}
if (-not $existing) {
  Start-Process -FilePath $mysqldExe -ArgumentList "--defaults-file=$myIni", '--console' -WindowStyle Hidden
  Start-Sleep -Seconds 6
}

if (-not (Test-NetConnection -ComputerName 127.0.0.1 -Port 3307).TcpTestSucceeded) {
  throw 'Local MySQL failed to start on port 3307'
}

Write-Host 'Local MySQL is listening on 127.0.0.1:3307'
