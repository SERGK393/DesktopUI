package ru.k3.desktopui;

import java.util.Collections;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class ItemManager extends SQLiteOpenHelper implements BaseColumns{
	
	public static final String APPS_TABLE="apps";
	public static final String TITLE="title";
	public static final String PACKAGE="pkg";
	public static final String ACTIVITY="activity";
	public static final String POSX="posx";
	public static final String POSY="posy";
		
	Context context;
	PackageManager manager;
	
	int count;
	
	public ItemManager(Context c){
  	super(c,ItemProvider.DB_APPS,null,1);
		context=c;
		manager=c.getPackageManager();
	}

	@Override
	public void onCreate(SQLiteDatabase db){
		db.execSQL("CREATE TABLE "+APPS_TABLE
				+ "(_id INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ TITLE + " TEXT, " + PACKAGE + " TEXT, " + ACTIVITY + " TEXT, "
				+ POSX + " INTEGER, " + POSY + " INTEGER);");
		load(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db,int olddb, int newdb){
		db.execSQL("DROP TABLE IF EXISTS "+APPS_TABLE);
		onCreate(db);
	}

	public void load(SQLiteDatabase db){
	ContentValues val=new ContentValues();
	
	Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
     	mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
	final List<ResolveInfo> apps = manager.queryIntentActivities(mainIntent, 0);
	Collections.sort(apps, new ResolveInfo.DisplayNameComparator(manager));

    	if (apps != null) {
       	count = apps.size();
            
     	for (int i = 0; i < count; i++) {
      	ResolveInfo info = apps.get(i); 
       	val.put(TITLE,info.loadLabel(manager).toString());
		    val.put(PACKAGE,info.activityInfo.applicationInfo.packageName);
		    val.put(ACTIVITY,info.activityInfo.name);
		    val.put(POSX,(i/8)*110);
		    val.put(POSY,(i/8)*85+((i%8<8)?85*(i%8):0));
		    db.insert(APPS_TABLE,TITLE,val);
      }
	  }
	}
}
