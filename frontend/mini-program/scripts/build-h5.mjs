import { spawn } from 'node:child_process'
import { execSync } from 'node:child_process'

const root = new URL('..', import.meta.url).pathname

const readGit = (command, fallback = '') => {
  try {
    return execSync(command, {
      cwd: root,
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'ignore']
    }).trim()
  } catch (error) {
    return fallback
  }
}

const gitCommit = readGit('git rev-parse HEAD', 'unknown')
const gitShortCommit = readGit('git rev-parse --short HEAD', 'unknown')
const gitBranch = readGit('git rev-parse --abbrev-ref HEAD', 'unknown')
const dirtyOutput = readGit('git status --porcelain --untracked-files=no', '')
const gitDirty = dirtyOutput ? '1' : '0'
const buildTime = new Date().toISOString()
const h5Version = `h5-${gitShortCommit}${gitDirty === '1' ? '-dirty' : ''}`

const env = {
  ...process.env,
  UNI_INPUT_DIR: '.',
  VITE_APP_GIT_COMMIT: gitCommit,
  VITE_APP_GIT_SHORT_COMMIT: gitShortCommit,
  VITE_APP_GIT_BRANCH: gitBranch,
  VITE_APP_GIT_DIRTY: gitDirty,
  VITE_APP_BUILD_TIME: buildTime,
  VITE_APP_H5_VERSION: h5Version,
  VITE_APP_BUILD_LABEL: h5Version
}

const command = process.platform === 'win32' ? 'npx.cmd' : 'npx'
const child = spawn(command, ['uni', 'build', '--platform', 'h5'], {
  cwd: root,
  env,
  stdio: 'inherit'
})

child.on('exit', (code) => {
  process.exit(code ?? 1)
})
