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
import android.widget.ArrayAdapter;
import android.view.View;


public final class Utilities {
    private static final String LOG_TAG="DesktopUI.Utilities";
	private static final String Dels="aouiey -аоуыэяёюие";
	public static final int POP_CONTEXT1=1;
	public static final int POP_CONTEXT2=2;
	public static final int POP_APPS=4;
	
	private static PopupList pop_c1=null;
	private static PopupList pop_c2=null;
	private static PopupList pop_apps=null;
 
    private static final Rect sOldBounds = new Rect();
    private static final Canvas sCanvas = new Canvas();
	private static DesktopUI desk=null;
	private static int sIconSize=-1;
	private static Bitmap.Config quality=null;
/*
    static {
        sCanvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.DITHER_FLAG,
                Paint.FILTER_BITMAP_FLAG));
    }
*/
    public static Bitmap createIconBitmap(Drawable icon, Context context) {
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

    private static void initStatics(Context context) {
		if(desk==null)desk=(DesktopUI)context;
        sIconSize=desk.getPref(0);
		quality=desk.getPref(2)==0?Bitmap.Config.ARGB_4444:Bitmap.Config.ARGB_8888;
    }
	public static void resetStatics(){
		sIconSize=-1;
		quality=null;
		desk=null;
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
		Log.d(LOG_TAG,"Api version:"+Build.VERSION.SDK_INT);
		return Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB;
	}
	
	public static boolean isNewPopup(int type,View anchor){
		switch(type){
			case POP_CONTEXT1:
		        if(pop_c1==null){
		            pop_c1=new PopupList(anchor);
			        return true;
		        }else return false;
		    case POP_CONTEXT2:
				if(pop_c2==null){
					pop_c2=new PopupList(anchor);
					return true;
				}else return false;
			case POP_APPS:
				if(pop_apps==null){
					pop_apps=new PopupList(anchor);
					return true;
				}else return false;
		}
		return false;
	}
	public static PopupList getPopupList(int type){
		switch(type){
			case POP_CONTEXT1:
		        return pop_c1;
		    case POP_CONTEXT2:
				return pop_c2;
			case POP_APPS:
				return pop_apps;
		}
		return null;
	}
}
