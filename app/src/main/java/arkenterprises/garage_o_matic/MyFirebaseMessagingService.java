package arkenterprises.garage_o_matic;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFBMessagingService";

    @Override
    public void onMessageReceived(RemoteMessage message){
        Log.d(TAG, "From: " + message.getFrom());

        Map data = message.getData();
        Log.d(TAG, "Data: " + data);
        String type = data.get("type").toString();
        String doorStatus = data.get("doorStatus").toString();
        String time = data.get("time").toString();
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
            String openTime = data.get("openTime").toString();
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