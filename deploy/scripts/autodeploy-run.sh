#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${AUTODEPLOY_ENV_FILE:-/etc/pz-autodeploy/autodeploy.env}"
if [[ -f "${ENV_FILE}" ]]; then
  # shellcheck disable=SC1090
  source "${ENV_FILE}"
fi

AUTODEPLOY_ROOT="${AUTODEPLOY_ROOT:-/opt/pz-autodeploy}"
SOURCE_DIR="${AUTODEPLOY_SOURCE_DIR:-${AUTODEPLOY_ROOT}/source}"
LOG_DIR="${AUTODEPLOY_LOG_DIR:-/var/log/pz-autodeploy}"
LOCK_FILE="${AUTODEPLOY_LOCK_FILE:-${AUTODEPLOY_ROOT}/deploy.lock}"
REPO_URL="${GITEE_REPO_URL:-https://gitee.com/wkq-karry/master.git}"
BRANCH="${AUTODEPLOY_BRANCH:-FF}"
REMOTE_ROOT="${REMOTE_ROOT:-/home/ops/PZ_yuanbao}"
WEB_ROOT="${WEB_ROOT:-/var/www/pz-mini}"
SERVICE_NAME="${SERVICE_NAME:-pz-app}"
HBUILDERX_CLI="${HBUILDERX_CLI:-/opt/HBuilderX/cli}"
HBUILDERX_OPEN_TIMEOUT="${HBUILDERX_OPEN_TIMEOUT:-45s}"
HBUILDERX_COMMAND_TIMEOUT="${HBUILDERX_COMMAND_TIMEOUT:-300s}"
HBUILDERX_PUBLISH_TIMEOUT="${HBUILDERX_PUBLISH_TIMEOUT:-900s}"
WGT_UPDATE_NOTES="${WGT_UPDATE_NOTES:-资源更新与问题修复}"
TARGET_COMMIT="${1:-}"

mkdir -p "${AUTODEPLOY_ROOT}" "${LOG_DIR}"
exec 9>"${LOCK_FILE}"
if ! flock -n 9; then
  echo "Another autodeploy run is already active; exiting." >&2
  exit 75
fi

log() {
  printf '[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S%z')" "$*"
}

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

hbuilderx_cli() {
  local timeout_value="$1"
  shift
  timeout "${timeout_value}" "${HBUILDERX_CLI}" "$@"
}

cleanup_hbuilderx() {
  if [[ -x "${HBUILDERX_CLI}" ]]; then
    hbuilderx_cli 20s app quit >/dev/null 2>&1 || true
  fi
}

read_manifest_field() {
  local field="$1"
  python3 - "$SOURCE_DIR/frontend/mini-program/manifest.json" "$field" <<'PY'
import json
import sys
from pathlib import Path

manifest = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
value = manifest.get(sys.argv[2], "")
print(value)
PY
}

publish_wgt_metadata() {
  local wgt_file="$1"
  local app_version="$2"
  local app_version_code="$3"
  local wgt_version="$4"
  local wgt_name="$5"
  local upload_dir="/opt/pz-app/uploads/app-updates/android"
  local metadata_dir="/opt/pz-app/uploads/app-updates"

  sudo install -d -m 0755 "${upload_dir}" "${metadata_dir}"
  sudo install -m 0644 "${wgt_file}" "${upload_dir}/${wgt_name}"
  local tmp_json
  tmp_json="$(mktemp)"
  APP_VERSION="${app_version}" \
  APP_VERSION_CODE="${app_version_code}" \
  WGT_VERSION="${wgt_version}" \
  WGT_NAME="${wgt_name}" \
  WGT_UPDATE_NOTES="${WGT_UPDATE_NOTES}" \
  python3 >"${tmp_json}" <<'PY'
import json
import os

payload = {
    "title": "发现新版本",
    "forceUpdate": False,
    "latestVersion": os.environ["APP_VERSION"],
    "latestVersionCode": int(os.environ["APP_VERSION_CODE"]),
    "notes": os.environ["WGT_UPDATE_NOTES"],
    "wgtVersion": os.environ["WGT_VERSION"],
    "wgtPath": f"/uploads/app-updates/android/{os.environ['WGT_NAME']}",
}
print(json.dumps(payload, ensure_ascii=False, indent=2))
PY
  sudo install -m 0644 "${tmp_json}" "${metadata_dir}/android.json"
  rm -f "${tmp_json}"
}

create_wgt_from_app_plus() {
  local app_plus_dir="$1"
  local wgt_file="$2"

  if [[ ! -f "${app_plus_dir}/manifest.json" ]]; then
    echo "wgt fallback failed: app-plus manifest not found: ${app_plus_dir}/manifest.json" >&2
    return 1
  fi

  APP_PLUS_DIR="${app_plus_dir}" WGT_FILE="${wgt_file}" python3 <<'PY'
import os
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile

source = Path(os.environ["APP_PLUS_DIR"])
target = Path(os.environ["WGT_FILE"])
target.parent.mkdir(parents=True, exist_ok=True)
with ZipFile(target, "w", ZIP_DEFLATED) as archive:
    for path in sorted(source.rglob("*")):
        if path.is_file():
            archive.write(path, path.relative_to(source).as_posix())
PY
}

