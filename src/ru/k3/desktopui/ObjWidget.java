package ru.k3.desktopui;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.graphics.Rect;
import android.content.ComponentName;
import android.appwidget.AppWidgetManager;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

public class ObjWidget extends AppWidgetHostView implements Obj
{
    
	private final Rect zone;
	private short w,h;
	private final Paint icp;
	private final Obj.Type type;
	
	private int id=0;
	private AppWidgetProviderInfo info=null;
	private Intent conf=null;
	
	private boolean clicked;
	private boolean moved;
	private boolean mov;
	private int dbId;
	
	public ObjWidget(Context c){
		super(c);
		zone=new Rect();
		icp=new Paint();
		icp.setAlpha(255);
		icp.setColor(0x770000FF);
		this.type = Obj.Type.APPWIDGET;
	}
	
	public ObjWidget(Context c, int id, AppWidgetProviderInfo inf){
		this(c);
		setAppWidget(id,inf);
		this.id=id;
		this.info=inf;
	}
	
	public void setLayoutParams(DeskView.LayoutParams lp){
		super.setLayoutParams(lp);
		zone.set(lp.left,lp.top,lp.right,lp.bottom);
		w=(short)lp.width;
		h=(short)lp.height;
	}
	public void draw(Canvas c){
//		c.save();
//		c.translate(zone.left,zone.top);
		//c.scale(2,2);
		super.draw(c);
//		c.restore();
		if(clicked)c.drawRect(zone, icp);
	}
	public void dispatchDraw(Canvas c){
//		c.save();
//		c.translate(zone.left,zone.top);
		//c.scale(2,2);
		super.dispatchDraw(c);
//		c.restore();
		if(clicked)c.drawRect(zone, icp);
	}
	public boolean contains(int x, int y){
		return zone.contains(x,y);
	}
	public boolean contains(Rect r){
		boolean ret=r.contains(zone);
		if(ret)return ret;
		else return
				r.contains(zone.left,zone.top)||
				r.contains(zone.left,zone.bottom)||
				r.contains(zone.right,zone.top)||
				r.contains(zone.right,zone.bottom);
	}
	public Intent run(){
		if(conf==null){
			if(info.configure!=null){
				conf = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
				conf.setComponent(info.configure);
				conf.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
			}else {
				try{
				conf=DesktopUI.getInstance().getPackageManager()
					.getLaunchIntentForPackage(info.provider.getPackageName());
				}catch(Exception e){
					Intent inte=new Intent(Intent.ACTION_MAIN);
					inte.setPackage(info.provider.getPackageName());
					conf=Intent.createChooser(inte,info.provider.getPackageName());
				}
			}
		}
		return conf;
	}

	public ObjWidget setDbId(int dbId){
		this.dbId=dbId;
		return this;
	}
	public void setPos(int x, int y){
		zone.set(x,y,x+w,y+h);
		setLayoutParams(((DeskView.LayoutParams)getLayoutParams()).setup(zone));
		//mov=true;
	}
	public void setClicked(boolean cl){
		clicked=cl;
	}
	public void setMoving(boolean mv){
		moved=mv;
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
		return zone.right;
	}
	public int getAbsoluteY2(){
		return zone.bottom;
	}
	public Obj.Type getType(){
		return type;
	}
	public String getPackage(){
		return info.provider.getPackageName();
	}
	public ComponentName getComponentName(){
		return info.provider;
	}
	public int getId(){
		return id;
	}
	
	public boolean isWidget(){
		return true;
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
	
	
	
	private boolean mHasPerformedLongPress;

    private CheckForLongPress mPendingCheckForLongPress;
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
//		return true;
        // Consume any touch events for ourselves after longpress is triggered
        if (mHasPerformedLongPress) {
            mHasPerformedLongPress = false;
            return true;
        }

        // Watch for longpress events at this level to make sure
        // users can always pick up this widget
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                postCheckForLongClick();
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mHasPerformedLongPress = false;
                if (mPendingCheckForLongPress != null) {
                    removeCallbacks(mPendingCheckForLongPress);
                }
                break;
        }

        // Otherwise continue letting touch events fall through to children
        return false;
    }

    class CheckForLongPress implements Runnable {
        private int mOriginalWindowAttachCount;

        public void run() {
            if ((getParent() != null) && hasWindowFocus()
                    && mOriginalWindowAttachCount == getWindowAttachCount()
                    && !mHasPerformedLongPress) {
                if (performLongClick()) {
                    mHasPerformedLongPress = true;
                }
            }
        }

        public void rememberWindowAttachCount() {
            mOriginalWindowAttachCount = getWindowAttachCount();
        }
    }

    private void postCheckForLongClick() {
        mHasPerformedLongPress = false;

        if (mPendingCheckForLongPress == null) {
            mPendingCheckForLongPress = new CheckForLongPress();
        }
        mPendingCheckForLongPress.rememberWindowAttachCount();
        postDelayed(mPendingCheckForLongPress, ViewConfiguration.getLongPressTimeout());
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        mHasPerformedLongPress = false;
        if (mPendingCheckForLongPress != null) {
            removeCallbacks(mPendingCheckForLongPress);
        }
    }
}
