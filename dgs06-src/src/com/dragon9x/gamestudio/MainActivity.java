package com.dragon9x.gamestudio;

import android.app.*;

import android.content.*;

import android.graphics.*;

import android.net.Uri;

import android.os.*;

import android.text.InputType;

import android.view.*;

import android.widget.*;

import java.io.*;

import java.net.URL;

import java.util.ArrayList;

public final class MainActivity extends Activity {

 static final int BACKGROUND=11,PLAYER=12,ENEMY=13,IMPORT=14,EXPORT=15,TILES=16,CROP_SOURCE=17,EFFECTS=18,EFFECT_DESIGN_SOURCE=19;

 ProjectStore store;
GameProject project;
Bitmap background,playerImage,enemyImage,tileImage,effectImage;
StageView editorStage,playStage;
TilePaletteView tilePalette;
FrameLayout pages;
LinearLayout projectPage,layoutPage,codePage,playPage,skillsPage,effectsPage,filesPage;
EditText name,width,height,maxHp,maxMp,speed,gravity,flyCost,damage,range,cooldown,enemyHp,enemyDamage,code,prompt,tileId;
Spinner movement,attackMode,gameMode;
TextView status;
AlertDialog activeAiDialog;
int activePage,pendingAsset;

 int dp(float v){
return (int)(v*getResources().getDisplayMetrics().density+.5f);
}

 @Override public void onCreate(Bundle state){
super.onCreate(state);
store=new ProjectStore(this);
boolean existing=store.hasProject();
project=store.load();
build();
refreshProject();
if(state==null)new android.os.Handler().post(existing?this::chooseStartup:this::chooseMode);
}

 void chooseStartup(){
new AlertDialog.Builder(this).setTitle("Chọn game khi mở xưởng").setItems(new String[]{
"Tiếp tục: "+project.name+" ("+project.mode+")","Tạo game 2D mới","Tạo game 2,5D mới"}
,(d,which)->{
if(which>0)confirmNewMode(which==1?"2D":"2.5D");
}
).show();
}

 void confirmNewMode(String mode){
new AlertDialog.Builder(this).setTitle("Tạo game "+mode+"?").setMessage("Dự án đang mở sẽ được thay. Xuất dự án hiện tại trước nếu anh muốn giữ bản sao.").setNegativeButton("Hủy",null).setPositiveButton("Tạo mới",(d,w)->createMode(mode)).show();
}

 void createMode(String mode){
try{
project=new GameProject();
project.mode=mode;
if(mode.equals("2.5D")){
project.joystickX=85;
project.joystickY=395;
project.attackX=710;
project.attackY=395;
}
store.reset(project);
refreshProject();
show(0);
}
catch(Exception e){
error(e);
}
}

 void chooseMode(){
new AlertDialog.Builder(this).setTitle("Chọn loại game mới").setItems(new String[]{
"2D • bản đồ tile nhìn từ trên", "2,5D • bố cục ảnh kiểu cũ"}
,(d,which)->createMode(which==0?"2D":"2.5D")).setNegativeButton("Để sau",null).show();
}

 TextView text(String s,int size,int color){
TextView t=new TextView(this);
t.setText(s);
t.setTextColor(color);
t.setTextSize(size);
t.setPadding(dp(8),dp(5),dp(8),dp(5));
return t;
}

 Button button(String s,View.OnClickListener click){
Button b=new Button(this);
b.setText(s);
b.setTextSize(11);
b.setAllCaps(false);
b.setOnClickListener(click);
return b;
}

 void row(LinearLayout parent,String label,View input){
LinearLayout line=new LinearLayout(this);
line.setGravity(Gravity.CENTER_VERTICAL);
TextView title=text(label,12,Color.DKGRAY);
line.addView(title,new LinearLayout.LayoutParams(dp(118),-2));
line.addView(input,new LinearLayout.LayoutParams(0,dp(45),1));
parent.addView(line);
}

 EditText number(LinearLayout parent,String label){
EditText edit=new EditText(this);
edit.setSingleLine(true);
edit.setInputType(InputType.TYPE_CLASS_NUMBER);
row(parent,label,edit);
return edit;
}

