package ru.k3.desktopui;

import android.content.*;
import android.graphics.*;
import android.view.*;
import java.util.*;

import android.content.res.TypedArray;
import android.database.Cursor;
import android.util.AttributeSet;
import android.widget.Scroller;
import android.util.Log;

public class DeskView extends View
{
    private static final String LOG_TAG="DesktopUI.DeskView";
	
    private ArrayList<Obj> itm;
	private Events clk=null;
	
	private final Context c;
	private final IconCache iCache;
	
	private final GestureDetector gestureDetector;
    private final Scroller scroller;
	
	private int sw,sh;
	private int sw1,sh1;
	private int is,fh;
	private int mtx,mty;
	
	public DeskView(Context co, AttributeSet attrs){
		super(co,attrs);
		Log.d(LOG_TAG,"Creating View");
		c=co;
		setFocusable(true);
		
//		setDrawingCacheEnabled(true);
//		setDrawingCacheQuality(DRAWING_CACHE_QUALITY_LOW);
		
		iCache=new IconCache(c);
		itm=new ArrayList<Obj>();
		is=70;
		fh=12;
		
		gestureDetector = new GestureDetector(c, new MyGestureListener());
        scroller = new Scroller(c);

        setHorizontalScrollBarEnabled(true);
        setVerticalScrollBarEnabled(true);

        TypedArray a = c.obtainStyledAttributes(R.styleable.View);
        initializeScrollbars(a);
        a.recycle();
	}
	
	public void clear(){
		Log.d(LOG_TAG,"Clearing "
		      +itm.size()+" items");
		itm.clear();
		sw=sh=0;
		initTableSize();
	}
	public void flushCache(){
		iCache.flush();
	}
	
	public void initTableSize(){
		if(sw<getWidth())sw=getWidth();
		if(sh<getHeight())sh=getHeight();
	}
	
	public void correctTableSize(){
		sw=sh=0;
		for(Obj it:itm){
			if(it.getAbsoluteX2()>sw)
				sw=it.getAbsoluteX2();
			if(it.getAbsoluteY2()>sh)
				sh=it.getAbsoluteY2();
		}
		initTableSize();
	}
	
	public void setMySettings(Cursor db){
		db.moveToFirst();
		is=db.getInt(0);
		fh=db.getInt(1);
		Log.d(LOG_TAG,"Params setted: is="+is+" fh="+fh);
	}
	
	public void setEvents(Events evt){
		clk=evt;
	}
	
	public void setMoveTouching(Obj it){
		mtx=is-(int)((((float)it.getXPos()-getScrollX())/getWidth())*is);
		mty=is-(int)((((float)it.getYPos()-getScrollY())/getHeight())*is);
	}
	
	public void addItem(int type,String title,String p1,String p2,int x,int y){
		itm.add(new Obj(new Rect(x,y,x+is,y+is),iCache,type,title,p1,p2,fh));
		if(sw<x+is)sw=x+is; if(sh<y+is+fh+5)sh=y+is+fh+5;
//	    postInvalidate();
	}
	
	private boolean check(MotionEvent ev,int i){
		return itm.get(i).contains(getScrollX()+(int)ev.getX(),getScrollY()+(int)ev.getY());
	}
	
	@Override
	public void onDraw(Canvas c){
		super.onDraw(c);
		for (Obj it:itm) it.draw(c);
	}
	
	@Override
    protected int computeHorizontalScrollRange()
    {
//        return img.getIntrinsicWidth();
		return sw;
    }

