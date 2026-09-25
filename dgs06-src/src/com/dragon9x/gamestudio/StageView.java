package com.dragon9x.gamestudio;

import android.content.Context;

import android.graphics.*;

import android.view.*;

import java.util.*;

/** Same canvas for map placement and actual playable local preview. */
final class StageView extends View {

 static final class Enemy {
float x,y;
int hp;
Enemy(float x,float y,int hp){
this.x=x;
this.y=y;
this.hp=hp;
}
}

 final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
final ArrayList<Enemy> enemies=new ArrayList<>();

 GameProject project;
List<GameRules.Rule> rules=Collections.emptyList();
Bitmap background,playerImage,enemyImage,tileImage,effectImage;
Runnable onChange;
float effectX,effectY;

 float cameraX,cameraY;
int tileBrush=1,lastPaintIndex=-1,brushColumns=1;
int[] brushIds;

 boolean playing;
int editTool,health,maxHealth,mp,maxMp,damage,speed,gravity,flyCost,gold,exp,level=1;

 int attackBonus,attackMultiplier=100,critChance,critDamage=150,lifesteal,defense,damageReduction;
boolean lastCrit,flying,deadEventSent,mpEmptySent;

 float playerX,playerY,axisX,axisY,airOffset,verticalSpeed;
long lastFrame,lastAttack,lastEnemyAttack,lastTick,lastMoveEvent,lastMpDrain;
String message="";
long messageUntil;

 final HashMap<String,Integer> vars=new HashMap<>(),items=new HashMap<>();
final HashMap<String,Long> statuses=new HashMap<>();
final Random rng=new Random();
int eventDepth;

 StageView(Context c){
super(c);
paint.setFilterBitmap(false);
setBackgroundColor(Color.rgb(20,30,39));
}

 void bind(GameProject p,Bitmap bg,Bitmap player,Bitmap enemy,Bitmap atlas,Bitmap effect,Runnable change){
project=p;
background=bg;
playerImage=player;
enemyImage=enemy;
tileImage=atlas;
effectImage=effect;
onChange=change;
cameraX=cameraY=0;
stop();
invalidate();
}

 void pan(int dx,int dy){
if(project==null)return;
cameraX=Math.max(0,Math.min(project.width-400,cameraX+dx*16));
cameraY=Math.max(0,Math.min(project.height-240,cameraY+dy*16));
invalidate();
}

 void start(){
if(project==null)return;
project.validate();
ArrayList<GameRules.Rule> all=new ArrayList<>(GameRules.parse(project.rules));
for(GameModule module:project.modules)for(GameRules.Rule rule:module.rules())if(!rule.event.equals("on_menu"))all.add(rule);
rules=all;
playing=true;
maxHealth=project.maxHp;
health=maxHealth;
maxMp=project.maxMp;
mp=maxMp;
damage=project.attackDamage;
speed=project.speed;
gravity=project.gravity;
flyCost=project.flyCost;
gold=exp=0;
level=1;
attackBonus=0;
attackMultiplier=100;
critChance=0;
critDamage=150;
lifesteal=defense=damageReduction=0;
lastCrit=false;
flying=false;
deadEventSent=mpEmptySent=false;
airOffset=verticalSpeed=0;
playerX=project.playerX;
playerY=project.playerY;
axisX=axisY=0;
vars.clear();
items.clear();
statuses.clear();
enemies.clear();
enemies.add(new Enemy(project.enemyX,project.enemyY,project.enemyHp));
lastFrame=System.currentTimeMillis();
lastTick=lastFrame;
lastAttack=lastEnemyAttack=lastMoveEvent=lastMpDrain=0;
message="";
eventDepth=0;
fire("on_start",null);
invalidate();
}

 void stop(){
playing=false;
axisX=axisY=0;
invalidate();
}

 void fire(String event,Enemy target){
if(eventDepth>=8)return;
eventDepth++;
try{
fireList(rules,event,target);
}
finally{
eventDepth--;
}
}

 void triggerMenu(GameModule module){
if(!playing)return;
fire("on_menu",null);
fireList(module.rules(),"on_menu",null);
invalidate();
}

 void fireList(List<GameRules.Rule> source,String event,Enemy target){
for(GameRules.Rule r:source)if(r.event.equals(event)){
GameRules.Action chosen=GameRules.matches(r,key->valueOf(key,target))?r.action:r.elseAction;
if(chosen!=null)apply(chosen,target);
}
}