 void build(){
LinearLayout root=new LinearLayout(this);
root.setOrientation(LinearLayout.VERTICAL);
root.setBackgroundColor(0xffeef3f2);

  TextView header=text("DRAGON GAME STUDIO 0.6 • MAP + SKILL DESIGNER",17,0xff087362);
header.setTypeface(null,1);
root.addView(header);

  HorizontalScrollView tabs=new HorizontalScrollView(this);
LinearLayout bar=new LinearLayout(this);
String[] labels={
"Dự án","Map + kho tile","Code / AI","Chạy thử","Skill","Thiết kế skill","Mã nguồn"}
;
for(int i=0;
i<labels.length;
i++){
final int page=i;
bar.addView(button(labels[i],v->show(page)));
}
tabs.addView(bar);
root.addView(tabs);

  pages=new FrameLayout(this);
root.addView(pages,new LinearLayout.LayoutParams(-1,0,1));

  projectPage=new LinearLayout(this);
projectPage.setOrientation(LinearLayout.VERTICAL);
ScrollView projectScroll=new ScrollView(this);
projectScroll.addView(projectPage);
pages.addView(projectScroll);
buildProject();

  layoutPage=new LinearLayout(this);
layoutPage.setOrientation(LinearLayout.VERTICAL);
pages.addView(layoutPage);
buildLayout();

  codePage=new LinearLayout(this);
codePage.setOrientation(LinearLayout.VERTICAL);
pages.addView(codePage);
buildCode();

  playPage=new LinearLayout(this);
playPage.setOrientation(LinearLayout.VERTICAL);
pages.addView(playPage);
buildPlay();
skillsPage=new LinearLayout(this);
skillsPage.setOrientation(LinearLayout.VERTICAL);
ScrollView skillScroll=new ScrollView(this);
skillScroll.addView(skillsPage);
pages.addView(skillScroll);
effectsPage=new LinearLayout(this);
effectsPage.setOrientation(LinearLayout.VERTICAL);
ScrollView effectScroll=new ScrollView(this);
effectScroll.addView(effectsPage);
pages.addView(effectScroll);
filesPage=new LinearLayout(this);
filesPage.setOrientation(LinearLayout.VERTICAL);
ScrollView fileScroll=new ScrollView(this);
fileScroll.addView(filesPage);
pages.addView(fileScroll);

  status=text("Dự án lưu trên điện thoại • không cần JAR",11,0xff244237);
status.setSingleLine(true);
root.addView(status);
setContentView(root);
show(0);

 }

 void buildProject(){
projectPage.addView(text("Anh đặt tên, chọn bố cục và thông số. Ảnh do anh tự vẽ sẽ nhập ở tab Bố cục.",12,Color.DKGRAY));

 name=new EditText(this);
name.setSingleLine(true);
row(projectPage,"Tên game",name);

  gameMode=new Spinner(this);
ArrayAdapter<String> modes=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,new String[]{
"2D • tile map","2,5D • ảnh nền"}
);
modes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
gameMode.setAdapter(modes);
row(projectPage,"Loại bản đồ",gameMode);

  width=number(projectPage,"Rộng map");
height=number(projectPage,"Cao map");

  movement=new Spinner(this);
String[] types={
"8 hướng","4 hướng"}
;
ArrayAdapter<String> moves=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,types);
moves.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
movement.setAdapter(moves);
row(projectPage,"Di chuyển",movement);

  attackMode=new Spinner(this);
String[] actions={
"Chạm","Giữ"}
;
ArrayAdapter<String> attacks=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,actions);
attacks.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
attackMode.setAdapter(attacks);
row(projectPage,"Ra đòn",attackMode);

  maxHp=number(projectPage,"HP nhân vật");
maxMp=number(projectPage,"MP nhân vật");
speed=number(projectPage,"Tốc độ px/s");
gravity=number(projectPage,"Trọng lực");
flyCost=number(projectPage,"MP bay / giây");
damage=number(projectPage,"Sát thương");
range=number(projectPage,"Tầm đánh px");
cooldown=number(projectPage,"Hồi chiêu ms");
enemyHp=number(projectPage,"HP quái");
enemyDamage=number(projectPage,"Quái đánh");

  projectPage.addView(button("Lưu thiết kế",v->saveForm()));
