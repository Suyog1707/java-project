#!/usr/bin/env bash
export PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ -f "$PROJECT_DIR/.env" ]]; then set -a; source "$PROJECT_DIR/.env"; set +a; fi
if [[ -x "$PROJECT_DIR/.tools/jdk/bin/javac" ]]; then export JAVA_HOME="$PROJECT_DIR/.tools/jdk"; export PATH="$JAVA_HOME/bin:$PATH"; fi
if [[ -x "$PROJECT_DIR/.tools/maven/bin/mvn" ]]; then export PATH="$PROJECT_DIR/.tools/maven/bin:$PATH"; fi

export CATALINA_HOME="${CATALINA_HOME:-$PROJECT_DIR/.tools/tomcat}"
export CATALINA_BASE="$CATALINA_HOME"
export CATALINA_PID="$PROJECT_DIR/.runtime/tomcat.pid"
export CATALINA_OPTS="${CATALINA_OPTS:-} -Duser.timezone=UTC"
