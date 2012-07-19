package ru.k3.desktopui;

import android.graphics.Rect;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.text.TextPaint;
import android.content.ComponentName;
import java.util.ArrayList;
import android.graphics.Color;

public class Obj{
	private final Rect zone;
	private final IconCache icc;
	private final ComponentName comp;
	private final ArrayList<String> title;
	private final int w,h,fh;
	private final Paint txt,icp;
	private boolean clicked;
	private boolean moved;
	public Obj(Rect zone,IconCache icc,ComponentName comp,String title,int fh){
		this.zone=zone;
		w=zone.right-zone.left;
		h=zone.bottom-zone.top;
		this.fh=fh;
		this.icc=icc;
		this.comp=comp;
		txt=new Paint();
		txt.setTextAlign(Paint.Align.CENTER);
//		txt.setShadowLayer(5,0,0,Color.BLACK);
		txt.setColor(Color.WHITE);
		txt.setTextSize(fh);
		icp=new Paint();
		icp.setAlpha(255);
		this.title=Utilities.partString(txt,title,w);
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
	public void setPos(int _x,int _y){
		int x,y;
		x=_x>0?_x:0;
		y=_y>0?_y:0;
		zone.set(x,y,x+w,y+h);
	}
	public void setClicked(boolean cl){
		clicked=cl;
//		txt.setUnderlineText(cl);
		if(cl)icp.setAlpha(128);
		else icp.setAlpha(255);
	}
	public void setMoving(boolean mv){
		moved=mv;
	}
	public int getXPos(){
		return zone.left;
	}
	public int getYPos(){
		return zone.top;
	}
	public boolean isClicked(){
		return clicked;
	}
	public boolean isMoved(){
		return moved;
	}
}
