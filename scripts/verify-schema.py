#!/usr/bin/env python3
"""Verify schema + seed in a unique temporary MySQL database, then remove it."""
from pathlib import Path
import subprocess,uuid
name='college_verify_'+uuid.uuid4().hex[:12]
def run(sql):
 return subprocess.run(['docker','exec','-i','college-survival-mysql','sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --default-character-set=utf8mb4 -u root -N -B'],input=sql,text=True,capture_output=True,check=True).stdout.strip()
try:
 schema=Path('database/schema.sql').read_text().replace('college_survival',name)
 seed=Path('database/seed.sql').read_text().replace('college_survival',name)
 run(schema+seed)
 assert run('USE '+name+'; SELECT COUNT(*) FROM scenarios; SELECT COUNT(*) FROM scenario_options;')=='30\n120'
 assert '’' in run('USE '+name+'; SELECT description FROM scenarios WHERE id=5;')
 # Second import must remain idempotent.
 run(schema+seed)
 assert run('USE '+name+'; SELECT COUNT(*) FROM scenarios; SELECT COUNT(*) FROM scenario_options;')=='30\n120'
 print('PASS fresh MySQL schema/seed import, UTF-8, and repeated import (30 scenarios / 120 options)')
finally:
 run('DROP DATABASE IF EXISTS '+name+';')