    @Override
    protected int computeVerticalScrollRange()
    {
//        return img.getIntrinsicHeight();
		return sh;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev)
    {
        // check for tap and cancel fling
        if ((ev.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN)
        {
            if (!scroller.isFinished()) scroller.abortAnimation();
			int i=0;
			while (i<itm.size()){
				if(check(ev,i)) break; i++;
			}
			if (i<itm.size()) itm.get(i).setClicked(true);
			invalidate();
        }

		if ((ev.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_MOVE){
			Obj it=itm.get(clk.getItemPos()>=0?clk.getItemPos():0);
			if (it.isMoved()){
				setMoveTouching(it);
				int newx,newy;
				newx = getScrollX();
				newy = getScrollY();
				if (it.getAbsoluteX2() > getScrollX()+getWidth()) newx=it.getAbsoluteX2()-getWidth();
				else if(it.getXPos()<getScrollX())newx=it.getXPos();
				if (it.getAbsoluteY2() > getScrollY()+getHeight()) newy=it.getAbsoluteY2()-getHeight();
				else if(it.getYPos()<getScrollY())newy=it.getYPos();
				if(newx!=getScrollX()||newy!=getScrollY()){
					sw=it.getAbsoluteX2();
					if(sw<sw1)sw=sw1;
					sh=it.getAbsoluteY2();
					if(sh<sh1)sh=sh1;
					scrollBy(newx-getScrollX(),newy-getScrollY());
				}
				clk.onMoveItem(it,(int)ev.getX()+getScrollX()-mtx,(int)ev.getY()+getScrollY()-mty);
				invalidate();
			}
		}

        if (gestureDetector.onTouchEvent(ev)) return true;

        // check for pointer release 
        if ((ev.getPointerCount() == 1) && ((ev.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP))
        {
			int i=0;
			while (i<itm.size()){
				if(check(ev,i)) break; i++;
			}
			if (i<itm.size()) {
				clk.onClick(i,itm.get(i));
				invalidate();
			}/*else{
			 int willSX=getScrollX()%is;
			 int willSY=getScrollY()%is;
			 scroller.startScroll(getScrollX(), getScrollY(),willSX+(willSX<is?((-willSX*2)-is):(is-is-willSX)),willSY+(willSY<is?((-willSY*2)-is):(is-is-willSY)));
			 awakenScrollBars(scroller.getDuration());
			 }*/
            int newScrollX = getScrollX();
			if(sw>getWidth()){
                if (getScrollX() < 0) newScrollX = 0;
                else if (getScrollX() > sw - getWidth()) newScrollX = sw- getWidth();
			}

            int newScrollY = getScrollY();
            if(sh>getHeight()){
			    if (getScrollY() < 0) newScrollY = 0;
                else if (getScrollY() > sh - getHeight()) newScrollY = sh - getHeight();
			}

			if ((newScrollX != getScrollX()) || (newScrollY != getScrollY()))
            {
                scroller.startScroll(getScrollX(), getScrollY(), newScrollX - getScrollX(), newScrollY - getScrollY());
                awakenScrollBars(scroller.getDuration());
            }
        }

        return true;
    }


    @Override
    public void computeScroll()
    {
        if (scroller.computeScrollOffset())
        {
            int oldX = getScrollX();
            int oldY = getScrollY();
            int x = scroller.getCurrX();
            int y = scroller.getCurrY();
            scrollTo(x, y);
            if (oldX != getScrollX() || oldY != getScrollY())
            {
                onScrollChanged(getScrollX(), getScrollY(), oldX, oldY);
            }

            postInvalidate();
        }
    }

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener
    {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
        {
            boolean scroll = (((velocityX<900)&&(velocityX>-900))&&((velocityY<900)&&(velocityY>-900)));
            if(scroll)scroll=getScrollX()<getWidth()&&getScrollY()<getHeight()&&(getScrollX()+getWidth()*2)>sw&&(getScrollY()+getHeight()*2)>sh;
            if(scroll)return false;

            scroller.fling(getScrollX(), getScrollY(), -(int)velocityX, -(int)velocityY, 0, sw-getWidth(), 0, sh-getHeight());
            awakenScrollBars(scroller.getDuration());

            return false;
        }
		
		@Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
        {
			boolean onMove=false;
			for (int i=0;i<itm.size();i++) {
				Obj it=itm.get(i);
				if(it.isMoved())onMove=true;
				else it.setClicked(false);
			}
			if(onMove)return true;
			
            boolean scrollOut = ((getScrollX() < -10) || (getScrollX() > sw-getWidth()+10) || (getScrollY() < -10) || (getScrollY() > sh-getHeight()+10));
            int distX=sw>getWidth()?(scrollOut?(int)distanceX/2:(int)distanceX):0;
			int distY=sh>getHeight()?(scrollOut?(int)distanceY/2:(int)distanceY):0;
			
			scrollBy(distX, distY);
			return true;
        }
		
		@Override
		public void onLongPress(MotionEvent ev){
			int i=0;
			while (i<itm.size()){
				if(check(ev,i)) break; i++;
			}
			if (i<itm.size()){
				sw1=sw; sh1=sh;
				clk.onLongClick(i,itm.get(i),(int)ev.getX()+getScrollX(),(int)ev.getY()+getScrollY());
			}else
			clk.onLongClick(0,null,(int)ev.getX()+getScrollX(),(int)ev.getY()+getScrollY());
		}
    }
	
	public interface Events{
		public abstract void onClick(int pos,Obj it);
		public abstract void onLongClick(int pos,Obj it,int x,int y);
		public abstract void onMoveItem(Obj it,int mx,int my);
		public abstract int getItemPos();
	}
}
