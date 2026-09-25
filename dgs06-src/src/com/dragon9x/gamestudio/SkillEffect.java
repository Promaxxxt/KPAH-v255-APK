package com.dragon9x.gamestudio;

import org.json.*;

/** One reusable visual effect made in the Skill Designer. Frames may be any pixel size. */
final class SkillEffect {

 int id=1,frameWidth=64,frameHeight=64,frameCount=1,frameMs=80;

 String name="Hiệu ứng 1",fileName="effects/eff_1.png";

 JSONObject json()throws JSONException{
JSONObject j=new JSONObject();
j.put("id",id);
j.put("name",name);
j.put("file",fileName);
j.put("frameWidth",frameWidth);
j.put("frameHeight",frameHeight);
j.put("frameCount",frameCount);
j.put("frameMs",frameMs);
return j;
}

 static SkillEffect parse(JSONObject j){
SkillEffect e=new SkillEffect();
e.id=j.optInt("id",e.id);
e.name=j.optString("name",e.name);
e.fileName=j.optString("file","effects/eff_"+e.id+".png");
e.frameWidth=j.optInt("frameWidth",e.frameWidth);
e.frameHeight=j.optInt("frameHeight",e.frameHeight);
e.frameCount=j.optInt("frameCount",e.frameCount);
e.frameMs=j.optInt("frameMs",e.frameMs);
e.validate();
return e;
}

 static SkillEffect copy(SkillEffect old){
SkillEffect e=new SkillEffect();
e.id=old.id;
e.name=old.name;
e.fileName=old.fileName;
e.frameWidth=old.frameWidth;
e.frameHeight=old.frameHeight;
e.frameCount=old.frameCount;
e.frameMs=old.frameMs;
return e;
}

 void validate(){
if(id<1||id>99999||name==null||name.trim().isEmpty()||name.length()>60||!fileName.matches("effects/eff_[0-9]{1,5}\\.png")||frameWidth<1||frameWidth>2048||frameHeight<1||frameHeight>2048||frameCount<1||frameCount>1024||frameMs<20||frameMs>5000)throw new IllegalArgumentException("Thông số hiệu ứng không hợp lệ");
}

 public String toString(){
return id+" • "+name+" • frame "+frameWidth+"×"+frameHeight+" • "+frameCount+" ảnh";
}

}