#!/usr/bin/env python3
"""Temporarily stops only the project DB to verify safe 503 handling. Run when idle."""
import subprocess,urllib.request,urllib.error,json,os,time
base=os.environ.get('APP_URL','http://127.0.0.1:20007/college-survival').rstrip('/')
try:
 subprocess.run(['docker','compose','stop','db'],check=True,capture_output=True)
 try:
  urllib.request.urlopen(base+'/api/leaderboard',timeout=15)
  raise AssertionError('Expected unavailable database response')
 except urllib.error.HTTPError as e:
  assert e.code==503
  body=json.load(e);assert body['success'] is False
  assert 'try again' in body['message'].lower()
  assert all(word not in json.dumps(body) for word in ['SQLException','Exception','DB_PASSWORD','jdbc:mysql'])
 print('PASS database outage returns friendly 503 without internal error details')
finally:
 subprocess.run(['docker','compose','up','-d','--wait','db'],check=True)
for attempt in range(40):
 try:
  with urllib.request.urlopen(base+'/api/leaderboard',timeout=15) as r:
   assert r.status==200 and json.load(r)['success']
  break
 except urllib.error.HTTPError as e:
  if e.code!=503:raise
  time.sleep(1)
else:
 raise AssertionError('Database did not recover within readiness window')
print('PASS database recovery restores normal API response')
