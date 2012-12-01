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

import java.io.*;
import java.util.ArrayList;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.View;
import java.util.*;
import android.content.pm.*;
import android.content.*;
import android.preference.PreferenceManager;
import android.content.res.Resources;
import android.net.Uri;


public final class Utilities {
    private static final String LOG_TAG="DesktopUI.Utilities";
	private static final String Dels="aouiey -аоуыэяёюие";
	public static final int POP_CONTEXT1=1;
	public static final int POP_CONTEXT2=2;
	public static final int POP_APPS=4;
	public static final int POP_ALL_APPS=8;
	
	private static PopupElement pop_c1=null;
	private static PopupElement pop_c2=null;
	private static PopupElement pop_apps=null;
	private static PopupElement pop_all_apps=null;
	
	private static List<ResolveInfo> apps_n;
 
    private static final Rect sOldBounds = new Rect();
    private static final Canvas sCanvas = new Canvas();
	private static DesktopUI desk=null;
	private static DeskView dv=null;
	private static PackageManager man=null;
	private static int sIconSize=-1;
	private static Bitmap.Config quality=null;
/*
    static {
        sCanvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.DITHER_FLAG,
                Paint.FILTER_BITMAP_FLAG));
    }
*/
    public static Bitmap createIconBitmap(Drawable icon, Context context) {
		if(icon==null)return null;
        synchronized (sCanvas) { // we share the statics :-(
            if (sIconSize==-1&&quality==null) initStatics(context);
			if (sIconSize==-1&&quality==null) return null;

            int width = sIconSize;
            int height = sIconSize;
			
            final Bitmap bitmap = Bitmap.createBitmap(width, height,quality);
            final Canvas canvas = sCanvas;
            canvas.setBitmap(bitmap);

            sOldBounds.set(icon.getBounds());
            icon.setBounds(0, 0, width, height);
            icon.draw(canvas);
            icon.setBounds(sOldBounds);

            return bitmap;
        }
    }

    public static void initStatics(Context context) {
		if(desk==null)desk=(DesktopUI)context;
		if(dv==null)dv=(DeskView)(desk.findViewById(R.id.desk));
		if(man==null)man=desk.getPackageManager();
        sIconSize=desk.getPrefInt(R.string.pref_is);
		quality=desk.getPrefBool(R.string.pref_bq)?Bitmap.Config.ARGB_8888:Bitmap.Config.ARGB_4444;
    }
	public static void resetStatics(){
		sIconSize=-1;
		quality=null;
		desk=null;
		dv=null;
		man=null;
		if(pop_apps!=null)pop_apps.resetAdapter();
		pop_apps=null;
		if(pop_c1!=null)pop_c1.resetAdapter();
		pop_c1=null;
		if(pop_c2!=null)pop_c2.resetAdapter();
		pop_c2=null;
		if(apps_n!=null)apps_n.clear();
		apps_n=null;
	}

