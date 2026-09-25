package com.dragon9x.gamestudio;

import org.json.*;
import java.util.*;

/** One reusable animation set for the playable character. */
final class CharacterAction {

 static final String[] KEYS={
 "stand","run","punch","kick","slash","jump","fly",
 "skill1","skill2","skill3","skill4","skill5","skill6","skill7","skill8","skill9"
 };
 static final String[] LABELS={
 "Đứng","Chạy","Đấm","Đá","Chém","Nhảy","Bay",
 "Tung skill 1","Tung skill 2","Tung skill 3","Tung skill 4","Tung skill 5","Tung skill 6","Tung skill 7","Tung skill 8","Tung skill 9"
 };

 String key="stand",name="Đứng",fileName="characters/player_stand.png";
 int frameWidth=32,frameHeight=32,frameCount=1,frameMs=180;
 boolean loop=true;

 static boolean allowedKey(String key){
 for(String k:KEYS)if(k.equals(key))return true;
 return false;
 }

 static int indexOf(String key){
 for(int i=0;i<KEYS.length;i++)if(KEYS[i].equals(key))return i;
 return -1;
 }

 static String labelOf(String key){
 int i=indexOf(key);
 return i<0?key:LABELS[i];
 }

 static boolean defaultLoop(String key){
 return key.equals("stand")||key.equals("run")||key.equals("fly");
 }

 static CharacterAction create(String key){
 if(!allowedKey(key))throw new IllegalArgumentException("Động tác nhân vật không hỗ trợ: "+key);
 CharacterAction a=new CharacterAction();
 a.key=key;
 a.name=labelOf(key);
 a.fileName="characters/player_"+key+".png";
 a.frameMs=key.equals("stand")?260:key.equals("run")?90:110;
 a.loop=defaultLoop(key);
 return a;
 }

 static CharacterAction copy(CharacterAction old){
 CharacterAction a=new CharacterAction();
 a.key=old.key;
 a.name=old.name;
 a.fileName=old.fileName;
 a.frameWidth=old.frameWidth;
 a.frameHeight=old.frameHeight;
 a.frameCount=old.frameCount;
 a.frameMs=old.frameMs;
 a.loop=old.loop;
 return a;
 }

 JSONObject json()throws JSONException{
 JSONObject j=new JSONObject();
 j.put("key",key);
 j.put("name",name);
 j.put("fileName",fileName);
 j.put("frameWidth",frameWidth);
 j.put("frameHeight",frameHeight);
 j.put("frameCount",frameCount);
 j.put("frameMs",frameMs);
 j.put("loop",loop);
 return j;
 }

 static CharacterAction parse(JSONObject j){
 String key=j.optString("key","stand");
 CharacterAction a=create(key);
 a.name=j.optString("name",a.name);
 a.fileName=j.optString("fileName",a.fileName);
 a.frameWidth=j.optInt("frameWidth",a.frameWidth);
 a.frameHeight=j.optInt("frameHeight",a.frameHeight);
 a.frameCount=j.optInt("frameCount",a.frameCount);
 a.frameMs=j.optInt("frameMs",a.frameMs);
 a.loop=j.optBoolean("loop",a.loop);
 a.validate();
 return a;
 }

 void validate(){
 if(!allowedKey(key))throw new IllegalArgumentException("Động tác nhân vật không hợp lệ");
 if(name==null||name.trim().isEmpty()||name.length()>60)throw new IllegalArgumentException("Tên động tác không hợp lệ");
 String expected="characters/player_"+key+".png";
 if(!expected.equals(fileName))throw new IllegalArgumentException("Tên file động tác phải là "+expected);
 if(frameWidth<1||frameWidth>2048||frameHeight<1||frameHeight>2048||frameCount<1||frameCount>4096||frameMs<20||frameMs>5000)throw new IllegalArgumentException("Thông số frame nhân vật không hợp lệ");
 }

 public String toString(){
 return name+" • "+frameWidth+"×"+frameHeight+" • "+frameCount+" frame • "+frameMs+"ms"+(loop?" • lặp":"");
 }
}