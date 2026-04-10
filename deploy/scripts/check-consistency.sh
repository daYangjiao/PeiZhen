#!/usr/bin/env bash
set -euo pipefail

SERVER_HOST="${SERVER_HOST:-101.245.94.141}"
SERVER_USER="${SERVER_USER:-ops}"
SSH_KEY="${SSH_KEY:-$HOME/.ssh/pz_ops_ed25519}"
LOCAL_MYSQL="${LOCAL_MYSQL:-/usr/local/mysql/bin/mysql}"
LOCAL_MYSQL_SOCKET="${LOCAL_MYSQL_SOCKET:-/tmp/mysql.sock}"
LOCAL_DB_USER="${LOCAL_DB_USER:-root}"
LOCAL_DB_PASSWORD="${LOCAL_DB_PASSWORD:-123456}"
LOCAL_DB_NAME="${LOCAL_DB_NAME:-student}"

run_local_counts() {
  "${LOCAL_MYSQL}" \
    --socket="${LOCAL_MYSQL_SOCKET}" \
    -u"${LOCAL_DB_USER}" \
    -p"${LOCAL_DB_PASSWORD}" \
    -D "${LOCAL_DB_NAME}" \
    -N <<'SQL'
SELECT 'user', COUNT(*) FROM user
UNION ALL SELECT 'attendant', COUNT(*) FROM attendant
UNION ALL SELECT 'attendant_qualification', COUNT(*) FROM attendant_qualification
UNION ALL SELECT 'service_type_mapping', COUNT(*) FROM service_type_mapping
UNION ALL SELECT 'order', COUNT(*) FROM `order`
UNION ALL SELECT 'order_evaluation', COUNT(*) FROM order_evaluation
UNION ALL SELECT 'chat_message', COUNT(*) FROM chat_message
UNION ALL SELECT 'guide_appointment', COUNT(*) FROM guide_appointment
UNION ALL SELECT 'ai_medical_qa', COUNT(*) FROM ai_medical_qa;
SQL
}

run_remote_report() {
  ssh -i "${SSH_KEY}" "${SERVER_USER}@${SERVER_HOST}" <<'EOF'
set -euo pipefail
echo "SERVER_OLD_FRONTEND_DIR=$(if [ -d /home/ops/PZ_yuanbao/frontend/user-mini-program ]; then echo exists; else echo removed; fi)"
echo "--- backend release ---"
cat /var/lib/pz-deploy/releases/backend-release.env
echo "--- frontend release ---"
cat /var/lib/pz-deploy/releases/frontend-release.env
echo "--- remote db counts ---"
mysql -u pzapp -p'JRFW232tuvmBkXl3p49r' -D student -N <<'SQL'
SELECT 'user', COUNT(*) FROM user
UNION ALL SELECT 'attendant', COUNT(*) FROM attendant
UNION ALL SELECT 'attendant_qualification', COUNT(*) FROM attendant_qualification
UNION ALL SELECT 'service_type_mapping', COUNT(*) FROM service_type_mapping
UNION ALL SELECT 'order', COUNT(*) FROM `order`
UNION ALL SELECT 'order_evaluation', COUNT(*) FROM order_evaluation
UNION ALL SELECT 'chat_message', COUNT(*) FROM chat_message
UNION ALL SELECT 'guide_appointment', COUNT(*) FROM guide_appointment
UNION ALL SELECT 'ai_medical_qa', COUNT(*) FROM ai_medical_qa;
SQL
EOF
}

echo "LOCAL_GIT_HEAD=$(git rev-parse HEAD)"
echo "REMOTE_FF_HEAD=$(git rev-parse origin/FF)"
echo "--- local db counts ---"
run_local_counts
echo "--- remote report ---"
run_remote_report
