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

import java.util.HashMap;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Log;

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

    private final Bitmap mDefaultIcon;
    private final Context mContext;
    private final PackageManager mPackageManager;
    private final HashMap<ComponentName, CacheEntry> mCache =
            new HashMap<ComponentName, CacheEntry>(INITIAL_ICON_CACHE_CAPACITY);

    public IconCache(Context context) {
		Log.d(LOG_TAG,"Create");
        mContext = context;
        mPackageManager = context.getPackageManager();
        mDefaultIcon = makeDefaultIcon();
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
    public void remove(ComponentName componentName) {
        synchronized (mCache) {
        	if (componentName != null)
        		mCache.remove(componentName);
        }
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
        CacheEntry entry = mCache.get(componentName);
        if (entry == null) {
            entry = new CacheEntry();
        }
        mCache.put(componentName, entry);

        entry.icon = icon;
    }

    public Bitmap getIcon(ComponentName component) {
        synchronized (mCache) {
            CacheEntry entry = mCache.get(component);
            if (component == null ||entry == null || entry.icon == null) {
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

    private CacheEntry cacheLocked(ComponentName componentName) {
        CacheEntry entry = mCache.get(componentName);
        if (entry == null) {
            entry = new CacheEntry();

            mCache.put(componentName, entry);

        }
        
        if ( entry.icon == null ) {
            try
			{
				entry.icon = Utilities.createIconBitmap(mPackageManager.getActivityIcon(componentName), mContext);
			}
			catch (Exception e)
			{}
        }else if(entry.icon.isRecycled()){
			try
			{
				entry.icon.createBitmap(Utilities.createIconBitmap(mPackageManager.getActivityIcon(componentName), mContext));
			}
			catch (Exception e)
			{}
		}
        return entry;
    }
}
