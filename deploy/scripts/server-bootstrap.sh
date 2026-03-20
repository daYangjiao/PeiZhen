#!/usr/bin/env bash
set -euo pipefail

APP_USER="${APP_USER:-pzapp}"
APP_HOME="${APP_HOME:-/opt/pz-app}"
WEB_ROOT="${WEB_ROOT:-/var/www/pz-mini}"
ENV_DIR="${ENV_DIR:-/etc/pz-app}"

apt-get update
apt-get install -y openjdk-17-jdk maven mysql-server nginx

if ! id -u "${APP_USER}" >/dev/null 2>&1; then
  useradd --system --create-home --home-dir "${APP_HOME}" --shell /usr/sbin/nologin "${APP_USER}"
fi

install -d -o "${APP_USER}" -g "${APP_USER}" "${APP_HOME}/app"
install -d -o "${APP_USER}" -g "${APP_USER}" "${APP_HOME}/uploads"
install -d -o "${APP_USER}" -g "${APP_USER}" "${APP_HOME}/logs"
install -d -o www-data -g www-data "${WEB_ROOT}"
install -d "${ENV_DIR}"

systemctl enable mysql
systemctl enable nginx

echo "Bootstrap complete."
echo "Next: copy deploy/env/pz-app.env.example to ${ENV_DIR}/pz-app.env and edit DB_PASSWORD."
