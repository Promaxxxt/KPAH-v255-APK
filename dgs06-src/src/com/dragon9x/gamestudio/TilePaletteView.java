package com.dragon9x.gamestudio;

import android.content.Context;

import android.graphics.*;

import android.view.*;

/** Scrollable tile bank shown beside the map canvas. */
final class TilePaletteView extends View {

 interface Listener{
void onTileSelected(int id);
}

 final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
Bitmap atlas;
Listener listener;
int selected=0;
float density,cell,scroll,downY,lastY;
boolean moved;

 TilePaletteView(Context c){
super(c);
density=getResources().getDisplayMetrics().density;
cell=44*density;
paint.setFilterBitmap(false);
setBackgroundColor(0xff24313a);
}

 void setListener(Listener l){
listener=l;
}

 void setAtlas(Bitmap b){
atlas=b;
selected=Math.max(0,Math.min(selected,count()-1));
scroll=0;
invalidate();
}

 void setSelected(int id){
if(id>=0&&id<count()){
selected=id;
invalidate();
}
}

 int count(){
return atlas==null?0:(atlas.getWidth()/16)*(atlas.getHeight()/16);
}

 int columns(){
return Math.max(1,(int)(getWidth()/cell));
}

 float maxScroll(){
int n=count(),cols=columns(),rows=(n+cols-1)/cols;
return Math.max(0,rows*cell-getHeight());
}

 @Override protected void onDraw(Canvas c){
super.onDraw(c);
if(atlas==null||count()==0){
paint.setColor(Color.LTGRAY);
paint.setTextSize(12*density);
c.drawText("Kho tile trống",8*density,24*density,paint);
return;
}
int atlasCols=atlas.getWidth()/16,cols=columns(),n=count();
paint.setTextSize(9*density);
for(int id=0;
id<n;
id++){
int col=id%cols,row=id/cols;
float l=col*cell,t=row*cell-scroll;
if(t+cell<0||t>getHeight())continue;
Rect src=new Rect((id%atlasCols)*16,(id/atlasCols)*16,(id%atlasCols+1)*16,(id/atlasCols+1)*16);
RectF dst=new RectF(l+4*density,t+4*density,l+cell-4*density,t+cell-4*density);
paint.setColor(0xffd9e3e8);
c.drawRect(dst,paint);
paint.setColor(Color.WHITE);
c.drawBitmap(atlas,src,dst,paint);
if(id==selected){
paint.setStyle(Paint.Style.STROKE);
paint.setStrokeWidth(3*density);
paint.setColor(0xffffc928);
c.drawRect(dst,paint);
paint.setStyle(Paint.Style.FILL);
}
paint.setColor(0xdd000000);
c.drawRect(l+2*density,t+2*density,l+22*density,t+14*density,paint);
paint.setColor(Color.WHITE);
c.drawText(""+id,l+4*density,t+12*density,paint);
}
}

 @Override public boolean onTouchEvent(MotionEvent e){
int a=e.getActionMasked();
if(a==MotionEvent.ACTION_DOWN){
downY=lastY=e.getY();
moved=false;
return true;
}
if(a==MotionEvent.ACTION_MOVE){
float dy=e.getY()-lastY;
if(Math.abs(e.getY()-downY)>6*density)moved=true;
scroll=Math.max(0,Math.min(maxScroll(),scroll-dy));
lastY=e.getY();
invalidate();
return true;
}
if(a==MotionEvent.ACTION_UP&&!moved&&count()>0){
int col=(int)(e.getX()/cell),row=(int)((e.getY()+scroll)/cell),id=row*columns()+col;
if(id>=0&&id<count()){
selected=id;
if(listener!=null)listener.onTileSelected(id);
invalidate();
}
return true;
}
return true;
}

}