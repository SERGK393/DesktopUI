package ru.k3.desktopui;

import android.graphics.Rect;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.content.ComponentName;
import java.util.ArrayList;
import android.graphics.Color;
import android.content.Intent;
import android.util.Log;
import android.graphics.Typeface;

public class Obj{
	private static final String LOG_TAG="DesktopUI.Obj";
	
	private final Rect zone;
	private final IconCache icc;
	private final ComponentName comp;
	private final ArrayList<String> title;
	private final byte w,h,fh,typeint,lw,lb,sn;
	private final Paint txt,icp;
	private final ObjType type;
	private final Intent start;
	private int ar,ab;
	private boolean clicked;
	private boolean moved;
	public Obj(Rect zone,IconCache icc,int type,String title,String p1,String p2,int fh){
		Log.d(LOG_TAG,"Creating object "+title);
		this.zone=zone;
		w=(byte)(zone.right-zone.left);
		h=(byte)(zone.bottom-zone.top);
		this.fh=(byte)fh;
		this.icc=icc;
		txt=new Paint();
		txt.setAntiAlias(true);
		txt.setTextAlign(Paint.Align.CENTER);
//		txt.setShadowLayer(5,0,0,Color.BLACK);
		txt.setColor(Color.WHITE);
		txt.setTextSize(fh);
		icp=new Paint();
		icp.setAlpha(255);
		this.typeint=(byte)type;
		switch(typeint){
			case 1: this.type = ObjType.APP;
				this.comp = new ComponentName(p1, p2);
				start = new Intent(Intent.ACTION_MAIN);
				start.addCategory(Intent.CATEGORY_LAUNCHER);
				start.setComponent(comp);
				start.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
				break;
			default: this.type = ObjType.UNK;
				this.comp = null;
				this.start = null;
		}
		this.title=Utilities.partString(txt,title,w);
		sn=(byte)this.title.size();
		Rect tmp=new Rect();
		byte tlw=0,tlb=0;
		for(String tit:this.title){
		    txt.getTextBounds(tit,0,tit.length(),tmp);
		    if(tlw<tmp.right-w)tlw=(byte)(tmp.right-w);
			if(tlb<tmp.bottom)tlb=(byte)tmp.bottom;
		}
		lw=tlw; lb=tlb;
		correctAbsoluteCoords();
	}
	
	public void draw(Canvas c){
		c.drawBitmap(icc.getIcon(comp),zone.left,zone.top,icp);
		for(int i=0;i<title.size();i++){
		    c.drawText(title.get(i),zone.left+w/2,zone.bottom+fh+fh*i,txt);
		}
	}
	public boolean contains(int x,int y){
		return zone.contains(x,y);
	}
	public Intent run(){
		return start;
	}
	
	public void setPos(int _x,int _y){
		int x,y;
		x=(_x-lw/2)>0?_x:0;
		y=_y>0?_y:0;
		zone.set(x,y,x+w,y+h);
		correctAbsoluteCoords();
	}
	public void setClicked(boolean cl){
		clicked=cl;
//		txt.setUnderlineText(cl);
		if(cl){
			icp.setAlpha(128);
			txt.setAlpha(128);
		}else{
			icp.setAlpha(255);
			txt.setAlpha(255);
		}
	}
	public void setMoving(boolean mv){
		moved=mv;
		if(mv){
			icp.setAlpha(128);
			txt.setAlpha(128);
		}else{
			icp.setAlpha(255);
			txt.setAlpha(255);
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
	public ObjType getType(){
		return type;
	}
	
	public boolean isClicked(){
		return clicked;
	}
	public boolean isMoved(){
		return moved;
	}
	
	private void correctAbsoluteCoords(){
		ar=zone.right+lw/2;
		ab=zone.bottom+fh*sn+lb;
	}
	
	public enum ObjType{UNK,APP,FULL,SHORTCUT,SETTINGS};
}
