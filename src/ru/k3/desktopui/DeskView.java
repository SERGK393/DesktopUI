package ru.k3.desktopui;

import android.content.*;
import android.graphics.*;
import android.view.*;
import java.util.*;

import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.Scroller;
import android.util.Log;
import android.app.WallpaperManager;
import android.os.IBinder;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;

public class DeskView extends View
{
    private static final String LOG_TAG="DesktopUI.DeskView";
	
    private ArrayList<Obj> itm;
	private Events clk=null;
	private WidgetSpace ws=null;
	
	private final Context c;
	private final IconCache iCache;
	private final Rect vRect;
	
	private final GestureDetector gestureDetector;
    private final Scroller scroller;
	private final WallpaperManager wpm;
	
	private boolean isDrawUnlocked=false;
	
	private int sw,sh;//screen width/height
	private int sw1,sh1;
	private int ss;//scroll speed
	private boolean cs,wm;//cicle scroll, wallpaper mode
	private int is,fh;//icon size, font height
	private int mtx,mty;//move touching
	private boolean mox,moy;//move out
	
	public DeskView(Context co, AttributeSet attrs){
		super(co,attrs);
		Log.d(LOG_TAG,"Creating View");
		c=co;
		setFocusable(true);
		
//		setDrawingCacheEnabled(true);
//		setDrawingCacheQuality(DRAWING_CACHE_QUALITY_LOW);
		
		iCache=IconCache.getInstance(c);
		itm=new ArrayList<Obj>();
		vRect=new Rect();
		is=70;
		fh=12;
		
		wpm=(WallpaperManager)c.getSystemService(c.WALLPAPER_SERVICE);
		
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
	public void flushUnVisibles(){
		Rect r=vRect;
		int i=0;
		for (Obj it:itm) 
		    if(!it.contains(r))
				if(iCache.remove(it.getComponentName()))
				    i++;
		Log.d(LOG_TAG,"flushUnVisibles():"+i);
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
		updateWallpaperOffset();
	}
	
	public void setMySettings(){
		Log.i(LOG_TAG,"setMySettings();");
		DesktopUI d=(DesktopUI)getContext();
		is=d.getPrefInt(R.string.pref_is);
		fh=d.getPrefInt(R.string.pref_fs);
		cs=d.getPrefBool(R.string.pref_cs);
		ss=d.getPrefInt(R.string.pref_ss);
		wm=d.getPrefInt(R.string.pref_wall)>1;
		Log.d(LOG_TAG,"Params setted: is="+is+" fh="+fh
			                       +" cs="+cs+" ss="+ss
								   +" wm="+wm);
		iCache.setSettings();
	}
	
	public void setDrawUnlock(boolean set){
		isDrawUnlocked=set;
	}
	
	public void setEvents(Events evt){
		clk=evt;
	}
	public void setWidgetSpace(WidgetSpace ws){
		this.ws=ws;
	}
	
	public void setMoveTouching(Obj it,float x,float y){
		if(it.ismov()){
		    mtx=is+is/2-(int)((x/(float)getWidth())*(is*2));
		    mty=is+is/2-(int)((y/(float)getHeight())*(is*2));
			if((x+getScrollX()-mtx)<0)mtx+=(int)x-mtx;
			if((y+getScrollY()-mty)<0)mty+=(int)y-mty;
			if(mox){
				if(((x-mtx)<0)||((x+is-mtx)>getWidth())){
				if((x-mtx)<0)mtx+=(int)x-mtx;
				if((x+is-mtx)>getWidth())mtx+=(int)x+is-mtx-getWidth();
				}else mox=false;
			}
			if(moy){
				if(((y-mty)<0)||((y+(it.getAbsoluteY2()-it.getYPos())-mty)>getHeight())){
					if((y-mty)<0)mty+=(int)y-mty;
					if((y+(it.getAbsoluteY2()-it.getYPos())-mty)>getHeight())
						mty+=(int)y+(it.getAbsoluteY2()-it.getYPos())-mty-getHeight();
				}else moy=false;
			}
		}else{
			if(it.contains((int)x+getScrollX(),(int)y+getScrollY())){
				mtx=(int)x-it.getXPos()+getScrollX();
			    mty=(int)y-it.getYPos()+getScrollY();
			}
			mox=moy=true;
		}
	}
	
	public void scroll(int x,int y){
		if(x!=getScrollX()||y!=getScrollY()){
			if(ss>=0)scroller.startScroll(getScrollX(),getScrollY(),x-getScrollX(),y-getScrollY(),ss);
			else scroller.startScroll(getScrollX(),getScrollY(),x-getScrollX(),y-getScrollY());
            awakenScrollBars(scroller.getDuration());
	    }
	}
	
	public void updateWallpaperOffset(){
		if(wm){
		    IBinder t=getWindowToken();
		    wpm.setWallpaperOffsets(t,Math.min(((float)getScrollX()+getWidth()/2)/sw,1.f),Math.min(((float)getScrollY()+getHeight()/2)/sh,1.f));
//		    wpm.setWallpaperOffsetSteps(getScrollX()/sw,getScrollY()/sh);
		}
	}
	
	public void addItem(int type,String title,String p1,String p2,int x,int y){
		itm.add(new ObjItem(new Rect(x,y,x+is,y+is),iCache,type,title,p1,p2,fh));
		if(sw<x+is)sw=x+is; if(sh<y+is+fh+5)sh=y+is+fh+5;
//	    postInvalidate();
	}
	public void editItem(int pos,int type,String title,String p1,String p2){
		int x=itm.get(pos).getXPos();
		int y=itm.get(pos).getYPos();
		itm.set(pos,new ObjItem(new Rect(x,y,x+is,y+is),iCache,type,title,p1,p2,fh).flush());
	    postInvalidate();
	}
	public void deleteItem(Obj it){
		itm.remove(it);
	}
	public void addWidget(AppWidgetHostView v,int x,int y){
		ws.addWidget(v,x,y);

		int swSpec = MeasureSpec.makeMeasureSpec(sw, MeasureSpec.EXACTLY);
		int shSpec = MeasureSpec.makeMeasureSpec(sh, MeasureSpec.EXACTLY);
		ws.onMeasure(swSpec,shSpec);
	}
	public void addWidget(int id,AppWidgetProviderInfo inf,int x,int y){
		ws.addWidget(id,inf,x,y);

		int swSpec = MeasureSpec.makeMeasureSpec(sw, MeasureSpec.EXACTLY);
		int shSpec = MeasureSpec.makeMeasureSpec(sh, MeasureSpec.EXACTLY);
		ws.onMeasure(swSpec,shSpec);
	}
	
	private boolean check(MotionEvent ev,int i){
		return check(ev,itm.get(i));
	}
	private boolean check(MotionEvent ev,Obj it){
		return it.contains(getScrollX()+(int)ev.getX()-mtx,getScrollY()+(int)ev.getY()-mty);
	}
	
	@Override
	public void dispatchDraw(Canvas c){
		super.dispatchDraw(c);
//		c.save();
//        c.scale(0.5f,0.5f);
		Rect r=vRect;
		getFocusedRect(r);
		if(isDrawUnlocked)for (Obj it:itm) 
		    if(it.contains(r))it.draw(c);
//        c.restore();
	}
	
//	@Override
//    protected void onMeasure(int wMeasureSpec, int hMeasureSpec) {
//		int widthMeasureSpec = MeasureSpec.makeMeasureSpec(sw, MeasureSpec.EXACTLY);
//        int heightMeasureSpec = MeasureSpec.makeMeasureSpec(sh, MeasureSpec.EXACTLY);
//        // TODO: currently ignoring padding
////        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
//        int widthSpecSize =  MeasureSpec.getSize(widthMeasureSpec);
//
////        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
//        int heightSpecSize =  MeasureSpec.getSize(heightMeasureSpec);
//
//        setMeasuredDimension(widthSpecSize, heightSpecSize);
//    }
//
//    @Override
//    protected void onLayout(boolean changed, int l, int t, int r, int b) {
////        int count = getChildCount();
////
////        for (int i = 0; i < count; i++) {
////            View child = getChildAt(i);
////            if (child.getVisibility() != GONE) {
////
//////                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
////
////                int childLeft = 500;//lp.x;
////                int childTop = 800;//lp.y;
////                child.layout(childLeft, childTop, childLeft + getWidth()/*lp.width*/, childTop + getHeight()/*lp.height*/);
////
//////                if (lp.dropped) {
//////                    lp.dropped = false;
//////
//////                    final int[] cellXY = mCellXY;
//////                    getLocationOnScreen(cellXY);
//////                    mWallpaperManager.sendWallpaperCommand(getWindowToken(), "android.home.drop",
//////														   cellXY[0] + childLeft + lp.width / 2,
//////														   cellXY[1] + childTop + lp.height / 2, 0, null);
//////                }
////            }
////        }
//    }
	
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
			mtx=mty=0;
			while (i<itm.size()){
				if(check(ev,i)) break; i++;
			}
			if (i<itm.size()) {
				itm.get(i).setClicked(true);
			    invalidate();
			}
        }

