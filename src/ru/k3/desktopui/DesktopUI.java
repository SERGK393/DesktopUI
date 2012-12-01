package ru.k3.desktopui;

import ru.k3.desktopui.db.*;
import ru.k3.desktopui.r.*;

import android.annotation.SuppressLint;
import android.app.*;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.*;
import android.content.*;
import android.net.*;
import android.util.*;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.content.pm.ResolveInfo;
import java.util.Collections;
import java.util.List;
import android.content.pm.PackageManager;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import android.preference.PreferenceManager;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.view.Display;
import android.view.WindowManager;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;

public class DesktopUI extends Activity implements DeskView.Events
{
    private static final String LOG_TAG="DesktopUI";
	private static final int DIALOG_EXIT=0xFF;
	private static final int DIALOG_EDIT=0x10;
	private static final int APPW_HOST_ID=1024;
	private static final int REQUEST_CREATE_APPWIDGET=2048;
	private static final int REQUEST_PICK_APPWIDGET=4096;
	private static boolean SIMPLEWALLCHANGE=false;//FIRSTLOAD=true;
	private static int TOUCHX,TOUCHY,DEFX,DEFY,itposedit,wallmode;
	
	private static AsyncTask<DesktopUI, Void, Void> task;

//    private MainReceiver receiver;
	private Cursor dbo;
	private DeskView dv;
	private WidgetSpace ws;
	private ImageView wall;
	
	private int appwIdNow=0;
	private AppWidgetHost appwHost;
	private AppWidgetManager appwMan;
	
	class pref_resets{
	    boolean ic=false;
		boolean scroll=false;
		boolean wall=false;
		boolean other=false;
	}
	final pref_resets pe=new pref_resets();
	class finalclass{
		Obj it;
		int pos;
		Dialog d;
	}
	final finalclass finc=new finalclass();

	private static final String[] itcontent=new String[]{DbManager._ID,DbManager.TYPE,DbManager.NAME,DbManager.PARAM_1,DbManager.PARAM_2,DbManager.PARAM_3, DbManager.POSX,DbManager.POSY};

    @Override
    public void onCreate(Bundle state)
	{
		if(getPrefInt(R.string.pref_is)<0)Utilities.sharedToDefault(getApplicationContext());
		if(getPrefBool(R.string.pref_oldtheme)&&Utilities.isNewApi())
			setTheme(R.style.Theme_Old_WithActionBar);
		super.onCreate(state);
		if(checkErrors())return;
		Log.d(LOG_TAG,"-------STARTED-------");

		registerMainReceiver();
		createActionBar();
//		setRequestedOrientation(Configuration.ORIENTATION_PORTRAIT);
//		android.R.style
        setContentView(R.layout.main);
		ws = (WidgetSpace)findViewById(R.id.widg);
        dv = (DeskView)findViewById(R.id.desk);
		wall=(ImageView)findViewById(R.id.wall);
		dv.setEvents(this);
		dv.setWidgetSpace(ws);
		checkWallpaper();
		initDeskPosition();
//		getWindow().setBackgroundDrawable(new Wallpaper(peekWallpaper()));
		
		appwMan = AppWidgetManager.getInstance(this);
        appwHost = new AppWidgetHost(this, APPW_HOST_ID);
        appwHost.startListening();
		
//		mysett=getSQLCursor(DbProvider.URI_MSETT,scontent,null,null,null);
		dbo=getSQLCursor(DbProvider.URI_OBJ,itcontent,null,null,null);
		accurateLoadObj();
		PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
		.registerOnSharedPreferenceChangeListener(preferenceChange);
    }
	
	@Override
	public void onStart(){
		Log.d(LOG_TAG,"onStart");
//		if(FIRSTLOAD)FIRSTLOAD=false;
//		else forceLoadObj();
		super.onStart();
	}
	
	@Override
	public void onPostResume(){
		Log.d(LOG_TAG,"onPostResume");
		Utilities.initStatics(this);
//		dv.postInvalidate();
		applyPrefs();
		if(wallmode==1&&SIMPLEWALLCHANGE)
			wall.setImageDrawable(peekWallpaper());
		super.onPostResume();
	}
	
