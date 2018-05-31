package arkenterprises.garage_o_matic;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.HashMap;
import java.util.Map;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class RegistrationIntentService extends IntentService {
    private static final String TAG = "RegIntentService";

    public RegistrationIntentService() {
        super("RegistrationIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        try {
            Bundle extras = intent.getExtras();
            String token;
            if (extras == null) {
                token = FirebaseInstanceId.getInstance().getToken();
            } else {
                token = extras.getString("TOKEN");
            }

            Log.i(TAG, "GCM Registration Token: " + token);

            String userID = sharedPreferences.getString(getString(R.string.pref_key_userID), null);
            String tokenAndID = token + "," + userID;
            sendRegistrationToServer(tokenAndID);

            sharedPreferences.edit().putBoolean(getString(R.string.pref_key_sentTokenToServer), true).apply();
        } catch (Exception e) {
            Log.d(TAG, "Failed to complete token refresh", e);
            sharedPreferences.edit().putBoolean(getString(R.string.pref_key_sentTokenToServer), false).apply();
        }

    }

    Map<String, String> createBasicAuthHeader(String username, String password) {
        Map<String, String> headerMap = new HashMap<>();

        String credentials = username + ":" + password;
        String base64EncodedCredentials =
                Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
        headerMap.put("Authorization", "Basic " + base64EncodedCredentials);

        return headerMap;
    }

    private void sendRegistrationToServer(String token) {
        RequestQueue queue = Volley.newRequestQueue(this);
        SharedPreferences settingsPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String baseURL = settingsPrefs.getString(getString(R.string.pref_key_pi_address), null);
        String postURL = baseURL + "/id/" + token;

        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.preference_file_key),
                Context.MODE_PRIVATE);
        final String username = sharedPref.getString(getString(R.string.username_hint), null);
        final String password = sharedPref.getString(getString(R.string.password_hint), null);

        Response.Listener postListener = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Volley response: " + response);
            }
        };

        Response.ErrorListener postErrorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "Something Went Wrong");
                error.printStackTrace();
            }
        };

        StringRequest postRegID = new StringRequest(Request.Method.POST, postURL, postListener,
                postErrorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return createBasicAuthHeader(username, password);
            }
        };

        Log.d(TAG, "PostRelay: " + postRegID);
        queue.add(postRegID);
    }

}