		if ((ev.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_MOVE&&!itm.isEmpty()){
			int pos=clk.getItemPos();
			if(pos>=0){
			Obj it=itm.get(pos);
			if (it.isMoved()){
				setMoveTouching(it,ev.getX(),ev.getY());
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
					scrollBy((newx-getScrollX())/6,(newy-getScrollY())/6);
					ws.scrollTo(getScrollX(),getScrollY());
					updateWallpaperOffset();
				}
				clk.onMoveItem(it,(int)ev.getX()+getScrollX()-mtx,(int)ev.getY()+getScrollY()-mty);
				invalidate();
			}
			}
		}

        if (gestureDetector.onTouchEvent(ev)) return true;

        // check for pointer release 
        if ((ev.getPointerCount() == 1) && ((ev.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP))
        {
			boolean notMove=true;
			int i=0;
			while (i<itm.size()){
				Obj it=itm.get(i);
				if(it.isMoved())notMove=false;
				if(check(ev,it)) break; i++;
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
			int w3=getWidth()/3;
			int newScrollX = getScrollX();
			if(sw>getWidth()||newScrollX>0){
				int sw2=sw-getWidth();
                if (cs&&notMove&&getScrollX() > sw2+w3||newScrollX < 0)
					newScrollX = 0;
                if (cs&&notMove&&getScrollX() < 0-w3||newScrollX > sw2)
				    newScrollX = sw2;
			}

            int newScrollY = getScrollY();
            if(sh>getHeight()||newScrollY>0){
				int sh2=sh-getHeight();
                if (cs&&notMove&&getScrollY() > sh2+w3||newScrollY < 0)
					newScrollY = 0;
                if (cs&&notMove&&getScrollY() < 0-w3||newScrollY > sh2)
				    newScrollY = sh2;
			}

			scroll(newScrollX,newScrollY);
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
			ws.scrollTo(x,y);
            if (oldX != getScrollX() || oldY != getScrollY())
            {
                onScrollChanged(getScrollX(), getScrollY(), oldX, oldY);
				updateWallpaperOffset();
            }

            postInvalidate();
        }
    }

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener
    {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
        {
//            boolean scroll = (((velocityX<900)&&(velocityX>-900))&&((velocityY<900)&&(velocityY>-900)));
//            if(scroll)scroll=getScrollX()<getWidth()&&getScrollY()<getHeight()&&(getScrollX()+getWidth()*2)>sw&&(getScrollY()+getHeight()*2)>sh;
//            if(scroll)return false;

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
			postInvalidate();
			if(onMove){
				postInvalidate();
				return true;
			}
			
            int distX=sw>getWidth()?(((getScrollX() < -10) || (getScrollX() > sw-getWidth()+10))?(int)distanceX/2:(int)distanceX):0;
			int distY=sh>getHeight()?(((getScrollY() < -10) || (getScrollY() > sh-getHeight()+10))?(int)distanceY/2:(int)distanceY):0;
			
			scrollBy(distX, distY);
			ws.scrollTo(getScrollX(),getScrollY());
			updateWallpaperOffset();
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
			clk.onLongClick(0,null,(int)ev.getX()+getScrollX()-is/2,(int)ev.getY()+getScrollY()-is/2);
		}
    }
	
	public interface Events{
		public abstract void onClick(int pos,Obj it);
		public abstract void onLongClick(int pos,Obj it,int x,int y);
		public abstract void onMoveItem(Obj it,int mx,int my);
		public abstract int getItemPos();
	}
}