 void apply(GameRules.Action a,Enemy target){
long now=System.currentTimeMillis();
String n=a.name;
String[] x=a.args;

  if(n.equals("message")){
message=formatMessage(a.text,target);
messageUntil=now+3200;
return;
}

  int v=x.length>0&&x[0].matches("-?\\d+")?Integer.parseInt(x[0]):0;

  if(n.equals("heal")){
health=Math.min(maxHealth,health+Math.max(0,v));
return;
}
if(n.equals("heal_percent")){
health=Math.min(maxHealth,health+(int)((long)maxHealth*v/100));
return;
}

  if(n.equals("damage")){
if(target!=null&&target.hp>0)target.hp=Math.max(0,target.hp-Math.max(0,v));
return;
}
if(n.equals("damage_percent")){
if(target!=null&&target.hp>0)target.hp=Math.max(0,target.hp-(int)((long)target.hp*Math.max(0,v)/100));
return;
}

  if(n.equals("enemy_heal")){
if(target!=null)target.hp=Math.min(project.enemyHp,target.hp+Math.max(0,v));
return;
}
if(n.equals("set_enemy_hp")){
if(target!=null)target.hp=Math.max(0,v);
return;
}

  if(n.equals("set_hp")){
health=Math.max(0,Math.min(maxHealth,v));
checkDeath(target);
return;
}
if(n.equals("set_max_hp")){
maxHealth=Math.max(1,v);
health=Math.min(health,maxHealth);
return;
}

  if(n.equals("mp_add")){
setMp(mp+Math.max(0,v));
return;
}
if(n.equals("mp_add_percent")){
setMp(mp+(int)((long)maxMp*v/100));
return;
}
if(n.equals("mp_sub")){
setMp(mp-Math.max(0,v));
return;
}
if(n.equals("set_mp")){
setMp(v);
return;
}
if(n.equals("set_max_mp")){
maxMp=Math.max(1,v);
mp=Math.min(mp,maxMp);
return;
}

  if(n.equals("speed")){
speed=Math.max(0,Math.min(5000,v));
return;
}
if(n.equals("speed_add")){
speed=Math.max(0,Math.min(5000,speed+v));
return;
}
if(n.equals("speed_sub")){
speed=Math.max(0,Math.min(5000,speed-v));
return;
}

  if(n.equals("spawn")){
for(int k=0;
k<v&&enemies.size()<60;
k++)enemies.add(new Enemy(Math.max(20,Math.min(project.width-20,project.enemyX+25*(k%5))),Math.max(20,Math.min(project.height-20,project.enemyY+25*(k/5))),project.enemyHp));
return;
}

  if(n.equals("teleport")){
playerX=Math.max(10,Math.min(project.width-10,Integer.parseInt(x[0])));
playerY=Math.max(10,Math.min(project.height-10,Integer.parseInt(x[1])));
return;
}

  if(n.equals("gravity")){
gravity=Math.max(0,v);
return;
}
if(n.equals("jump")){
jump(v);
return;
}
if(n.equals("fly_cost")){
flyCost=Math.max(0,v);
return;
}
if(n.equals("fly")){
boolean next=x[0].equals("toggle")?!flying:x[0].equals("on");
setFlying(next);
return;
}

  if(n.equals("gold_add")){
gold=clampState((long)gold+v);
return;
}
if(n.equals("gold_sub")){
gold=Math.max(0,clampState((long)gold-v));
return;
}
if(n.equals("exp_add")){
exp=Math.max(0,clampState((long)exp+v));
return;
}
if(n.equals("level_set")){
level=Math.max(1,v);
return;
}
if(n.equals("level_add")){
level=Math.max(1,clampState((long)level+v));
return;
}

  if(n.equals("set")||n.equals("add")||n.equals("sub")){
String key=x[0];
int old=vars.containsKey(key)?vars.get(key):0,amount=Integer.parseInt(x[1]);
vars.put(key,n.equals("set")?amount:clampState((long)old+(n.equals("add")?amount:-amount)));
return;
}

  if(n.equals("random_set")){
int min=Integer.parseInt(x[1]),max=Integer.parseInt(x[2]);
int span=max-min+1;
int result=span<=1?min:min+rng.nextInt(span);
vars.put(x[0],result);
return;
}

  if(n.equals("item_add")||n.equals("item_remove")){
String id=x[0];
int old=items.containsKey(id)?items.get(id):0,qty=Integer.parseInt(x[1]);
int next=n.equals("item_add")?clampState((long)old+qty):Math.max(0,old-qty);
items.put(id,next);
return;
}

  if(n.equals("status_add")){
statuses.put(x[0],now+Long.parseLong(x[1]));
return;
}
if(n.equals("status_remove")){
statuses.remove(x[0]);
return;
}
if(n.equals("status_clear")){
statuses.clear();
return;
}
if(n.equals("item_clear")){
items.clear();
return;
}

  if(n.equals("attack_bonus")){
attackBonus=v;
return;
}
if(n.equals("attack_multiplier")){
attackMultiplier=Math.max(0,v);
return;
}
if(n.equals("crit_chance")){
critChance=Math.max(0,Math.min(100,v));
return;
}
if(n.equals("crit_damage")){
critDamage=Math.max(0,v);
return;
}
if(n.equals("lifesteal")){
lifesteal=Math.max(0,Math.min(100,v));
return;
}
if(n.equals("defense")){
defense=Math.max(0,v);
return;
}
if(n.equals("damage_reduction")){
damageReduction=Math.max(0,Math.min(100,v));
return;
}

  if(n.equals("revive")){
if(health<=0){
health=Math.min(maxHealth,Math.max(1,v));
deadEventSent=false;
message="Đã hồi sinh";
messageUntil=now+1800;
}
return;
}

 }

