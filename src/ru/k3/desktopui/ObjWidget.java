package ru.k3.desktopui;

import android.content.Intent;
import android.graphics.Canvas;
import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.graphics.Rect;
import android.content.ComponentName;

public class ObjWidget extends AppWidgetHostView implements Obj
{
    
	private final Rect zone;
	private short w,h;
	
	public ObjWidget(Context c){
		super(c);
		zone=new Rect();
	}
	
	public void setLayoutParams(WidgetSpace.LayoutParams lp){
		super.setLayoutParams(lp);
		zone.set(lp.left,lp.top,lp.right,lp.bottom);
		w=(short)lp.width;
		h=(short)lp.height;
	}
	
	public void draw(Canvas c){
		super.draw(c);
	}

	public boolean contains(int x, int y){
		return zone.contains(x,y);
	}

	public boolean contains(Rect r){
		return true;
	}
	
	public Intent run(){
		
		return null;
	}

	public void setPos(int x, int y){
		zone.set(x,y,x+w,y+h);
		((WidgetSpace.LayoutParams)getLayoutParams()).setup(zone);
		//...
	}

	public void setClicked(boolean cl){
		
	}

	public void setMoving(boolean mv){
		
	}

	public int getXPos(){
		
		return 0;
	}

	public int getYPos(){
		
		return 0;
	}

	public int getAbsoluteX2(){
		
		return 0;
	}

	public int getAbsoluteY2(){
		
		return 0;
	}

	public Obj.Type getType(){
		
		return null;
	}

	public String getPackage(){
		
		return null;
	}

	public ComponentName getComponentName(){
		
		return null;
	}
	
	public boolean isClicked(){
		
		return false;
	}

	public boolean isMoved(){
		
		return false;
	}

	public boolean ismov(){
		
		return false;
	}
	
}