projectPage.addView(button("Xuất dự án (.dgproject)",v->exportProject()));
projectPage.addView(button("Nhập dự án",v->pick(IMPORT,"application/zip")));

  projectPage.addView(button("Dự án mới • chọn 2D / 2,5D",v->new AlertDialog.Builder(this).setTitle("Thay dự án hiện tại?").setMessage("Dự án hiện tại sẽ được thay trong app. Hãy xuất dự án nếu cần giữ bản sao.").setNegativeButton("Hủy",null).setPositiveButton("Chọn loại game",(d,w)->chooseMode()).show()));

 }

 void buildLayout(){
layoutPage.addView(text("Map 2D bản 0.6: kho tile nằm riêng ở bên trái, canvas map ở bên phải. Cắt tile xong sẽ lưu vào file tile-bank.png và xuất cùng dự án.",12,Color.DKGRAY));

  HorizontalScrollView toolScroll=new HorizontalScrollView(this);
LinearLayout tools=new LinearLayout(this);
tools.addView(button("Đặt nhân vật",v->{
editorStage.editTool=0;
status.setText("Đang đặt nhân vật");
}
));
tools.addView(button("Đặt quái",v->{
editorStage.editTool=1;
status.setText("Đang đặt quái");
}
));
tools.addView(button("Đặt nút đi",v->{
editorStage.editTool=2;
status.setText("Đang đặt nút di chuyển");
}
));
tools.addView(button("Đặt nút đánh",v->{
editorStage.editTool=3;
status.setText("Đang đặt nút đánh");
}
));
tools.addView(button("Tô tile",v->{
editorStage.editTool=4;
setTileBrush();
}
));
tools.addView(button("Ô cản",v->{
editorStage.editTool=5;
status.setText("Kéo để thêm ô cản");
}
));
tools.addView(button("Xóa ô",v->{
editorStage.editTool=6;
status.setText("Kéo để xóa tile và ô cản");
}
));
toolScroll.addView(tools);
layoutPage.addView(toolScroll);

  LinearLayout mapRow=new LinearLayout(this);
tileId=new EditText(this);
tileId.setInputType(InputType.TYPE_CLASS_NUMBER);
tileId.setSingleLine(true);
tileId.setText("1");
tileId.setHint("ID tile");
mapRow.addView(tileId,new LinearLayout.LayoutParams(0,dp(45),1));
mapRow.addView(button("◀",v->editorStage.pan(-8,0)));
mapRow.addView(button("▲",v->editorStage.pan(0,-8)));
mapRow.addView(button("▼",v->editorStage.pan(0,8)));
mapRow.addView(button("▶",v->editorStage.pan(8,0)));
layoutPage.addView(mapRow);

  HorizontalScrollView assetScroll=new HorizontalScrollView(this);
LinearLayout imageRow=new LinearLayout(this);
imageRow.addView(button("Cắt tile vào kho",v->pick(CROP_SOURCE,"image/png")));
imageRow.addView(button("Nhập kho tile PNG",v->pick(TILES,"image/png")));
imageRow.addView(button("Ảnh nền",v->pick(BACKGROUND,"image/png")));
imageRow.addView(button("Nhân vật",v->pick(PLAYER,"image/png")));
imageRow.addView(button("Quái",v->pick(ENEMY,"image/png")));
assetScroll.addView(imageRow);
layoutPage.addView(assetScroll);

  LinearLayout workspace=new LinearLayout(this);
workspace.setOrientation(LinearLayout.HORIZONTAL);
LinearLayout bank=new LinearLayout(this);
bank.setOrientation(LinearLayout.VERTICAL);
bank.setBackgroundColor(0xffdfe9e7);
TextView bankTitle=text("KHO TILE",12,0xff087362);
bankTitle.setGravity(Gravity.CENTER);
bank.addView(bankTitle);
tilePalette=new TilePaletteView(this);
tilePalette.setListener(id->{
tileId.setText(""+id);
editorStage.tileBrush=id;
editorStage.brushIds=null;
editorStage.editTool=4;
status.setText("Đã chọn tile #"+id+" trong kho");
}
);
bank.addView(tilePalette,new LinearLayout.LayoutParams(-1,0,1));
workspace.addView(bank,new LinearLayout.LayoutParams(dp(132),-1));
editorStage=new StageView(this);
workspace.addView(editorStage,new LinearLayout.LayoutParams(0,-1,1));
layoutPage.addView(workspace,new LinearLayout.LayoutParams(-1,0,1));

  layoutPage.addView(button("Chạy thử map này",v->show(3)));

 }

 void setTileBrush(){
try{
int id=Integer.parseInt(tileId.getText().toString());
if(id<0||id>255)throw new IllegalArgumentException("ID tile phải từ 0 đến 255");
editorStage.tileBrush=id;
editorStage.brushIds=null;
if(tilePalette!=null)tilePalette.setSelected(id);
status.setText("Kéo để tô tile số "+id);
}
catch(Exception e){
error(e);
editorStage.editTool=0;
}
}

 void showMenus(){
ArrayList<GameModule> menus=new ArrayList<>();
for(GameModule m:project.modules)if(!m.menuLabel().isEmpty()||!m.screenTitle().isEmpty())menus.add(m);
if(menus.isEmpty()){
toast("Chưa có menu. Vào Mã nguồn → Thêm class menu.");
return;
}
String[] labels=new String[menus.size()];
for(int i=0;
i<menus.size();
i++)labels[i]=menus.get(i).menuLabel().isEmpty()?menus.get(i).screenTitle():menus.get(i).menuLabel();
new AlertDialog.Builder(this).setTitle("Menu • "+project.name).setItems(labels,(d,which)->{
GameModule selected=menus.get(which);
if(selected.screenTitle().isEmpty())playStage.triggerMenu(selected);
else new AlertDialog.Builder(this).setTitle(selected.screenTitle()).setMessage(selected.screenText().isEmpty()?"Màn hình game":selected.screenText()).setNegativeButton("Đóng",null).setPositiveButton("Chạy hành động",(dd,w)->playStage.triggerMenu(selected)).show();
}
).setNegativeButton("Đóng",null).show();
}

 void refreshFiles(){
if(filesPage==null)return;
filesPage.removeAllViews();
filesPage.addView(text("CÂY DỰ ÁN • "+project.name,15,0xff087362));
filesPage.addView(button("/project.json • xem dữ liệu map, skill và luật",v->showProjectJson()));
filesPage.addView(text("/src/game/ • class script .gsc có thể chạy thử",12,Color.DKGRAY));
for(GameModule module:project.modules)filesPage.addView(button("  "+module.toString(),v->editModule(module)));

  filesPage.addView(text("ẢNH DỰ ÁN • tile-bank.png là kho tile riêng",12,Color.DKGRAY));
for(String asset:ProjectStore.ASSETS)filesPage.addView(text("  /"+asset+(store.hasAsset(asset)?" ✓":" (chưa nhập)"),11,Color.DKGRAY));
filesPage.addView(text("/effects/ • kho hiệu ứng skill không giới hạn 16×16",12,Color.DKGRAY));
for(SkillEffect e:project.effects)filesPage.addView(text("  /"+e.fileName+(store.hasEffect(e.fileName)?" ✓":" (thiếu)")+" • "+e.frameWidth+"×"+e.frameHeight+" • "+e.frameCount+" frame",11,Color.DKGRAY));

  filesPage.addView(text("/engine/ • mã Java của xưởng, chỉ xem; đóng trong APK",12,Color.DKGRAY));
String[] sources={
"GameProject.java","StageView.java","GameRules.java","GameModule.java","Skill.java","SkillEffect.java","TilePaletteView.java","ImageCropView.java","EffectPreviewView.java","AiClient.java","MainActivity.java"}
;
for(String file:sources)filesPage.addView(button("  engine/"+file,v->showEngineSource(file)));
filesPage.addView(button("  HƯỚNG DẪN ENGINE GSC",v->showGuide()));

  filesPage.addView(text("/server/ • chưa tạo; bản chạy thử hiện ở điện thoại",11,0xff996020));
filesPage.addView(button("+ Thêm class luật / client / menu",v->newModule()));
filesPage.addView(button("AI viết class vào dự án",v->generateModule()));
filesPage.addView(text("GSC 0.6 hỗ trợ if/then/else, and/or, HP/MP, vật phẩm, biến, trạng thái, trọng lực, nhảy/bay, chiến đấu và nhiều sự kiện mới. Bấm HƯỚNG DẪN ENGINE GSC để xem cú pháp đầy đủ.",12,Color.DKGRAY));
}

 void showEngineSource(String file){
try(InputStream in=getAssets().open("com/dragon9x/gamestudio/"+file)){
String content=new String(ProjectStore.readStream(in,120000),java.nio.charset.StandardCharsets.UTF_8);
TextView view=text(content,11,Color.DKGRAY);
view.setTypeface(android.graphics.Typeface.MONOSPACE);
ScrollView scroll=new ScrollView(this);
scroll.addView(view);
new AlertDialog.Builder(this).setTitle("engine/"+file+" • chỉ đọc").setView(scroll).setPositiveButton("Đóng",null).show();
}
catch(Exception e){
error(e);
}
}

 void showGuide(){
try(InputStream in=getAssets().open("guide/GSC_ENGINE_GUIDE.txt")){
String content=new String(ProjectStore.readStream(in,200000),java.nio.charset.StandardCharsets.UTF_8);
TextView view=text(content,11,Color.DKGRAY);
view.setTypeface(android.graphics.Typeface.MONOSPACE);
ScrollView scroll=new ScrollView(this);
scroll.addView(view);
new AlertDialog.Builder(this).setTitle("Hướng dẫn Engine GSC 0.6").setView(scroll).setPositiveButton("Đóng",null).show();
}
catch(Exception e){
error(e);
}
}

 void showProjectJson(){
try{
org.json.JSONObject view=project.json();
view.put("tiles","(đã lược mảng tile; xem đủ trong file xuất)");
view.put("solid","(đã lược mảng ô cản; xem đủ trong file xuất)");
TextView content=text(view.toString(2),11,Color.DKGRAY);
content.setTypeface(android.graphics.Typeface.MONOSPACE);
ScrollView scroll=new ScrollView(this);
scroll.addView(content);
new AlertDialog.Builder(this).setTitle("project.json • cấu hình dự án").setView(scroll).setPositiveButton("Đóng",null).show();
}
catch(Exception e){
error(e);
}
}

 void newModule(){
String[] options={
"Class logic game","Class menu client","Class màn hình client"}
;
new AlertDialog.Builder(this).setTitle("Tạo class mới").setItems(options,(d,which)->{
int n=1;
boolean exists;
do{
exists=false;
for(GameModule m:project.modules)if(m.name.equals("GameClass"+n))exists=true;
if(exists)n++;
}
while(exists);
String name="GameClass"+n,template="class "+name+"\n"+(which==1?"menu Gặp gỡ\non_menu: message Xin chào từ menu mới":which==2?"menu Mở bảng\nscreen Bảng thông tin\ntext Xin chào! Anh có thể sửa nội dung này.\non_menu: message Đã mở màn hình":"on_start: message Class mới đã chạy");
editModule(new GameModule(name,template));
}
).show();
}

 void editModule(GameModule original){
LinearLayout form=new LinearLayout(this);
form.setOrientation(LinearLayout.VERTICAL);
form.addView(text("File: "+original.path()+" • đổi tên class cần sửa cả dòng đầu.",12,Color.DKGRAY));
EditText className=new EditText(this);
className.setSingleLine(true);
className.setText(original.name);
row(form,"Tên class",className);
EditText source=new EditText(this);
source.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
source.setTypeface(android.graphics.Typeface.MONOSPACE);
source.setTextSize(12);
source.setText(original.source);
source.setMinLines(10);
form.addView(source);
ScrollView scroll=new ScrollView(this);
scroll.addView(form);
AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Sửa "+original.path()).setView(scroll).setNegativeButton("Đóng",null).setNeutralButton("Xóa class",null).setPositiveButton("Kiểm tra + thêm vào game",null).create();
dialog.show();
dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
try{
GameModule candidate=new GameModule(className.getText().toString().trim(),source.getText().toString());
applyModule(candidate,original.name);
dialog.dismiss();
}
catch(Exception e){
error(e);
}
}
);
dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{
boolean exists=false;
for(GameModule m:project.modules)if(m.name.equals(original.name))exists=true;
if(!exists){
toast("Class chưa được thêm vào dự án");
return;
}
new AlertDialog.Builder(this).setTitle("Xóa "+original.path()+"?").setNegativeButton("Hủy",null).setPositiveButton("Xóa",(d,w)->{
try{
GameProject next=GameProject.fromJson(project.json().toString());
next.modules.removeIf(m->m.name.equals(original.name));
store.save(next);
project=next;
refreshFiles();
dialog.dismiss();
}
catch(Exception e){
error(e);
}
}
).show();
}
);
}

 void applyModule(GameModule candidate,String previous)throws Exception{
GameProject next=GameProject.fromJson(project.json().toString());
next.modules.removeIf(m->m.name.equals(previous));
next.modules.add(candidate);
next.validate();
store.save(next);
project=next;
reloadImages();
refreshFiles();
status.setText("Đã thêm " +candidate.path()+" vào game hiện tại");
}

 EditText cropNumber(LinearLayout panel,String label,int initial){
EditText e=new EditText(this);
e.setSingleLine(true);
e.setInputType(InputType.TYPE_CLASS_NUMBER);
e.setText(""+initial);
row(panel,label,e);
return e;
}

 void cropDialog(Bitmap source){
MapCropView view=new MapCropView(this,source);
LinearLayout box=new LinearLayout(this);
box.setOrientation(LinearLayout.VERTICAL);
box.addView(text("Chọn vùng, kéo để mở rộng. Đơn vị là bội số của 16×16 px.",12,Color.DKGRAY));
box.addView(view,new LinearLayout.LayoutParams(-1,dp(300)));

  LinearLayout actions=new LinearLayout(this);
actions.addView(button("Phóng +",v->view.zoom(1.5f)));
actions.addView(button("Thu −",v->view.zoom(1/1.5f)));
box.addView(actions);

  CheckBox snap=new CheckBox(this);
snap.setText("Bám lưới 16 px");
snap.setChecked(true);
snap.setOnCheckedChangeListener((v,checked)->view.setSnap(checked));
box.addView(snap);

  CheckBox ratio=new CheckBox(this);
ratio.setText("Khóa tỉ lệ theo khối chọn");
ratio.setChecked(false);
ratio.setOnCheckedChangeListener((v,checked)->{
view.lockRatio=checked;
}
);
box.addView(ratio);

  CheckBox move=new CheckBox(this);
move.setText("Kéo để di chuyển vùng cắt");
move.setOnCheckedChangeListener((v,checked)->view.selectMode=!checked);
box.addView(move);

  EditText cols=cropNumber(box,"Rộng ×16",1),rows=cropNumber(box,"Cao ×16",1);
box.addView(button("Áp dụng kích thước khối",v->{
try{
view.setUnit(Integer.parseInt(cols.getText().toString()),Integer.parseInt(rows.getText().toString()));
ratio.setChecked(true);
}
catch(Exception e){
error(e);
}
}
));

  EditText replace=cropNumber(box,"ID thay (tùy chọn)",0);
replace.setText("");
replace.setHint("Trống = thêm; số = thay từ ID đó");
ScrollView scroll=new ScrollView(this);
scroll.addView(box);

  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Cắt tile → kho riêng • 0.6").setView(scroll).setNegativeButton("Hủy",null).setPositiveButton("Lưu vào kho + chọn",null).create();
dialog.show();
dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
try{

   int replaceId=replace.getText().toString().trim().isEmpty()?-1:Integer.parseInt(replace.getText().toString().trim());
TileLibrary.Result result=TileLibrary.importRegion(tileImage,source,view.cropX,view.cropY,view.cropColumns,view.cropRows,replaceId);
store.image("tile-bank.png",TileLibrary.png(result.atlas));
reloadImages();
editorStage.brushIds=result.ids;
editorStage.brushColumns=view.cropColumns;
editorStage.tileBrush=result.ids[0];
editorStage.editTool=4;
tileId.setText(""+result.ids[0]);
if(tilePalette!=null)tilePalette.setSelected(result.ids[0]);
status.setText("Đã lưu "+result.added+" tile mới vào tile-bank.png • chọn bên trái rồi vẽ bên phải");
dialog.dismiss();

  }
catch(Exception e){
error(e);
}
}
);

 }

 void refreshSkills(){
if(skillsPage==null)return;
skillsPage.removeAllViews();
skillsPage.addView(text("Danh sách skill 0.6. Mỗi skill có thể gán một hiệu ứng đã tạo ở tab Thiết kế skill; frame không còn bị khóa 16×16.",12,Color.DKGRAY));
skillsPage.addView(button("+ Thêm skill",v->{
if(project.skills.size()>=100){
toast("Tối đa 100 skill");
return;
}
Skill s=new Skill();
int id=1;
for(Skill item:project.skills)id=Math.max(id,item.id+1);
s.id=id;
s.name="Chiêu "+id;
editSkill(s,true);
}
));
for(Skill s:project.skills){
LinearLayout card=new LinearLayout(this);
card.setOrientation(LinearLayout.VERTICAL);
SkillEffect eff=project.effectById(s.effectId);
card.addView(text((s.id==project.activeSkillId?"▶ ":"")+s.toString()+" • "+(eff==null?"không gán eff":"eff: "+eff.name),11,0xff244237));
LinearLayout line=new LinearLayout(this);
line.addView(button("Sửa / gán eff",v->editSkill(s,false)));
line.addView(button("Test",v->{
try{
project.activeSkillId=s.id;
store.save(project);
reloadImages();
show(3);
}
catch(Exception e){
error(e);
}
}
));
line.addView(button("Xóa",v->deleteSkill(s)));
card.addView(line);
skillsPage.addView(card);
}
}

 void deleteSkill(Skill target){
if(project.skills.size()<2){
toast("Cần giữ ít nhất một skill");
return;
}
new AlertDialog.Builder(this).setTitle("Xóa skill "+target.name+"?").setNegativeButton("Hủy",null).setPositiveButton("Xóa",(d,w)->{
try{
GameProject next=GameProject.fromJson(project.json().toString());
next.skills.removeIf(s->s.id==target.id);
if(next.activeSkillId==target.id)next.activeSkillId=next.skills.get(0).id;
next.validate();
store.save(next);
project=next;
reloadImages();
refreshSkills();
}
catch(Exception e){
error(e);
}
}
).show();
}

 void editSkill(Skill original,boolean adding){
Skill s=Skill.parseUnchecked(original);
LinearLayout form=new LinearLayout(this);
form.setOrientation(LinearLayout.VERTICAL);
EditText id=cropNumber(form,"ID skill",s.id);
EditText skillName=new EditText(this);
skillName.setSingleLine(true);
skillName.setText(s.name);
row(form,"Tên skill",skillName);
EditText faction=new EditText(this);
faction.setSingleLine(true);
faction.setText(s.faction);
row(form,"Phái",faction);

  EditText dmg=cropNumber(form,"Sát thương",s.damage),rng=cropNumber(form,"Tầm đánh px",s.range),cd=cropNumber(form,"Hồi chiêu ms",s.cooldown),frameMs=cropNumber(form,"Tốc độ frame ms",s.frameMs);
Spinner effectPicker=new Spinner(this);
ArrayAdapter<String> effectAdapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item);
effectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
effectAdapter.add("Không gán hiệu ứng");
int selected=0;
for(int i=0;
i<project.effects.size();
i++){
SkillEffect e=project.effects.get(i);
effectAdapter.add("#"+e.id+" • "+e.name+" • "+e.frameWidth+"×"+e.frameHeight+" • "+e.frameCount+" frame");
if(e.id==s.effectId)selected=i+1;
}
effectPicker.setAdapter(effectAdapter);
effectPicker.setSelection(selected);
row(form,"Hiệu ứng",effectPicker);
form.addView(text("Hiệu ứng được tạo/chỉnh/cắt frame ở tab Thiết kế skill. Kích thước frame có thể là 24×32, 64×64, 128×96… không bắt buộc 16×16.",11,Color.DKGRAY));
ScrollView scroll=new ScrollView(this);
scroll.addView(form);

  AlertDialog dialog=new AlertDialog.Builder(this).setTitle(adding?"Thêm skill":"Chỉnh skill + gán hiệu ứng").setView(scroll).setNegativeButton("Hủy",null).setPositiveButton("Lưu skill",null).create();
