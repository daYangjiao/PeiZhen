#!/usr/bin/env bash
set -euo pipefail

if [[ "${EUID}" -ne 0 ]]; then
  echo "Run this script as root or via sudo." >&2
  exit 1
fi

OPS_USER="${OPS_USER:-ops}"
OPS_HOME="${OPS_HOME:-/home/${OPS_USER}}"
PROJECT_SOURCE="${PROJECT_SOURCE:-/root/PZ_yuanbao}"
PROJECT_TARGET="${PROJECT_TARGET:-${OPS_HOME}/PZ_yuanbao}"
APPLY_SSH_HARDENING="${APPLY_SSH_HARDENING:-0}"
SSH_HARDENING_FILE="/etc/ssh/sshd_config.d/99-${OPS_USER}-hardening.conf"

OPS_PUBLIC_KEY="${OPS_PUBLIC_KEY:-}"
if [[ -z "${OPS_PUBLIC_KEY}" && $# -ge 1 ]]; then
  if [[ -f "$1" ]]; then
    OPS_PUBLIC_KEY="$(<"$1")"
  else
    OPS_PUBLIC_KEY="$1"
  fi
fi

if [[ -z "${OPS_PUBLIC_KEY}" ]]; then
  echo "Set OPS_PUBLIC_KEY or pass a public key string/file as the first argument." >&2
  exit 1
fi

if ! id -u "${OPS_USER}" >/dev/null 2>&1; then
  adduser --disabled-password --gecos "" "${OPS_USER}"
fi

usermod -aG sudo "${OPS_USER}"

install -d -m 700 -o "${OPS_USER}" -g "${OPS_USER}" "${OPS_HOME}/.ssh"
touch "${OPS_HOME}/.ssh/authorized_keys"
chown "${OPS_USER}:${OPS_USER}" "${OPS_HOME}/.ssh/authorized_keys"
chmod 600 "${OPS_HOME}/.ssh/authorized_keys"

if ! grep -qxF "${OPS_PUBLIC_KEY}" "${OPS_HOME}/.ssh/authorized_keys"; then
  printf '%s\n' "${OPS_PUBLIC_KEY}" >> "${OPS_HOME}/.ssh/authorized_keys"
fi

cat >"/etc/sudoers.d/90-${OPS_USER}-nopasswd" <<EOF
${OPS_USER} ALL=(ALL) NOPASSWD:ALL
EOF
chmod 440 "/etc/sudoers.d/90-${OPS_USER}-nopasswd"

if [[ -d "${PROJECT_SOURCE}" ]]; then
  install -d -o "${OPS_USER}" -g "${OPS_USER}" "${PROJECT_TARGET}"
  rsync -a \
    --exclude '.git/' \
    --exclude 'frontend/mini-program/node_modules/' \
    --exclude 'frontend/mini-program/dist/' \
    "${PROJECT_SOURCE}/" "${PROJECT_TARGET}/"
  chown -R "${OPS_USER}:${OPS_USER}" "${PROJECT_TARGET}"
fi

if [[ "${APPLY_SSH_HARDENING}" == "1" ]]; then
  cat >"${SSH_HARDENING_FILE}" <<EOF
PermitRootLogin no
PasswordAuthentication no
PubkeyAuthentication yes
EOF

  sshd -t
  systemctl reload ssh
fi

echo "Ops user ready: ${OPS_USER}"
echo "Verify from macOS:"
echo "  ssh -i ~/.ssh/pz_ops_ed25519 ${OPS_USER}@101.245.94.141"
echo "  ssh -i ~/.ssh/pz_ops_ed25519 ${OPS_USER}@101.245.94.141 'sudo -n true && id'"

if [[ "${APPLY_SSH_HARDENING}" == "1" ]]; then
  echo "SSH hardening applied."
  echo "Verify root/password login is blocked after confirming the ops login still works."
else
  echo "SSH hardening not applied yet. Re-run with APPLY_SSH_HARDENING=1 after verifying the ops login."
fi
