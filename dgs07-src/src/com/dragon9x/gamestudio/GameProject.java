package com.dragon9x.gamestudio;

import org.json.*;

import java.util.*;

/** The game's own data, entirely independent of imported JARs. */
final class GameProject {

 String name="Game mới",mode="2D",movement="8 hướng",attackMode="Chạm",rules="on_start: message Chạm trái để đi, chạm phải để đánh\non_kill: heal 5";

 int width=800,height=480,playerX=170,playerY=230,enemyX=560,enemyY=230,joystickX=50,joystickY=190,attackX=350,attackY=190;

 int maxHp=100,maxMp=100,speed=160,gravity=900,flyCost=5,attackDamage=20,attackRange=95,attackCooldown=450,enemyHp=60,enemyDamage=5;

 byte[] tiles,solid;

 ArrayList<Skill> skills=new ArrayList<>();
int activeSkillId=1;

 ArrayList<SkillEffect> effects=new ArrayList<>();

 ArrayList<CharacterAction> characterActions=new ArrayList<>();
 int[] quickSkillIds=new int[9];
 String basicAction="punch";

 ArrayList<GameModule> modules=new ArrayList<>();

 GameProject(){
resizeMap(width,height);
skills.add(new Skill());
quickSkillIds[0]=1;
}

 Skill activeSkill(){
for(Skill s:skills)if(s.id==activeSkillId)return s;
return skills.get(0);
}

 SkillEffect effectById(int id){
for(SkillEffect e:effects)if(e.id==id)return e;
return null;
}

 SkillEffect activeEffect(){
return effectById(activeSkill().effectId);
}

 Skill skillById(int id){
for(Skill s:skills)if(s.id==id)return s;
return null;
}

 CharacterAction characterAction(String key){
for(CharacterAction a:characterActions)if(a.key.equals(key))return a;
return null;
}

 int columns(){
return width/16;
}
int rows(){
return height/16;
}

 void resizeMap(int w,int h){
if(w<320||w>4096||h<240||h>4096||w%16!=0||h%16!=0)throw new IllegalArgumentException("Map phải chia hết cho 16; rộng 320–4096, cao 240–4096");
byte[] t=new byte[w/16*h/16],s=new byte[t.length];
if(tiles!=null&&solid!=null){
int cols=Math.min(columns(),w/16),rows=Math.min(rows(),h/16);
for(int y=0;
y<rows;
y++){
System.arraycopy(tiles,y*columns(),t,y*(w/16),cols);
System.arraycopy(solid,y*columns(),s,y*(w/16),cols);
}
}
width=w;
height=h;
tiles=t;
solid=s;
}

 int tileAt(int x,int y){
if(x<0||y<0||x>=width||y>=height)return -1;
return (y/16)*columns()+x/16;
}

 boolean blocked(float x,float y){
int index=tileAt((int)x,(int)y);
return index<0||solid[index]!=0;
}

