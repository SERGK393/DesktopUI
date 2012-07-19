package ru.k3.desktopui;

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

public class ItemProvider extends ContentProvider{
  public static final String DB_APPS="apps.db";
  
  public static final Uri URI=Uri.parse("content://ru.k3.desktopui.ItemProvider/apps");
  public static final int URI_CODE=1;
  public static final int URI_CODE_ID=2;
  
  public static final UriMatcher umatch;
  public static HashMap<String,String> map;
  
  private SQLiteDatabase db;

  static{
     umatch=new UriMatcher(UriMatcher.NO_MATCH);
     umatch.addURI("ru.k3.desktopui.ItemProvider/apps",ItemManager.APPS_TABLE,URI_CODE);
     umatch.addURI("ru.k3.desktopui.ItemProvider/apps",ItemManager.APPS_TABLE+"/#",URI_CODE_ID);
     
     map=new HashMap<String,String>();
     map.put(ItemManager._ID,ItemManager._ID);
     map.put(ItemManager.TITLE,ItemManager.TITLE);
     map.put(ItemManager.PACKAGE,ItemManager.PACKAGE);
     map.put(ItemManager.ACTIVITY,ItemManager.ACTIVITY);
	 map.put(ItemManager.POSX,ItemManager.POSX);
	 map.put(ItemManager.POSY,ItemManager.POSY);
  }
  
  @Override
  public boolean onCreate(){
     return (db=(new ItemManager(getContext())).getWritableDatabase())==null?false:true;
  }
  
  @Override
  public Cursor query(Uri url,String[]project,String select,String[]selArgs,String sort){
     String orderBy;
     if(TextUtils.isEmpty(sort))orderBy=ItemManager.TITLE;
       else orderBy=sort;
     
     Cursor c=db.query(ItemManager.APPS_TABLE,project,select,selArgs,null,null,orderBy);
     c.setNotificationUri(getContext().getContentResolver(),url);
     return c;
  }
  
  @Override
  public Uri insert(Uri url,ContentValues inval){
     ContentValues val=new ContentValues(inval);
     
     long rowId=db.insert(ItemManager.APPS_TABLE,ItemManager.TITLE,val);
     if(rowId>0){
       Uri uri=ContentUris.withAppendedId(URI,rowId);
       getContext().getContentResolver().notifyChange(uri,null);
       return uri;
     }else throw new SQLException("Insert item filed: "+url);
  }
  
  @Override
  public int delete(Uri url,String where,String[]whereArgs){
     int ret=db.delete(ItemManager.APPS_TABLE,where,whereArgs);
     
     getContext().getContentResolver().notifyChange(url,null);
     return ret;
  }
  
  @Override
  public int update(Uri url,ContentValues val,String where,String[]whereArgs){
     int ret=db.update(ItemManager.APPS_TABLE,val,where,whereArgs);
     
     getContext().getContentResolver().notifyChange(url,null);
     return ret;
  }
  
  @Override
  public String getType(Uri uri){
     return null;
  }
}
