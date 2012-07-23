package ru.k3.desktopui.db;

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

public class DbManager extends SQLiteOpenHelper implements BaseColumns{
	
	public static final int DB_VERSION=2;
	
	public static final String OBJ="obj";
	public static final String TYPE="type";
	public static final String NAME="name";
	public static final String PARAM_1="param_1";
	public static final String PARAM_2="param_2";
	public static final String PARAM_3="param_3";
	public static final String POSX="posx";
	public static final String POSY="posy";
	
	public static final String MSETT="mysettings";
	public static final String ICN_SZ="icon_size";
	public static final String FNT_SZ="font_size";
	public static final String BMP_Q="bmp_quality";
		
	Context context;
	PackageManager manager;
	
	public DbManager(Context c){
  	super(c,DbProvider.DB_MAIN,null,DB_VERSION);
		context=c;
		manager=c.getPackageManager();
	}

	@Override
	public void onCreate(SQLiteDatabase db){
		loadObj(db);
		loadMySettings(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db,int olddb, int newdb){
		switch(olddb){
			case 1: db.execSQL("DROP TABLE IF EXISTS "+OBJ);
		            db.execSQL("DROP TABLE IF EXISTS "+MSETT);
		            onCreate(db);
		}
//		db.execSQL("DROP TABLE IF EXISTS "+OBJ);
//		db.execSQL("DROP TABLE IF EXISTS "+MSETT);
//		onCreate(db);
	}

	public void loadObj(SQLiteDatabase db){
	    db.execSQL("CREATE TABLE "+OBJ
				   + "(_id INTEGER PRIMARY KEY AUTOINCREMENT, "
				   + TYPE + " INTEGER, " + NAME + " TEXT, "
				   + PARAM_1 + " TEXT, " + PARAM_2 + " TEXT, "
				   + PARAM_3 + " TEXT, " + POSX + " INTEGER, "
				   + POSY + " INTEGER);");

	    ContentValues val=new ContentValues();
	
	    Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
	    final List<ResolveInfo> apps = manager.queryIntentActivities(mainIntent, 0);
	    Collections.sort(apps, new ResolveInfo.DisplayNameComparator(manager));

    	if (apps != null) {
			int i=0;
     	    for (ResolveInfo info:apps) {
//				if(!info.activityInfo.name.startsWith("com.android.")) continue;
				val.put(TYPE,1);
       	        val.put(NAME,info.loadLabel(manager).toString());
		        val.put(PARAM_1,info.activityInfo.applicationInfo.packageName);
		        val.put(PARAM_2,info.activityInfo.name);
		        val.put(POSX,(i/4)*75+5);
		        val.put(POSY,((i%4<4)?88*(i%4):0));
		        db.insert(OBJ,NAME,val);
				i++;
            }
	    }
	}
	public void loadMySettings(SQLiteDatabase db){
		db.execSQL("CREATE TABLE "+MSETT
				   + "(_id INTEGER PRIMARY KEY AUTOINCREMENT, "
				   + ICN_SZ + " INTEGER, " + FNT_SZ + " INTEGER, "
				   + BMP_Q + " INTEGER);");

		ContentValues val=new ContentValues();
		val.put(ICN_SZ,60);
		val.put(FNT_SZ,12);
		val.put(BMP_Q,0);
		
		db.insert(MSETT,ICN_SZ,val);
	}
}
