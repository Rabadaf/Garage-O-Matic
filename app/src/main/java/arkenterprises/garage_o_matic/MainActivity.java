package arkenterprises.garage_o_matic;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.HashMap;
import java.util.Map;

// TODO: prevent double click on toggle button
// TODO: nicer error handling
// TODO: notify more than once if open too long, maybe every hour?
// TODO: add ability to tell the app that being open is ok for now. Make sure it has some sort of failsafe if we forget to undo that.

public class MainActivity extends AppCompatActivity {

    private static TextView statusTextView;
    private static Button toggleButton;
    private RequestQueue queue;
    private String username;
    private String password;
    private String baseURL;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "MainActivity";
    TextView connectionTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.preference_file_key),
                Context.MODE_PRIVATE);
        username = sharedPref.getString(getString(R.string.username_hint), null);
        password = sharedPref.getString(getString(R.string.password_hint), null);
        Log.d(TAG, "username: " + username);
        Log.d(TAG, "password: " + password);

        SharedPreferences settingsPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        baseURL = settingsPrefs.getString(getString(R.string.pref_key_pi_address), null);

        // Register with GCM, if we have the right play services installed
        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(doorOpenReceiver,
                new IntentFilter("door_status_changed"));

        toggleButton = (Button) findViewById(R.id.toggleButton);
        statusTextView = (TextView) findViewById(R.id.statusText);
        connectionTextView = (TextView) findViewById(R.id.connectionStatusText);
        queue = Volley.newRequestQueue(this);
        checkDoorStatus();
    }

    public void onResume() {
        super.onResume();

        // Make sure the door status is current
        checkDoorStatus();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_appbar, menu);
        return true;
    }

    Map<String, String> createBasicAuthHeader(String username, String password) {
        Map<String, String> headerMap = new HashMap<>();

        String credentials = username + ":" + password;
        String base64EncodedCredentials =
                Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
        headerMap.put("Authorization", "Basic " + base64EncodedCredentials);

        return headerMap;
    }

    public void checkDoorStatus() {
        String GPIOStatusURL = baseURL + "/GPIO/8/value";

        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnected();

        if (isConnected) {
            final StringRequest getDoorStatus = new StringRequest(Request.Method.GET, GPIOStatusURL, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.i(TAG, "Door Status Response: " + response);
                    String statusText;
                    String buttonText;
                    int backColor;
                    if (response.equals("1")) {
                        statusText = getString(R.string.text_status_closed);
                        buttonText = getString(R.string.button_toggle_open);
                        backColor = ContextCompat.getColor(getBaseContext(), R.color.colorBackgroundClosed);
                    } else {
                        statusText = getString(R.string.text_status_open);
                        buttonText = getString(R.string.button_toggle_close);
                        backColor = ContextCompat.getColor(getBaseContext(), R.color.colorBackgroundOpen);
                    }
                    statusTextView.setText(statusText);
                    statusTextView.setBackgroundColor(backColor);
                    toggleButton.setText(buttonText);
                    toggleButton.setEnabled(true);
                    connectionTextView.setText("");
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "Something Went Wrong");
                    error.printStackTrace();
                    connectionTextView.setText(R.string.connection_error_message);
                    statusTextView.setText(getString(R.string.text_status_unknown));
                    statusTextView.setBackgroundColor(ContextCompat.getColor(getBaseContext(), R.color.colorBackgroundUnknown));
                    toggleButton.setText(getString(R.string.button_toggle_unknown));
                    toggleButton.setEnabled(false);
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

    public void toggleDoor(View view) {

        String postURL = baseURL + "/GPIO/17/sequence/100,101";

        Response.Listener postListener = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Toggle door response: " + response);
                connectionTextView.setText("");
            }
        };

        Response.ErrorListener postErrorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Something Went Wrong");
                error.printStackTrace();
                connectionTextView.setText(R.string.connection_error_message);
            }
        };

        StringRequest postRelay = new StringRequest(Request.Method.POST, postURL, postListener,
                postErrorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return createBasicAuthHeader(username, password);
            }
        };

        Log.i(TAG, "PostRelay: " + postRelay);
        queue.add(postRelay);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                // User chose the "Settings" item, show the app settings UI
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;

            case R.id.action_logout:
                // User chose the "Logout" item, do it, then show Login UI
                SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.preference_file_key),
                        Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();

                editor.remove(getString(R.string.username_hint));
                editor.remove(getString(R.string.password_hint));
                editor.commit();

                Intent loginIntent = new Intent(this, LoginActivity.class);
                startActivity(loginIntent);
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    private BroadcastReceiver doorOpenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String time = intent.getStringExtra("time");
            Log.i(TAG, "Door status changed message received: " + time);
            checkDoorStatus();
        }
    };
}
