package arkenterprises.garage_o_matic;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
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

import java.util.HashMap;
import java.util.Map;

class Common extends Application {
    private String TAG;
//    private Context context;
//    public Common(String TAG, Context context) {
//        this.TAG = TAG;
//        this.context = context;
//    }
    public Common(String TAG) {
        this.TAG = TAG;
    }

    Map<String, String> createBasicAuthHeader(String username, String password) {
        Map<String, String> headerMap = new HashMap<>();

        String credentials = username + ":" + password;
        String base64EncodedCredentials =
                Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
        headerMap.put("Authorization", "Basic " + base64EncodedCredentials);

        return headerMap;
    }

    protected void sendRegistrationToServer(String token, Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        String userID = sharedPreferences.getString(context.getResources().getString(R.string.pref_key_userID), null);
        Log.d(TAG, "userID: " + userID);
        String tokenAndID = token + "," + userID;

        RequestQueue queue = Volley.newRequestQueue(context);
//        SharedPreferences settingsPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        String baseURL = sharedPreferences.getString(context.getResources().getString(R.string.pref_key_pi_address), null);
        Log.d(TAG, "baseURL: " + baseURL);
        String postURL = baseURL + "/id/" + tokenAndID;

        SharedPreferences sharedPref = context.getSharedPreferences(context.getResources().getString(R.string.preference_file_key),
                Context.MODE_PRIVATE);
        final String username = sharedPref.getString(context.getResources().getString(R.string.username_hint), null);
        final String password = sharedPref.getString(context.getResources().getString(R.string.password_hint), null);

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
