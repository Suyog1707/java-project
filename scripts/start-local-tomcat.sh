#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
cd "$PROJECT_DIR"
docker compose up -d --wait db
mvn -B clean package
cp target/college-survival.war "$CATALINA_HOME/webapps/college-survival.war"
exec "$CATALINA_HOME/bin/catalina.sh" run