 static GameProject fromJson(String data)throws JSONException{

  JSONObject j=new JSONObject(data);
GameProject p=new GameProject();

  p.name=j.optString("name",p.name);
p.mode=j.optString("mode","2.5D");
p.movement=j.optString("movement",p.movement);
p.attackMode=j.optString("attackMode",p.attackMode);
p.rules=j.optString("rules",p.rules);

  p.width=j.optInt("width",p.width);
p.height=j.optInt("height",p.height);
p.playerX=j.optInt("playerX",p.playerX);
p.playerY=j.optInt("playerY",p.playerY);
p.enemyX=j.optInt("enemyX",p.enemyX);
p.enemyY=j.optInt("enemyY",p.enemyY);
p.joystickX=j.optInt("joystickX",85);
p.joystickY=j.optInt("joystickY",p.height-85);
p.attackX=j.optInt("attackX",p.width-90);
p.attackY=j.optInt("attackY",p.height-85);

  p.maxHp=j.optInt("maxHp",p.maxHp);
p.maxMp=j.optInt("maxMp",p.maxMp);
p.speed=j.optInt("speed",p.speed);
p.gravity=j.optInt("gravity",p.gravity);
p.flyCost=j.optInt("flyCost",p.flyCost);
p.attackDamage=j.optInt("attackDamage",p.attackDamage);
p.attackRange=j.optInt("attackRange",p.attackRange);
p.attackCooldown=j.optInt("attackCooldown",p.attackCooldown);
p.enemyHp=j.optInt("enemyHp",p.enemyHp);
p.enemyDamage=j.optInt("enemyDamage",p.enemyDamage);

  p.tiles=null;
p.solid=null;
p.resizeMap(p.width,p.height);
String tiles64=j.optString("tiles","");
String solid64=j.optString("solid","");
if(!tiles64.isEmpty()||!solid64.isEmpty()){
byte[] t=android.util.Base64.decode(tiles64,android.util.Base64.DEFAULT),s=android.util.Base64.decode(solid64,android.util.Base64.DEFAULT);
if(t.length!=p.tiles.length||s.length!=p.solid.length)throw new IllegalArgumentException("Dữ liệu tile không khớp kích thước map");
p.tiles=t;
p.solid=s;
for(byte b:s)if(b!=0&&b!=1)throw new IllegalArgumentException("Ô cản không hợp lệ");
}

  if(p.mode.equals("2D")&&(p.joystickX>400||p.joystickY>240||p.attackX>400||p.attackY>240)){
p.joystickX=50;
p.joystickY=190;
p.attackX=350;
p.attackY=190;
}

  JSONArray skillData=j.optJSONArray("skills");
if(skillData!=null){
p.skills.clear();
if(skillData.length()>100)throw new IllegalArgumentException("Tối đa 100 chiêu thức");
for(int i=0;
i<skillData.length();
i++){
JSONObject entry=skillData.optJSONObject(i);
if(entry==null)throw new IllegalArgumentException("Skill không hợp lệ");
p.skills.add(Skill.parse(entry));
}
}
else{
p.skills.get(0).damage=p.attackDamage;
p.skills.get(0).range=p.attackRange;
p.skills.get(0).cooldown=p.attackCooldown;
}
p.activeSkillId=j.optInt("activeSkillId",1);

  JSONArray effectData=j.optJSONArray("effects");
if(effectData!=null){
if(effectData.length()>100)throw new IllegalArgumentException("Tối đa 100 hiệu ứng skill");
for(int i=0;
i<effectData.length();
i++){
JSONObject entry=effectData.optJSONObject(i);
if(entry==null)throw new IllegalArgumentException("Hiệu ứng skill không hợp lệ");
p.effects.add(SkillEffect.parse(entry));
}
}

  JSONArray actionData=j.optJSONArray("characterActions");
if(actionData!=null){
if(actionData.length()>CharacterAction.KEYS.length)throw new IllegalArgumentException("Quá nhiều động tác nhân vật");
for(int i=0;i<actionData.length();i++){
JSONObject entry=actionData.optJSONObject(i);
if(entry==null)throw new IllegalArgumentException("Động tác nhân vật không hợp lệ");
p.characterActions.add(CharacterAction.parse(entry));
}
}
p.basicAction=j.optString("basicAction",p.basicAction);
JSONArray quick=j.optJSONArray("quickSkillIds");
if(quick!=null){
for(int i=0;i<p.quickSkillIds.length;i++)p.quickSkillIds[i]=i<quick.length()?quick.optInt(i,0):0;
}
else{
java.util.Arrays.fill(p.quickSkillIds,0);
p.quickSkillIds[0]=p.activeSkillId;
}

  JSONArray moduleData=j.optJSONArray("modules");
if(moduleData!=null){
if(moduleData.length()>40)throw new IllegalArgumentException("Tối đa 40 class game");
for(int i=0;
i<moduleData.length();
i++){
JSONObject entry=moduleData.optJSONObject(i);
if(entry==null)throw new IllegalArgumentException("Class game không hợp lệ");
p.modules.add(GameModule.parse(entry));
}
}

  p.validate();
return p;

 }

 JSONObject json()throws JSONException {
JSONObject j=new JSONObject();
j.put("format",7);
j.put("mode",mode);
j.put("name",name);
j.put("movement",movement);
j.put("attackMode",attackMode);
j.put("rules",rules);
j.put("width",width);
j.put("height",height);
j.put("playerX",playerX);
j.put("playerY",playerY);
j.put("enemyX",enemyX);
j.put("enemyY",enemyY);
j.put("joystickX",joystickX);
j.put("joystickY",joystickY);
j.put("attackX",attackX);
j.put("attackY",attackY);
j.put("maxHp",maxHp);
j.put("maxMp",maxMp);
j.put("speed",speed);
j.put("gravity",gravity);
j.put("flyCost",flyCost);
j.put("attackDamage",attackDamage);
j.put("attackRange",attackRange);
j.put("attackCooldown",attackCooldown);
j.put("enemyHp",enemyHp);
j.put("enemyDamage",enemyDamage);
j.put("activeSkillId",activeSkillId);
JSONArray list=new JSONArray();
for(Skill s:skills)list.put(s.json());
j.put("skills",list);
JSONArray effectList=new JSONArray();
for(SkillEffect e:effects)effectList.put(e.json());
j.put("effects",effectList);
JSONArray actionList=new JSONArray();
for(CharacterAction a:characterActions)actionList.put(a.json());
j.put("characterActions",actionList);
j.put("basicAction",basicAction);
JSONArray quick=new JSONArray();
for(int id:quickSkillIds)quick.put(id);
j.put("quickSkillIds",quick);
JSONArray files=new JSONArray();
for(GameModule m:modules)files.put(m.json());
j.put("modules",files);
if(tiles!=null){
j.put("tiles",android.util.Base64.encodeToString(tiles,android.util.Base64.NO_WRAP));
j.put("solid",android.util.Base64.encodeToString(solid,android.util.Base64.NO_WRAP));
}
return j;
}

