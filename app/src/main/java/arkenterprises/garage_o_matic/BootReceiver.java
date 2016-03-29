package arkenterprises.garage_o_matic;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String TAG = "BootReceiver";
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.preference_file_key),
                    Context.MODE_PRIVATE);
            String username = sharedPref.getString(context.getString(R.string.username_hint), null);
            String password = sharedPref.getString(context.getString(R.string.password_hint), null);
            Log.d(TAG, "username: " + username);
            Log.d(TAG, "password: " + password);

            SharedPreferences settingsPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            String baseURL = settingsPrefs.getString(context.getString(R.string.pref_key_pi_address), null);
            Log.d(TAG, "baseURL: " + baseURL);

            Boolean showNotifications = settingsPrefs.getBoolean(context.getString(R.string.pref_key_show_notifications), false);
            Boolean serverDownPref = showNotifications && settingsPrefs.getBoolean(context.getString(R.string.pref_key_server_down_notification), false);
            if (serverDownPref) {
                Intent alarmIntent = new Intent(context, AlarmReceiver.class);
                alarmIntent.putExtra("baseURL", baseURL);
                alarmIntent.putExtra("username", username);
                alarmIntent.putExtra("password", password);
                PendingIntent pIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);
                AlarmManager aManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                aManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, AlarmManager.INTERVAL_HALF_DAY, AlarmManager.INTERVAL_HALF_DAY, pIntent);
            }
        }
    }
}
