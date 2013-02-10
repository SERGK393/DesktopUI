package ru.k3.desktopui;

import android.graphics.Rect;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.content.ComponentName;
import java.util.ArrayList;
import android.graphics.Color;
import android.content.Intent;
import android.util.Log;
import android.appwidget.AppWidgetHostView;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;

public class ObjItem implements Obj{
	private static final String LOG_TAG="DesktopUI.Obj";

	private final Rect zone,minizone;
	private final AppWidgetHostView wv;
	private final IconCache icc;
	private final ComponentName comp;
	private final ArrayList<String> title;
	private final short w,h,fh,lw,lb,sn;
	private final int id;
	private final Paint txt,icp;
	private final Obj.Type type;
	private final Intent start;
	private int ar,ab;
	private boolean enabled;
	private boolean clicked;
	private boolean moved;
	private boolean mov;
	private int dbId;
	public ObjItem(Rect zone,IconCache icc,int type,String title,String p1,String p2,int fh){
		Log.d(LOG_TAG,"Creating object "+title);
		enabled=true;
		wv=null;
		this.zone=zone;
		w=(short)(zone.right-zone.left);
		h=(short)(zone.bottom-zone.top);
		this.minizone=new Rect();
		updateMinizone();
		this.fh=(short)fh;
		this.icc=icc;
		txt=new Paint();
		txt.setAntiAlias(true);
		txt.setTextAlign(Paint.Align.CENTER);
//		txt.setShadowLayer(5,0,0,Color.BLACK);
		txt.setColor(Color.WHITE);
		txt.setTextSize(fh);
		icp=new Paint();
		icp.setAlpha(255);
		this.id=(short)type;
		switch(id){
			case 1: this.type = Obj.Type.APP;
				this.comp = new ComponentName(p1, p2);
				start = new Intent(Intent.ACTION_MAIN);
//				start.addCategory(Intent.CATEGORY_LAUNCHER);
				start.setComponent(comp);
				start.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
				break;
			default: this.type = Obj.Type.UNK;
				this.comp = null;
				this.start = null;
		}
		this.title=Utilities.partString(txt,title,w);
		sn=(short)this.title.size();
		Rect tmp=new Rect();
		short tlw=0,tlb=0;
		for(String tit:this.title){
		    txt.getTextBounds(tit,0,tit.length(),tmp);
		    if(tlw<tmp.right-w)tlw=(short)(tmp.right-w);
			if(tlb<tmp.bottom)tlb=(short)tmp.bottom;
		}
		lw=tlw; lb=tlb;
		correctAbsoluteCoords();
	}

	public void draw(Canvas c){
		c.drawBitmap(icc.getIcon(comp,this),zone.left,zone.top,icp);
		for(int i=0;i<title.size();i++)
		    c.drawText(title.get(i),zone.left+w/2,zone.bottom+fh+fh*i,txt);
	}
	public boolean contains(int x,int y){
		return zone.contains(x,y);
	}
	public boolean contains(Rect r){
		boolean ret=r.contains(zone);
		if(ret)return ret;
		else return
				r.contains(zone.left,zone.top)||
				r.contains(zone.left,ab)||
				r.contains(ar,zone.top)||
				r.contains(ar,ab);
	}
	public ObjItem flush(){
		icc.remove(this.comp);
		Log.d(LOG_TAG,"flush():"+this.comp);
		return this;
	}
	public Intent run(){
		return start;
	}

	public ObjItem setDbId(int dbId){
		this.dbId=dbId;
		return this;
	}
	public void setPos(int x,int y){
		if(mov||!minizone.contains(x+w/2,y+w/2)){
		    zone.set(x,y,x+w,y+h);
		    correctAbsoluteCoords();
			updateMinizone();
			mov=true;
		}
	}
	public void setWidgetTouch(MotionEvent ev){
		if(wv!=null)wv.onTouchEvent(ev);
	}
	public void setEnabled(boolean e){
		enabled=e;
	}
	public void setClicked(boolean cl){
		if(enabled){
			clicked=cl;
//			txt.setUnderlineText(cl);
			if(icp!=null&&txt!=null){
		    	if(cl){
		    		icp.setAlpha(128);
		    		txt.setAlpha(128);
		    	}else{
		    		icp.setAlpha(255);
		    		txt.setAlpha(255);
		    	}
			}
		}else clicked=false;
	}
	public void setMoving(boolean mv){
		if(enabled){
			moved=mv;
			if(icp!=null&&txt!=null){
		    	if(mv){
		    		icp.setAlpha(128);
		    		txt.setAlpha(128);
		    	}else{
		    		icp.setAlpha(255);
		    		txt.setAlpha(255);
		    		mov=false;
		    	}
			}
		}else moved=mov=false;
	}

	public int getDbId(){
		return dbId;
	}
	public int getXPos(){
		return zone.left;
	}
	public int getYPos(){
		return zone.top;
	}
	public int getAbsoluteX2(){
		return ar;
	}
	public int getAbsoluteY2(){
		return ab;
	}
	public Obj.Type getType(){
		return type;
	}
	public String getPackage(){
		return comp.getPackageName();
	}
	public ComponentName getComponentName(){
		return comp;
	}

	public boolean isWidget(){
		return false;
	}
	public boolean isEnabled(){
		return enabled;
	}
	public boolean isClicked(){
		return clicked;
	}
	public boolean isMoved(){
		return moved;
	}
	public boolean ismov(){
		return mov;
	}

	private void correctAbsoluteCoords(){
		ar=zone.right+lw/2;
		ab=zone.bottom+fh*sn+lb;
	}
	private void updateMinizone(){
		minizone.set(zone.left+w/4,zone.top+h/4,zone.right-w/4,zone.bottom-h/4);
	}
}