 void validate(){

  if(name==null||name.trim().isEmpty()||name.length()>80)throw new IllegalArgumentException("Tên game phải có 1–80 ký tự");

  if(width<320||width>4096||height<240||height>4096||width%16!=0||height%16!=0)throw new IllegalArgumentException("Map phải chia hết cho 16; rộng 320–4096, cao 240–4096");

  if(!mode.equals("2D")&&!mode.equals("2.5D"))throw new IllegalArgumentException("Loại game không hợp lệ");

  if(effects.size()>100)throw new IllegalArgumentException("Tối đa 100 hiệu ứng skill");
Set<Integer> effectIds=new HashSet<>();
for(SkillEffect effect:effects){
effect.validate();
if(!effectIds.add(effect.id))throw new IllegalArgumentException("ID hiệu ứng bị trùng");
}

  if(skills.isEmpty()||skills.size()>100)throw new IllegalArgumentException("Cần 1–100 chiêu thức");
Set<Integer> ids=new HashSet<>();
boolean found=false;
for(Skill skill:skills){
skill.validate();
if(!ids.add(skill.id))throw new IllegalArgumentException("ID skill bị trùng");
if(skill.effectId!=0&&!effectIds.contains(skill.effectId))throw new IllegalArgumentException("Skill "+skill.name+" đang gán hiệu ứng không tồn tại: "+skill.effectId);
if(skill.id==activeSkillId)found=true;
}
if(!found)throw new IllegalArgumentException("Skill đang test không tồn tại");

  if(characterActions.size()>CharacterAction.KEYS.length)throw new IllegalArgumentException("Quá nhiều động tác nhân vật");
Set<String> actionKeys=new HashSet<>();
for(CharacterAction action:characterActions){
action.validate();
if(!actionKeys.add(action.key))throw new IllegalArgumentException("Động tác nhân vật bị trùng: "+action.key);
}
if(!basicAction.equals("punch")&&!basicAction.equals("kick")&&!basicAction.equals("slash"))throw new IllegalArgumentException("Động tác đánh thường phải là đấm, đá hoặc chém");
if(quickSkillIds==null||quickSkillIds.length!=9)throw new IllegalArgumentException("Danh sách nút skill nhanh phải có 9 ô");
for(int id:quickSkillIds)if(id!=0&&!ids.contains(id))throw new IllegalArgumentException("Nút skill nhanh đang gán skill không tồn tại: "+id);

  if(modules.size()>40)throw new IllegalArgumentException("Tối đa 40 class game");
Set<String> names=new HashSet<>();
for(GameModule module:modules){
module.validate();
if(!names.add(module.name))throw new IllegalArgumentException("Tên class bị trùng: "+module.name);
}

  if(mode.equals("2D")&&(tiles==null||solid==null||tiles.length!=columns()*rows()||solid.length!=tiles.length))throw new IllegalArgumentException("Tile map thiếu hoặc sai kích thước");

  if(playerX<0||playerX>width||enemyX<0||enemyX>width||playerY<0||playerY>height||enemyY<0||enemyY>height||joystickX<0||joystickX>width||joystickY<0||joystickY>height||attackX<0||attackX>width||attackY<0||attackY>height)throw new IllegalArgumentException("Vị trí nhân vật/quái/nút điều khiển vượt map");

  if(maxHp<1||maxHp>1000000||maxMp<1||maxMp>1000000||enemyHp<1||enemyHp>1000000||speed<0||speed>5000||gravity<0||gravity>5000||flyCost<0||flyCost>10000||attackDamage<0||attackDamage>1000000||enemyDamage<0||enemyDamage>1000000||attackRange<1||attackRange>4096||attackCooldown<50||attackCooldown>30000)throw new IllegalArgumentException("Thông số chiến đấu ngoài giới hạn");

  if(!movement.equals("4 hướng")&&!movement.equals("8 hướng"))throw new IllegalArgumentException("Cách di chuyển chưa hỗ trợ");

  if(!attackMode.equals("Chạm")&&!attackMode.equals("Giữ"))throw new IllegalArgumentException("Cách đánh chưa hỗ trợ");

  GameRules.parse(rules);

 }

}