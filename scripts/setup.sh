#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p .tools .runtime
if [[ ! -f .env ]]; then
 python3 - <<'PY'
from pathlib import Path
import secrets,os
text=Path('.env.example').read_text().replace('replace_with_local_password',secrets.token_hex(24)).replace('replace_with_another_local_password',secrets.token_hex(24))
Path('.env').write_text(text);os.chmod('.env',0o600)
PY
fi
if ! command -v javac >/dev/null && [[ ! -x .tools/jdk/bin/javac ]]; then
 curl -fL --retry 3 https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse -o .tools/jdk.tar.gz
 mkdir -p .tools/jdk; tar -xzf .tools/jdk.tar.gz -C .tools/jdk --strip-components=1
fi
if ! command -v mvn >/dev/null && [[ ! -x .tools/maven/bin/mvn ]]; then
 curl -fL --retry 3 https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.tar.gz -o .tools/maven.tar.gz
 mkdir -p .tools/maven; tar -xzf .tools/maven.tar.gz -C .tools/maven --strip-components=1
fi
if [[ ! -x "${CATALINA_HOME:-.tools/tomcat}/bin/catalina.sh" ]]; then
 curl -fL --retry 3 https://archive.apache.org/dist/tomcat/tomcat-10/v10.1.60/bin/apache-tomcat-10.1.60.tar.gz -o .tools/tomcat-current.tar.gz
 mkdir -p .tools/tomcat; tar -xzf .tools/tomcat-current.tar.gz -C .tools/tomcat --strip-components=1
fi
source scripts/env.sh
java -version
mvn -version
echo 'Setup ready. Run ./scripts/start.sh'
