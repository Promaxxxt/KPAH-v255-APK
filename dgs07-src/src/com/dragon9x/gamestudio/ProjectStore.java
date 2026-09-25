package com.dragon9x.gamestudio;

import android.content.Context;

import android.graphics.*;

import java.io.*;

import java.nio.charset.StandardCharsets;

import java.util.*;

import java.util.zip.*;

/** Local project files. Version 0.7 adds reusable character action frame files. */
final class ProjectStore {

 private final File dir;
static final String[] ASSETS={
"background.png","player.png","enemy.png","tiles.png","tile-bank.png","effects.png"}
;

 ProjectStore(Context context){
dir=new File(context.getFilesDir(),"game-project");
dir.mkdirs();
recoverImport();
}

 void recoverImport(){
File stage=new File(dir,"import-stage");
if(!stage.isDirectory())return;
deleteTree(stage);
}

 boolean hasProject(){
return new File(dir,"project.json").isFile();
}

 boolean hasAsset(String name){
return Arrays.asList(ASSETS).contains(name)&&new File(dir,name).isFile();
}

 GameProject load(){
try{
GameProject project=GameProject.fromJson(new String(read(new File(dir,"project.json"),1024*1024),StandardCharsets.UTF_8));
try{
syncModules(project);
}
catch(IOException ignored){
}
return project;
}
catch(Exception e){
return new GameProject();
}
}

 void save(GameProject project)throws Exception {
project.validate();
byte[] data=project.json().toString(2).getBytes(StandardCharsets.UTF_8);
File temp=new File(dir,"project.tmp");
write(temp,data);
File target=new File(dir,"project.json");
if(target.exists()&&!target.delete())throw new IOException("Không thay được project.json");
if(!temp.renameTo(target))throw new IOException("Không lưu được dự án");
syncModules(project);
}

 void syncModules(GameProject project)throws IOException{
File folder=new File(dir,"src/game");
if(!folder.exists()&&!folder.mkdirs())throw new IOException("Không tạo được thư mục class game");
HashSet<String> expected=new HashSet<>();
for(GameModule module:project.modules){
String file=module.name+".gsc";
expected.add(file);
File target=new File(folder,file),temp=new File(folder,file+".tmp");
write(temp,module.source.getBytes(StandardCharsets.UTF_8));
if(target.exists())target.delete();
if(!temp.renameTo(target))throw new IOException("Không lưu được "+file);
}
File[] all=folder.listFiles();
if(all!=null)for(File file:all)if(file.getName().endsWith(".gsc")&&!expected.contains(file.getName()))file.delete();
}

 static byte[] read(File file,int limit)throws IOException{
try(InputStream in=new FileInputStream(file)){
return readStream(in,limit);
}
}

 static byte[] readStream(InputStream in,int limit)throws IOException{
ByteArrayOutputStream out=new ByteArrayOutputStream();
byte[] chunk=new byte[8192];
for(int n;
(n=in.read(chunk))!=-1;
){
out.write(chunk,0,n);
if(out.size()>limit)throw new IOException("Tệp vượt giới hạn "+limit+" byte");
}
return out.toByteArray();
}

 static void write(File f,byte[] data)throws IOException{
File parent=f.getParentFile();
if(parent!=null&&!parent.exists()&&!parent.mkdirs())throw new IOException("Không tạo được thư mục "+parent.getName());
try(FileOutputStream out=new FileOutputStream(f)){
out.write(data);
out.getFD().sync();
}
}

 Bitmap bitmap(String name){
try{
byte[] data=read(new File(dir,name),8*1024*1024);
return BitmapFactory.decodeByteArray(data,0,data.length);
}
catch(Exception e){
return null;
}
}

 void image(String name,byte[] data)throws Exception{
if(!Arrays.asList(ASSETS).contains(name))throw new IOException("Tên ảnh không hợp lệ");
validatePng(data,name,true);
File tmp=new File(dir,name+".tmp");
write(tmp,data);
File target=new File(dir,name);
if(target.exists()&&!target.delete())throw new IOException("Không thay được ảnh cũ");
if(!tmp.renameTo(target))throw new IOException("Không lưu được ảnh");
}