 void attack(){
if(!playing||health<=0)return;
if(hasStatus("stun")||hasStatus("silence")){
message=hasStatus("stun")?"Đang bị choáng":"Đang bị khóa kỹ năng";
messageUntil=System.currentTimeMillis()+700;
return;
}
Skill skill=project.activeSkill();
long now=System.currentTimeMillis();
if(now-lastAttack<skill.cooldown)return;
lastAttack=now;
effectX=playerX;
effectY=playerY;
Enemy target=nearestEnemy();
double distance=target==null?Double.MAX_VALUE:Math.hypot(target.x-playerX,target.y-playerY);

  if(target==null){
message="Đã thắng!";
messageUntil=now+3000;
return;
}
if(distance>skill.range){
message="Ngoài tầm đánh";
messageUntil=now+800;
return;
}

  effectX=target.x;
effectY=target.y;
long base=Math.max(0,(long)skill.damage+attackBonus);
long dealt=base*Math.max(0,attackMultiplier)/100;
lastCrit=critChance>0&&rng.nextInt(100)<critChance;
if(lastCrit)dealt=dealt*Math.max(0,critDamage)/100;
int hit=Math.max(0,Math.min(Integer.MAX_VALUE,(int)Math.min(Integer.MAX_VALUE,dealt)));
target.hp=Math.max(0,target.hp-hit);
if(lifesteal>0&&hit>0)health=Math.min(maxHealth,health+(int)((long)hit*lifesteal/100));
fire("on_attack",target);

  if(target.hp==0){
fire("on_kill",target);
boolean alive=false;
for(Enemy e:enemies)if(e.hp>0)alive=true;
if(!alive){
message="Đã thắng!";
messageUntil=now+4000;
}
}
invalidate();

 }

