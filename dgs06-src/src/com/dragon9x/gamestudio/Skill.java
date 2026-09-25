package com.dragon9x.gamestudio;

import org.json.*;

/** Independent game skill. Visuals are assigned from the reusable SkillEffect library. */
final class Skill {

 int id=1,damage=20,range=95,cooldown=450,firstFrame=0,frameCount=1,frameMs=80,effectId=0;

 String name="Đòn cơ bản",faction="Chung";

 JSONObject json()throws JSONException{
JSONObject j=new JSONObject();
j.put("id",id);
j.put("name",name);
j.put("faction",faction);
j.put("damage",damage);
j.put("range",range);
j.put("cooldown",cooldown);
j.put("firstFrame",firstFrame);
j.put("frameCount",frameCount);
j.put("frameMs",frameMs);
j.put("effectId",effectId);
return j;
}

 static Skill parse(JSONObject j){
Skill s=new Skill();
s.id=j.optInt("id",s.id);
s.name=j.optString("name",s.name);
s.faction=j.optString("faction",s.faction);
s.damage=j.optInt("damage",s.damage);
s.range=j.optInt("range",s.range);
s.cooldown=j.optInt("cooldown",s.cooldown);
s.firstFrame=j.optInt("firstFrame",0);
s.frameCount=j.optInt("frameCount",1);
s.frameMs=j.optInt("frameMs",80);
s.effectId=j.optInt("effectId",0);
s.validate();
return s;
}

 static Skill parseUnchecked(Skill old){
Skill s=new Skill();
s.id=old.id;
s.name=old.name;
s.faction=old.faction;
s.damage=old.damage;
s.range=old.range;
s.cooldown=old.cooldown;
s.firstFrame=old.firstFrame;
s.frameCount=old.frameCount;
s.frameMs=old.frameMs;
s.effectId=old.effectId;
return s;
}

 void validate(){
if(id<1||id>99999||name.trim().isEmpty()||name.length()>60||faction.trim().isEmpty()||faction.length()>40||damage<0||damage>1000000||range<1||range>4096||cooldown<50||cooldown>30000||firstFrame<0||firstFrame>4095||frameCount<1||frameCount>4096||frameMs<20||frameMs>5000||effectId<0||effectId>99999)throw new IllegalArgumentException("Thông số skill/animation không hợp lệ");
}

 public String toString(){
return id+" • "+name+" ["+faction+"] • sát thương "+damage+(effectId>0?" • eff #"+effectId:"");
}

}