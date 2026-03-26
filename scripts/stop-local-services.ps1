$ErrorActionPreference = 'SilentlyContinue'

$backendProcIds = @(Get-NetTCPConnection -LocalPort 8081 -State Listen | Select-Object -ExpandProperty OwningProcess -Unique)
foreach ($procId in $backendProcIds) {
  if ($procId) {
    Stop-Process -Id $procId -Force
  }
}

$adminListenProcIds = @(Get-NetTCPConnection -LocalPort 5173 -State Listen | Select-Object -ExpandProperty OwningProcess -Unique)
foreach ($procId in $adminListenProcIds) {
  if ($procId) {
    Stop-Process -Id $procId -Force
  }
}

$adminProcIds = Get-CimInstance Win32_Process | Where-Object {
  $_.Name -match 'node(.exe)?' -and $_.CommandLine -like '*vite*' -and $_.CommandLine -like '*frontend\\admin*'
} | Select-Object -ExpandProperty ProcessId
foreach ($procId in $adminProcIds) {
  if ($procId) {
    Stop-Process -Id $procId -Force
  }
}

Write-Host 'Local backend and admin processes stopped if they were running.'
