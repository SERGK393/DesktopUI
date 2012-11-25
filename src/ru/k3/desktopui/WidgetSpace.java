package ru.k3.desktopui;
import android.view.ViewGroup;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.graphics.Canvas;
import android.view.View;
import android.appwidget.AppWidgetHostView;
import android.graphics.Rect;
import android.appwidget.AppWidgetProviderInfo;

public class WidgetSpace extends ViewGroup
{
	private final String LOG_TAG="DesktopUI.WidgetSpace";
    
	private final Context c;
	
	
	
	public WidgetSpace(Context co, AttributeSet attrs){
		super(co,attrs);
		Log.d(LOG_TAG,"Creating View");
		c=co;
		setFocusable(true);
	}

	public void addWidget(int id,AppWidgetProviderInfo inf,int x,int y){
		LayoutParams lp=new LayoutParams(x,y,inf.minWidth,inf.minHeight);
		
//		ObjWidget v= ((DesktopUI)c).getAppWidgetHost().createView(c,id,inf);
		AppWidgetHostView v= ((DesktopUI)c).getAppWidgetHost().createView(c,id,inf);
		v.setAppWidget(id,inf);

		v.setLayoutParams(lp);
		addView(v,lp);
	}
	public void addWidget(AppWidgetHostView v,int x,int y){
		LayoutParams lp=new LayoutParams(x,y,
		                    v.getAppWidgetInfo().minWidth,
							v.getAppWidgetInfo().minHeight);
		
		v.setLayoutParams(lp);
		addView(v,lp);
	}
	
	@Override
	public void dispatchDraw(Canvas c){
		super.dispatchDraw(c);
	}
	
	@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // currently ignoring padding
        int widthSpecSize =  MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecSize =  MeasureSpec.getSize(heightMeasureSpec);

        int count = getChildCount();

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();

            int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
            int childheightMeasureSpec =
				MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
            child.measure(childWidthMeasureSpec, childheightMeasureSpec);
        }

        setMeasuredDimension(widthSpecSize, heightSpecSize);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {

                LayoutParams lp = (LayoutParams) child.getLayoutParams();

                child.layout(lp.left, lp.top, lp.right, lp.bottom);

//                if (lp.dropped) {
//                    lp.dropped = false;
//
//                    final int[] cellXY = mCellXY;
//                    getLocationOnScreen(cellXY);
//                    mWallpaperManager.sendWallpaperCommand(getWindowToken(), "android.home.drop",
//														   cellXY[0] + childLeft + lp.width / 2,
//														   cellXY[1] + childTop + lp.height / 2, 0, null);
//                }
            }
        }
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new WidgetSpace.LayoutParams(getContext(), attrs);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof WidgetSpace.LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new WidgetSpace.LayoutParams(p);
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
		
		public int left;
		public int top;
		public int right;
		public int bottom;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        @SuppressWarnings("deprecation")
		public LayoutParams(int left, int top, int width, int height) {
            super(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
            setup(left,top,width,height);
        }

        public void setup(int left, int top, int width, int height) {
            this.left = left;
            this.top = top;
            this.width = width;
            this.height = height;
			this.right = left+width;
			this.bottom = top+height;
        }
        public void setup(Rect r) {
            this.left = r.left;
            this.top = r.top;
			this.right = r.right;
			this.bottom = r.bottom;
            this.width = r.right-r.left;
            this.height = r.bottom-r.top;
        }
    }
}
