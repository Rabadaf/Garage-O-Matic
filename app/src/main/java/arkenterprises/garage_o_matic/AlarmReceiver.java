package arkenterprises.garage_o_matic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String TAG = "AlarmReceiver";
        String baseURL = intent.getStringExtra("baseURL");
        String username = intent.getStringExtra("username");
        String password = intent.getStringExtra("password");
        Log.d(TAG, baseURL);
        Log.d(TAG, username);
        Log.d(TAG, password);

        checkServerStatus(context, baseURL, username, password);
    }

    Map<String, String> createBasicAuthHeader(String username, String password) {
        Map<String, String> headerMap = new HashMap<>();

        String credentials = username + ":" + password;
        String base64EncodedCredentials =
                Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
        headerMap.put("Authorization", "Basic " + base64EncodedCredentials);

        return headerMap;
    }

    public void checkServerStatus(final Context context, String baseURL, final String username, final String password) {
        String GPIOStatusURL = baseURL + "/GPIO/8/value";
        RequestQueue queue = Volley.newRequestQueue(context);

        final StringRequest getDoorStatus = new StringRequest(Request.Method.GET, GPIOStatusURL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Door Status Response: " + response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Something Went Wrong");
                error.printStackTrace();

                SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d H:m:s yyyy");
                String nowString = sdf.format(new Date());
                DoorOpenNotification don = new DoorOpenNotification();
                don.notify(context, nowString, null, DoorOpenNotification.SERVER_NOT_AVAILABLE_CODE);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return createBasicAuthHeader(username, password);
            }
        };

        Log.i(TAG, "Door status request: " + getDoorStatus);
        queue.add(getDoorStatus);
    }
}
