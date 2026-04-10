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

if [[ ! -f "${SSH_KEY}" ]]; then
  echo "SSH key not found: ${SSH_KEY}" >&2
  exit 1
fi

rsync -av --delete -e "ssh ${SSH_OPTS[*]}" \
  --exclude ".git" \
  --exclude ".idea" \
  --exclude ".DS_Store" \
  --exclude "audit" \
  --exclude "node_modules" \
  --exclude "frontend/mini-program/dist" \
  --exclude "frontend/mini-program/unpackage" \
  --exclude "target" \
  "${SYNC_ROOT}/" \
  "${SERVER_USER}@${SERVER_HOST}:${REMOTE_ROOT}/"

ssh "${SSH_OPTS[@]}" "${SERVER_USER}@${SERVER_HOST}" \
  "cd '${REMOTE_ROOT}' && sudo \
  RELEASE_GIT_COMMIT='${RELEASE_GIT_COMMIT}' \
  RELEASE_GIT_SHORT_COMMIT='${RELEASE_GIT_SHORT_COMMIT}' \
  RELEASE_GIT_BRANCH='${RELEASE_GIT_BRANCH}' \
  RELEASE_GIT_REF='${RELEASE_GIT_REF}' \
  RELEASE_GIT_DIRTY='${RELEASE_GIT_DIRTY}' \
  RELEASE_TIMESTAMP='${RELEASE_TIMESTAMP}' \
  RELEASE_ACTOR='${RELEASE_ACTOR}' \
  bash deploy/scripts/deploy-backend.sh"

ssh "${SSH_OPTS[@]}" "${SERVER_USER}@${SERVER_HOST}" \
  "sudo test -f /var/lib/pz-deploy/releases/backend-release.env && \
   sudo grep -q '^RELEASE_GIT_COMMIT=${RELEASE_GIT_COMMIT}\$' /var/lib/pz-deploy/releases/backend-release.env"

echo "Backend publish complete on ${SERVER_HOST} (${RELEASE_GIT_SHORT_COMMIT})"
