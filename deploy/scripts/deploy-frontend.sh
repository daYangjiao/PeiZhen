#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <local-h5-dist-directory> [web-root]" >&2
  exit 1
fi

SOURCE_DIR="$1"
WEB_ROOT="${2:-/var/www/pz-mini}"
STATIC_SOURCE_DIR="${3:-}"
ADMIN_SOURCE_DIR="${4:-}"
RELEASE_META_DIR="${RELEASE_META_DIR:-/var/lib/pz-deploy/releases}"
RELEASE_META_FILE="${RELEASE_META_DIR}/frontend-release.env"

if [[ ! -d "${SOURCE_DIR}" ]]; then
  echo "Source directory not found: ${SOURCE_DIR}" >&2
  exit 1
fi

install -d "${WEB_ROOT}"
rsync -av --delete "${SOURCE_DIR}/" "${WEB_ROOT}/"

if [[ -z "${STATIC_SOURCE_DIR}" ]]; then
  PROJECT_ROOT="$(cd "${SOURCE_DIR}/../../.." && pwd)"
  if [[ -d "${PROJECT_ROOT}/static" ]]; then
    STATIC_SOURCE_DIR="${PROJECT_ROOT}/static"
  fi
fi

if [[ -n "${STATIC_SOURCE_DIR}" ]]; then
  if [[ ! -d "${STATIC_SOURCE_DIR}" ]]; then
    echo "Static directory not found: ${STATIC_SOURCE_DIR}" >&2
    exit 1
  fi
  install -d "${WEB_ROOT}/static"
  rsync -av --delete "${STATIC_SOURCE_DIR}/" "${WEB_ROOT}/static/"
fi

if [[ -n "${ADMIN_SOURCE_DIR}" ]]; then
  if [[ ! -d "${ADMIN_SOURCE_DIR}" ]]; then
    echo "Admin directory not found: ${ADMIN_SOURCE_DIR}" >&2
    exit 1
  fi
  install -d "${WEB_ROOT}/admin"
  rsync -av --delete "${ADMIN_SOURCE_DIR}/" "${WEB_ROOT}/admin/"
fi

install -d "${RELEASE_META_DIR}"
cat > "${RELEASE_META_FILE}" <<EOF
RELEASE_COMPONENT=frontend
RELEASE_GIT_COMMIT=${RELEASE_GIT_COMMIT:-unknown}
RELEASE_GIT_SHORT_COMMIT=${RELEASE_GIT_SHORT_COMMIT:-unknown}
RELEASE_GIT_BRANCH=${RELEASE_GIT_BRANCH:-unknown}
RELEASE_GIT_REF=${RELEASE_GIT_REF:-unknown}
RELEASE_GIT_DIRTY=${RELEASE_GIT_DIRTY:-0}
RELEASE_TIMESTAMP=${RELEASE_TIMESTAMP:-unknown}
RELEASE_ACTOR=${RELEASE_ACTOR:-unknown}
DEPLOYED_AT=$(date '+%Y-%m-%dT%H:%M:%S%z')
DEPLOYED_WEB_ROOT=${WEB_ROOT}
EOF

echo "Frontend deployed to ${WEB_ROOT}"