 private static void validatePng(byte[] data,String name,boolean atlasRules)throws IOException{
if(data.length>8*1024*1024)throw new IOException("PNG vượt 8 MB");
if(data.length<8||data[0]!=(byte)137||data[1]!=80||data[2]!=78||data[3]!=71)throw new IOException("Chỉ nhận ảnh PNG");
BitmapFactory.Options b=new BitmapFactory.Options();
b.inJustDecodeBounds=true;
BitmapFactory.decodeByteArray(data,0,data.length,b);
if(b.outWidth<1||b.outHeight<1||b.outWidth>4096||b.outHeight>4096||(long)b.outWidth*b.outHeight>8000000)throw new IOException("Ảnh PNG không hợp lệ hoặc quá lớn");
if(atlasRules&&(name.equals("tiles.png")||name.equals("tile-bank.png"))&&(b.outWidth%16!=0||b.outHeight%16!=0||(long)b.outWidth*b.outHeight>1048576))throw new IOException("Kho tile phải chia hết cho 16×16 pixel");
}

 Bitmap effectBitmap(String fileName){
if(fileName==null||!fileName.matches("effects/eff_[0-9]{1,5}\\.png"))return null;
try{
byte[] data=read(new File(dir,fileName),8*1024*1024);
return BitmapFactory.decodeByteArray(data,0,data.length);
}
catch(Exception e){
return null;
}
}

 boolean hasEffect(String fileName){
return fileName!=null&&fileName.matches("effects/eff_[0-9]{1,5}\\.png")&&new File(dir,fileName).isFile();
}

 void saveEffect(SkillEffect effect,byte[] data)throws Exception{
effect.validate();
validatePng(data,effect.fileName,false);
BitmapFactory.Options b=new BitmapFactory.Options();
b.inJustDecodeBounds=true;
BitmapFactory.decodeByteArray(data,0,data.length,b);
if(b.outWidth%effect.frameWidth!=0||b.outHeight%effect.frameHeight!=0)throw new IOException("Ảnh hiệu ứng phải chia đều theo kích thước frame "+effect.frameWidth+"×"+effect.frameHeight);
int capacity=(b.outWidth/effect.frameWidth)*(b.outHeight/effect.frameHeight);
if(effect.frameCount>capacity)throw new IOException("Số frame vượt ảnh: tối đa "+capacity);
File target=new File(dir,effect.fileName),tmp=new File(dir,effect.fileName+".tmp");
write(tmp,data);
if(target.exists()&&!target.delete())throw new IOException("Không thay được hiệu ứng cũ");
if(!tmp.renameTo(target))throw new IOException("Không lưu được hiệu ứng");
}

 void deleteEffect(SkillEffect effect){
if(effect!=null&&effect.fileName.matches("effects/eff_[0-9]{1,5}\\.png"))new File(dir,effect.fileName).delete();
}

 Bitmap characterBitmap(String fileName){
if(fileName==null||!fileName.matches("characters/player_[a-z0-9]{2,16}\\.png"))return null;
try{
byte[] data=read(new File(dir,fileName),8*1024*1024);
return BitmapFactory.decodeByteArray(data,0,data.length);
}
catch(Exception e){
return null;
}
}

 boolean hasCharacterAction(String fileName){
return fileName!=null&&fileName.matches("characters/player_[a-z0-9]{2,16}\\.png")&&new File(dir,fileName).isFile();
}

 void saveCharacterAction(CharacterAction action,byte[] data)throws Exception{
action.validate();
validatePng(data,action.fileName,false);
BitmapFactory.Options b=new BitmapFactory.Options();
b.inJustDecodeBounds=true;
BitmapFactory.decodeByteArray(data,0,data.length,b);
if(b.outWidth%action.frameWidth!=0||b.outHeight%action.frameHeight!=0)throw new IOException("Ảnh nhân vật phải chia đều theo frame "+action.frameWidth+"×"+action.frameHeight);
int capacity=(b.outWidth/action.frameWidth)*(b.outHeight/action.frameHeight);
if(action.frameCount>capacity)throw new IOException("Số frame động tác vượt ảnh: tối đa "+capacity);
File target=new File(dir,action.fileName),tmp=new File(dir,action.fileName+".tmp");
write(tmp,data);
if(target.exists()&&!target.delete())throw new IOException("Không thay được động tác nhân vật cũ");
if(!tmp.renameTo(target))throw new IOException("Không lưu được động tác nhân vật");
}

