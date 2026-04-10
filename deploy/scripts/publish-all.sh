#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Publishing backend..."
bash "${SCRIPT_DIR}/publish-backend.sh"

echo "Publishing frontend..."
bash "${SCRIPT_DIR}/publish-frontend.sh"

echo "Publish complete."
