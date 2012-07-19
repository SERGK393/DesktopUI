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
		Log.d(LOG_TAG,"Clearing");
		itm.clear();
		iCache.flush();
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
			if(it.getXPos()+is>sw)
				sw=it.getXPos()+is;
			if(it.getYPos()+is+fh>sh+5)
				sh=it.getYPos()+is+fh+5;
		}
		initTableSize();
	}
	
	public void setMySettings(Cursor db){
		Log.d(LOG_TAG,"Setting params");
		db.moveToFirst();
		is=db.getInt(0);
		fh=db.getInt(1);
	}
	
	public void setEvents(Events evt){
		clk=evt;
	}
	
	public void setMoveTouching(MotionEvent ev,Obj it){
		mtx=getScrollX()+(int)ev.getX()-it.getXPos();
		mty=getScrollY()+(int)ev.getY()-it.getYPos();
	}
	
	public void addItem(String title,ComponentName comp,int x,int y){
		itm.add(new Obj(new Rect(x,y,x+is,y+is),iCache,comp,title,fh));
		if(sw<x+is)sw=x+is; if(sh<y+is+fh+5)sh=y+is+fh+5;
	    postInvalidate();
	}
	
	private boolean check(MotionEvent ev,int i){
		return itm.get(i).contains(getScrollX()+(int)ev.getX(),getScrollY()+(int)ev.getY());
	}
	
	@Override
	public void onDraw(Canvas c){
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
			Obj it=itm.get(clk.getItemPos());
			if (it.isMoved()){
				int newx,newy;
				newx = getScrollX();
				newy = getScrollY();
				if (it.getXPos()+is > getScrollX()+getWidth()) newx=it.getXPos()+is-getWidth();
				else if(it.getXPos()<getScrollX())newx=it.getXPos();
				if (it.getYPos()+is+fh+5 > getScrollY()+getHeight()) newy=it.getYPos()+is+fh+5-getHeight();
				else if(it.getYPos()<getScrollY())newy=it.getYPos();
				if(newx!=getScrollX()||newy!=getScrollY()){
					sw=it.getXPos()+is;
					if(sw<sw1)sw=sw1;
					sh=it.getYPos()+is;
					if(sh<sh1)sh=sh1;
					scrollBy((newx-getScrollX())/2,(newy-getScrollY())/2);
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
            if (getScrollX() < 0) newScrollX = 0;
            else if (getScrollX() > sw - getWidth()) newScrollX = sw- getWidth();

            int newScrollY = getScrollY();
            if (getScrollY() < 0) newScrollY = 0;
            else if (getScrollY() > sh - getHeight()) newScrollY = sh - getHeight();

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
            boolean scroll = (((velocityX<1800)&&(velocityX>-1800))&&((velocityY<1800)&&(velocityY>-1800)));
            if (scroll) return false;

            scroller.fling(getScrollX(), getScrollY(), -(int)velocityX, -(int)velocityY, 0, sw - getWidth(), 0, sh - getHeight());
            awakenScrollBars(scroller.getDuration());

            return true;
        }
		
		@Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
        {
			for (int i=0;i<itm.size();i++) itm.get(i).setClicked(false);
			
            boolean scrollOut = ((getScrollX() < -10) || (getScrollX() > sw-getWidth()+10) || (getScrollY() < -10) || (getScrollY() > sh-getHeight()+10));
            int distX=scrollOut?(int)distanceX/2:(int)distanceX;
			int distY=scrollOut?(int)distanceY/2:(int)distanceY;
			
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
				setMoveTouching(ev,itm.get(i));
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
