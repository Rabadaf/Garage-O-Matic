package arkenterprises.garage_o_matic;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

public class MyGcmListenerService extends GcmListenerService {

    private static final String TAG = "MyGcmListenerService";

    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.d(TAG, "Data: " + data);
        String message = data.getString("message");
        String doorStatus = data.getString("DoorStatus");
        String time = data.getString("time");
        String notification = data.getString("notification");
        Log.d(TAG, "From: " + from);
        Log.d(TAG, "Message: " + message);
        Log.d(TAG, "DoorStatus: " + doorStatus);
        Log.d(TAG, "Time: " + time);
        Log.d(TAG, "Notification: " + notification);

        //TODO: Process message here
    }


}
