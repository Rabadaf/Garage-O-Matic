package arkenterprises.garage_o_matic;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

public class MyGcmListenerService extends GcmListenerService {

    private static final String TAG = "MyGcmListenerService";

    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.d(TAG, "Data: " + data);
        String type = data.getString("type");
        String doorStatus = data.getString("doorStatus");
        String time = data.getString("time");
        Log.d(TAG, "From: " + from);
        Log.d(TAG, "Type: " + type);
        Log.d(TAG, "DoorStatus: " + doorStatus);
        Log.d(TAG, "Time: " + time);

        SharedPreferences settingsPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean showNotifications = settingsPrefs.getBoolean(getString(R.string.pref_key_show_notifications), false);
        Boolean openNotificationsPref;
        Boolean openTooLongPref;
        Boolean autoClosedPref;
        if (showNotifications) {
            openNotificationsPref = settingsPrefs.getBoolean(getString(R.string.pref_key_door_open_notification), false);
            openTooLongPref = settingsPrefs.getBoolean(getString(R.string.pref_key_door_open_too_long_notification), false);
            autoClosedPref = settingsPrefs.getBoolean(getString(R.string.pref_key_door_auto_closed_notification), false);
        } else {
            openNotificationsPref = false;
            openTooLongPref = false;
            autoClosedPref = false;
        }


        if (type.equals("door_status_changed")) {
            processStatusChange(doorStatus, time, openNotificationsPref);
        }else if (type.equals("door_open_too_long") && openTooLongPref) {
            String openTime = data.getString("openTime");
            processOpenTooLong(time, openTime);
        }else if (type.equals("door_closed_automatically") && autoClosedPref) {
            processAutoClosed(time);
        }
    }

    private void processAutoClosed(String time) {
        DoorOpenNotification don = new DoorOpenNotification();
        don.notify(this, time, null, DoorOpenNotification.DOOR_AUTO_CLOSED_CODE);
    }

    private void processOpenTooLong(String time, String openTime) {
        DoorOpenNotification don = new DoorOpenNotification();
        don.notify(this, time, openTime, DoorOpenNotification.DOOR_OPEN_TOO_LONG_CODE);
    }

    private void processStatusChange(String doorStatus, String time, Boolean openNotificationsPref) {
        if (doorStatus.equals("open") && openNotificationsPref) {
            DoorOpenNotification don = new DoorOpenNotification();
            don.notify(this, time, null, DoorOpenNotification.DOOR_OPEN_CODE);
        } else {
            DoorOpenNotification don = new DoorOpenNotification();
            don.cancel(this, don.DOOR_OPEN_TOO_LONG_CODE);
        }

        Intent intent = new Intent("door_status_changed");
        intent.putExtra("time", time);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

}