 void update(){
long now=System.currentTimeMillis();
float delta=Math.min(.05f,Math.max(0,(now-lastFrame)/1000f));
lastFrame=now;
if(!playing)return;
cleanStatuses(now);
if(health<=0){
checkDeath(null);
return;
}

  if(now-lastTick>=1000){
lastTick=now;
fire("on_tick",null);
if(flying&&flyCost>0){
setMp(mp-flyCost);
if(mp<=0)setFlying(false);
}
}

  if(flying){
airOffset=-28;
verticalSpeed=0;
}
else if(airOffset<0||verticalSpeed!=0){
verticalSpeed+=gravity*delta;
airOffset+=verticalSpeed*delta;
if(airOffset>=0){
airOffset=0;
verticalSpeed=0;
fire("on_land",null);
}
}

  float x=axisX,y=axisY;
if(hasStatus("stun")||hasStatus("root")){
x=0;
y=0;
}
if(project.movement.equals("4 hướng")){
if(Math.abs(x)>Math.abs(y))y=0;
else x=0;
}
float len=(float)Math.hypot(x,y);
if(len>1){
x/=len;
y/=len;
}

  float oldX=playerX,oldY=playerY,nx=Math.max(10,Math.min(project.width-10,playerX+x*speed*delta)),ny=Math.max(10,Math.min(project.height-10,playerY+y*speed*delta));
if(project.mode.equals("2D")){
if(!solidAt(nx,playerY))playerX=nx;
if(!solidAt(playerX,ny))playerY=ny;
}
else{
playerX=nx;
playerY=ny;
}

  if((playerX!=oldX||playerY!=oldY)&&now-lastMoveEvent>=250){
lastMoveEvent=now;
fire("on_move",null);
}

  if(now-lastEnemyAttack>900&&!hasStatus("invincible")){
for(Enemy enemy:enemies)if(enemy.hp>0&&Math.hypot(enemy.x-playerX,enemy.y-playerY)<48){
lastEnemyAttack=now;
int incoming=Math.max(0,project.enemyDamage-defense);
incoming=(int)((long)incoming*(100-damageReduction)/100);
health=Math.max(0,health-incoming);
fire("on_hit",enemy);
checkDeath(enemy);
break;
}
}

 }

 void setMp(int value){
int before=mp;
mp=Math.max(0,Math.min(maxMp,value));
if(mp>0)mpEmptySent=false;
if(before>0&&mp==0&&!mpEmptySent){
mpEmptySent=true;
fire("on_mp_empty",null);
}
}

 void jump(int power){
if(flying||airOffset<0||verticalSpeed!=0)return;
verticalSpeed=-Math.max(1,power);
fire("on_jump",null);
}

 void manualJump(){
if(playing)jump(420);
invalidate();
}

 void toggleFly(){
if(playing)setFlying(!flying);
invalidate();
}

 void setFlying(boolean value){
if(flying==value)return;
flying=value;
if(flying){
airOffset=-28;
verticalSpeed=0;
fire("on_fly",null);
}
else if(airOffset>=0)airOffset=-28;
}

 void checkDeath(Enemy target){
if(health<=0&&!deadEventSent){
deadEventSent=true;
message="Nhân vật đã hết HP";
messageUntil=System.currentTimeMillis()+4000;
fire("on_death",target);
}
}

 void cleanStatuses(long now){
Iterator<Map.Entry<String,Long>> it=statuses.entrySet().iterator();
while(it.hasNext())if(it.next().getValue()<=now)it.remove();
}

 boolean hasStatus(String name){
Long until=statuses.get(name);
if(until==null)return false;
if(until<=System.currentTimeMillis()){
statuses.remove(name);
return false;
}
return true;
}

 Enemy nearestEnemy(){
Enemy target=null;
double best=Double.MAX_VALUE;
for(Enemy e:enemies)if(e.hp>0){
double d=Math.hypot(e.x-playerX,e.y-playerY);
if(d<best){
best=d;
target=e;
}
}
return target;
}

 int valueOf(String key,Enemy target){
if(key.equals("hp"))return health;
if(key.equals("max_hp"))return maxHealth;
if(key.equals("hp_percent"))return maxHealth<=0?0:(int)((long)health*100/maxHealth);
if(key.equals("mp"))return mp;
if(key.equals("max_mp"))return maxMp;
if(key.equals("mp_percent"))return maxMp<=0?0:(int)((long)mp*100/maxMp);
if(key.equals("gold"))return gold;
if(key.equals("exp"))return exp;
if(key.equals("level"))return level;
if(key.equals("x"))return (int)playerX;
if(key.equals("y"))return (int)playerY;
if(key.equals("speed"))return speed;
if(key.equals("enemy_count")){
int count=0;
for(Enemy e:enemies)if(e.hp>0)count++;
return count;
}
if(key.equals("enemy_alive"))return nearestEnemy()==null?0:1;
if(key.equals("enemy_hp")){
Enemy e=target!=null?target:nearestEnemy();
return e==null?0:e.hp;
}
if(key.equals("flying"))return flying?1:0;
if(key.equals("grounded"))return !flying&&airOffset==0&&verticalSpeed==0?1:0;
if(key.equals("last_crit"))return lastCrit?1:0;
if(key.equals("attack_bonus"))return attackBonus;
if(key.equals("attack_multiplier"))return attackMultiplier;
if(key.equals("defense"))return defense;
if(key.equals("damage_reduction"))return damageReduction;
if(key.equals("random"))return rng.nextInt(100)+1;
if(key.startsWith("var:")){
String k=key.substring(4);
return vars.containsKey(k)?vars.get(k):0;
}
if(key.startsWith("item:")){
String k=key.substring(5);
return items.containsKey(k)?items.get(k):0;
}
if(key.startsWith("status:"))return hasStatus(key.substring(7))?1:0;
return 0;
}

