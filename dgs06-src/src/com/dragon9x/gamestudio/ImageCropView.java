package com.dragon9x.gamestudio;

import android.content.Context;

import android.graphics.*;

import android.view.*;

/** Free-pixel crop selector used by the skill/effect designer. */
final class ImageCropView extends View {

 final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
Bitmap image;
float scale,left,top,zoom=1,downX,downY;
int cropX,cropY,cropW,cropH;
boolean dragging;

 ImageCropView(Context c,Bitmap b){
super(c);
paint.setFilterBitmap(false);
setBackgroundColor(0xff1c2834);
setBitmap(b);
}

 void setBitmap(Bitmap b){
image=b;
cropX=0;
cropY=0;
cropW=b==null?1:b.getWidth();
cropH=b==null?1:b.getHeight();
zoom=1;
invalidate();
}

 void resetCrop(){
if(image!=null){
cropX=0;
cropY=0;
cropW=image.getWidth();
cropH=image.getHeight();
invalidate();
}
}

 void zoom(float f){
zoom=Math.max(0.5f,Math.min(8f,zoom*f));
invalidate();
}

 int px(float x){
return image==null?0:Math.max(0,Math.min(image.getWidth()-1,(int)((x-left)/scale)));
}

 int py(float y){
return image==null?0:Math.max(0,Math.min(image.getHeight()-1,(int)((y-top)/scale)));
}

 Bitmap crop(){
if(image==null)throw new IllegalStateException("Chưa có ảnh");
int w=Math.max(1,Math.min(cropW,image.getWidth()-cropX)),h=Math.max(1,Math.min(cropH,image.getHeight()-cropY));
return Bitmap.createBitmap(image,cropX,cropY,w,h);
}

 @Override protected void onDraw(Canvas c){
super.onDraw(c);
if(image==null)return;
float fit=Math.min((getWidth()-16f)/image.getWidth(),(getHeight()-16f)/image.getHeight());
scale=Math.max(0.01f,fit*zoom);
left=(getWidth()-image.getWidth()*scale)/2;
top=(getHeight()-image.getHeight()*scale)/2;
paint.setColor(Color.WHITE);
c.drawBitmap(image,null,new RectF(left,top,left+image.getWidth()*scale,top+image.getHeight()*scale),paint);
paint.setColor(0x55ffd038);
c.drawRect(left+cropX*scale,top+cropY*scale,left+(cropX+cropW)*scale,top+(cropY+cropH)*scale,paint);
paint.setStyle(Paint.Style.STROKE);
paint.setStrokeWidth(3);
paint.setColor(Color.YELLOW);
c.drawRect(left+cropX*scale,top+cropY*scale,left+(cropX+cropW)*scale,top+(cropY+cropH)*scale,paint);
paint.setStyle(Paint.Style.FILL);
}

 @Override public boolean onTouchEvent(MotionEvent e){
if(image==null||scale<=0)return true;
int a=e.getActionMasked();
if(a==MotionEvent.ACTION_DOWN){
downX=e.getX();
downY=e.getY();
cropX=px(downX);
cropY=py(downY);
cropW=cropH=1;
dragging=true;
invalidate();
return true;
}
if((a==MotionEvent.ACTION_MOVE||a==MotionEvent.ACTION_UP)&&dragging){
int x=px(e.getX()),y=py(e.getY()),sx=px(downX),sy=py(downY);
cropX=Math.min(sx,x);
cropY=Math.min(sy,y);
cropW=Math.max(1,Math.abs(x-sx)+1);
cropH=Math.max(1,Math.abs(y-sy)+1);
invalidate();
if(a==MotionEvent.ACTION_UP)dragging=false;
return true;
}
return true;
}

}