    @Override
    public void onDestroy() {
		Log.d(LOG_TAG,"onDestroy");
        super.onDestroy();
		
		if(appwIdNow>0)appwHost.deleteAppWidgetId(appwIdNow);
		
		try {
            appwHost.stopListening();
        } catch (NullPointerException e) {
            Log.w(LOG_TAG,"problem while stopping AppWidgetHost during Launcher destruction", e);
        }
    }
	@Override
	public void onStop(){
		Log.d(LOG_TAG,"onStop");
		Utilities.resetStatics();
//		dv.clear();
//		dv.flushUnVisibles();
//		dv.invalidate();
		saveDeskPosition();
		System.gc();
		super.onStop();
	}
	
	private void createActionBar(){
//		Log.d(LOG_TAG,"Action Bar");
	    if(Utilities.isNewApi()){
	        ActionBar a=getActionBar();
//	        a.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
//	        a.setCustomView(R.layout.action);
	        if(a!=null){
				a.setTitle(" ");
				if(getPrefBool(R.string.pref_actionbar))
					a.show();
				else a.hide();
			}
	    }else{
		    View t=getWindow().findViewById(android.R.id.title);
		    View titleBar=(View)t.getParent();
		    ViewGroup titleParent=(ViewGroup)titleBar.getParent();
		    titleParent.removeViewInLayout(titleBar);
		}
//		titleBar.setBackgroundColor(0x77FFFFFF);
	}
	