 String formatMessage(String text,Enemy target){
return text.replace("{hp}",String.valueOf(health)).replace("{max_hp}",String.valueOf(maxHealth)).replace("{mp}",String.valueOf(mp)).replace("{max_mp}",String.valueOf(maxMp)).replace("{gold}",String.valueOf(gold)).replace("{exp}",String.valueOf(exp)).replace("{level}",String.valueOf(level)).replace("{x}",String.valueOf((int)playerX)).replace("{y}",String.valueOf((int)playerY)).replace("{enemy_hp}",String.valueOf(valueOf("enemy_hp",target)));
}

 int clampState(long value){
return (int)Math.max(-100000000L,Math.min(100000000L,value));
}

 boolean solidAt(float x,float y){
return project.blocked(x-7,y-7)||project.blocked(x+7,y-7)||project.blocked(x-7,y+7)||project.blocked(x+7,y+7);
}

 @Override protected void onDraw(Canvas canvas){
super.onDraw(canvas);
if(project==null)return;
if(project.mode.equals("2D")){
draw2D(canvas);
return;
}
if(playing)update();
float scale=Math.min(getWidth()/(float)project.width,getHeight()/(float)project.height);
if(scale<=0)return;
float ox=(getWidth()-project.width*scale)/2,oy=(getHeight()-project.height*scale)/2;

  canvas.save();
canvas.translate(ox,oy);
canvas.scale(scale,scale);

  paint.setColor(Color.rgb(43,71,61));
canvas.drawRect(0,0,project.width,project.height,paint);

  if(background!=null){
paint.setColor(Color.WHITE);
canvas.drawBitmap(background,null,new Rect(0,0,project.width,project.height),paint);
}

  if(!playing){
paint.setColor(0x448BD7A8);
for(int x=0;
x<project.width;
x+=32)canvas.drawLine(x,0,x,project.height,paint);
for(int y=0;
y<project.height;
y+=32)canvas.drawLine(0,y,project.width,y,paint);
}

  paint.setColor(Color.rgb(40,35,34));
for(Enemy e:enemies)if(e.hp>0)drawActor(canvas,enemyImage,e.x,e.y,0xffbd4b44);
if(!playing)drawActor(canvas,enemyImage,project.enemyX,project.enemyY,0xffbd4b44);

  drawActor(canvas,playerImage,playing?playerX:project.playerX,playing?playerY+airOffset:project.playerY,0xff53bbdb);
drawEffect(canvas);

  paint.setColor(Color.WHITE);
paint.setTextSize(20);
paint.setTypeface(Typeface.DEFAULT_BOLD);
canvas.drawText(project.name+" • "+(playing?"HP "+health+"/"+maxHealth+"  MP "+mp+"/"+maxMp:"BỐ CỤC • chạm map để đặt"),12,27,paint);

  paint.setColor(playing?0x9964bdd6:0x7764bdd6);
canvas.drawCircle(project.joystickX,project.joystickY,66,paint);
paint.setColor(playing?0x99ef996e:0x77ef996e);
canvas.drawCircle(project.attackX,project.attackY,56,paint);
paint.setColor(Color.WHITE);
paint.setTextSize(17);
canvas.drawText("DI CHUYỂN",project.joystickX-49,project.joystickY+4,paint);
canvas.drawText("ĐÁNH",project.attackX-21,project.attackY+4,paint);

  if(System.currentTimeMillis()<messageUntil){
paint.setColor(Color.WHITE);
paint.setTextSize(20);
canvas.drawText(message,16,55,paint);
}

  canvas.restore();
if(playing)postInvalidateDelayed(33);

 }

