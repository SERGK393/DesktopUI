package ru.k3.desktopui.db;

import ru.k3.desktopui.R;

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
import java.util.ArrayList;

public class DbManager extends SQLiteOpenHelper implements BaseColumns{
	
	public static final int DB_VERSION=3;
	
	public static final String OBJ="obj";
	public static final String TYPE="type";
	public static final String NAME="name";
	public static final String PARAM_1="param_1";
	public static final String PARAM_2="param_2";
	public static final String PARAM_3="param_3";
	public static final String POSX="posx";
	public static final String POSY="posy";
		
	Context c;
	PackageManager manager;
	
	public DbManager(Context c){
  	super(c,DbProvider.DB_MAIN,null,DB_VERSION);
		this.c=c;
		manager=c.getPackageManager();
	}

	@Override
	public void onCreate(SQLiteDatabase db){
		loadObj(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db,int olddb, int newdb){
		switch(olddb){
			case 1: db.execSQL("DROP TABLE IF EXISTS "+OBJ);
				    db.execSQL("DROP TABLE IF EXISTS mysettings");
		            onCreate(db);
		    case 2: db.execSQL("DROP TABLE IF EXISTS mysettings");
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
	
	    final String[]categories={Intent.CATEGORY_APP_MARKET,Intent.ACTION_CALL_BUTTON,Intent.CATEGORY_APP_MESSAGING,Intent.CATEGORY_APP_BROWSER};
		final ArrayList<ResolveInfo> apps=new ArrayList<ResolveInfo>();
		
		for(String cat:categories){
			Intent intent = new Intent();
			if(cat.contains(".category.")){
			    intent.setAction(Intent.ACTION_MAIN);
			    intent.addCategory(cat);
			}else intent.setAction(cat);
			List<ResolveInfo> apps1= manager.queryIntentActivities(intent, 0);
			apps.addAll(apps1);
		}

    	int is=(int)c.getResources().getDimension(R.dimen.itm);
		int fs=(int)c.getResources().getDimension(R.dimen.fnt);
		
		if (apps != null) {
			int i=0;
			
     	    for (ResolveInfo info:apps) {
				boolean markt=(info.activityInfo.name.startsWith("com.android.vending"));
				if(!markt&&i==0)i=3;
				val.put(TYPE,1);
       	        val.put(NAME,info.loadLabel(manager).toString());
		        val.put(PARAM_1,info.activityInfo.applicationInfo.packageName);
		        val.put(PARAM_2,info.activityInfo.name);
		        val.put(POSX,(i/4)*(is+20)+5);
		        val.put(POSY,((i%4<4)?(is+fs*3+25)*(i%4):0));
		        db.insert(OBJ,NAME,val);
				i+=4;
				if(markt&&i==4)i--;
            }
	    }
	}
}
