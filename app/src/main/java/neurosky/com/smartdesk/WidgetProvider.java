package neurosky.com.smartdesk;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import neurosky.com.smartdesk.service.BluetoothService;

/**
 * Created by yeonjukko on 16. 7. 21..
 */
public class WidgetProvider extends AppWidgetProvider {

    private String temperature;
    private String humidity;
    private String light;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, getClass()));
        for (int i = 0; i < appWidgetIds.length; i++) {
            updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
        }
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.widget_main);
        updateViews.setTextViewText(R.id.tv_degree, temperature);
        updateViews.setTextViewText(R.id.tv_humidity, humidity);
        updateViews.setTextViewText(R.id.tv_lux, light);
        Log.d("tttt", "update"+":"+temperature+":"+humidity+":"+light);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent.getAction().equals(BluetoothService.ACTION_RECEIVED_DATA)) {
            Log.d("tttt", "re");


            temperature = intent.getStringExtra(BluetoothService.FLAG_TEMPERATURE);
            humidity = intent.getStringExtra(BluetoothService.FLAG_HUMIDITY);
            light = intent.getStringExtra(BluetoothService.FLAG_LIGHT);

            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context.getApplicationContext());
            ComponentName thisWidget = new ComponentName(context.getApplicationContext(), WidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            if (appWidgetIds != null && appWidgetIds.length > 0) {
                onUpdate(context, appWidgetManager, appWidgetIds);
            }
        }
    }
}