write_wgt_release_metadata() {
  local wgt_file="$1"
  local wgt_version="$2"
  local release_meta_dir="${RELEASE_META_DIR:-/var/lib/pz-deploy/releases}"
  sudo install -d "${release_meta_dir}"
  sudo tee "${release_meta_dir}/wgt-release.env" >/dev/null <<EOF
RELEASE_COMPONENT=wgt
RELEASE_GIT_COMMIT=${RELEASE_GIT_COMMIT}
RELEASE_GIT_SHORT_COMMIT=${RELEASE_GIT_SHORT_COMMIT}
RELEASE_GIT_BRANCH=${RELEASE_GIT_BRANCH}
RELEASE_GIT_REF=${RELEASE_GIT_REF}
RELEASE_GIT_DIRTY=${RELEASE_GIT_DIRTY}
RELEASE_TIMESTAMP=${RELEASE_TIMESTAMP}
RELEASE_ACTOR=${RELEASE_ACTOR}
DEPLOYED_AT=$(date '+%Y-%m-%dT%H:%M:%S%z')
WGT_VERSION=${wgt_version}
WGT_FILE=${wgt_file}
EOF
}

require_cmd git
require_cmd java
require_cmd mvn
require_cmd node
require_cmd npm
require_cmd python3
require_cmd timeout

log "Preparing source ${REPO_URL} branch=${BRANCH}"
if [[ ! -d "${SOURCE_DIR}/.git" ]]; then
  rm -rf "${SOURCE_DIR}"
  git clone --branch "${BRANCH}" "${REPO_URL}" "${SOURCE_DIR}"
else
  git -C "${SOURCE_DIR}" remote set-url origin "${REPO_URL}"
fi

git -C "${SOURCE_DIR}" fetch --prune origin "${BRANCH}" --tags
if [[ -z "${TARGET_COMMIT}" ]]; then
  TARGET_COMMIT="origin/${BRANCH}"
fi
git -C "${SOURCE_DIR}" checkout --force "${TARGET_COMMIT}"
sudo rm -rf "${SOURCE_DIR}/target"
git -C "${SOURCE_DIR}" clean -ffdx

RELEASE_GIT_COMMIT="$(git -C "${SOURCE_DIR}" rev-parse HEAD)"
RELEASE_GIT_SHORT_COMMIT="$(git -C "${SOURCE_DIR}" rev-parse --short HEAD)"
RELEASE_GIT_BRANCH="${BRANCH}"
RELEASE_GIT_REF="${BRANCH}"
RELEASE_GIT_DIRTY="0"
RELEASE_TIMESTAMP="$(date '+%Y-%m-%dT%H:%M:%S%z')"
RELEASE_ACTOR="${RELEASE_ACTOR:-gitee-webhook}"

export RELEASE_GIT_COMMIT RELEASE_GIT_SHORT_COMMIT RELEASE_GIT_BRANCH RELEASE_GIT_REF
export RELEASE_GIT_DIRTY RELEASE_TIMESTAMP RELEASE_ACTOR

log "Installing frontend dependencies"
npm ci --prefix "${SOURCE_DIR}/frontend/mini-program"
npm ci --prefix "${SOURCE_DIR}/frontend/admin"

log "Building backend ${RELEASE_GIT_SHORT_COMMIT}"
(
  cd "${SOURCE_DIR}"
  mvn -DskipTests clean package
)
BACKEND_JAR_FILE="$(find "${SOURCE_DIR}/target" -maxdepth 1 -type f -name '*.jar' ! -name 'original-*.jar' | head -n 1)"
if [[ -z "${BACKEND_JAR_FILE}" ]]; then
  echo "No runnable jar found in ${SOURCE_DIR}/target." >&2
  exit 1
fi

log "Deploying backend ${RELEASE_GIT_SHORT_COMMIT}"
(
  cd "${SOURCE_DIR}"
  sudo \
    BACKEND_SKIP_BUILD=1 \
    BACKEND_JAR_FILE="${BACKEND_JAR_FILE}" \
    RELEASE_GIT_COMMIT="${RELEASE_GIT_COMMIT}" \
    RELEASE_GIT_SHORT_COMMIT="${RELEASE_GIT_SHORT_COMMIT}" \
    RELEASE_GIT_BRANCH="${RELEASE_GIT_BRANCH}" \
    RELEASE_GIT_REF="${RELEASE_GIT_REF}" \
    RELEASE_GIT_DIRTY="${RELEASE_GIT_DIRTY}" \
    RELEASE_TIMESTAMP="${RELEASE_TIMESTAMP}" \
    RELEASE_ACTOR="${RELEASE_ACTOR}" \
    SERVICE_NAME="${SERVICE_NAME}" \
    bash deploy/scripts/deploy-backend.sh
)
systemctl is-active --quiet "${SERVICE_NAME}"

