package ru.k3.desktopui;

import android.preference.PreferenceActivity;
import android.os.Bundle;
import android.preference.PreferenceScreen;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.content.Intent;

public class Settings extends PreferenceActivity
{
	PreferenceScreen scr;
	
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle state){
		super.onCreate(state);
		setTitle(R.string.sett);
		scr=getPreferenceManager().createPreferenceScreen(getApplicationContext());
		setPreferenceScreen(scr);
		Preference pr=new Preference(getApplicationContext());
		pr.setTitle(R.string.mysett);
		pr.setOnPreferenceClickListener(click);
		scr.addPreference(pr);
		Preference pr1=new Preference(getApplicationContext());
		pr1.setTitle(R.string.sett_all);
		pr1.setOnPreferenceClickListener(click);
		scr.addPreference(pr1);
	}
	

	OnPreferenceClickListener click=new OnPreferenceClickListener(){
		public boolean onPreferenceClick(Preference pr){
			if(pr.getOrder()==0){
				Intent go=new Intent();
				go.setClass(getApplicationContext(),DUISettings.class);
				startActivity(go);
				finish();
			}
			if(pr.getOrder()==1){
				startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
			}
			return true;
		}
	};
}
