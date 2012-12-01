package ru.k3.desktopui;

import ru.k3.desktopui.db.DbProvider;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.Preference;
import android.app.Dialog;
import android.app.AlertDialog;
import android.content.DialogInterface;

public class DUISettings extends PreferenceActivity
{
	private static final int DIALOG_CLEAR=0xFF;
	private PreferenceScreen scr;
	
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle state){
		super.onCreate(state);
		setTitle(R.string.mysett);
		addPreferencesFromResource(R.xml.mysettings);
		scr=getPreferenceScreen();
		Preference pr=new Preference(getApplicationContext());
		pr.setTitle(R.string.mst_reset);
		scr.addPreference(pr);
		
		if(!Utilities.isNewApi()){
			scr.removePreference(scr.findPreference(getString(R.string.pref_actionbar)));
			scr.removePreference(scr.findPreference(getString(R.string.pref_oldtheme)));
		}
        if(!Utilities.isFromApi15())((PreferenceScreen)findPreference(getString(R.string.pref_icons_category))).removePreference(findPreference(getString(R.string.pref_icdensity)));
		else{
			Preference icres=findPreference(getString(R.string.pref_icdensity));
			icres.setEnabled(icres.getSharedPreferences().getBoolean(getString(R.string.pref_icres),false));
		}
	}
	
	public void onBackPressed(){
		super.onBackPressed();
	}
	
	protected Dialog onCreateDialog(int id)
	{
        Dialog dialog;
        switch (id)
		{
			case DIALOG_CLEAR:
				dialog = ClearMenu();
				break;
			default:
				dialog = null;
        }

		return dialog;
    }
	
	public AlertDialog ClearMenu()
    {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage(R.string.clear_pref)
			.setCancelable(true)
			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id)
				{
					deleteDatabase(DbProvider.DB_MAIN);
					Utilities.sharedToDefault(getApplicationContext());
					System.exit(0);
				}
			})
			.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id)
				{
					dialog.cancel();
				}
			});
    	AlertDialog alert = builder.create();
    	return alert;
    }
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen s,Preference p){
		if(p.getTitle().toString().equals(getString(R.string.mst_reset))){
			showDialog(DIALOG_CLEAR);
			return true;
		}
		
		if(Utilities.isFromApi15()&&p.getTitle().toString().equals(getString(R.string.mst_res_icons))){
			Preference icres=findPreference(getString(R.string.pref_icdensity));
			icres.setEnabled(icres.getSharedPreferences().getBoolean(getString(R.string.pref_icres),false));
		}
		
		return super.onPreferenceTreeClick(s,p);
	}
}
