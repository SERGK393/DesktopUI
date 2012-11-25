package ru.k3.desktopui;

import android.graphics.Rect;
import android.graphics.Canvas;
import android.content.ComponentName;
import android.content.Intent;

public interface Obj{
	
	public void draw(Canvas c);
	
	public boolean contains(int x,int y);
	public boolean contains(Rect r);
	public Intent run();
	
	public void setPos(int x,int y);
	public void setClicked(boolean cl);
	public void setMoving(boolean mv);
	
	public int getXPos();
	public int getYPos();
	public int getAbsoluteX2();
	public int getAbsoluteY2();
	public Type getType();
	public String getPackage();
	public ComponentName getComponentName();
	
	public boolean isClicked();
	public boolean isMoved();
	public boolean ismov();
	
	public enum Type{UNK,APP,FULL,SHORTCUT,SETTINGS,APPWIDGET};
}
