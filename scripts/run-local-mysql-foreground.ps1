$ErrorActionPreference = 'Stop'

$rootDir = Join-Path $env:USERPROFILE 'mysql-local'
$myIni = Join-Path $rootDir 'my.ini'
$mysqldExe = 'D:\mysql\bin\mysqld.exe'

if (-not (Test-Path $myIni)) {
  throw "Config file not found: $myIni"
}

& $mysqldExe "--defaults-file=$myIni" --console
