package ru.k3.desktopui.r;

import java.util.ArrayList;

import ru.k3.desktopui.IconCache;
import ru.k3.desktopui.Utilities;
import ru.k3.desktopui.DeskView;
import ru.k3.desktopui.Obj;
import ru.k3.desktopui.ObjItem;

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
			Log.w(LOG_TAG,"EVENT:"+action);
			if(action.equals(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE)||
			   action.equals(Intent.ACTION_PACKAGE_ADDED)){
				IconCache.getInstance(c).clearDefaultIcons(true);
				refreshIcons();
				Utilities.invalidate();
			}
			else if(action.equals(Intent.ACTION_PACKAGE_REMOVED)||
			   action.equals(Intent.ACTION_PACKAGE_CHANGED)){
				IconCache.getInstance(c).clearDefaultIcons(false);
				Utilities.invalidate();
			}
			
		}
	}
	
	private void refreshIcons(){
		ArrayList<Obj> itm=DeskView.getObjS();
		if(itm!=null)for(Obj it:itm){
			it.setEnabled(true);
		}
	}
}
