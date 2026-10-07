#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
cd "$PROJECT_DIR"
[[ -f .env ]] || { echo 'Run scripts/setup.sh first.'; exit 1; }
docker compose up -d --wait db
mvn -B clean package
docker compose up -d --force-recreate --wait web
for attempt in {1..40}; do
 if curl --fail --silent http://127.0.0.1:20007/college-survival/api/session >/dev/null; then
  echo 'Ready: http://localhost:20007/college-survival/'
  exit 0
 fi
 sleep 1
done
echo 'Tomcat did not become ready. Check: docker compose logs web'
exit 1
