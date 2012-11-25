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

	private final Rect zone;
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
	private boolean clicked;
	private boolean moved;
	private boolean mov;
	public ObjItem(Rect zone,IconCache icc,int type,String title,String p1,String p2,int fh){
		Log.d(LOG_TAG,"Creating object "+title);
		wv=null;
		this.zone=zone;
		w=(short)(zone.right-zone.left);
		h=(short)(zone.bottom-zone.top);
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
				start.addCategory(Intent.CATEGORY_LAUNCHER);
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
	public ObjItem(Rect zone,AppWidgetHostView wv,int id){
		Log.d(LOG_TAG,"Creating widget object "+id);
		this.zone=zone;
		this.wv=wv;
		this.id=id;
		this.type=Obj.Type.APPWIDGET;
		w=(short)(zone.right-zone.left);
		h=(short)(zone.bottom-zone.top);
		icp=new Paint();
		icp.setColor(0x990000AA);

		wv.setVisibility(View.VISIBLE);
		int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY);
        int childheightMeasureSpec = MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY);
        wv.measure(childWidthMeasureSpec, childheightMeasureSpec);
		wv.layout(zone.left,zone.top,
		          zone.right,zone.bottom);

		icc=null;
		comp=null;
		title=null;
		txt=null;
		start=null;
		fh=lw=lb=sn=0;
	}

	public void draw(Canvas c){
		if(type!=Obj.Type.APPWIDGET){
		    c.drawBitmap(icc.getIcon(comp),zone.left,zone.top,icp);
		    for(int i=0;i<title.size();i++){
		        c.drawText(title.get(i),zone.left+w/2,zone.bottom+fh+fh*i,txt);
		    }
		}else {
			c.drawRect(zone,icp);
//			wv.draw(c);
		}
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

	public void setPos(int x,int y){
		if(!zone.contains(x+w/2,y+w/2)||mov){
		    zone.set(x,y,x+w,y+h);
		    correctAbsoluteCoords();
			mov=true;
		}
	}
	public void setWidgetTouch(MotionEvent ev){
		if(wv!=null)wv.onTouchEvent(ev);
	}
	public void setClicked(boolean cl){
		clicked=cl;
//		txt.setUnderlineText(cl);
		if(icp!=null&&txt!=null){
		    if(cl){
		    	icp.setAlpha(128);
		    	txt.setAlpha(128);
		    }else{
		    	icp.setAlpha(255);
		    	txt.setAlpha(255);
		    }
		}
	}
	public void setMoving(boolean mv){
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
		return type==Obj.Type.APPWIDGET;
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
}
