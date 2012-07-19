package ru.k3.desktopui;

import ru.k3.desktopui.db.*;

import android.annotation.SuppressLint;
import android.app.*;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.*;
import android.content.*;
import android.net.*;
import android.util.*;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class DesktopUI extends Activity implements DeskView.Events
{
    private static final String LOG_TAG="DesktopUI";
	private static final int DIALOG_EXIT=0xFF;
	private static final int DIALOG_SETTINGS=0x01;
	private static final int DIALOG_EDIT=0x10;
	private static boolean CLOSE=false;
	private static int TOUCHX,TOUCHY;

	private Cursor mysett;
	private Cursor dbo;
	private DeskView dv;
	private ImageView wall;

	private static final String[] scontent=new String[]{DbManager.ICN_SZ,DbManager.FNT_SZ,DbManager.BMP_Q};
	private static final String[] itcontent=new String[]{DbManager._ID,DbManager.NAME,DbManager.PARAM_1,DbManager.PARAM_2,DbManager.PARAM_3, DbManager.POSX,DbManager.POSY};

    @Override
    public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Log.d(LOG_TAG,"-------STARTED-------");

        createActionBar();
//		setRequestedOrientation(Configuration.ORIENTATION_PORTRAIT);
		
        setContentView(R.layout.main);
        dv = (DeskView)findViewById(R.id.desk);
		wall=(ImageView)findViewById(R.id.wall);
        wall.setImageDrawable(peekWallpaper());
		new Handler(){
			public void handleMessage(Message msg){
				mysett=getSQLCursor(DbProvider.URI_MSETT,scontent,null,null,null);
				dbo=getSQLCursor(DbProvider.URI_OBJ,itcontent,null,null,null);
				LoadApps();
			}
		}.sendEmptyMessageDelayed(0,100);
		dv.setEvents(this);
    }
	
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(CLOSE)System.exit(0);
    }
	@Override
	public void onPause(){
		super.onPause();
//		dv.flushCache();
	}
	
	private void createActionBar(){
		Log.d(LOG_TAG,"Action Bar");
	    if(Utilities.isNewApi()){
	        ActionBar a=getActionBar();
//	        a.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
//	        a.setCustomView(R.layout.action);
	        if(a!=null)a.setTitle(" ");
	    }/*
		View t=getWindow().findViewById(android.R.id.title);
		View titleBar=(View)t.getParent();
		ViewGroup titleParent=(ViewGroup)titleBar.getParent();
		titleParent.removeViewInLayout(titleBar);
		*/
//		titleBar.setBackgroundColor(0x77FFFFFF);
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
		if(Utilities.isNewApi())
			if(url.equals(DbProvider.URI_OBJ))dbo=getSQLCursor(url, content, null, null, null);
			else mysett=getSQLCursor(url, content, null, null, null);
		else
		if(url.equals(DbProvider.URI_OBJ))dbo.requery();
		else mysett.requery();
	}
	
	@Override
	public void onBackPressed(){
		
	}
	
	@Override
	public void onConfigurationChanged(Configuration conf){
		Log.d(LOG_TAG,"onConfigurationChanged");
		super.onConfigurationChanged(conf);
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
	{
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
	{
        if (optionsSelect(item.getItemId())) return true;
    	else return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("deprecation")
	public boolean optionsSelect(int id)
	{
    	switch (id)
		{
			case R.id.mysett: 
				showDialog(DIALOG_SETTINGS);
				return true;
			case R.id.srch:
				onSearchRequested();
				return true;
			case R.id.sett:
				startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
				return true;
			case R.id.apps:
				startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS));
				return true;
			case R.id.exit:
				showDialog(DIALOG_EXIT);
				return true;
        }
    	return false;
    }
	
	public int getPref(int p){
		return mysett!=null?mysett.getInt(p):-1;
	}

    public AlertDialog ExitMenu()
    {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage(R.string.exit_al)
			.setCancelable(true)
			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id)
				{
					CLOSE=true;
					finish();
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

    public AlertDialog SettingsMenu()
	{
		final View v=getLayoutInflater().inflate(R.layout.mysett,(ViewGroup)findViewById(R.id.mysett_view));
		final EditText is=(EditText)v.findViewById(R.id.itm_size);
		is.setText(String.valueOf(mysett.getInt(0)));
		final EditText fs=(EditText)v.findViewById(R.id.fnt_size);
		fs.setText(String.valueOf(mysett.getInt(1)));
		final CheckBox bq=(CheckBox)v.findViewById(R.id.bmp_quality);
		bq.setChecked(mysett.getInt(2)==1);
		final Button re=(Button)v.findViewById(R.id.btn_reset);
		re.setOnClickListener(new View.OnClickListener(){
			public void onClick(View v){
				getApplicationContext().deleteDatabase(DbProvider.DB_MAIN);
				System.exit(0);
			}
		});
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	return builder.setTitle(R.string.mysett)
			.setView(v)
			.setOnCancelListener(new DialogInterface.OnCancelListener(){
				public void onCancel(DialogInterface di){
					int pis=Integer.parseInt(is.getText().toString());
					int pfs=Integer.parseInt(fs.getText().toString());
					boolean pbq=bq.isChecked();
					if(mysett.getInt(0)!=pis||mysett.getInt(1)!=pfs||(mysett.getInt(2)==1)!=pbq){
					    ContentValues val=new ContentValues(3);
					    val.put(DbManager.ICN_SZ,pis);
					    val.put(DbManager.FNT_SZ,pfs);
						val.put(DbManager.BMP_Q,pbq?1:0);
					    getContentResolver().update(DbProvider.URI_MSETT,val,"_ID=1",null);
					    updateSQLCursor(DbProvider.URI_MSETT,scontent);
						Utilities.resetStatics();
					    LoadApps();
					}
				}
			})
		.create();
    }
	
	public AlertDialog EditMenu()
	{
		final View v=getLayoutInflater().inflate(R.layout.edit,(ViewGroup)findViewById(R.id.edit_view));
		final EditText n =(EditText)v.findViewById(R.id.edit_n);
		final EditText p1=(EditText)v.findViewById(R.id.edit_p1);
		final EditText p2=(EditText)v.findViewById(R.id.edit_p2);
		final ImageButton ns=(ImageButton)v.findViewById(R.id.edit_n_srch);
		final ImageButton p1s=(ImageButton)v.findViewById(R.id.edit_p1_srch);
		final ImageButton p2s=(ImageButton)v.findViewById(R.id.edit_p2_srch);

		View.OnClickListener clickl=new View.OnClickListener(){
			public void onClick(View v){
				switch (v.getId()){
					case R.id.edit_n_srch:
					case R.id.edit_p1_srch:
					case R.id.edit_p2_srch:
					    MessageBox("Search",2000);
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
			.setCancelable(false)
			.setPositiveButton(R.string.save,new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface d,int id){
					String sn =n.getText().toString();
					String sp1=p1.getText().toString();
					String sp2=p2.getText().toString();
					if(sn.length()>0&&sp1.length()>0&&sp2.length()>0){
					    ContentValues val=new ContentValues(5);
						val.put(DbManager.NAME,sn);
						val.put(DbManager.PARAM_1,sp1);
						val.put(DbManager.PARAM_2,sp2);
						val.put(DbManager.POSX,TOUCHX);
						val.put(DbManager.POSY,TOUCHY);
					    getContentResolver().insert(DbProvider.URI_OBJ,val);
					    updateSQLCursor(DbProvider.URI_OBJ,itcontent);
						dbo.moveToLast();
						dv.addItem(dbo.getString(1),new ComponentName(dbo.getString(2),dbo.getString(3)),dbo.getInt(5),dbo.getInt(6));
						dv.postInvalidate();
					}
				}
			})
			.setNegativeButton(R.string.cancel,new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface d,int id){
					d.cancel();
				}
			})
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
			case DIALOG_SETTINGS:
				dialog = SettingsMenu();
				break;
			case DIALOG_EDIT:
				dialog = EditMenu();
				break;
			default:
				dialog = null;
        }

        return dialog;
    }

    void MessageBox(String str, int dur)
	{
    	Toast.makeText(getApplicationContext(), str, dur)
    	.show();
    }
	
	void LoadApps()
	{
		dv.clear();
		dv.setMySettings(mysett);
		dbo.moveToPosition(-1);
		new Handler(){
			@SuppressLint("HandlerLeak")
			public void handleMessage(Message msg){
				if(dbo.moveToNext()){
					dv.addItem(dbo.getString(1),new ComponentName(dbo.getString(2),dbo.getString(3)),dbo.getInt(5),dbo.getInt(6));
					sendEmptyMessage(0);
				}else {
					dbo.moveToFirst();
					Log.d(LOG_TAG,"Load Complete");
					MessageBox("Load Complete",3000);
				}
			}
		}.sendEmptyMessage(0);
    }

	public void onClick(int pos,Obj it)
	{
		if (!it.isMoved())
		try
		{
			if(it.isClicked()){
			    dbo.moveToPosition(pos);
			    Intent start=new Intent(Intent.ACTION_MAIN);
		        start.addCategory(Intent.CATEGORY_LAUNCHER);
		        start.setComponent(new ComponentName(dbo.getString(2), dbo.getString(3)));
		        start.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		        startActivity(start);
			    Log.d(LOG_TAG,"Started Activity: "+dbo.getString(1)+" ("+dbo.getString(3)+")");
			    it.setClicked(false);
			    dv.invalidate();
		    }
		}
		catch (Exception e)
		{
			MessageBox("Not found", 3000);
			Log.e(LOG_TAG,"Start Activity failed: "+dbo.getString(1)+" ("+dbo.getString(3)+")");
		}else{
			dbo.moveToPosition(pos);
			it.setMoving(false);
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
		if (it != null){
			if (it.isClicked()){
				dbo.moveToPosition(pos);
				it.setMoving(true);
				MessageBox("item x=" + dbo.getInt(5) + "\nitem y=" + dbo.getInt(6)
						   + "\ntouch x=" + x + "\ntouch y=" + y, 5000);
				it.setClicked(false);
				dv.invalidate();
			}
		}
		else{
			MessageBox("touch x=" + x + "\ntouch y=" + y, 5000);
			TOUCHX=x; TOUCHY=y;
			showDialog(DIALOG_EDIT);
		}
	}
	public void onMoveItem(Obj it,int mx,int my){
		if (it.isMoved()) it.setPos(mx,my);
	}
	public int getItemPos(){
		return dbo.getPosition();
	}
}