 void drawActor(Canvas c,Bitmap bitmap,float x,float y,int color){
paint.setColor(0xaa000000);
c.drawOval(new RectF(x-18,y+18,x+18,y+23),paint);
paint.setColor(color);
if(bitmap==null)c.drawCircle(x,y,22,paint);
else{
float ratio=Math.min(48f/bitmap.getWidth(),48f/bitmap.getHeight());
float w=bitmap.getWidth()*ratio,h=bitmap.getHeight()*ratio;
paint.setColor(Color.WHITE);
c.drawBitmap(bitmap,null,new RectF(x-w/2,y+16-h,x+w/2,y+16),paint);
}
}

 float scale2D(){
return Math.min(getWidth()/400f,getHeight()/240f);
}

 float left2D(){
return (getWidth()-400*scale2D())/2;
}

 float top2D(){
return (getHeight()-240*scale2D())/2;
}

 float screenX(float x){
return (x-left2D())/scale2D();
}

 float screenY(float y){
return (y-top2D())/scale2D();
}

 void draw2D(Canvas c){
if(playing)update();
float scale=scale2D();
if(scale<=0)return;

  if(playing){
cameraX=Math.max(0,Math.min(project.width-400,playerX-200));
cameraY=Math.max(0,Math.min(project.height-240,playerY-120));
}

  c.save();
c.translate(left2D(),top2D());
c.scale(scale,scale);
c.clipRect(0,0,400,240);
paint.setColor(0xff214536);
c.drawRect(0,0,400,240,paint);

  c.save();
c.translate(-cameraX,-cameraY);

  if(background!=null){
paint.setColor(Color.WHITE);
c.drawBitmap(background,null,new Rect(0,0,project.width,project.height),paint);
}

  int x0=Math.max(0,(int)cameraX/16),y0=Math.max(0,(int)cameraY/16),x1=Math.min(project.columns(),(int)(cameraX+400)/16+2),y1=Math.min(project.rows(),(int)(cameraY+240)/16+2);

  for(int y=y0;
y<y1;
y++)for(int x=x0;
x<x1;
x++){
int index=y*project.columns()+x,id=project.tiles[index]&255;
Rect dest=new Rect(x*16,y*16,x*16+16,y*16+16);

   if(tileImage!=null){
int cols=tileImage.getWidth()/16,rows=tileImage.getHeight()/16;
if(cols>0&&id<cols*rows){
paint.setColor(Color.WHITE);
c.drawBitmap(tileImage,new Rect((id%cols)*16,(id/cols)*16,(id%cols+1)*16,(id/cols+1)*16),dest,paint);
}
else{
paint.setColor(0xff477b58);
c.drawRect(dest,paint);
}
}

   else {
paint.setColor(id==0?0xff477b58:id%3==1?0xffad9b71:id%3==2?0xff3d7894:0xff607f52);
c.drawRect(dest,paint);
}

   if(!playing){
if(project.solid[index]!=0){
paint.setColor(0x999c3a30);
c.drawRect(dest,paint);
}
paint.setColor(0x557ee5b3);
paint.setStrokeWidth(.5f);
c.drawRect(dest,paint);
}

  }

  for(Enemy e:enemies)if(playing&&e.hp>0)drawFlatActor(c,enemyImage,e.x,e.y,0xffbd4b44);

  if(!playing)drawFlatActor(c,enemyImage,project.enemyX,project.enemyY,0xffbd4b44);

  drawFlatActor(c,playerImage,playing?playerX:project.playerX,playing?playerY+airOffset:project.playerY,0xff53bbdb);
drawEffect(c);
c.restore();

  paint.setTypeface(Typeface.DEFAULT_BOLD);
paint.setTextSize(12);
paint.setColor(Color.WHITE);
c.drawText(project.name+" • "+(playing?"HP "+health+"/"+maxHealth+" MP "+mp+"/"+maxMp:"MAP 2D • "+((int)cameraX)+","+((int)cameraY)),7,15,paint);

  if(playing){
paint.setColor(0x8864bdd6);
c.drawCircle(project.joystickX,project.joystickY,30,paint);
paint.setColor(0x99ef996e);
c.drawCircle(project.attackX,project.attackY,27,paint);
paint.setColor(Color.WHITE);
paint.setTextSize(10);
c.drawText("ĐI",project.joystickX-7,project.joystickY+3,paint);
c.drawText("ĐÁNH",project.attackX-14,project.attackY+3,paint);
}

  if(System.currentTimeMillis()<messageUntil){
paint.setColor(Color.WHITE);
paint.setTextSize(12);
c.drawText(message,7,31,paint);
}
c.restore();
if(playing)postInvalidateDelayed(33);

 }