    public static byte[] flattenBitmap(Bitmap bitmap) {
        // Try go guesstimate how much space the icon will take when serialized
        // to avoid unnecessary allocations/copies during the write.
        int size = bitmap.getWidth() * bitmap.getHeight() * 4;
        ByteArrayOutputStream out = new ByteArrayOutputStream(size);
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            return out.toByteArray();
        } catch (IOException e) {
            Log.w(LOG_TAG, "Could not write icon");
            return null;
        }
    }
	
	public static ArrayList<String> partString(Paint p,String s,int w){
		ArrayList<String> a=new ArrayList<String>();
		if (s==null){
			a.add("n/a");
			return a;
		}
		Rect b=new Rect();
		int len=s.length();
		for(int i=1,j=0,k=0;i<=len;i++){
			p.getTextBounds(s,j,i,b);
			char c=s.charAt(i-1);
			char n2c='\0';
			char pc='\0';
			char p2c='\0';
			if(i>1)pc=s.charAt(i-2);
			if(i>2)p2c=s.charAt(i-3);
			if(i<len-2)n2c=s.charAt(i+1);
			if(Dels.contains(String.valueOf(c)))
				if(pc!=' '&&p2c!=' '&&n2c!=' ')
					k=i;
			if(b.right>w&&i<len-1){
				a.add(s.substring(j,k));
				j=k; k=i; continue;
			}
			if(i==len)a.add(s.substring(j,i));
		}
		return a;
	}
	
	public static boolean isNewApi(){
		return Build.VERSION.SDK_INT>=11;
	}
	public static boolean isFromApi15(){
		return Build.VERSION.SDK_INT>=15;
	}
	
	public static DesktopUI getDesktopUI(){
		return desk;
	}
	
	public static void invalidate(){
		if(dv!=null)dv.postInvalidate();
	}
	
	public static void execAppInfo(Obj it){
		try
		{
			String appPackage = it.getPackage();
			if ( appPackage != null && desk != null)
			{
				Intent intent = new Intent();
				final int apiLevel = Build.VERSION.SDK_INT;
				if (apiLevel >= 9)
				{ // above 2.3
					intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
					Uri uri = Uri.fromParts("package", appPackage, null);
					intent.setData(uri);
				}
				else
				{ // below 2.3
					final String appPkgName = (apiLevel == 8 ? "pkg" : "com.android.settings.ApplicationPkgName");
					intent.setAction(Intent.ACTION_VIEW);
					intent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
					intent.putExtra(appPkgName, appPackage);
				}
				desk.startActivity(intent);
			}
		}
		catch (Exception e)
		{
			// failed to tell start app info
		}
	}
	
	public static boolean isNewPopup(int type,View anchor){
		switch(type){
			case POP_CONTEXT1:
		        if(pop_c1==null){
		            pop_c1=new PopupElement(anchor);
			        return true;
		        }else return false;
		    case POP_CONTEXT2:
				if(pop_c2==null){
					pop_c2=new PopupElement(anchor);
					return true;
				}else return false;
			case POP_APPS:
				if(pop_apps==null){
					pop_apps=new PopupElement(anchor);
					return true;
				}else return false;
			case POP_ALL_APPS:
				if(pop_all_apps==null){
					pop_all_apps=new PopupElement(anchor);
					return true;
				}else return false;
		}
		return false;
	}
	public static PopupElement getPopupList(int type){
		switch(type){
			case POP_CONTEXT1:
		        return pop_c1;
		    case POP_CONTEXT2:
				return pop_c2;
			case POP_APPS:
				return pop_apps;
			case POP_ALL_APPS:
				return pop_all_apps;
		}
		return null;
	}
	public static List<ResolveInfo> getResolveInfos(int type){
		switch(type){
			case POP_APPS:
			    Intent intent_n = new Intent(Intent.ACTION_MAIN, null);
				intent_n.addCategory(Intent.CATEGORY_LAUNCHER);

		        boolean newl=false;
				if(apps_n==null)newl=true;
				else if(apps_n.isEmpty()){
					apps_n=null;
					newl=true;
				}
				if(newl){
					apps_n = man.queryIntentActivities(intent_n, 0);
					Collections.sort(apps_n, new ResolveInfo.DisplayNameComparator(man));
		        }
				return apps_n;
		}
		return null;
	}
	
	public static void sharedToDefault(Context c){
		SharedPreferences.Editor sh=PreferenceManager.getDefaultSharedPreferences(c).edit();
		Resources res=c.getResources();
		sh.clear();
		sh.putBoolean(res.getString(R.string.pref_bq),false);
		sh.putString(res.getString(R.string.pref_is),String.valueOf((int)res.getDimension(R.dimen.itm)));
		sh.putString(res.getString(R.string.pref_fs),String.valueOf((int)res.getDimension(R.dimen.fnt)));
		sh.putBoolean(res.getString(R.string.pref_actionbar),false);
		sh.putBoolean(res.getString(R.string.pref_cs),false);
		sh.putString(res.getString(R.string.pref_ss),"-1");
		sh.putBoolean(res.getString(R.string.pref_icres),false);
		sh.putString(res.getString(R.string.pref_icdensity),"320");
		sh.putString(res.getString(R.string.pref_wall),"2");
		sh.putBoolean(res.getString(R.string.pref_oldtheme),false);
		
		sh.putInt(res.getString(R.string.prefex_defx),0);
		sh.putInt(res.getString(R.string.prefex_defy),0);
		sh.apply();
	}
}
