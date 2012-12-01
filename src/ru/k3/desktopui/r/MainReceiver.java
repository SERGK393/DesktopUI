package ru.k3.desktopui.r;

import ru.k3.desktopui.IconCache;
import ru.k3.desktopui.Utilities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import android.util.Log;

public class MainReceiver extends BroadcastReceiver{
	
	private static String LOG_TAG="DesktopUI.MainReceiver";
	
	@Override
	public void onReceive(Context c, Intent i){
		String action=i.getAction();
		if(action!=null){
			if(action.equals(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE)){
				Log.w(LOG_TAG,"EXTERNAL_APPLICATIONS_AVAILABLE EVENT!!!");
				IconCache.getInstance(c).clearDefaultIcons();
				Utilities.invalidate();
			}
			
		}
	}
}