log "Building and deploying H5/admin"
(
  cd "${SOURCE_DIR}/frontend/mini-program"
  npm run build:h5
)
(
  cd "${SOURCE_DIR}/frontend/admin"
  npm run build
)
(
  cd "${SOURCE_DIR}"
  sudo \
    RELEASE_GIT_COMMIT="${RELEASE_GIT_COMMIT}" \
    RELEASE_GIT_SHORT_COMMIT="${RELEASE_GIT_SHORT_COMMIT}" \
    RELEASE_GIT_BRANCH="${RELEASE_GIT_BRANCH}" \
    RELEASE_GIT_REF="${RELEASE_GIT_REF}" \
    RELEASE_GIT_DIRTY="${RELEASE_GIT_DIRTY}" \
    RELEASE_TIMESTAMP="${RELEASE_TIMESTAMP}" \
    RELEASE_ACTOR="${RELEASE_ACTOR}" \
    bash deploy/scripts/deploy-frontend.sh \
      "${SOURCE_DIR}/frontend/mini-program/dist/build/h5" \
      "${WEB_ROOT}" \
      "${SOURCE_DIR}/frontend/mini-program/static" \
      "${SOURCE_DIR}/frontend/admin/dist" \
      "${SOURCE_DIR}/frontend/mini-program/public"
)

log "Building WGT with HBuilderX CLI"
if [[ ! -x "${HBUILDERX_CLI}" ]]; then
  echo "wgt failed: HBuilderX CLI is not executable: ${HBUILDERX_CLI}" >&2
  exit 1
fi
trap cleanup_hbuilderx EXIT
hbuilderx_cli "${HBUILDERX_OPEN_TIMEOUT}" open || true
sleep 2
if [[ -n "${HBUILDERX_USERNAME:-}" && -n "${HBUILDERX_PASSWORD:-}" ]]; then
  hbuilderx_cli "${HBUILDERX_COMMAND_TIMEOUT}" user login --username "${HBUILDERX_USERNAME}" --password "${HBUILDERX_PASSWORD}"
else
  echo "wgt warning: HBUILDERX_USERNAME/HBUILDERX_PASSWORD are empty; trying without login." >&2
fi
HBUILDERX_PROJECT_PATH="${HBUILDERX_PROJECT_PATH:-${SOURCE_DIR}/frontend/mini-program}"
hbuilderx_cli "${HBUILDERX_COMMAND_TIMEOUT}" project open --path "${HBUILDERX_PROJECT_PATH}"

APP_VERSION="$(read_manifest_field versionName)"
APP_VERSION_CODE="$(read_manifest_field versionCode)"
WGT_VERSION="${APP_VERSION}-wgt.${RELEASE_GIT_SHORT_COMMIT}"
WGT_NAME="yuanban-${WGT_VERSION}.wgt"
WGT_RELEASE_DIR="${AUTODEPLOY_ROOT}/wgt-release"
rm -rf "${WGT_RELEASE_DIR}"
mkdir -p "${WGT_RELEASE_DIR}"

if ! hbuilderx_cli "${HBUILDERX_PUBLISH_TIMEOUT}" publish \
  app \
  --type wgt \
  --project "${HBUILDERX_PROJECT_PATH}" \
  --path "${WGT_RELEASE_DIR}" \
  --name "${WGT_NAME}"; then
  echo "wgt warning: HBuilderX CLI publish returned non-zero; checking compiled app-plus output." >&2
fi

WGT_FILE="${WGT_RELEASE_DIR}/${WGT_NAME}"
if [[ ! -f "${WGT_FILE}" ]]; then
  WGT_FILE="$(find "${WGT_RELEASE_DIR}" -maxdepth 1 -type f -name '*.wgt' | head -n 1)"
fi
if [[ -z "${WGT_FILE}" || ! -f "${WGT_FILE}" ]]; then
  APP_PLUS_DIR="${HBUILDERX_PROJECT_PATH}/unpackage/dist/build/app-plus"
  echo "wgt warning: no .wgt file found; creating fallback package from ${APP_PLUS_DIR}" >&2
  WGT_FILE="${WGT_RELEASE_DIR}/${WGT_NAME}"
  create_wgt_from_app_plus "${APP_PLUS_DIR}" "${WGT_FILE}" || {
    echo "wgt failed: HBuilderX CLI completed but no .wgt file was found in ${WGT_RELEASE_DIR}" >&2
    exit 1
  }
fi

publish_wgt_metadata "${WGT_FILE}" "${APP_VERSION}" "${APP_VERSION_CODE}" "${WGT_VERSION}" "${WGT_NAME}"
write_wgt_release_metadata "${WGT_FILE}" "${WGT_VERSION}"

curl -fsS -o /dev/null "http://127.0.0.1:8080/api/app-upgrade/check" || true
curl -fsS -o /dev/null "${WEB_HEALTH_URL:-http://127.0.0.1/}"

log "Autodeploy complete commit=${RELEASE_GIT_SHORT_COMMIT} wgt=${WGT_VERSION}"
