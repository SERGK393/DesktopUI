package ru.k3.desktopui;

import android.app.Application;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class DUIApp extends Application
{
    
	@Override
	public void onCreate(){
		  Thread.setDefaultUncaughtExceptionHandler(new ErrorCatch());
		  super.onCreate();
	}
	

    public void MessageBox(String str, int dur)
	{
    	Toast.makeText(getApplicationContext(), str, dur)
			.show();
    }
	  
	private class ErrorCatch implements Thread.UncaughtExceptionHandler{
		
		private final Thread.UncaughtExceptionHandler oldHandler;
		
		public ErrorCatch(){
			oldHandler=Thread.getDefaultUncaughtExceptionHandler();
		}
		
		public void uncaughtException(Thread t, Throwable e){
			SharedPreferences.Editor sh=PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
			sh.putString(ErrorMonitor.ERROR_TEXT,e.toString());
			sh.apply();

			oldHandler.uncaughtException(t,e);
		}
	};
}
