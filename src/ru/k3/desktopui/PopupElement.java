package ru.k3.desktopui;

import android.view.View;
import android.view.View.OnTouchListener;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.graphics.Bitmap;

public class PopupElement{
    private int width;
	private String max="";

	protected final View anchor;
	protected final PopupWindow window;
	private View root;
	private final Context c;
	private final ArrayAdapter<String> adapt;
	private ListView lv;
	protected final WindowManager windowManager;

	public PopupElement(View anchor) {
		this.anchor = anchor;
		c=anchor.getContext();
		adapt=new ArrayAdapter<String>(c,R.layout.listitem,R.id.list_item);

		this.window = new PopupWindow(c);
		lv=new ListView(c);
		lv.setAdapter(adapt);
		setContentView(lv);

		// when a touch even happens outside of the window
		// make the window go away
		window.setTouchInterceptor(new OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
						PopupElement.this.window.dismiss();

						return true;
					}

					return false;
				}
			});

		windowManager = (WindowManager) anchor.getContext().getSystemService(Context.WINDOW_SERVICE);

		onCreate();
	}
	
	public void addToAdapter(String str){
		if(max.length()<str.length())max=str;
		adapt.add(str);
	}
	public void resetAdapter(){
		max="";
		width=0;
		adapt.clear();
	}
	public void setOnItemClickListener(ListView.OnItemClickListener l){
		lv.setOnItemClickListener(l);
	}
	public void setWidth(int width){
		if(width>0)
		    this.width=width;
		else
		    this.width=(int)(anchor.getContext().getResources().getDimension(R.dimen.listitem_h)-4)*max.length();
	}

	/**
	 * Anything you want to have happen when created. Probably should create a view and setup the event listeners on
	 * child views.
	 */
	protected void onCreate() {}

	/**
	 * In case there is stuff to do right before displaying.
	 */
	protected void onShow() {}

	@SuppressWarnings("deprecation")
	protected void preShow() {
		if (root == null) {
			throw new IllegalStateException("setContentView was not called with a view to display.");
		}

		onShow();

		window.setBackgroundDrawable(new BitmapDrawable(Bitmap.createBitmap(new int[]{0xFF333333},1,1,Bitmap.Config.ARGB_4444)));

		// if using PopupWindow#setBackgroundDrawable this is the only values of the width and hight that make it work
		// otherwise you need to set the background of the root viewgroup
		// and set the popupwindow background to an empty BitmapDrawable

		lv.scrollTo(0,0);
		
		window.setWidth(width);
		window.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
		window.setTouchable(true);
		window.setFocusable(true);
		window.setOutsideTouchable(true);

		window.setContentView(root);
	}

	/**
	 * Sets the content view. Probably should be called from {@link onCreate}
	 *
	 * @param root
	 *            the view the popup will display
	 */
	public void setContentView(View root) {
		this.root = root;

		window.setContentView(root);
	}

	/**
	 * If you want to do anything when {@link dismiss} is called
	 *
	 * @param listener
	 */
	public void setOnDismissListener(PopupWindow.OnDismissListener listener) {
		window.setOnDismissListener(listener);
	}

	/**
	 * Displays like a popdown menu from the anchor view
	 */
	public void showDropDown() {
		showDropDown(0, 0);
	}

	/**
	 * Displays like a popdown menu from the anchor view.
	 *
	 * @param xOffset
	 *            offset in X direction
	 * @param yOffset
	 *            offset in Y direction
	 */
	public void showDropDown(int xOffset, int yOffset) {
		preShow();

//		window.setAnimationStyle(R.style.Animations_PopDownMenu);

		window.showAsDropDown(anchor, xOffset, yOffset);
	}

	/**
	 * Displays like a QuickAction from the anchor view.
	 */
	public void showLikeQuickAction() {
		showLikeQuickAction(0, 0);
	}

	/**
	 * Displays like a QuickAction from the anchor view.
	 *
	 * @param xOffset
	 *            offset in the X direction
	 * @param yOffset
	 *            offset in the Y direction
	 */
	public void showLikeQuickAction(int xOffset, int yOffset) {
		preShow();

//		window.setAnimationStyle(R.style.Animations_PopUpMenu_Center);

		int[] location = new int[2];
		anchor.getLocationOnScreen(location);

		Rect anchorRect =
			new Rect(location[0], location[1], location[0] + anchor.getWidth(), location[1]
					 + anchor.getHeight());

		int rootWidth 		= width;//root.getWidth();
		int rootHeight 		= lv.getHeight();//*(int)(c.getResources().getDimension(R.dimen.listitem_h)+c.getResources().getDimension(R.dimen.listitem_p)*2);//root.getHeight();

//		int screenWidth 	= windowManager.getDefaultDisplay().getWidth();
		//int screenHeight 	= windowManager.getDefaultDisplay().getHeight();

		int xPos 			= -(rootWidth / 2) + xOffset - anchor.getScrollX();
		int yPos	 		= anchorRect.top - rootHeight + yOffset - anchor.getScrollY();

		// display on bottom
		if (rootHeight > yOffset - anchor.getScrollY()){//anchorRect.top) {
			yPos = anchorRect.top + yOffset - anchor.getScrollY();

//			window.setAnimationStyle(R.style.Animations_PopDownMenu_Center);
		}

		window.showAtLocation(anchor, Gravity.NO_GRAVITY, xPos, yPos);
	}

	public void dismiss() {
		window.dismiss();
	}
	
	public boolean visible(){
		return window.isShowing();
	}
}