 void drawFlatActor(Canvas c,Bitmap bitmap,float x,float y,int color){
if(bitmap==null){
paint.setColor(color);
c.drawCircle(x,y,9,paint);
}
else{
paint.setColor(Color.WHITE);
float ratio=Math.min(24f/bitmap.getWidth(),24f/bitmap.getHeight());
float w=bitmap.getWidth()*ratio,h=bitmap.getHeight()*ratio;
c.drawBitmap(bitmap,null,new RectF(x-w/2,y-h/2,x+w/2,y+h/2),paint);
}
}

 void drawEffect(Canvas c){
if(!playing||lastAttack==0)return;
Skill skill=project.activeSkill();
SkillEffect effect=project.effectById(skill.effectId);
long elapsed=System.currentTimeMillis()-lastAttack;
if(effect!=null&&effectImage!=null){
long duration=skill.frameMs*(long)effect.frameCount;
if(elapsed<0||elapsed>=duration)return;
int frame=(int)(elapsed/skill.frameMs);
int fw=effect.frameWidth,fh=effect.frameHeight,columns=effectImage.getWidth()/fw,rows=effectImage.getHeight()/fh;
if(columns>0&&rows>0&&frame<Math.min(effect.frameCount,columns*rows)){
int sx=(frame%columns)*fw,sy=(frame/columns)*fh;
paint.setColor(Color.WHITE);
c.drawBitmap(effectImage,new Rect(sx,sy,sx+fw,sy+fh),new RectF(effectX-fw/2f,effectY-fh/2f,effectX+fw/2f,effectY+fh/2f),paint);
return;
}
}
long legacyDuration=skill.frameMs*(long)skill.frameCount;
if(elapsed<0||elapsed>=legacyDuration)return;
int frame=skill.firstFrame+(int)(elapsed/skill.frameMs);
if(effectImage!=null&&effectImage.getWidth()%16==0&&effectImage.getHeight()%16==0){
int columns=effectImage.getWidth()/16;
if(frame<columns*(effectImage.getHeight()/16)){
paint.setColor(Color.WHITE);
c.drawBitmap(effectImage,new Rect((frame%columns)*16,(frame/columns)*16,(frame%columns+1)*16,(frame/columns+1)*16),new RectF(effectX-16,effectY-16,effectX+16,effectY+16),paint);
return;
}
}
paint.setColor(0x99ffc443);
c.drawCircle(effectX,effectY,12,paint);
}

 boolean touch2D(MotionEvent e){
int action=e.getActionMasked();
if(scale2D()<=0)return true;

  if(!playing){
if(action==MotionEvent.ACTION_DOWN)lastPaintIndex=-1;
if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_MOVE||action==MotionEvent.ACTION_UP){
float sx=screenX(e.getX()),sy=screenY(e.getY());
if(sx>=0&&sx<400&&sy>=0&&sy<240){
int x=(int)(sx+cameraX),y=(int)(sy+cameraY),index=project.tileAt(x,y);
if(index>=0){
if(editTool==0&&action==MotionEvent.ACTION_UP){
project.playerX=x;
project.playerY=y;
}
else if(editTool==1&&action==MotionEvent.ACTION_UP){
project.enemyX=x;
project.enemyY=y;
}
else if(editTool==2&&action==MotionEvent.ACTION_UP){
project.joystickX=(int)sx;
project.joystickY=(int)sy;
}
else if(editTool==3&&action==MotionEvent.ACTION_UP){
project.attackX=(int)sx;
project.attackY=(int)sy;
}
else if(editTool>=4&&index!=lastPaintIndex){
if(editTool==4){
int[] ids=brushIds==null?new int[]{
tileBrush}
:brushIds;
int cols=Math.max(1,brushColumns),startX=x/16,startY=y/16;
for(int n=0;
n<ids.length;
n++){
int col=startX+n%cols,row=startY+n/cols;
if(col<project.columns()&&row<project.rows())project.tiles[row*project.columns()+col]=(byte)ids[n];
}
}
if(editTool==5)project.solid[index]=1;
if(editTool==6){
project.solid[index]=0;
project.tiles[index]=0;
}
lastPaintIndex=index;
}
invalidate();
}
}
if(action==MotionEvent.ACTION_UP&&onChange!=null)onChange.run();
}
return true;
}