dialog.show();
dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
try{
s.id=Integer.parseInt(id.getText().toString());
s.name=skillName.getText().toString().trim();
s.faction=faction.getText().toString().trim();
s.damage=Integer.parseInt(dmg.getText().toString());
s.range=Integer.parseInt(rng.getText().toString());
s.cooldown=Integer.parseInt(cd.getText().toString());
s.frameMs=Integer.parseInt(frameMs.getText().toString());
s.effectId=effectPicker.getSelectedItemPosition()==0?0:project.effects.get(effectPicker.getSelectedItemPosition()-1).id;
s.validate();
GameProject draft=GameProject.fromJson(project.json().toString());
if(!adding)draft.skills.removeIf(item->item.id==original.id);
draft.skills.add(s);
if(!adding&&draft.activeSkillId==original.id)draft.activeSkillId=s.id;
draft.validate();
project=draft;
store.save(project);
reloadImages();
refreshSkills();
dialog.dismiss();
}
catch(Exception e){
error(e);
}
}
);

 }

 void refreshEffects(){
if(effectsPage==null)return;
effectsPage.removeAllViews();
effectsPage.addView(text("THIẾT KẾ SKILL / EFFECT 0.6 • nhập ảnh → cắt tự do → thu/phóng → xoay/lật → khai báo frame → chạy thử. Không còn giới hạn frame 16×16.",12,Color.DKGRAY));
effectsPage.addView(button("+ Nhập ảnh để tạo hiệu ứng mới",v->pick(EFFECT_DESIGN_SOURCE,"image/png")));

  if(!project.skills.isEmpty()){
effectsPage.addView(text("GÁN HIỆU ỨNG CHO SKILL",13,0xff087362));
Spinner skills=new Spinner(this),effects=new Spinner(this);
ArrayAdapter<String> sa=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item);
sa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
for(Skill sk:project.skills)sa.add("#"+sk.id+" • "+sk.name);
skills.setAdapter(sa);
ArrayAdapter<String> ea=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item);
ea.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
ea.add("Không gán hiệu ứng");
for(SkillEffect e:project.effects)ea.add("#"+e.id+" • "+e.name);
effects.setAdapter(ea);
row(effectsPage,"Skill",skills);
row(effectsPage,"Hiệu ứng",effects);
effectsPage.addView(button("Gán skill ↔ hiệu ứng",v->{
try{
int si=skills.getSelectedItemPosition(),ei=effects.getSelectedItemPosition();
GameProject next=GameProject.fromJson(project.json().toString());
next.skills.get(si).effectId=ei==0?0:next.effects.get(ei-1).id;
next.validate();
project=next;
store.save(project);
reloadImages();
refreshEffects();
refreshSkills();
status.setText("Đã gán hiệu ứng cho "+project.skills.get(si).name);
}
catch(Exception ex){
error(ex);
}
}
));
}

  effectsPage.addView(text("KHO HIỆU ỨNG ĐÃ TẠO",13,0xff087362));
