#!/usr/bin/env bash
set -euo pipefail

TEMP_WORKTREE=""
SYNC_ROOT=""
RELEASE_GIT_COMMIT=""
RELEASE_GIT_SHORT_COMMIT=""
RELEASE_GIT_BRANCH=""
RELEASE_GIT_REF=""
RELEASE_GIT_DIRTY="0"
RELEASE_TIMESTAMP=""
RELEASE_ACTOR=""

cleanup_release_workspace() {
  if [[ -n "${TEMP_WORKTREE}" && -d "${TEMP_WORKTREE}" ]]; then
    git -C "${PROJECT_ROOT}" worktree remove --force "${TEMP_WORKTREE}" >/dev/null 2>&1 || true
  fi
}

prepare_release_workspace() {
  if ! git -C "${PROJECT_ROOT}" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    echo "Project root is not a git repository: ${PROJECT_ROOT}" >&2
    exit 1
  fi

  RELEASE_ACTOR="${RELEASE_ACTOR:-${USER:-unknown}}"
  RELEASE_TIMESTAMP="$(date '+%Y-%m-%dT%H:%M:%S%z')"

  if [[ -n "${GIT_REF:-}" ]]; then
    TEMP_WORKTREE="$(mktemp -d "${TMPDIR:-/tmp}/pz-publish-XXXXXX")"
    git -C "${PROJECT_ROOT}" worktree add --detach "${TEMP_WORKTREE}" "${GIT_REF}" >/dev/null
    SYNC_ROOT="${TEMP_WORKTREE}"
    RELEASE_GIT_REF="${GIT_REF}"
    trap cleanup_release_workspace EXIT
  else
    if [[ -n "$(git -C "${PROJECT_ROOT}" status --porcelain --untracked-files=no)" ]]; then
      RELEASE_GIT_DIRTY="1"
    fi
    if [[ "${ALLOW_DIRTY:-0}" != "1" ]] && [[ "${RELEASE_GIT_DIRTY}" == "1" ]]; then
      echo "Refusing to publish from a dirty tracked worktree. Commit or stash changes first, or set ALLOW_DIRTY=1." >&2
      exit 1
    fi
    SYNC_ROOT="${PROJECT_ROOT}"
    RELEASE_GIT_REF="$(git -C "${PROJECT_ROOT}" rev-parse --abbrev-ref HEAD)"
  fi

  RELEASE_GIT_COMMIT="$(git -C "${SYNC_ROOT}" rev-parse HEAD)"
  RELEASE_GIT_SHORT_COMMIT="$(git -C "${SYNC_ROOT}" rev-parse --short HEAD)"
  RELEASE_GIT_BRANCH="$(git -C "${SYNC_ROOT}" rev-parse --abbrev-ref HEAD)"
}