  if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){
axisX=axisY=0;
invalidate();
return true;
}
if(action==MotionEvent.ACTION_POINTER_UP){
axisX=axisY=0;
for(int i=0;
i<e.getPointerCount();
i++){
if(i==e.getActionIndex())continue;
float x=screenX(e.getX(i)),y=screenY(e.getY(i));
if(Math.hypot(x-project.joystickX,y-project.joystickY)<48){
axisX=(x-project.joystickX)/30f;
axisY=(y-project.joystickY)/30f;
}
}
return true;
}

  boolean move=false;
for(int i=0;
i<e.getPointerCount();
i++){
float x=screenX(e.getX(i)),y=screenY(e.getY(i));
if(Math.hypot(x-project.joystickX,y-project.joystickY)<48){
axisX=(x-project.joystickX)/30f;
axisY=(y-project.joystickY)/30f;
move=true;
}
if(Math.hypot(x-project.attackX,y-project.attackY)<38&&(action!=MotionEvent.ACTION_MOVE||project.attackMode.equals("Giữ")))attack();
}
if(!move)axisX=axisY=0;
invalidate();
return true;

 }

 float worldX(float touch){
float scale=Math.min(getWidth()/(float)project.width,getHeight()/(float)project.height);
return Math.max(0,Math.min(project.width,(touch-(getWidth()-project.width*scale)/2)/scale));
}

 float worldY(float touch){
float scale=Math.min(getWidth()/(float)project.width,getHeight()/(float)project.height);
return Math.max(0,Math.min(project.height,(touch-(getHeight()-project.height*scale)/2)/scale));
}

 boolean insideWorld(float x,float y){
float scale=Math.min(getWidth()/(float)project.width,getHeight()/(float)project.height);
float left=(getWidth()-project.width*scale)/2,top=(getHeight()-project.height*scale)/2;
return scale>0&&x>=left&&x<=left+project.width*scale&&y>=top&&y<=top+project.height*scale;
}

 @Override public boolean onTouchEvent(android.view.MotionEvent e){
if(project==null)return true;
if(project.mode.equals("2D"))return touch2D(e);
int action=e.getActionMasked();
if(!playing){
if(action==MotionEvent.ACTION_UP&&insideWorld(e.getX(),e.getY())){
int x=(int)worldX(e.getX()),y=(int)worldY(e.getY());
if(editTool==0){
project.playerX=x;
project.playerY=y;
}
else if(editTool==1){
project.enemyX=x;
project.enemyY=y;
}
else if(editTool==2){
project.joystickX=x;
project.joystickY=y;
}
else{
project.attackX=x;
project.attackY=y;
}
if(onChange!=null)onChange.run();
invalidate();
}
return true;
}

  if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){
axisX=axisY=0;
invalidate();
return true;
}

  if(action==MotionEvent.ACTION_POINTER_UP){
axisX=axisY=0;
for(int i=0;
i<e.getPointerCount();
i++){
if(i==e.getActionIndex())continue;
float x=worldX(e.getX(i)),y=worldY(e.getY(i));
if(Math.hypot(x-project.joystickX,y-project.joystickY)<100){
axisX=(x-project.joystickX)/66f;
axisY=(y-project.joystickY)/66f;
}
}
invalidate();
return true;
}

  if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN||action==MotionEvent.ACTION_MOVE){
boolean move=false;
for(int i=0;
i<e.getPointerCount();
i++){
float x=worldX(e.getX(i)),y=worldY(e.getY(i));
if(Math.hypot(x-project.joystickX,y-project.joystickY)<100){
axisX=(x-project.joystickX)/66f;
axisY=(y-project.joystickY)/66f;
move=true;
}
if(Math.hypot(x-project.attackX,y-project.attackY)<72&&(action!=MotionEvent.ACTION_MOVE||project.attackMode.equals("Giữ")))attack();
}
if(!move)axisX=axisY=0;
invalidate();
return true;
}
return true;

 }

}