if(project.effects.isEmpty())effectsPage.addView(text("Chưa có hiệu ứng. Bấm nút nhập ảnh ở trên để tạo.",11,Color.DKGRAY));
for(SkillEffect e:project.effects){
LinearLayout card=new LinearLayout(this);
card.setOrientation(LinearLayout.VERTICAL);
card.addView(text(e.toString()+" • "+e.fileName+(store.hasEffect(e.fileName)?" ✓":" • thiếu file"),11,0xff244237));
LinearLayout actions=new LinearLayout(this);
actions.addView(button("Sửa",v->editEffect(e)));
actions.addView(button("▶ Frame",v->previewEffect(e)));
actions.addView(button("Xóa",v->deleteEffect(e)));
card.addView(actions);
effectsPage.addView(card);
}
}

 void editEffect(SkillEffect effect){
Bitmap bitmap=store.effectBitmap(effect.fileName);
if(bitmap==null){
toast("Không đọc được "+effect.fileName);
return;
}
effectDesignDialog(bitmap,effect);
}

 void previewEffect(SkillEffect effect){
Bitmap bitmap=store.effectBitmap(effect.fileName);
if(bitmap==null){
toast("Thiếu file hiệu ứng");
return;
}
LinearLayout body=new LinearLayout(this);
body.setOrientation(LinearLayout.VERTICAL);
body.addView(text(effect.toString(),12,Color.DKGRAY));
EffectPreviewView preview=new EffectPreviewView(this);
preview.setEffect(bitmap,effect.frameWidth,effect.frameHeight,effect.frameCount,effect.frameMs);
body.addView(preview,new LinearLayout.LayoutParams(-1,dp(280)));
AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Chạy thử frame • "+effect.name).setView(body).setNegativeButton("Đóng",null).setPositiveButton("Play / Pause",null).create();
dialog.setOnDismissListener(d->preview.stop());
dialog.show();
preview.start();
dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->preview.toggle());
}

 void deleteEffect(SkillEffect target){
new AlertDialog.Builder(this).setTitle("Xóa hiệu ứng "+target.name+"?").setMessage("Các skill đang gán hiệu ứng này sẽ được chuyển về không hiệu ứng.").setNegativeButton("Hủy",null).setPositiveButton("Xóa",(d,w)->{
try{
GameProject next=GameProject.fromJson(project.json().toString());
next.effects.removeIf(e->e.id==target.id);
for(Skill sk:next.skills)if(sk.effectId==target.id)sk.effectId=0;
next.validate();
store.deleteEffect(target);
store.save(next);
project=next;
reloadImages();
refreshEffects();
refreshSkills();
}
catch(Exception e){
error(e);
}
}
).show();
}

 void setCropBitmap(FrameLayout holder,ImageCropView[] ref,Bitmap bitmap){
holder.removeAllViews();
ref[0]=new ImageCropView(this,bitmap);
holder.addView(ref[0],new FrameLayout.LayoutParams(-1,-1));
}

 Bitmap rotate90(Bitmap src){
Matrix m=new Matrix();
m.postRotate(90);
return Bitmap.createBitmap(src,0,0,src.getWidth(),src.getHeight(),m,true);
}

 Bitmap flip(Bitmap src,boolean horizontal){
Matrix m=new Matrix();
m.setScale(horizontal?-1:1,horizontal?1:-1);
return Bitmap.createBitmap(src,0,0,src.getWidth(),src.getHeight(),m,true);
}

 int positive(EditText e,String label){
int v=Integer.parseInt(e.getText().toString());
if(v<1)throw new IllegalArgumentException(label+" phải > 0");
return v;
}

 void effectDesignDialog(Bitmap source,SkillEffect original){
if(source==null)return;
final Bitmap[] working={
source.copy(Bitmap.Config.ARGB_8888,true)}
;
final ImageCropView[] cropRef=new ImageCropView[1];
LinearLayout panel=new LinearLayout(this);
panel.setOrientation(LinearLayout.VERTICAL);
panel.addView(text("Kéo trên ảnh để chọn vùng cắt. Các thao tác xoay/lật/thu nhỏ được áp dụng trực tiếp vào ảnh đang thiết kế.",11,Color.DKGRAY));
SkillEffect base=original==null?new SkillEffect():SkillEffect.copy(original);
if(original==null){
int next=1;
for(SkillEffect e:project.effects)next=Math.max(next,e.id+1);
base.id=next;
base.name="Hiệu ứng "+next;
base.fileName="effects/eff_"+next+".png";
base.frameWidth=Math.min(64,source.getWidth());
base.frameHeight=Math.min(64,source.getHeight());
}

  EditText effectId=cropNumber(panel,"ID hiệu ứng",base.id);
EditText effectName=new EditText(this);
effectName.setSingleLine(true);
effectName.setText(base.name);
row(panel,"Tên hiệu ứng",effectName);
FrameLayout holder=new FrameLayout(this);
panel.addView(holder,new LinearLayout.LayoutParams(-1,dp(300)));
setCropBitmap(holder,cropRef,working[0]);

  LinearLayout cropTools=new LinearLayout(this);
cropTools.addView(button("Cắt vùng",v->{
try{
working[0]=cropRef[0].crop();
setCropBitmap(holder,cropRef,working[0]);
status.setText("Đã cắt ảnh skill còn "+working[0].getWidth()+"×"+working[0].getHeight());
}
catch(Exception e){
error(e);
}
}
));
cropTools.addView(button("To +",v->cropRef[0].zoom(1.4f)));
cropTools.addView(button("Nhỏ −",v->cropRef[0].zoom(1/1.4f)));
cropTools.addView(button("Chọn toàn ảnh",v->cropRef[0].resetCrop()));
panel.addView(cropTools);

  LinearLayout transform=new LinearLayout(this);
transform.addView(button("Xoay 90°",v->{
working[0]=rotate90(working[0]);
setCropBitmap(holder,cropRef,working[0]);
}
));
transform.addView(button("Lật ngang",v->{
working[0]=flip(working[0],true);
setCropBitmap(holder,cropRef,working[0]);
}
));
transform.addView(button("Lật dọc",v->{
working[0]=flip(working[0],false);
setCropBitmap(holder,cropRef,working[0]);
}
));
panel.addView(transform);

  LinearLayout resize=new LinearLayout(this);
EditText outW=new EditText(this),outH=new EditText(this);
outW.setInputType(InputType.TYPE_CLASS_NUMBER);
outH.setInputType(InputType.TYPE_CLASS_NUMBER);
outW.setText(""+working[0].getWidth());
outH.setText(""+working[0].getHeight());
outW.setHint("Rộng px");
outH.setHint("Cao px");
resize.addView(outW,new LinearLayout.LayoutParams(0,dp(45),1));
resize.addView(outH,new LinearLayout.LayoutParams(0,dp(45),1));
resize.addView(button("Thu/phóng",v->{
try{
int w=positive(outW,"Rộng"),h=positive(outH,"Cao");
if(w>4096||h>4096||(long)w*h>8000000)throw new IllegalArgumentException("Ảnh sau resize quá lớn");
working[0]=Bitmap.createScaledBitmap(working[0],w,h,true);
setCropBitmap(holder,cropRef,working[0]);
}
catch(Exception e){
error(e);
}
}
));
panel.addView(resize);

  EditText frameW=cropNumber(panel,"Rộng 1 frame px",base.frameWidth),frameH=cropNumber(panel,"Cao 1 frame px",base.frameHeight),frameCount=cropNumber(panel,"Số frame",base.frameCount),frameMs=cropNumber(panel,"Frame ms",base.frameMs);
EffectPreviewView preview=new EffectPreviewView(this);
panel.addView(preview,new LinearLayout.LayoutParams(-1,dp(240)));
panel.addView(button("Cắt frame + chạy thử",v->{
try{
int fw=positive(frameW,"Rộng frame"),fh=positive(frameH,"Cao frame"),count=positive(frameCount,"Số frame"),ms=positive(frameMs,"Frame ms");
if(working[0].getWidth()%fw!=0||working[0].getHeight()%fh!=0)throw new IllegalArgumentException("Ảnh "+working[0].getWidth()+"×"+working[0].getHeight()+" chưa chia đều cho frame "+fw+"×"+fh);
int cap=(working[0].getWidth()/fw)*(working[0].getHeight()/fh);
if(count>cap)throw new IllegalArgumentException("Ảnh chỉ có "+cap+" frame theo kích thước này");
preview.setEffect(working[0],fw,fh,count,ms);
preview.start();
}
catch(Exception e){
error(e);
}
}
));

  ScrollView scroll=new ScrollView(this);
scroll.addView(panel);
AlertDialog dialog=new AlertDialog.Builder(this).setTitle(original==null?"Tạo hiệu ứng skill":"Sửa hiệu ứng skill").setView(scroll).setNegativeButton("Hủy",null).setPositiveButton("Lưu hiệu ứng",null).create();
dialog.setOnDismissListener(d->preview.stop());
dialog.show();
dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
try{
SkillEffect e=new SkillEffect();
e.id=positive(effectId,"ID");
e.name=effectName.getText().toString().trim();
e.fileName="effects/eff_"+e.id+".png";
e.frameWidth=positive(frameW,"Rộng frame");