	private void registerMainReceiver(){
		final IntentFilter intf=new IntentFilter(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
		registerReceiver(new MainReceiver(),intf);
	}
	
	@SuppressWarnings("deprecation")
	private void setWallpaperDimension() {
		wallmode=getPrefInt(R.string.pref_wall);
		if(wallmode>1){
			wall.setVisibility(View.GONE);
			
            WallpaperManager wpm = (WallpaperManager)getSystemService(WALLPAPER_SERVICE);
			
            Display display = getWindowManager().getDefaultDisplay();
            boolean isPortrait = display.getWidth() < display.getHeight();
            int width = isPortrait ? display.getWidth() : display.getHeight();
            int height = isPortrait ? display.getHeight() : display.getWidth();
			
			switch(wallmode){
				case 2:
				    width*=2;
					break;
				case 3:
				    width*=3;
					height*=2;
					break;
			}
			
			setWallpaperVisible(true);
            wpm.suggestDesiredDimensions(width,height);
		}else{
			setWallpaperVisible(false);
            wall.setImageDrawable(peekWallpaper());
			wall.setVisibility(View.VISIBLE);
		}
    }
	private void checkWallpaper(){
		wallmode=getPrefInt(R.string.pref_wall);
		if(wallmode>1){
			wall.setVisibility(View.GONE);
			setWallpaperVisible(true);
		}else{
			setWallpaperVisible(false);
            wall.setImageDrawable(peekWallpaper());
			wall.setVisibility(View.VISIBLE);
		}
	}
	void setWallpaperVisible(boolean visible) {
        int wpflags = visible ?
			WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER : 0;
        int curflags = getWindow()
			.getAttributes().flags
			&
			WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER;
        if (wpflags != curflags) {
            getWindow().setFlags(wpflags,
								 WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        }
    }
	
	OnSharedPreferenceChangeListener preferenceChange=new OnSharedPreferenceChangeListener(){
        public void onSharedPreferenceChanged(SharedPreferences p, String k){
			if(k.startsWith(getString(R.string.pref_icons_category)))
			pe.ic=true;
			
			else if(k.startsWith(getString(R.string.pref_scroll_category)))
			pe.scroll=true;
			
			else if(k.startsWith(getString(R.string.pref_wall_category)))
			pe.wall=true;
			
			else pe.other=true;
		}
	};
	private void applyPrefs(){
		if(pe.other){
			pe.other=false;
			System.exit(0);
			return;
		}
		if(pe.wall){
			pe.wall=false;
			setWallpaperDimension();
			if(wallmode>1)setWallpaper();
		}
		if(pe.ic){
			pe.ic=false;
			reloadObj();
			return;
		}
		if(pe.scroll){
			pe.scroll=false;
			dv.setMySettings();
			return;
		}
	}
	private void initDeskPosition(){
		SharedPreferences pr=PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		DEFX=pr.getInt(getString(R.string.prefex_defx),0);
		DEFY=pr.getInt(getString(R.string.prefex_defy),0);
		dv.scrollTo(DEFX,DEFY);
	}
	private void saveDeskPosition(){
		SharedPreferences.Editor pr=PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
		pr.putInt(getString(R.string.prefex_defx),DEFX);
		pr.putInt(getString(R.string.prefex_defy),DEFY);
		pr.apply();
	}

	public boolean checkErrors(){
		Log.i(LOG_TAG,"checkErrors();");
		SharedPreferences pr=PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		if(pr.getString(ErrorMonitor.ERROR_TEXT,"No errors").equals("No errors"))
			return false;
		Intent go=new Intent();
		go.setClass(getApplicationContext(),ErrorMonitor.class);
		startActivity(go);
		finish();
		return true;
	}
	
	@SuppressWarnings("deprecation")
	private Cursor getSQLCursor(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder){
		Log.d(LOG_TAG,"getSQLCursor("+uri+", "+projection+", "+selection+", "+selectionArgs+", "+sortOrder+");");
		if(Utilities.isNewApi()){
			CursorLoader cl;
			cl=new CursorLoader(getApplicationContext());
			cl.setUri(uri);
			cl.setProjection(projection);
			cl.setSelection(selection);
			cl.setSelectionArgs(selectionArgs);
			cl.setSortOrder(sortOrder);
			return cl.loadInBackground();
		}else
		return managedQuery(uri,projection,selection,selectionArgs,sortOrder);
	}
	@SuppressWarnings("deprecation")
	private void updateSQLCursor(Uri url,String[] content){
		if(Utilities.isNewApi()) dbo=getSQLCursor(url, content, null, null, null);
		else dbo.requery();
	}
	private String getVendor(String src){
		String vendor="";
		for(int i=0,j=0;i<src.length();i++)
			if(src.charAt(i)=='.'){
				if(j==0)j=1;
				else if(i+1<src.length())
					vendor=src.substring(0,i+1);
			}
		return vendor;
	}
	
	@Override
	public void onBackPressed(){
		dv.scroll(DEFX,DEFY);
	}
	
	@Override
	public void onNewIntent(Intent intent){
		if (Intent.ACTION_MAIN.equals(intent.getAction())) {

            boolean alreadyOnHome = ((intent.getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
                        != Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
			if(alreadyOnHome)
				openOptionsMenu();
        }
	}
	
	@Override
	public void onConfigurationChanged(Configuration conf){
		Log.d(LOG_TAG,"onConfigurationChanged");
		setWallpaperDimension();
		super.onConfigurationChanged(conf);
	}
	
	@Override
    public boolean onSearchRequested() {
        startSearch(null, false, null, true);
        return true;
    }
	
    @SuppressLint("AlwaysShowAction")
	@Override
    public boolean onCreateOptionsMenu(Menu menu)
	{
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.menu, menu);
		if(Utilities.isNewApi()){
			if(getPrefBool(R.string.pref_actionbar)){
				for(int i=2;i<=4;i++){
					MenuItem it=menu.getItem(i);
					it.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
				}
			}
		}
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
	{
        if (optionsSelect(item.getItemId())) return true;
    	else return super.onOptionsItemSelected(item);
    }

    public boolean optionsSelect(int id)
	{
    	switch (id)
		{
			case R.id.add:
//			    itposedit=-1;
//				showDialog(DIALOG_EDIT);
			    pickAppWidget();
				return true;
			case R.id.wall:
			    setWallpaper();
				return true;
			case R.id.srch:
				onSearchRequested();
				return true;
			case R.id.sett:
				openSettingsMenu();
				return true;
			case R.id.apps:
				startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS));
				return true;
			case R.id.restart:
				System.exit(0);
				return true;
        }
    	return false;
    }
	
	public int getPrefInt(int k){
		Log.i(LOG_TAG,"getPrefInt(String k);");
		SharedPreferences pr=PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		return Integer.parseInt(pr.getString(getString(k),"-1"));
	}
	public boolean getPrefBool(int k){
		Log.i(LOG_TAG,"getPrefBool(String k);");
		SharedPreferences pr=PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		return pr.getBoolean(getString(k),false);
	}
	public DeskView getDeskView(){
		return dv;
	}
	public Cursor getCursorDBO(){
		return dbo;
	}
	public AppWidgetHost getAppWidgetHost(){
		return appwHost;
	}
	
	public void openSettingsMenu(){
		try{
			startActivity(new Intent().setClass(this,Settings.class));
		}catch(Exception e){MessageBox(e.toString(),5000);}
	}
	
	public void setWallpaper(){
		if(wallmode==1)SIMPLEWALLCHANGE=true;
		else setWallpaperDimension();
		final Intent pickWallpaper = new Intent(Intent.ACTION_SET_WALLPAPER);
        Intent chooser = Intent.createChooser(pickWallpaper,getString(R.string.wall));
        startActivity(chooser);
	}

    public AlertDialog ExitMenu()
    {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage(R.string.exit_al)
			.setCancelable(true)
			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id)
				{
					finish();
					System.exit(0);
				}
			})
			.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id)
				{
					dialog.cancel();
				}
			});
    	AlertDialog alert = builder.create();
    	return alert;
    }
	
	public AlertDialog EditMenu()
	{
//		final Context c=getApplicationContext();
		final PackageManager man=getPackageManager();
		final View v=getLayoutInflater().inflate(R.layout.edit,(ViewGroup)findViewById(R.id.edit_view));
		final EditText n =(EditText)v.findViewById(R.id.edit_n);
		final EditText p1=(EditText)v.findViewById(R.id.edit_p1);
		final EditText p2=(EditText)v.findViewById(R.id.edit_p2);
		final ImageButton ns=(ImageButton)v.findViewById(R.id.edit_n_srch);
		final ImageButton p1s=(ImageButton)v.findViewById(R.id.edit_p1_srch);
		final ImageButton p2s=(ImageButton)v.findViewById(R.id.edit_p2_srch);

		View.OnClickListener clickl=new View.OnClickListener(){
			public void onClick(View v){
				final PopupElement pop;
				final String[] categories={Intent.CATEGORY_LAUNCHER,Intent.CATEGORY_DEFAULT,Intent.CATEGORY_PREFERENCE,Intent.CATEGORY_EMBED};
				/*boolean isnew_pop=*/Utilities.isNewPopup(Utilities.POP_APPS,n);
				pop=Utilities.getPopupList(Utilities.POP_APPS);
				pop.resetAdapter();
				switch (v.getId()){
					case R.id.edit_n_srch:
						final List<ResolveInfo> apps_n = Utilities.getResolveInfos(Utilities.POP_APPS);
						
						for (ResolveInfo inf:apps_n)pop.addToAdapter(inf.loadLabel(man).toString());
						pop.setWidth(n.getWidth());
						pop.setOnItemClickListener(new ListView.OnItemClickListener(){
							public void onItemClick(AdapterView<?> a,View v,int pos,long id){
								ResolveInfo inf=apps_n.get(pos);
								n.setText(inf.loadLabel(man));
								p1.setText(inf.activityInfo.applicationInfo.packageName);
								p2.setText(inf.activityInfo.name);
								apps_n.clear();
								pop.dismiss();
							}
						});
						pop.showDropDown();
						break;
					case R.id.edit_p1_srch:
						final ArrayList<ResolveInfo> apps_p1=new ArrayList<ResolveInfo>();
						String vendor=getVendor(p1.getText().toString());
						for(String cat:categories){
							Intent intent = new Intent();
							intent.setAction(Intent.ACTION_MAIN);
							intent.addCategory(cat);
							List<ResolveInfo> apps= man.queryIntentActivities(intent, 0);
							apps_p1.addAll(apps);
						}
						Collections.sort(apps_p1, new ResolveInfo.DisplayNameComparator(man));

						final ArrayList<Integer> lnk_p1=new ArrayList<Integer>();
						for (ResolveInfo inf:apps_p1)if(inf.activityInfo.applicationInfo.packageName.startsWith(vendor)){
							                         lnk_p1.add(apps_p1.indexOf(inf));
						                             pop.addToAdapter(inf.loadLabel(man).toString()
						                                       +" ("+inf.activityInfo.applicationInfo.packageName+")");
													 }
						pop.setWidth(p1.getWidth());
						pop.setOnItemClickListener(new ListView.OnItemClickListener(){
								public void onItemClick(AdapterView<?> a,View v,int pos,long id){
									ResolveInfo inf=apps_p1.get(lnk_p1.get(pos));
									n.setText(inf.loadLabel(man));
									p1.setText(inf.activityInfo.applicationInfo.packageName);
									p2.setText(inf.activityInfo.name);
									apps_p1.clear();
									pop.dismiss();
								}
							});
						pop.showLikeQuickAction();
						break;
					case R.id.edit_p2_srch:
						String pak=p1.getText().toString();
						final ArrayList<ResolveInfo> apps_p2=new ArrayList<ResolveInfo>();
						for(String cat:categories){
							Intent intent = new Intent();
							intent.setAction(Intent.ACTION_MAIN);
							intent.addCategory(cat);
							List<ResolveInfo> apps= man.queryIntentActivities(intent, 0);
							apps_p2.addAll(apps);
						}
						Collections.sort(apps_p2, new ResolveInfo.DisplayNameComparator(man));

						final ArrayList<Integer> lnk_p2=new ArrayList<Integer>();
						for (ResolveInfo inf:apps_p2)if(inf.activityInfo.applicationInfo.packageName.startsWith(pak)){
								lnk_p2.add(apps_p2.indexOf(inf));
								pop.addToAdapter(inf.loadLabel(man).toString()
										  +" ("+inf.activityInfo.name+")");
							}
						pop.setWidth(p2.getWidth());
						pop.setOnItemClickListener(new ListView.OnItemClickListener(){
								public void onItemClick(AdapterView<?> a,View v,int pos,long id){
									ResolveInfo inf=apps_p2.get(lnk_p2.get(pos));
									n.setText(inf.loadLabel(man));
									p1.setText(inf.activityInfo.applicationInfo.packageName);
									p2.setText(inf.activityInfo.name);
									apps_p2.clear();
									pop.dismiss();
								}
							});
						pop.showLikeQuickAction();
						break;
				}
			}
		};
		
		ns.setOnClickListener(clickl);
		p1s.setOnClickListener(clickl);
		p2s.setOnClickListener(clickl);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	return builder.setTitle(R.string.edit)
			.setView(v)
			.setCancelable(true)
			.create();
    }

    protected Dialog onCreateDialog(int id)
	{
        Dialog dialog;
        switch (id)
		{
			case DIALOG_EXIT:
				dialog = ExitMenu();
				break;
			case DIALOG_EDIT:
				dialog = EditMenu();
				break;
			default:
				dialog = null;
        }

		return dialog;
    }
	protected void onPrepareDialog(int id, Dialog d){
		switch (id)
		{
			case DIALOG_EXIT:
				break;
			case DIALOG_EDIT:
//			    AlertDialog d=(AlertDialog)di;
				final EditText n =(EditText)d.findViewById(R.id.edit_n);
				final EditText p1=(EditText)d.findViewById(R.id.edit_p1);
				final EditText p2=(EditText)d.findViewById(R.id.edit_p2);
				final Button b=(Button)d.findViewById(R.id.edit_save);
				finc.d=d;
				if(itposedit>=0){
					dbo.moveToPosition(itposedit);
					n.setText(dbo.getString(2));
					p1.setText(dbo.getString(3));
					p2.setText(dbo.getString(4));

					b.setOnClickListener(new View.OnClickListener(){
							public void onClick(View v){
								String sn =n.getText().toString();
								String sp1=p1.getText().toString();
								String sp2=p2.getText().toString();
								finc.d.cancel();
								if(sn.length()>0&&sp1.length()>0&&sp2.length()>0){
									ContentValues val=new ContentValues(4);
									val.put(DbManager.TYPE,1);
									val.put(DbManager.NAME,sn);
									val.put(DbManager.PARAM_1,sp1);
									val.put(DbManager.PARAM_2,sp2);
									getContentResolver().update(DbProvider.URI_OBJ,val,"_ID=" + dbo.getInt(0),null);
									updateSQLCursor(DbProvider.URI_OBJ,itcontent);
									dbo.moveToPosition(itposedit);
									dv.editItem(itposedit,dbo.getInt(1),dbo.getString(2),dbo.getString(3),dbo.getString(4));
									dv.correctTableSize();
									dv.postInvalidate();
								}
							}
						});
				}else{
					n.setText("");
					p1.setText("");
					p2.setText("");

					b.setOnClickListener(new View.OnClickListener(){
							public void onClick(View v){
								String sn =n.getText().toString();
								String sp1=p1.getText().toString();
								String sp2=p2.getText().toString();
								finc.d.cancel();
								if(sn.length()>0&&sp1.length()>0&&sp2.length()>0){
									ContentValues val=new ContentValues(6);
									val.put(DbManager.TYPE,1);
									val.put(DbManager.NAME,sn);
									val.put(DbManager.PARAM_1,sp1);
									val.put(DbManager.PARAM_2,sp2);
									val.put(DbManager.POSX,TOUCHX);
									val.put(DbManager.POSY,TOUCHY);
									getContentResolver().insert(DbProvider.URI_OBJ,val);
									updateSQLCursor(DbProvider.URI_OBJ,itcontent);
									dbo.moveToLast();
									dv.addItem(dbo.getInt(1),dbo.getString(2),dbo.getString(3),dbo.getString(4),dbo.getInt(6),dbo.getInt(7));
									dv.correctTableSize();
									dv.postInvalidate();
								}
							}
						});
				}
				break;
			default:
        }
	}
	
	@Override
	protected void onActivityResult(int request,int result,Intent data){
//		super.onActivityResult(request,result,data);
		if(result==RESULT_OK){
			switch(request){
                case REQUEST_PICK_APPWIDGET:
                    addAppWidget(data);
                    break;
                case REQUEST_CREATE_APPWIDGET:
                    completeAddAppWidget(data);
                    break;
            }
        } else if ((request == REQUEST_PICK_APPWIDGET ||
                request == REQUEST_CREATE_APPWIDGET) && result == RESULT_CANCELED &&
                data != null) {
            // Clean up the appWidgetId if we canceled
            int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if (appWidgetId != -1) {
                appwHost.deleteAppWidgetId(appWidgetId);
            }
        }
	}
	
	private void pickAppWidget(){
        int appWidgetId = this.appwHost.allocateAppWidgetId();

        Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
        pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		addEmptyData(pickIntent);
        // start the pick activity
        startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET);
    }
	
	void addEmptyData(Intent pickIntent) {
		ArrayList<AppWidgetProviderInfo> customInfo = new ArrayList<AppWidgetProviderInfo>();
		pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, customInfo);
		ArrayList<Bundle> customExtras = new ArrayList<Bundle>();
		pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS, customExtras);
	}
	
	void addAppWidget(Intent data) {
        // catch bad widget exception when sent
        int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        AppWidgetProviderInfo appWidget = appwMan.getAppWidgetInfo(appWidgetId);

        if (appWidget.configure != null) {
            // Launch over to configure widget, if needed
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            intent.setComponent(appWidget.configure);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

            startActivityForResult(intent, REQUEST_CREATE_APPWIDGET);//+Safely
        } else {
            // Otherwise just add it
            onActivityResult(REQUEST_CREATE_APPWIDGET, Activity.RESULT_OK, data);
        }
    }
	
	private void completeAddAppWidget(Intent data) {
        Bundle extras = data.getExtras();
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

        AppWidgetProviderInfo appWidgetInfo = appwMan.getAppWidgetInfo(appWidgetId);

//        mModel.addItemToDatabase(this, launcherInfo,
//                LauncherSettings.Favorites.CONTAINER_DESKTOP,
//                mWorkspace.getCurrentScreen(), xy[0], xy[1], false);

//        if (!mRestoring) {
//            mDesktopItems.add(launcherInfo);

            // Perform actual inflation because we're live
         //  // //AppWidgetHostView hostView = appwHost.createView(this, appWidgetId, appWidgetInfo);

        //  //  //hostView.setAppWidget(appWidgetId, appWidgetInfo);
//            hostView.setTag(launcherInfo);

//            mWorkspace.addInCurrentScreen(launcherInfo.hostView, xy[0], xy[1],
//                    launcherInfo.spanX, launcherInfo.spanY, isWorkspaceLocked());

//            mModel.mAppWidgets.add(launcherInfo);
            // finish load a widget, send it an intent
//            if(appWidgetInfo!=null)
//            	appwidgetReadyBroadcast(appWidgetId, appWidgetInfo.provider);
//        }
		if(appwIdNow>0)appwHost.deleteAppWidgetId(appwIdNow);
		appwIdNow=appWidgetId;
		
		dv.addWidget(appWidgetId,appWidgetInfo,50,60);
//		dv.addView(hostView);
//		rootview.addView(hostView);
//		dv.addWidget(appWidgetId,hostView,appWidgetInfo,50,160);
    }

