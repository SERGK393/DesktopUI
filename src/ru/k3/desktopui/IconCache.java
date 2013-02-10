/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.k3.desktopui;

import java.lang.ref.Reference;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.content.res.Resources;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import java.util.Iterator;

/**
 * Cache of application icons.  Icons can be made from any thread.
 */
public class IconCache {

    private static final String LOG_TAG="DesktopUI.IconCache";
	
	private static final int INITIAL_ICON_CACHE_CAPACITY = 50;

    private static IconCache sIconCache=null;
	
	private static class CacheEntry {
        public Bitmap icon;
//		public Reference<Bitmap> ref;
//		public Bitmap small;
    }
	private static class AsyncEntry{
		AsyncEntry(CacheEntry e,ComponentName c,ObjItem it){
			this.e=e;
			this.c=c;
			this.it=it;
		}
		CacheEntry e;
		ComponentName c;
		ObjItem it;
	}

    private Bitmap mDefaultIcon;
	
    private final Context mContext;
    private final PackageManager mPackageManager;
    private final HashMap<ComponentName, CacheEntry> mCache =
            new HashMap<ComponentName, CacheEntry>(INITIAL_ICON_CACHE_CAPACITY);
	
	private boolean res_icon,ic_unuse;
	private int res_density;
	
	public static IconCache getInstance(Context c){
		if(sIconCache==null&&c!=null)sIconCache=new IconCache(c);
		return sIconCache;
	}

    private IconCache(Context context) {
		Log.d(LOG_TAG,"Create");
        mContext = context;
        mPackageManager = context.getPackageManager();
    }
	
	public void setSettings(){
		DesktopUI d=(DesktopUI)mContext;
		res_icon=d.getPrefBool(R.string.pref_icres);
		res_density=d.getPrefInt(R.string.pref_icdensity);
		ic_unuse=d.getPrefBool(R.string.pref_icunuse);
        mDefaultIcon = makeDefaultIcon(mContext);
	}

    public Bitmap getDefaultIcon() {
    	return mDefaultIcon;
    }

    public Context getContext() {
    	return mContext;
    }

    private Bitmap makeDefaultIcon(Context c) {
        return Utilities.createIconBitmap(mPackageManager.getDefaultActivityIcon(),c);
    }
	
	public void clearDefaultIcons(boolean yes){
		Iterator<ComponentName> it=mCache.keySet().iterator();
		while(it.hasNext()){
			Log.i(LOG_TAG,"Iterator.hasNext()");
			CacheEntry e=mCache.get(it.next());
			if(isDefaultIcon((e.icon)))e.icon=null;
			else if(!yes)e.icon=null;
		}
	}

    /**
     * Remove any records for the supplied ComponentName.
     */
    public boolean remove(ComponentName componentName) {
        synchronized (mCache) {
        	if (componentName != null)
        		return mCache.remove(componentName)!=null;
        }
		return false;
    }

    /**
     * Empty out the cache.
     */
    public void flush() {
		Log.d(LOG_TAG,"Flush "
		      +mCache.size()+" objects");
        synchronized (mCache) {
            mCache.clear();
        }
    }

    public void addToCache(ComponentName componentName, String title, Bitmap icon) {
    	CacheEntry en=mCache.get(componentName);
    	if (en == null) {
            en = new CacheEntry();
        }
        mCache.put(componentName, en);

        en.icon = icon;
    }

    public Bitmap getIcon(ComponentName component, ObjItem it) {
        synchronized (mCache) {
        	CacheEntry en=mCache.get(component);
            if (component == null||en==null||en.icon == null) {
	            if (component == null) {
	                return mDefaultIcon;
	            }
	            return cacheLocked(component,it).icon;
            } else {
	            return en.icon;
            }
        }
    }

    public boolean isDefaultIcon(Bitmap icon) {
        return mDefaultIcon == icon;
    }
	
	@SuppressLint("NewApi")
	private Drawable getResIcon(Resources res,int icId){
		Drawable d;
		try{
		    if(Utilities.isFromApi15()&&res_density>0)
			d=res.getDrawableForDensity(icId,res_density);
			else d=res.getDrawable(icId);
		}catch(Resources.NotFoundException e){
			d=null;
		}
		
		return d;
	}
	
	private Drawable getActivityIcon(ComponentName componentName){
		Log.d(LOG_TAG,"getActivityIcon("+componentName+");");
		if (!res_icon){
			Drawable d;
			try{
			    d=mPackageManager.getActivityIcon(componentName);
		    }catch (PackageManager.NameNotFoundException e){
				d=null;
			}
			return d;
		}
		Resources res;
		ActivityInfo inf;
		try{
			res=mPackageManager.getResourcesForActivity(componentName);
			inf=mPackageManager.getActivityInfo(componentName, 0);
		}catch(PackageManager.NameNotFoundException e){
			res=null;
			inf=null;
		}
		
		if(res!=null&&inf!=null){
			int icId=inf.getIconResource();
			if(icId!=0){
				return getResIcon(res,icId);
			}
		}
		return null;
	}

    private CacheEntry cacheLocked(ComponentName componentName, ObjItem it) {
    	CacheEntry en=mCache.get(componentName);
    	if (en == null) {
            en = new CacheEntry();
        }
        mCache.put(componentName, en);
        
        if (en.icon == null || en.icon.isRecycled()) 
            try
			{
				en.icon = mDefaultIcon;
				new LoadIcon().execute(new AsyncEntry(en,componentName,it));
//				en.icon = Utilities.createIconBitmap(getActivityIcon(componentName), mContext);
				if(en.icon==null || en.icon.isRecycled()) throw new Exception();
			}
			catch (Exception e)
			{
				Log.e(LOG_TAG,"CacheLocked function failed: en="+en+" en.icon="+en.icon);
				en.icon = mDefaultIcon;
			}
			Log.d(LOG_TAG,"Cache size: "+mCache.size());
        
        return en;
    }
	
	private class LoadIcon extends AsyncTask<AsyncEntry,Void,Void>
	{

		protected Void doInBackground(AsyncEntry... ae)
		{
			Log.d(LOG_TAG,"AsyncTask started: ae[0]="+ae[0]);
			ae[0].e.icon = Utilities.createIconBitmap(getActivityIcon(ae[0].c), mContext);
			if(ae[0].e.icon==null){
				ae[0].e.icon=mDefaultIcon;
				ae[0].it.setEnabled(ic_unuse);
			}else ae[0].it.setEnabled(true);
			Utilities.invalidate();
			Log.d(LOG_TAG,"AsyncTask ended: ae[0]="+ae[0]+
			              " ae[0].e.icon="+ae[0].e.icon);
			return null;
		}

		
	}
}