 void deleteCharacterAction(CharacterAction action){
if(action!=null&&action.fileName.matches("characters/player_[a-z0-9]{2,16}\\.png"))new File(dir,action.fileName).delete();
}

 void exportTo(OutputStream stream,GameProject project)throws Exception{
save(project);
ZipOutputStream zip=new ZipOutputStream(stream);
try{
add(zip,"project.json",read(new File(dir,"project.json"),1024*1024));
for(GameModule module:project.modules)add(zip,module.path(),module.source.getBytes(StandardCharsets.UTF_8));
for(String n:ASSETS){
File f=new File(dir,n);
if(f.isFile())add(zip,n,read(f,8*1024*1024));
}
for(SkillEffect effect:project.effects){
File f=new File(dir,effect.fileName);
if(f.isFile())add(zip,effect.fileName,read(f,8*1024*1024));
}
for(CharacterAction action:project.characterActions){
File f=new File(dir,action.fileName);
if(f.isFile())add(zip,action.fileName,read(f,8*1024*1024));
}
zip.finish();
}
finally{
zip.close();
}
}

 private static void add(ZipOutputStream zip,String name,byte[] data)throws IOException{
zip.putNextEntry(new ZipEntry(name));
zip.write(data);
zip.closeEntry();
}

 GameProject importFrom(InputStream stream)throws Exception{

  HashMap<String,byte[]> entries=new HashMap<>();
int total=0;
try(ZipInputStream zip=new ZipInputStream(stream)){
for(ZipEntry entry;
(entry=zip.getNextEntry())!=null;
){
String name=entry.getName();
boolean allowed=name.equals("project.json")||Arrays.asList(ASSETS).contains(name)||name.matches("src/game/[A-Za-z][A-Za-z0-9_]{0,39}\\.gsc")||name.matches("effects/eff_[0-9]{1,5}\\.png")||name.matches("characters/player_[a-z0-9]{2,16}\\.png");
if(entry.isDirectory()||!allowed||entries.containsKey(name))throw new IOException("Dự án chứa mục không hợp lệ: "+name);
byte[] data=readStream(zip,8*1024*1024);
total+=data.length;
if(total>50*1024*1024)throw new IOException("Dự án vượt 50 MB");
entries.put(name,data);
zip.closeEntry();
}
}

  if(!entries.containsKey("project.json"))throw new IOException("Thiếu project.json");
GameProject next=GameProject.fromJson(new String(entries.get("project.json"),StandardCharsets.UTF_8));
for(GameModule m:next.modules){
byte[] raw=entries.get(m.path());
if(raw!=null&&!Arrays.equals(raw,m.source.getBytes(StandardCharsets.UTF_8)))throw new IOException("Mã class trong ZIP khác project.json: "+m.path());
}
for(String name:entries.keySet())if(name.startsWith("src/game/")){
boolean found=false;
for(GameModule m:next.modules)if(m.path().equals(name))found=true;
if(!found)throw new IOException("File class không có trong project.json: "+name);
}

  for(String asset:ASSETS)if(entries.containsKey(asset))validatePng(entries.get(asset),asset,true);
for(SkillEffect e:next.effects){
byte[] raw=entries.get(e.fileName);
if(raw==null)throw new IOException("Thiếu file hiệu ứng: "+e.fileName);
validatePng(raw,e.fileName,false);
BitmapFactory.Options b=new BitmapFactory.Options();
b.inJustDecodeBounds=true;
BitmapFactory.decodeByteArray(raw,0,raw.length,b);
if(b.outWidth%e.frameWidth!=0||b.outHeight%e.frameHeight!=0||e.frameCount>(b.outWidth/e.frameWidth)*(b.outHeight/e.frameHeight))throw new IOException("Frame hiệu ứng không khớp: "+e.name);
}
for(CharacterAction a:next.characterActions){
byte[] raw=entries.get(a.fileName);
if(raw==null)throw new IOException("Thiếu file động tác nhân vật: "+a.fileName);
validatePng(raw,a.fileName,false);
BitmapFactory.Options b=new BitmapFactory.Options();
b.inJustDecodeBounds=true;
BitmapFactory.decodeByteArray(raw,0,raw.length,b);
if(b.outWidth%a.frameWidth!=0||b.outHeight%a.frameHeight!=0||a.frameCount>(b.outWidth/a.frameWidth)*(b.outHeight/a.frameHeight))throw new IOException("Frame nhân vật không khớp: "+a.name);
}

  File stage=new File(dir,"import-stage");
deleteTree(stage);
if(!stage.mkdirs())throw new IOException("Không tạo được vùng nhập dự án");
String[] targets={
"project.json","background.png","player.png","enemy.png","tiles.png","tile-bank.png","effects.png"}
;

  try{
write(new File(stage,"project.json"),next.json().toString(2).getBytes(StandardCharsets.UTF_8));
for(String asset:ASSETS)if(entries.containsKey(asset))write(new File(stage,asset),entries.get(asset));
File newEffects=new File(stage,"effects");
for(SkillEffect e:next.effects)write(new File(newEffects,new File(e.fileName).getName()),entries.get(e.fileName));
File newCharacters=new File(stage,"characters");
for(CharacterAction a:next.characterActions)write(new File(newCharacters,new File(a.fileName).getName()),entries.get(a.fileName));
File old=new File(dir,"import-backup");
deleteTree(old);
if(!old.mkdirs())throw new IOException("Không tạo được vùng sao lưu");
for(String target:targets){
File cur=new File(dir,target);
if(cur.exists()&&!cur.renameTo(new File(old,target)))throw new IOException("Không sao lưu được "+target);
}
File curEffects=new File(dir,"effects");
if(curEffects.exists()&&!curEffects.renameTo(new File(old,"effects")))throw new IOException("Không sao lưu được kho effect");
File curCharacters=new File(dir,"characters");
if(curCharacters.exists()&&!curCharacters.renameTo(new File(old,"characters")))throw new IOException("Không sao lưu được kho nhân vật");
boolean ok=false;
try{
for(String target:targets){
File incoming=new File(stage,target);
if(incoming.exists()&&!incoming.renameTo(new File(dir,target)))throw new IOException("Không ghi được "+target);
}
if(newEffects.exists()&&!newEffects.renameTo(new File(dir,"effects")))throw new IOException("Không ghi được kho effect");
if(newCharacters.exists()&&!newCharacters.renameTo(new File(dir,"characters")))throw new IOException("Không ghi được kho nhân vật");
ok=true;
}
finally{
if(!ok){
for(String target:targets){
new File(dir,target).delete();
File backup=new File(old,target);
if(backup.exists())backup.renameTo(new File(dir,target));
}
deleteTree(new File(dir,"effects"));
File backupEffects=new File(old,"effects");
if(backupEffects.exists())backupEffects.renameTo(new File(dir,"effects"));
deleteTree(new File(dir,"characters"));
File backupCharacters=new File(old,"characters");
if(backupCharacters.exists())backupCharacters.renameTo(new File(dir,"characters"));
}
deleteTree(old);
}
}

  finally{
deleteTree(stage);
}
try{
syncModules(next);
}
catch(IOException ignored){
}
return next;

 }

 void reset(GameProject p)throws Exception{
for(String asset:ASSETS)new File(dir,asset).delete();
deleteTree(new File(dir,"effects"));
deleteTree(new File(dir,"characters"));
save(p);
}

 static void deleteTree(File f){
if(f==null||!f.exists())return;
if(f.isDirectory()){
File[] parts=f.listFiles();
if(parts!=null)for(File c:parts)deleteTree(c);
}
f.delete();
}

}