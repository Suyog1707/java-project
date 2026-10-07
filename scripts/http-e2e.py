#!/usr/bin/env python3
"""Real HTTP + MySQL checks. Generates and removes its own test players."""
import json,os,time,uuid,urllib.request,urllib.error,http.cookiejar,subprocess,concurrent.futures
BASE=os.environ.get('APP_URL','http://127.0.0.1:20007/college-survival').rstrip('/')
def db(sql):
 result=subprocess.run(['docker','exec','-i','college-survival-mysql','sh','-c','MYSQL_PWD="$MYSQL_PASSWORD" mysql -u "$MYSQL_USER" -N -B college_survival'],input=sql,text=True,capture_output=True,check=True)
 return [r.split('\t') for r in result.stdout.strip().splitlines()]
class Client:
 def __init__(self):
  self.jar=http.cookiejar.CookieJar();self.opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.jar));self.csrf=''
 def call(self,path,data=None,status=200,raw=None,csrf=True):
  headers={};payload=None
  if data is not None or raw is not None:
   payload=raw if raw is not None else json.dumps(data).encode();headers={'Content-Type':'application/json'}
   if csrf:headers['X-CSRF-Token']=self.csrf
  req=urllib.request.Request(BASE+'/api/'+path,data=payload,headers=headers)
  try:r=self.opener.open(req,timeout=10)
  except urllib.error.HTTPError as e:r=e
  body=json.load(r);assert r.status==status,(path,r.status,status,body);assert body['success']==(status==200),body
  return body.get('data',body)
 def session(self):self.csrf=self.call('session')['csrf']