/*    public void removeAppWidget(LauncherAppWidgetInfo launcherInfo) {
    	int appWidgetId = launcherInfo.appWidgetId;
        if(mWorkspace.isWidgetScrollable(appWidgetId))
        	mWorkspace.unbindWidgetScrollableId(appWidgetId);
        mDesktopItems.remove(launcherInfo);
        mModel.mAppWidgets.remove(launcherInfo);
        launcherInfo.hostView = null;
    }*/

    public void MessageBox(String str, int dur)
	{
    	Toast.makeText(getApplicationContext(), str, dur)
    	.show();
    }
	
	public void reloadObj(){
		dv.flushCache();
		accurateLoadObj();
	}
	private void accurateLoadObj()
	{
		Log.d(LOG_TAG,"accutate load begins");
		dv.clear();
		dv.setMySettings();
		dbo.moveToPosition(-1);
/*		hand=new Handler(){
			public void handleMessage(Message msg){
				if(dbo.moveToNext()){
					dv.addItem(dbo.getInt(1),dbo.getString(2),dbo.getString(3),dbo.getString(4),dbo.getInt(6),dbo.getInt(7));
					dv.postInvalidate();
					sendEmptyMessage(0);
				}else {
					dbo.moveToFirst();
					dv.correctTableSize();
					Log.d(LOG_TAG,"accurate load complete");
//					MessageBox("Load Complete",3000);
				}
			}
		};
		hand.sendEmptyMessageDelayed(0,dbo.getCount()>1?0:800);
*/		task=new AsyncTask<DesktopUI,Void,Void>(){
			protected Void doInBackground(DesktopUI... d){
				Cursor dbo=d[0].getCursorDBO();
				DeskView dv=d[0].getDeskView();
				
				try{
				if(dbo.getCount()<=1)TimeUnit.MILLISECONDS.sleep(800);
				
				dv.setDrawUnlock(false);
				while(dbo.moveToNext())
					dv.addItem(dbo.getInt(1),dbo.getString(2),dbo.getString(3),dbo.getString(4),dbo.getInt(6),dbo.getInt(7));

				dv.setDrawUnlock(true);
				dbo.moveToFirst();
				dv.correctTableSize();
				dv.postInvalidate();
				Log.d(LOG_TAG,"accurate load complete");
//				MessageBox("Load Complete",3000);
				}catch (InterruptedException e){
					e.printStackTrace();
				}
				return null;
			}
		};
		task.execute(this);
    }
