#!/usr/bin/env bash
# Project-local Maven launcher. Uses tools prepared by scripts/setup.sh.
set -euo pipefail
source "$(dirname "$0")/scripts/env.sh"
cd "$PROJECT_DIR"
exec mvn "$@"
