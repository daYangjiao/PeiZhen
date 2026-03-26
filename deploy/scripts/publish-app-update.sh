#!/usr/bin/env bash
set -euo pipefail

SERVER_HOST="${SERVER_HOST:-101.245.94.141}"
SERVER_USER="${SERVER_USER:-ops}"
SSH_KEY="${SSH_KEY:-$HOME/.ssh/pz_ops_ed25519}"
UPLOAD_DIR="${UPLOAD_DIR:-/opt/pz-app/uploads/app-updates/android}"
METADATA_DIR="${METADATA_DIR:-/opt/pz-app/uploads/app-updates}"
TMP_JSON="$(mktemp)"

APP_VERSION="${APP_VERSION:?APP_VERSION is required}"
APP_VERSION_CODE="${APP_VERSION_CODE:?APP_VERSION_CODE is required}"
UPDATE_NOTES="${UPDATE_NOTES:-}"
FORCE_UPDATE="${FORCE_UPDATE:-false}"
APK_FILE="${APK_FILE:-}"
APK_NAME="${APK_NAME:-}"
WGT_FILE="${WGT_FILE:-}"
WGT_VERSION="${WGT_VERSION:-}"
WGT_NAME="${WGT_NAME:-}"

if [[ -z "${APK_FILE}" && -z "${WGT_FILE}" ]]; then
  echo "Provide at least one of APK_FILE or WGT_FILE." >&2
  exit 1
fi

if [[ -n "${APK_FILE}" && ! -f "${APK_FILE}" ]]; then
  echo "APK file not found: ${APK_FILE}" >&2
  exit 1
fi

if [[ -n "${WGT_FILE}" && ! -f "${WGT_FILE}" ]]; then
  echo "WGT file not found: ${WGT_FILE}" >&2
  exit 1
fi

APK_NAME="${APK_NAME:-yuanban-${APP_VERSION}.apk}"
WGT_NAME="${WGT_NAME:-yuanban-${WGT_VERSION:-$APP_VERSION}.wgt}"

TMP_JSON_PATH="${TMP_JSON}" \
APP_VERSION_ENV="${APP_VERSION}" \
APP_VERSION_CODE_ENV="${APP_VERSION_CODE}" \
UPDATE_NOTES_ENV="${UPDATE_NOTES}" \
FORCE_UPDATE_ENV="${FORCE_UPDATE}" \
APK_FILE_ENV="${APK_FILE}" \
APK_NAME_ENV="${APK_NAME}" \
WGT_FILE_ENV="${WGT_FILE}" \
WGT_VERSION_ENV="${WGT_VERSION:-$APP_VERSION}" \
WGT_NAME_ENV="${WGT_NAME}" \
python3 <<'PY'
import json
import os
from pathlib import Path

payload = {
    "title": "发现新版本",
    "forceUpdate": os.environ["FORCE_UPDATE_ENV"].lower() == "true",
    "latestVersion": os.environ["APP_VERSION_ENV"],
    "latestVersionCode": int(os.environ["APP_VERSION_CODE_ENV"]),
}

notes = os.environ.get("UPDATE_NOTES_ENV", "").strip()
if notes:
    payload["notes"] = notes

apk_file = os.environ.get("APK_FILE_ENV", "").strip()
if apk_file:
    payload["apkPath"] = f"/uploads/app-updates/android/{os.environ['APK_NAME_ENV']}"

wgt_file = os.environ.get("WGT_FILE_ENV", "").strip()
if wgt_file:
    payload["wgtVersion"] = os.environ["WGT_VERSION_ENV"]
    payload["wgtPath"] = f"/uploads/app-updates/android/{os.environ['WGT_NAME_ENV']}"

Path(os.environ["TMP_JSON_PATH"]).write_text(
    json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
    encoding="utf-8",
)
PY

SSH_OPTS=(-i "${SSH_KEY}")
ssh "${SSH_OPTS[@]}" "${SERVER_USER}@${SERVER_HOST}" "sudo mkdir -p '${UPLOAD_DIR}' '${METADATA_DIR}'"

if [[ -n "${APK_FILE}" ]]; then
  scp "${SSH_OPTS[@]}" "${APK_FILE}" "${SERVER_USER}@${SERVER_HOST}:/tmp/${APK_NAME}"
  ssh "${SSH_OPTS[@]}" "${SERVER_USER}@${SERVER_HOST}" "sudo mv '/tmp/${APK_NAME}' '${UPLOAD_DIR}/${APK_NAME}' && sudo chmod 644 '${UPLOAD_DIR}/${APK_NAME}'"
fi

if [[ -n "${WGT_FILE}" ]]; then
  scp "${SSH_OPTS[@]}" "${WGT_FILE}" "${SERVER_USER}@${SERVER_HOST}:/tmp/${WGT_NAME}"
  ssh "${SSH_OPTS[@]}" "${SERVER_USER}@${SERVER_HOST}" "sudo mv '/tmp/${WGT_NAME}' '${UPLOAD_DIR}/${WGT_NAME}' && sudo chmod 644 '${UPLOAD_DIR}/${WGT_NAME}'"
fi

scp "${SSH_OPTS[@]}" "${TMP_JSON}" "${SERVER_USER}@${SERVER_HOST}:/tmp/android.json"
ssh "${SSH_OPTS[@]}" "${SERVER_USER}@${SERVER_HOST}" "sudo mv /tmp/android.json '${METADATA_DIR}/android.json' && sudo chmod 644 '${METADATA_DIR}/android.json'"

rm -f "${TMP_JSON}"
echo "App update metadata published to ${SERVER_HOST}:${METADATA_DIR}/android.json"
