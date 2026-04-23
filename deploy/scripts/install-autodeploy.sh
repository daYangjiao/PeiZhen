#!/usr/bin/env bash
set -euo pipefail

if [[ "${EUID}" -ne 0 ]]; then
  echo "Run as root: sudo bash deploy/scripts/install-autodeploy.sh" >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

OPS_USER="${OPS_USER:-ops}"
AUTODEPLOY_ROOT="${AUTODEPLOY_ROOT:-/opt/pz-autodeploy}"
ENV_DIR="${ENV_DIR:-/etc/pz-autodeploy}"
ENV_FILE="${ENV_FILE:-${ENV_DIR}/autodeploy.env}"
SERVICE_FILE="/etc/systemd/system/pz-autodeploy-webhook.service"
POLL_SERVICE_FILE="/etc/systemd/system/pz-autodeploy-poll.service"
POLL_TIMER_FILE="/etc/systemd/system/pz-autodeploy-poll.timer"
NGINX_SITE="/etc/nginx/sites-available/pz-mini.conf"

install_node20() {
  if command -v node >/dev/null 2>&1 && node -v | grep -q '^v20\.'; then
    return
  fi
  apt-get update
  apt-get install -y ca-certificates curl gnupg
  curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
  apt-get install -y nodejs
}

install_hbuilderx() {
  # shellcheck disable=SC1090
  source "${ENV_FILE}"
  HBUILDERX_DIR="${HBUILDERX_DIR:-/opt/HBuilderX}"
  HBUILDERX_CLI="${HBUILDERX_CLI:-/opt/HBuilderX/cli}"
  HBUILDERX_DOWNLOAD_URL="${HBUILDERX_DOWNLOAD_URL:-}"

  if [[ -x "${HBUILDERX_CLI}" ]]; then
    return
  fi
  if [[ -z "${HBUILDERX_DOWNLOAD_URL}" ]]; then
    HBUILDERX_DOWNLOAD_URL="$(
      python3 <<'PY'
import json
import urllib.request

with urllib.request.urlopen("https://download1.dcloud.net.cn/hbuilderx/release.json", timeout=20) as response:
    payload = json.load(response)

for item in payload.get("files", []):
    if item.get("code") == "linux_full_x64":
        print(item.get("path", ""))
        break
PY
    )"
  fi
  if [[ -z "${HBUILDERX_DOWNLOAD_URL}" ]]; then
    echo "HBuilderX CLI not installed and no Linux download URL could be resolved." >&2
    echo "Edit ${ENV_FILE}, set HBUILDERX_DOWNLOAD_URL, then rerun this installer." >&2
    return
  fi

  local tmp_dir archive extracted cli_path
  tmp_dir="$(mktemp -d)"
  archive="${tmp_dir}/hbuilderx.tar.gz"
  curl -fL "${HBUILDERX_DOWNLOAD_URL}" -o "${archive}"
  tar -xzf "${archive}" -C "${tmp_dir}"
  extracted="$(find "${tmp_dir}" -mindepth 1 -maxdepth 1 -type d | head -n 1)"
  if [[ -z "${extracted}" ]]; then
    echo "Could not find extracted HBuilderX directory." >&2
    exit 1
  fi
  rm -rf "${HBUILDERX_DIR}"
  mkdir -p "$(dirname "${HBUILDERX_DIR}")"
  cp -a "${extracted}" "${HBUILDERX_DIR}"
  cli_path="$(find "${HBUILDERX_DIR}" -type f -name cli | head -n 1)"
  if [[ -n "${cli_path}" && "${cli_path}" != "${HBUILDERX_CLI}" ]]; then
    ln -sf "${cli_path}" "${HBUILDERX_CLI}"
  fi
  chmod +x "${HBUILDERX_CLI}" 2>/dev/null || true
  rm -rf "${tmp_dir}"
}

install -d -o "${OPS_USER}" -g "${OPS_USER}" "${AUTODEPLOY_ROOT}"
install -d -o "${OPS_USER}" -g "${OPS_USER}" "${AUTODEPLOY_ROOT}/bin"
install -d -o "${OPS_USER}" -g "${OPS_USER}" "${AUTODEPLOY_ROOT}/webhook"
install -d -o "${OPS_USER}" -g "${OPS_USER}" "${AUTODEPLOY_ROOT}/poll"
install -d -o "${OPS_USER}" -g "${OPS_USER}" "${AUTODEPLOY_ROOT}/source"
install -d -o "${OPS_USER}" -g "${OPS_USER}" "${AUTODEPLOY_ROOT}/wgt-release"
install -d -o "${OPS_USER}" -g "${OPS_USER}" /var/log/pz-autodeploy

install -m 0755 "${PROJECT_ROOT}/deploy/scripts/autodeploy-run.sh" "${AUTODEPLOY_ROOT}/bin/autodeploy-run.sh"
install -m 0644 "${PROJECT_ROOT}/deploy/autodeploy/webhook_server.py" "${AUTODEPLOY_ROOT}/webhook/webhook_server.py"
install -m 0755 "${PROJECT_ROOT}/deploy/autodeploy/poll_deploy.py" "${AUTODEPLOY_ROOT}/poll/poll_deploy.py"

install -d -o "${OPS_USER}" -g "${OPS_USER}" "${ENV_DIR}"
if [[ ! -f "${ENV_FILE}" ]]; then
  token="$(openssl rand -hex 32)"
  install -o "${OPS_USER}" -g "${OPS_USER}" -m 0600 "${PROJECT_ROOT}/deploy/env/pz-autodeploy.env.example" "${ENV_FILE}"
  sed -i "s/^GITEE_WEBHOOK_TOKEN=.*/GITEE_WEBHOOK_TOKEN=${token}/" "${ENV_FILE}"
  echo "Created ${ENV_FILE} with generated GITEE_WEBHOOK_TOKEN."
else
  chown "${OPS_USER}:${OPS_USER}" "${ENV_FILE}"
  chmod 0600 "${ENV_FILE}"
fi

install_node20
install_hbuilderx

install -m 0644 "${PROJECT_ROOT}/deploy/systemd/pz-autodeploy-webhook.service" "${SERVICE_FILE}"
install -m 0644 "${PROJECT_ROOT}/deploy/systemd/pz-autodeploy-poll.service" "${POLL_SERVICE_FILE}"
install -m 0644 "${PROJECT_ROOT}/deploy/systemd/pz-autodeploy-poll.timer" "${POLL_TIMER_FILE}"
install -m 0644 "${PROJECT_ROOT}/deploy/nginx/pz-mini.conf" "${NGINX_SITE}"
ln -sf "${NGINX_SITE}" /etc/nginx/sites-enabled/pz-mini.conf
rm -f /etc/nginx/sites-enabled/default

nginx -t
systemctl reload nginx
systemctl daemon-reload
systemctl enable --now pz-autodeploy-webhook.service
systemctl restart pz-autodeploy-webhook.service
systemctl enable --now pz-autodeploy-poll.timer

echo "Autodeploy webhook installed."
echo "Gitee WebHook URL: http://<server>/gitee-webhook"
echo "Token is in ${ENV_FILE}."
echo "Autodeploy poll timer installed: pz-autodeploy-poll.timer"
