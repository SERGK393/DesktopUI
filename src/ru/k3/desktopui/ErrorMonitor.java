package ru.k3.desktopui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.view.View;
import android.widget.Button;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.content.Intent;

public class ErrorMonitor extends Activity{
    
	public static final String ERROR_TEXT="error_text";
	
	public void onCreate(Bundle state){
		SharedPreferences sh=PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		final String text=sh.getString(ERROR_TEXT,"No errors");
		sh.edit().remove(ERROR_TEXT).apply();
		
		super.onCreate(state);
		setContentView(R.layout.error);
		
		final EditText et=(EditText)findViewById(R.id.error_text);
		final Button cl=(Button)findViewById(R.id.error_continue);
		final Button st=(Button)findViewById(R.id.error_settings);
		et.setText(text);
		cl.setOnClickListener(click);
		st.setOnClickListener(click);
	}
	
	View.OnClickListener click=new View.OnClickListener(){
		public void onClick(View v){
			switch(v.getId()){
				case R.id.error_continue:
				    Intent go=new Intent();
					go.setClass(getApplicationContext(),DesktopUI.class);
					startActivity(go);
					finish();
				    break;
				case R.id.error_settings:
				    startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
					finish();
				    break;
			}
		}
	};
}
