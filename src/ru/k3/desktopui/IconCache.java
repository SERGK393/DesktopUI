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
import java.lang.ref.SoftReference;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.content.res.Resources;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;

/**
 * Cache of application icons.  Icons can be made from any thread.
 */
public class IconCache {

    private static final String LOG_TAG="DesktopUI.IconCache";
	
	private static final int INITIAL_ICON_CACHE_CAPACITY = 50;

    private static class CacheEntry {
        public Bitmap icon;
//		public Bitmap small;
    }
	private static class AsyncEntry{
		AsyncEntry(CacheEntry e,ComponentName c){
			this.e=e;
			this.c=c;
		}
		CacheEntry e;
		ComponentName c;
	}

    private final Bitmap mDefaultIcon;
    private final Context mContext;
    private final PackageManager mPackageManager;
    private final HashMap<ComponentName, Reference<CacheEntry>> mCache =
            new HashMap<ComponentName, Reference<CacheEntry>>(INITIAL_ICON_CACHE_CAPACITY);
	
	private boolean res_icon,delload;
	private int res_density;

    public IconCache(Context context) {
		Log.d(LOG_TAG,"Create");
        mContext = context;
        mPackageManager = context.getPackageManager();
        mDefaultIcon = makeDefaultIcon();
    }
	
	public void setSettings(){
		DesktopUI d=(DesktopUI)mContext;
		res_icon=d.getPrefBool(R.string.pref_icres);
		res_density=d.getPrefInt(R.string.pref_icdensity);
		delload=d.getPrefBool(R.string.pref_icdelload);
	}

    public Bitmap getDefaultIcon() {
    	return mDefaultIcon;
    }

    public Context getContext() {
    	return mContext;
    }

    private Bitmap makeDefaultIcon() {
        Drawable d = mPackageManager.getDefaultActivityIcon();
        Bitmap b = Bitmap.createBitmap(Math.max(d.getIntrinsicWidth(), 1),
                Math.max(d.getIntrinsicHeight(), 1),
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0, 0, b.getWidth(), b.getHeight());
        d.draw(c);
        return b;
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
    	Reference<CacheEntry> ref=mCache.get(componentName);
    	if (ref == null) {
            ref = new SoftReference<CacheEntry>(new CacheEntry());
        }
        CacheEntry entry = ref.get();
        if (entry == null) {
            entry = new CacheEntry();
        }
        mCache.put(componentName, ref);

        entry.icon = icon;
    }

    public Bitmap getIcon(ComponentName component) {
        synchronized (mCache) {
        	Reference<CacheEntry> ref=mCache.get(component);
            CacheEntry entry;
            if (component == null||ref==null||(entry=ref.get())==null||entry == null || entry.icon == null) {
	            if (component == null) {
	                return mDefaultIcon;
	            }
	            return cacheLocked(component).icon;
            } else {
	            return entry.icon;
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
			d=mPackageManager.getDefaultActivityIcon();
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
				d=mPackageManager.getDefaultActivityIcon();
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
		return mPackageManager.getDefaultActivityIcon();
	}

    private CacheEntry cacheLocked(ComponentName componentName) {
    	Reference<CacheEntry> ref=mCache.get(componentName);
    	if (ref == null) {
            ref = new SoftReference<CacheEntry>(new CacheEntry());
        }
        CacheEntry entry = ref.get();
        if (entry == null) {
            entry = new CacheEntry();
        }
        mCache.put(componentName, ref);
        
        if ( entry.icon == null ) {
            try
			{
				if(delload)new LoadIcon().execute(new AsyncEntry(entry,componentName));
				else entry.icon = Utilities.createIconBitmap(getActivityIcon(componentName), mContext);
				if(entry.icon==null) throw new Exception();
			}
			catch (Exception e)
			{
				entry.icon = mDefaultIcon;
			}
        }else if(entry.icon.isRecycled())
        			entry.icon=mDefaultIcon;
        
        return entry;
    }
	
	private class LoadIcon extends AsyncTask<AsyncEntry,Void,Void>
	{

		protected Void doInBackground(AsyncEntry... e)
		{
			e[0].e.icon = mDefaultIcon;
			e[0].e.icon = Utilities.createIconBitmap(getActivityIcon(e[0].c), mContext);
			Utilities.invalidate();
			return null;
		}

		
	}
}