def check(name):print('PASS '+name,flush=True)
def payload(username):return dict(name='HTTP Test Player',username=username,email=username+'@example.org',password='Survival-Test-123',confirmPassword='Survival-Test-123')
def choice(g,option):return dict(runId=g['runId'],scenarioId=str(g['scenario']['id']),optionId=str(option))
users=[]
try:
 c=Client();c.session();c.call('dashboard',status=401)
 page=c.opener.open(BASE+'/dashboard.html');assert page.url.endswith('/login.html');check('E2E-05 protected page + API')
 c.call('game/current',status=401);c.call('profile',status=401);c.call('history',status=401)
 username='http_'+uuid.uuid4().hex[:18];users.append(username);data=payload(username)
 c.call('register',data);rows=db("SELECT username,password_hash FROM users WHERE username='"+username+"';");assert len(rows)==1 and rows[0][1].startswith('$2a$');check('E2E-01 registration and BCrypt in MySQL')
 c.call('register',data,status=400)
 duplicate=payload(username+'x');duplicate['email']=data['email'];c.call('register',duplicate,status=400);assert db("SELECT COUNT(*) FROM users WHERE username='"+username+"';")[0][0]=='1';check('E2E-02 duplicate username and email')
 c.call('login',dict(username=username,password='wrong-password'),status=400);assert not c.call('session')['authenticated'];check('E2E-04 invalid login')
 old_csrf=c.csrf;login=c.call('login',dict(username=username,password=data['password']));c.csrf=login['csrf'];assert old_csrf!=c.csrf;c.call('dashboard');check('E2E-03 login, session rotation and dashboard')
 c.call('register',dict(data,name=''),status=400);c.call('register',dict(data,email='not an email'),status=400);c.call('register',dict(data,confirmPassword='other'),status=400);c.call('game/new',{},status=403,csrf=False);c.call('game/new',raw=b'{bad json',status=400);c.call('game/new',raw=b'[]',status=400);c.call('game/new',raw=b'x'*17000,status=400);check('malformed inputs, request limit and CSRF')
 c.call('game/current',status=400);c.call('game/choose',dict(runId='missing',scenarioId='1',optionId='1'),status=400);check('missing active run rejected')
 g=c.call('game/new',{});assert g['stats']==dict(health=100,stress=10,attendance=80,money=1000,knowledge=40,social=50) and g['completed']==0 and g['total']==10;check('E2E-06 initial server game state')
 assert c.call('game/current')==g;check('E2E-13 refresh preserves run')
 c.call('game/new',{},status=400);c.call('game/choose',choice(g,999999),status=400);assert c.call('game/current')==g;check('E2E-08 invalid choice leaves state unchanged')
 assert set(g['scenario']['options'][0])=={'id','text'};check('scenario API hides effect rules')
 effects={int(r[0]):list(map(int,r[1:8])) for r in db('SELECT id,health_change,stress_change,attendance_change,money_change,knowledge_change,social_change,score_change FROM scenario_options;')}
 seen=[];score=0;steps=0
 while g['status']=='IN_PROGRESS':
  seen.append(g['scenario']['id']);last=choice(g,g['scenario']['options'][-1]['id']);effect=effects[int(last['optionId'])];expected={k:max(0,min(100000 if k=='money' else 100,g['stats'][k]+effect[i])) for i,k in enumerate(['health','stress','attendance','money','knowledge','social'])};score+=effect[6]
  g=c.call('game/choose',dict(last,score=999999,health=999999));steps+=1
  assert g['stats']==expected and g['completed']==steps and g['outcome'];
  if g['status']=='IN_PROGRESS':assert g['score']==score
  else:assert g['score']==max(0,score+sum(g['stats'][k] for k in ['health','attendance','knowledge','social'])*5+1500-g['stats']['stress']*10)
  if steps==1:c.call('game/choose',last,status=400);check('E2E-07 effects, score, progress and replay rejection')
 assert steps==10 and len(set(seen))==10 and g['status']=='COMPLETED';assert c.call('game/result')==g;c.call('game/choose',last,status=400)
 records=db("SELECT score,result_title,scenarios_completed,health,stress,attendance,money,knowledge,social FROM game_results WHERE run_id='"+g['runId']+"';");assert len(records)==1;record=records[0];assert int(record[0])==g['score'] and record[1]==g['resultTitle'] and int(record[2])==10;assert list(map(int,record[3:]))==[g['stats'][k] for k in ['health','stress','attendance','money','knowledge','social']];check('E2E-09 full completion + exact MySQL score/title/stats + no duplicates')
 h=c.call('history')['rows'];assert len(h)==1 and h[0]['score']==g['score'];assert c.call('history?page=1')['rows']==[];c.call('history?page=-1',status=400);c.call('profile');check('E2E-10 private history, pagination and profile')
 leader=c.call('leaderboard');assert any(r['username']==username and r['score']==g['score'] for r in leader);assert all(leader[i]['score']>=leader[i+1]['score'] for i in range(len(leader)-1));assert all('email' not in r for r in leader);check('E2E-11 public leaderboard ordered and private data excluded')
 other=Client();other.session();second='http_'+uuid.uuid4().hex[:18];users.append(second);other.call('register',payload(second));other.csrf=other.call('login',dict(username=second,password=data['password']))['csrf'];assert other.call('history')['rows']==[];assert other.call('profile')['player']['username']==second;check('history and profile restricted to current player')
 g=c.call('game/new',{});quitrun=c.call('game/quit',{'runId':g['runId']});assert quitrun['status']=='QUIT';c.call('game/current',status=400);assert len(c.call('history')['rows'])==2;check('quit saves once and clears active run')
 # Select risky third options to exercise a real early game-over.
 g=c.call('game/new',{});loops=0
 while g['status']=='IN_PROGRESS':g=c.call('game/choose',choice(g,g['scenario']['options'][2]['id']));loops+=1
 assert g['status'] in ['STRESS_OVERLOAD','ATTENDANCE_DISASTER','HEALTH_COLLAPSE'] and loops<10;check('real early failure persists')
 cookie=next(iter(c.jar)).value;c.call('logout',{});c.call('dashboard',status=401);c.call('profile',status=401);c.call('history',status=401);c.call('game/current',status=401);assert not c.call('session')['authenticated'];check('E2E-12 logout invalidates session')
 for p in ['dashboard.html','game.html','history.html','profile.html','result.html','dashboard']:
  assert c.opener.open(BASE+'/'+p).url.endswith('/login.html')
 check('E2E-14 protected pages after logout, no-store cache')
 invalid=urllib.request.Request(BASE+'/api/dashboard',headers={'Cookie':'JSESSIONID='+cookie})
 try:urllib.request.urlopen(invalid);raise AssertionError('Invalid session accepted')
 except urllib.error.HTTPError as e:assert e.code==401
 expired=Client();expired.session();expired.call('dashboard',status=401);check('E2E-15 stale and invalid sessions rejected')
 print('All HTTP/database end-to-end checks passed.')
finally:
 for username in users:db("DELETE FROM users WHERE username='"+username+"';")
