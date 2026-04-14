#!/usr/bin/env bash
set -euo pipefail

if [[ "${EUID}" -ne 0 ]]; then
  echo "Run this script as root or via sudo." >&2
  exit 1
fi

OPS_USER="${OPS_USER:-ops}"
LOG_GROUP="${LOG_GROUP:-pzlog}"
APP_ROOT="${APP_ROOT:-/opt/pz-app}"
LOG_DIR="${LOG_DIR:-${APP_ROOT}/logs}"
LOG_FILE="${LOG_FILE:-${LOG_DIR}/application.log}"
ENV_DIR="${ENV_DIR:-/etc/pz-app}"
ENV_FILE="${ENV_FILE:-${ENV_DIR}/pz-app.env}"

if ! id -u "${OPS_USER}" >/dev/null 2>&1; then
  echo "User does not exist: ${OPS_USER}" >&2
  exit 1
fi

if ! getent group "${LOG_GROUP}" >/dev/null 2>&1; then
  groupadd --system "${LOG_GROUP}"
fi

usermod -aG "${LOG_GROUP}" "${OPS_USER}"

install -d -m 710 -o pzapp -g "${LOG_GROUP}" "${APP_ROOT}"
install -d -m 750 -o pzapp -g "${LOG_GROUP}" "${LOG_DIR}"

if [[ -f "${LOG_FILE}" ]]; then
  chown pzapp:"${LOG_GROUP}" "${LOG_FILE}"
  chmod 640 "${LOG_FILE}"
fi

if [[ -d "${ENV_DIR}" ]]; then
  chown root:root "${ENV_DIR}"
  chmod 700 "${ENV_DIR}"
fi

if [[ -f "${ENV_FILE}" ]]; then
  chown root:root "${ENV_FILE}"
  chmod 600 "${ENV_FILE}"
fi

echo "Granted direct log access to ${OPS_USER} via group ${LOG_GROUP}."
echo "Protected env files under ${ENV_DIR}."
echo "Re-login as ${OPS_USER} for refreshed group membership."
