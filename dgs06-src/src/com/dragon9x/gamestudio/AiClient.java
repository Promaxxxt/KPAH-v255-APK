package com.dragon9x.gamestudio;

import org.json.*;

import java.io.*;

import java.net.*;

import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;

/** One checked DSL draft transport; different providers use their own wire format. */
final class AiClient {

 static final String[] IDS={
"openai","gemini","claude","compatible1","compatible2"}
;

 static final String[] LABELS={
"OpenAI","Gemini","Claude","Tương thích OpenAI 1","Tương thích OpenAI 2"}
;

 static final String[] URLS={
"https://api.openai.com/v1/responses","https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent","https://api.anthropic.com/v1/messages","https://api.example.com/v1/chat/completions","https://api.example.com/v1/chat/completions"}
;

 static final String[] MODELS={
"gpt-4o-mini","gemini-2.5-flash","claude-sonnet-4-5","model-id","model-id"}
;

 static String generate(String provider,String endpoint,String model,String key,String request,GameProject project)throws Exception{

  return generateInternal(provider,endpoint,model,key,request,project,false);

 }

 static String generateModule(String provider,String endpoint,String model,String key,String request,GameProject project)throws Exception{

  return generateInternal(provider,endpoint,model,key,request,project,true);

 }

 private static String generateInternal(String provider,String endpoint,String model,String key,String request,GameProject project,boolean module)throws Exception{

  if(request==null||request.trim().isEmpty()||request.length()>2000)throw new IOException("Yêu cầu cần 1–2000 ký tự");

  if(key==null||key.trim().isEmpty())throw new IOException("Chưa nhập API key cho "+provider);

  if(model==null||!model.matches("[a-zA-Z0-9._/-]{1,80}"))throw new IOException("Tên model không hợp lệ");

  URL url=new URL(endpoint.replace("{model}",model));
if(!url.getProtocol().equals("https")||url.getUserInfo()!=null||url.getHost().isEmpty())throw new IOException("API URL phải dùng HTTPS");

  if(provider.startsWith("compatible")){
String path=url.getPath();
if(path.isEmpty()||path.equals("/"))url=new URL(url.toString().replaceAll("/$","")+"/v1/chat/completions");
else if(path.endsWith("/v1")||path.endsWith("/api/v1"))url=new URL(url.toString().replaceAll("/$","")+"/chat/completions");
}

  String dsl="GSC 0.6. Mỗi dòng dạng su_kien: lenh. Sự kiện: on_start,on_attack,on_hit,on_kill,on_menu,on_tick,on_move,on_jump,on_land,on_fly,on_mp_empty,on_death. "
   +"Có điều kiện một dòng: if A OP B then LENH hoặc if A OP B and/or C OP D then LENH else LENH; OP là == != > >= < <=. "
   +"Biến điều kiện: hp,max_hp,hp_percent,mp,max_mp,mp_percent,gold,exp,level,x,y,speed,enemy_hp,enemy_count,enemy_alive,flying,grounded,last_crit,attack_bonus,attack_multiplier,defense,damage_reduction,random,var:TEN,item:ID,status:TEN. "
   +"Lệnh: message TEXT; heal N; heal_percent N; damage N; damage_percent N; enemy_heal N; set_hp N; set_enemy_hp N; mp_add N; mp_add_percent N; mp_sub N; set_mp N; set_max_hp N; set_max_mp N; speed N; speed_add N; speed_sub N; spawn N; teleport X Y; gravity N; jump N; fly on|off|toggle; fly_cost N; gold_add N; gold_sub N; exp_add N; level_set N; level_add N; set TEN N; add TEN N; sub TEN N; random_set TEN MIN MAX; item_add ID QTY; item_remove ID QTY; item_clear; status_add TEN MS; status_remove TEN; status_clear; attack_bonus N; attack_multiplier N; crit_chance N; crit_damage N; lifesteal N; defense N; damage_reduction N; revive N. "
   +"Không dùng Java/import/shell/file/mạng/vòng lặp. Không bịa lệnh ngoài danh sách. ";

  String instructions=module?"Bạn tạo một class script cho game Android độc lập. CHỈ trả JSON hợp lệ đúng dạng {\"path\":\"src/game/TenClass.gsc\",\"source\":\"class TenClass\\nmenu Tên menu\\non_menu: message Xin chào\",\"explanation\":\"Mô tả ngắn\"}. Tên class chỉ dùng chữ ASCII, số, dấu gạch dưới; bắt đầu bằng chữ. menu/screen/text là tùy chọn. "+dsl+" Chỉ 1 file, tối đa 12000 ký tự. KHÔNG trả Markdown.":
   "Bạn hỗ trợ tạo logic cho game độc lập. Chỉ trả các dòng DSL, không Markdown, không JSON. "+dsl+" Tối đa 240 lệnh, 30000 ký tự.";

  JSONObject context=project.json();
context.remove("tiles");
context.remove("solid");
String input="Dự án hiện tại (không gửi mảng tile và ô cản):\n"+context.toString()+"\n\nYêu cầu của chủ game: "+request;

  JSONObject payload=new JSONObject();
if(provider.startsWith("openai")){
payload.put("model",model);
payload.put("instructions",instructions);
payload.put("input",input);
payload.put("max_output_tokens",module?3200:1800);
}

  else if(provider.startsWith("gemini")){
payload.put("systemInstruction",new JSONObject().put("parts",new JSONArray().put(new JSONObject().put("text",instructions))));
payload.put("contents",new JSONArray().put(new JSONObject().put("parts",new JSONArray().put(new JSONObject().put("text",input)))));
payload.put("generationConfig",new JSONObject().put("maxOutputTokens",module?3200:1800));
}

  else if(provider.startsWith("claude")){
payload.put("model",model);
payload.put("max_tokens",module?3200:1800);
payload.put("system",instructions);
payload.put("messages",new JSONArray().put(new JSONObject().put("role","user").put("content",input)));
}

  else if(provider.startsWith("compatible")){
payload.put("model",model);
payload.put("max_tokens",module?3200:1800);
payload.put("messages",new JSONArray().put(new JSONObject().put("role","system").put("content",instructions)).put(new JSONObject().put("role","user").put("content",input)));
}

  else throw new IOException("Nguồn API không hỗ trợ");

  HttpsURLConnection conn=(HttpsURLConnection)url.openConnection();
try{
conn.setInstanceFollowRedirects(false);
conn.setRequestMethod("POST");
conn.setRequestProperty("Content-Type","application/json");
if(provider.startsWith("gemini"))conn.setRequestProperty("x-goog-api-key",key);
else if(provider.startsWith("claude")){
conn.setRequestProperty("x-api-key",key);
conn.setRequestProperty("anthropic-version","2023-06-01");
}
else conn.setRequestProperty("Authorization","Bearer "+key);
conn.setConnectTimeout(20000);
conn.setReadTimeout(90000);
conn.setDoOutput(true);

   try(OutputStream out=conn.getOutputStream()){
out.write(payload.toString().getBytes(StandardCharsets.UTF_8));
}

   int status=conn.getResponseCode();
InputStream stream=status>=200&&status<300?conn.getInputStream():conn.getErrorStream();
if(stream==null)throw new IOException("API lỗi HTTP "+status);
byte[] bytes=ProjectStore.readStream(stream,256*1024);

   if(status<200||status>=300){
String detail=new String(bytes,StandardCharsets.UTF_8);
throw new IOException("API lỗi HTTP "+status+": "+detail.substring(0,Math.min(250,detail.length())));
}

   JSONObject response=new JSONObject(new String(bytes,StandardCharsets.UTF_8));
StringBuilder text=new StringBuilder();

   if(provider.startsWith("openai")){
JSONArray output=response.optJSONArray("output");
if(output!=null)for(int i=0;
i<output.length();
i++){
JSONObject item=output.optJSONObject(i);
if(item==null)continue;
JSONArray content=item.optJSONArray("content");
if(content!=null)for(int n=0;
n<content.length();
n++){
JSONObject part=content.optJSONObject(n);
if(part!=null&&part.optString("type").equals("output_text"))text.append(part.optString("text"));
}
}
}

   else if(provider.startsWith("gemini")){
JSONArray candidates=response.optJSONArray("candidates");
if(candidates!=null)for(int i=0;
i<candidates.length();
i++){
JSONObject c=candidates.optJSONObject(i);
if(c==null)continue;
JSONObject content=c.optJSONObject("content");
JSONArray parts=content==null?null:content.optJSONArray("parts");
if(parts!=null)for(int n=0;
n<parts.length();
n++){
JSONObject part=parts.optJSONObject(n);
if(part!=null)text.append(part.optString("text"));
}
}
}

   else if(provider.startsWith("claude")){
JSONArray parts=response.optJSONArray("content");
if(parts!=null)for(int i=0;
i<parts.length();
i++){
JSONObject part=parts.optJSONObject(i);
if(part!=null&&part.optString("type").equals("text"))text.append(part.optString("text"));
}
}

   else {
JSONArray choices=response.optJSONArray("choices");
JSONObject choice=choices==null?null:choices.optJSONObject(0),msg=choice==null?null:choice.optJSONObject("message");
if(msg!=null)text.append(msg.optString("content"));
}

   String draft=text.toString().trim();
if(draft.isEmpty())throw new IOException("API không trả DSL; kiểm tra model và quota");
if(draft.length()>30000)throw new IOException("AI trả code vượt giới hạn bản chạy thử");
return draft;

  }
finally{
conn.disconnect();
}

 }

 private AiClient(){
}

}