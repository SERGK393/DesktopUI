package ru.k3.desktopui.db;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;

public class DbProvider extends ContentProvider{
  public static final String DB_MAIN="main.db";
  
  public static final Uri URI_OBJ=Uri.parse("content://ru.k3.desktopui.db.DbProvider/obj");
  public static final int URI_CODE=1;
  public static final int URI_CODE_ID=2;
  
  public static final UriMatcher umatch;
  public static HashMap<String,String> map;
  
  private SQLiteDatabase db;

  static{
     umatch=new UriMatcher(UriMatcher.NO_MATCH);
     umatch.addURI("ru.k3.desktopui.db.DbProvider/obj",DbManager.OBJ,URI_CODE);
     umatch.addURI("ru.k3.desktopui.db.DbProvider/obj",DbManager.OBJ+"/#",URI_CODE_ID);
     
     map=new HashMap<String,String>();
     map.put(DbManager._ID,DbManager._ID);
	 map.put(DbManager.TYPE,DbManager.TYPE);
	 map.put(DbManager.NAME,DbManager.NAME);
     map.put(DbManager.PARAM_1,DbManager.PARAM_1);
     map.put(DbManager.PARAM_2,DbManager.PARAM_2);
	 map.put(DbManager.PARAM_3,DbManager.PARAM_3);
	 map.put(DbManager.POSX,DbManager.POSX);
	 map.put(DbManager.POSY,DbManager.POSY);
  }
  
  @Override
  public boolean onCreate(){
     return (db=(new DbManager(getContext())).getWritableDatabase())==null?false:true;
  }
  
  @Override
  public Cursor query(Uri url,String[]project,String select,String[]selArgs,String sort){
	 Cursor c=db.query(getType(url),project,select,selArgs,null,null,sort);
     c.setNotificationUri(getContext().getContentResolver(),url);
     return c;
  }
  
  @Override
  public Uri insert(Uri url,ContentValues inval){
     ContentValues val=new ContentValues(inval);
     
	  long rowId=db.insert(getType(url),url.equals(URI_OBJ)?DbManager.NAME:null,val);
     if(rowId>0){
       Uri uri=ContentUris.withAppendedId(url,rowId);
       getContext().getContentResolver().notifyChange(uri,null);
       return uri;
     }else throw new SQLException("Insert item filed: "+url);
  }
  
  @Override
  public int delete(Uri url,String where,String[]whereArgs){
	  int ret=db.delete(getType(url),where,whereArgs);
     
     getContext().getContentResolver().notifyChange(url,null);
     return ret;
  }
  
  @Override
  public int update(Uri url,ContentValues val,String where,String[]whereArgs){
	  int ret=db.update(getType(url),val,where,whereArgs);
     
     getContext().getContentResolver().notifyChange(url,null);
     return ret;
  }
  
  @Override
  public String getType(Uri url){
	  return url.equals(URI_OBJ)?DbManager.OBJ:null;
  }
}
