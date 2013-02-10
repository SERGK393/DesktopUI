package ru.k3.desktopui;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;

public class ObjWidgetHost extends AppWidgetHost {

	public ObjWidgetHost(Context context, int hostId) {
		super(context, hostId);
	}
	


	@Override
	public AppWidgetHostView onCreateView(Context c, int id, AppWidgetProviderInfo inf) {
		return new ObjWidget(c,id,inf);
	}
}
