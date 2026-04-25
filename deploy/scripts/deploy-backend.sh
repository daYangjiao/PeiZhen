#!/usr/bin/env bash
set -euo pipefail

APP_HOME="${APP_HOME:-/opt/pz-app}"
SERVICE_NAME="${SERVICE_NAME:-pz-app}"
SERVICE_FILE="/etc/systemd/system/${SERVICE_NAME}.service"
ENV_FILE="${ENV_FILE:-/etc/pz-app/pz-app.env}"
RELEASE_META_DIR="${RELEASE_META_DIR:-/var/lib/pz-deploy/releases}"
RELEASE_META_FILE="${RELEASE_META_DIR}/backend-release.env"

if [[ "${BACKEND_SKIP_BUILD:-0}" != "1" ]]; then
  mvn -DskipTests clean package
fi

JAR_FILE="${BACKEND_JAR_FILE:-}"
if [[ -z "${JAR_FILE}" ]]; then
  JAR_FILE="$(find target -maxdepth 1 -type f -name '*.jar' ! -name 'original-*.jar' | head -n 1)"
fi
if [[ -z "${JAR_FILE}" ]]; then
  echo "No runnable jar found in target/." >&2
  exit 1
fi

install -d "${APP_HOME}/app"
install -d "${APP_HOME}/logs"
install -d "${APP_HOME}/uploads"
install -m 0644 "${JAR_FILE}" "${APP_HOME}/app/pz-app.jar"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing ${ENV_FILE}. Copy deploy/env/pz-app.env.example first." >&2
  exit 1
fi

if ! grep -q '^JWT_SECRET=' "${ENV_FILE}"; then
  echo "Warning: JWT_SECRET is not set in ${ENV_FILE}. Production should provide a stable secret." >&2
fi

set -a
# shellcheck disable=SC1090
source "${ENV_FILE}"
set +a

DB_NAME="$(
  printf '%s' "${DB_URL:-}" \
    | sed -nE 's#^jdbc:mysql://[^/]+/([^?]+).*$#\1#p'
)"
DB_NAME="${DB_NAME:-student}"
MYSQL_ARGS=(-u"${DB_USERNAME:-root}")
if [[ -n "${DB_PASSWORD:-}" ]]; then
  MYSQL_ARGS+=(-p"${DB_PASSWORD}")
fi
if [[ -f database/sys_admin_role.sql ]]; then
  mysql "${MYSQL_ARGS[@]}" "${DB_NAME}" < database/sys_admin_role.sql
fi
if compgen -G "db/*.sql" >/dev/null; then
  for migration_file in $(find db -maxdepth 1 -type f -name '*.sql' | sort); do
    echo "Applying database migration ${migration_file}"
    mysql "${MYSQL_ARGS[@]}" "${DB_NAME}" < "${migration_file}"
  done
fi

install -m 0644 deploy/systemd/pz-app.service "${SERVICE_FILE}"
systemctl daemon-reload
systemctl enable --now "${SERVICE_NAME}"
systemctl restart "${SERVICE_NAME}"

install -d "${RELEASE_META_DIR}"
JAR_SHA256="$(sha256sum "${APP_HOME}/app/pz-app.jar" | awk '{print $1}')"
cat > "${RELEASE_META_FILE}" <<EOF
RELEASE_COMPONENT=backend
RELEASE_GIT_COMMIT=${RELEASE_GIT_COMMIT:-unknown}
RELEASE_GIT_SHORT_COMMIT=${RELEASE_GIT_SHORT_COMMIT:-unknown}
RELEASE_GIT_BRANCH=${RELEASE_GIT_BRANCH:-unknown}
RELEASE_GIT_REF=${RELEASE_GIT_REF:-unknown}
RELEASE_GIT_DIRTY=${RELEASE_GIT_DIRTY:-0}
RELEASE_TIMESTAMP=${RELEASE_TIMESTAMP:-unknown}
RELEASE_ACTOR=${RELEASE_ACTOR:-unknown}
DEPLOYED_AT=$(date '+%Y-%m-%dT%H:%M:%S%z')
DEPLOYED_JAR_SHA256=${JAR_SHA256}
EOF

echo "Backend deployed. Check status with: systemctl status ${SERVICE_NAME}"
