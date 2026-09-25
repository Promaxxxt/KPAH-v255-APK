package com.dragon9x.gamestudio;

import android.content.Context;

import android.graphics.*;

import android.view.View;

/** Small animation player for arbitrary-size skill effect frames. */
final class EffectPreviewView extends View {

 final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
Bitmap bitmap;
int frameWidth=1,frameHeight=1,frameCount=1,frameMs=80,frame;
boolean running;

 EffectPreviewView(Context c){
super(c);
paint.setFilterBitmap(false);
setBackgroundColor(0xff17212b);
}

 void setEffect(Bitmap b,int fw,int fh,int count,int ms){
bitmap=b;
frameWidth=Math.max(1,fw);
frameHeight=Math.max(1,fh);
frameCount=Math.max(1,count);
frameMs=Math.max(20,ms);
frame=0;
invalidate();
}

 void start(){
running=true;
frame=0;
invalidate();
}

 void stop(){
running=false;
invalidate();
}

 void toggle(){
if(running)stop();
else start();
}

 @Override protected void onDetachedFromWindow(){
running=false;
super.onDetachedFromWindow();
}

 @Override protected void onDraw(Canvas c){
super.onDraw(c);
if(bitmap==null)return;
int cols=Math.max(1,bitmap.getWidth()/frameWidth),capacity=cols*Math.max(1,bitmap.getHeight()/frameHeight),safeCount=Math.max(1,Math.min(frameCount,capacity));
frame%=safeCount;
int sx=(frame%cols)*frameWidth,sy=(frame/cols)*frameHeight;
if(sx+frameWidth>bitmap.getWidth()||sy+frameHeight>bitmap.getHeight())return;
float scale=Math.min((getWidth()-20f)/frameWidth,(getHeight()-20f)/frameHeight);
float w=frameWidth*scale,h=frameHeight*scale,l=(getWidth()-w)/2,t=(getHeight()-h)/2;
paint.setColor(Color.WHITE);
c.drawBitmap(bitmap,new Rect(sx,sy,sx+frameWidth,sy+frameHeight),new RectF(l,t,l+w,t+h),paint);
paint.setColor(Color.WHITE);
paint.setTextSize(12*getResources().getDisplayMetrics().density);
c.drawText((frame+1)+" / "+safeCount,8,getHeight()-8,paint);
if(running){
frame=(frame+1)%safeCount;
postInvalidateDelayed(frameMs);
}
}

}