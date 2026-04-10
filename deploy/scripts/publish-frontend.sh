#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
source "${SCRIPT_DIR}/publish-lib.sh"

SERVER_HOST="${SERVER_HOST:-101.245.94.141}"
SERVER_USER="${SERVER_USER:-ops}"
SSH_KEY="${SSH_KEY:-$HOME/.ssh/pz_ops_ed25519}"
REMOTE_ROOT="${REMOTE_ROOT:-/home/${SERVER_USER}/PZ_yuanbao}"
SSH_OPTS=(-i "${SSH_KEY}")

prepare_release_workspace

FRONTEND_DIR="${SYNC_ROOT}/frontend/mini-program"
DIST_DIR="${FRONTEND_DIR}/dist/build/h5"
STATIC_DIR="${FRONTEND_DIR}/static"
PUBLIC_DIR="${FRONTEND_DIR}/public"
ADMIN_DIR="${SYNC_ROOT}/frontend/admin"
ADMIN_DIST_DIR="${ADMIN_DIR}/dist"

if [[ ! -f "${SSH_KEY}" ]]; then
  echo "SSH key not found: ${SSH_KEY}" >&2
  exit 1
fi

if [[ ! -d "${FRONTEND_DIR}" ]]; then
  echo "Frontend directory not found: ${FRONTEND_DIR}" >&2
  exit 1
fi

if [[ ! -d "${PUBLIC_DIR}" ]]; then
  echo "Public directory not found: ${PUBLIC_DIR}" >&2
  exit 1
fi

cd "${FRONTEND_DIR}"
npm run build:h5

if [[ -d "${ADMIN_DIR}" ]]; then
  cd "${ADMIN_DIR}"
  npm run build
fi

rsync -av --delete -e "ssh ${SSH_OPTS[*]}" \
  "${DIST_DIR}/" \
  "${SERVER_USER}@${SERVER_HOST}:${REMOTE_ROOT}/frontend/mini-program/dist/build/h5/"

rsync -av --delete -e "ssh ${SSH_OPTS[*]}" \
  "${STATIC_DIR}/" \
  "${SERVER_USER}@${SERVER_HOST}:${REMOTE_ROOT}/frontend/mini-program/static/"

rsync -av --delete -e "ssh ${SSH_OPTS[*]}" \
  "${PUBLIC_DIR}/" \
  "${SERVER_USER}@${SERVER_HOST}:${REMOTE_ROOT}/frontend/mini-program/public/"

if [[ -d "${ADMIN_DIR}" ]]; then
  rsync -av --delete -e "ssh ${SSH_OPTS[*]}" \
    "${ADMIN_DIST_DIR}/" \
    "${SERVER_USER}@${SERVER_HOST}:${REMOTE_ROOT}/frontend/admin/dist/"
fi

rsync -av -e "ssh ${SSH_OPTS[*]}" \
  "${PROJECT_ROOT}/deploy/" \
  "${SERVER_USER}@${SERVER_HOST}:${REMOTE_ROOT}/deploy/"

ssh "${SSH_OPTS[@]}" "${SERVER_USER}@${SERVER_HOST}" \
  "cd '${REMOTE_ROOT}' && sudo \
  RELEASE_GIT_COMMIT='${RELEASE_GIT_COMMIT}' \
  RELEASE_GIT_SHORT_COMMIT='${RELEASE_GIT_SHORT_COMMIT}' \
  RELEASE_GIT_BRANCH='${RELEASE_GIT_BRANCH}' \
  RELEASE_GIT_REF='${RELEASE_GIT_REF}' \
  RELEASE_GIT_DIRTY='${RELEASE_GIT_DIRTY}' \
  RELEASE_TIMESTAMP='${RELEASE_TIMESTAMP}' \
  RELEASE_ACTOR='${RELEASE_ACTOR}' \
  bash deploy/scripts/deploy-frontend.sh '${REMOTE_ROOT}/frontend/mini-program/dist/build/h5' '/var/www/pz-mini' '${REMOTE_ROOT}/frontend/mini-program/static' '${REMOTE_ROOT}/frontend/admin/dist' '${REMOTE_ROOT}/frontend/mini-program/public'"

ssh "${SSH_OPTS[@]}" "${SERVER_USER}@${SERVER_HOST}" \
  "sudo test -f /var/lib/pz-deploy/releases/frontend-release.env && \
   sudo grep -q '^RELEASE_GIT_COMMIT=${RELEASE_GIT_COMMIT}\$' /var/lib/pz-deploy/releases/frontend-release.env"

echo "Frontend publish complete: http://${SERVER_HOST}/ (${RELEASE_GIT_SHORT_COMMIT})"