/*	
	private void forceLoadObj(){
		Log.d(LOG_TAG,"force load begins");
		dv.clear();
		dv.setMySettings(mysett);
		dbo.moveToPosition(-1);
		while(dbo.moveToNext())
			dv.addItem(dbo.getInt(1),dbo.getString(2),dbo.getString(3),dbo.getString(4),dbo.getInt(6),dbo.getInt(7));
		dbo.moveToFirst();
		dv.correctTableSize();
		Log.d(LOG_TAG,"force load complete");
		MessageBox("Load Complete",3000);
	}
*/
	private void start(int pos,Obj it,boolean need){
		if(it.isClicked()||need){
		    try
		    {
			    dbo.moveToPosition(pos);
				Intent start=it.run();
				if(start!=null){
				    startActivity(start);
			        Log.d(LOG_TAG,"Started Object: "+dbo.getString(2)+" ("+dbo.getString(4)+")");
				}
		    }
		    catch (Exception e)
		    {
			    MessageBox("Not found", 3000);
			    Log.e(LOG_TAG,"Start Object failed: "+dbo.getString(2)+" ("+dbo.getString(4)+")");
		    }
		it.setClicked(false);
		dv.invalidate();
		}
	}
	private void start(finalclass en){
		start(en.pos,en.it,true);
	}
	
	public void onClick(int pos,Obj it)
	{
		if (!it.isMoved())
			start(pos,it,false);
		else{
			dbo.moveToPosition(pos);
			it.setMoving(false);
			dv.postInvalidate();
			ContentValues val=new ContentValues(2);
			val.put(DbManager.POSX,it.getXPos());
			val.put(DbManager.POSY,it.getYPos());
			getContentResolver().update(DbProvider.URI_OBJ,val,"_ID=" + dbo.getInt(0),null);
			updateSQLCursor(DbProvider.URI_OBJ,itcontent);
			dbo.moveToPosition(pos);
			dv.correctTableSize();
		}
	}
	@SuppressWarnings("deprecation")
	public void onLongClick(int pos,Obj it,int x,int y){
//		MessageBox("touch x=" + x + "\ntouch y=" + y, 5000);
		TOUCHX=x; TOUCHY=y;
		
		if (it != null){
			dbo.moveToPosition(pos);
			if (it.isClicked()){
				it.setClicked(false);
				it.setMoving(true);
//				MessageBox("item x=" + dbo.getInt(6) + "\nitem y=" + dbo.getInt(7)
//						   + "\ntouch x=" + x + "\ntouch y=" + y, 5000);
				dv.invalidate();
			}

			final PopupElement pop;
			finc.it=it;
			finc.pos=pos;
			if(Utilities.isNewPopup(Utilities.POP_CONTEXT2,dv)){
				pop=Utilities.getPopupList(Utilities.POP_CONTEXT2);
				String[]strs=getResources().getStringArray(R.array.context_menu);
				for(String str:strs)pop.addToAdapter(str);
				pop.setWidth(0);
				pop.setOnItemClickListener(new ListView.OnItemClickListener(){
						public void onItemClick(AdapterView<?> a,View v,int pos,long id){
							switch(pos){
							case 0:
							    start(finc);
							    break;
							case 1:
								itposedit=finc.pos;
								showDialog(DIALOG_EDIT);
							    break;
							case 2:
							    Utilities.execAppInfo(finc.it);
							    break;
							case 3:
								getContentResolver().delete(DbProvider.URI_OBJ,"_ID=" + dbo.getInt(0),null);
								updateSQLCursor(DbProvider.URI_OBJ,itcontent);
								dbo.moveToLast();
								dv.deleteItem(finc.it);
								dv.correctTableSize();
								dv.postInvalidate();
							    break;
							}

							pop.dismiss();
						}
					});
			}else pop=Utilities.getPopupList(Utilities.POP_CONTEXT2);
			pop.showLikeQuickAction(x,y);
		}
		else{
			final PopupElement pop;
			if(Utilities.isNewPopup(Utilities.POP_CONTEXT1,dv)){
			    pop=Utilities.getPopupList(Utilities.POP_CONTEXT1);
			    String[]strs=getResources().getStringArray(R.array.empty_context_menu);
			    for(String str:strs)pop.addToAdapter(str);
			    pop.setWidth(0);
			    pop.setOnItemClickListener(new ListView.OnItemClickListener(){
					public void onItemClick(AdapterView<?> a,View v,int pos,long id){
						switch(pos){
							case 0:
							    itposedit=-1;
								showDialog(DIALOG_EDIT);
							    break;
							case 1:
							    setWallpaper();
								break;
							case 2:
							    DEFX=dv.getScrollX();
								DEFY=dv.getScrollY();
								break;
							case 3:
							    Utilities.isNewPopup(Utilities.POP_ALL_APPS,dv);
								final PopupElement pop1=Utilities.getPopupList(Utilities.POP_ALL_APPS);
								pop1.resetAdapter();
								final List<ResolveInfo> apps = Utilities.getResolveInfos(Utilities.POP_APPS);

								for (ResolveInfo inf:apps)pop1.addToAdapter(inf.loadLabel(getPackageManager()).toString());
								pop1.setWidth(dv.getWidth()-dv.getWidth()/3);
								pop1.setOnItemClickListener(new ListView.OnItemClickListener(){
										public void onItemClick(AdapterView<?> a,View v,int pos,long id){
											ResolveInfo inf=apps.get(pos);
											Intent start=new Intent(Intent.ACTION_MAIN);
											start.setClassName(inf.activityInfo.packageName,inf.activityInfo.name);
											startActivity(start);
											apps.clear();
											pop1.dismiss();
										}
									});
								pop1.showLikeQuickAction();
								break;
							case 4:
							    openSettingsMenu();
								break;
							}
						
						pop.dismiss();
					}
					});
			}else pop=Utilities.getPopupList(Utilities.POP_CONTEXT1);
			pop.showLikeQuickAction(x,y);
		}
	}
	public void onMoveItem(Obj it,int mx,int my){
		if(it.isMoved()){
			it.setPos(mx,my);
		    if(it.ismov()){
			    if(!Utilities.isNewPopup(Utilities.POP_CONTEXT2,dv)){
			        PopupElement pop=Utilities.getPopupList(Utilities.POP_CONTEXT2);
				    if(pop.visible())pop.dismiss();
			    }
		    }
		}
	}
	public int getItemPos(){
		int pos=dbo.getPosition();
		return (dbo.getCount()>pos)?pos:-1;
	}
	
}
