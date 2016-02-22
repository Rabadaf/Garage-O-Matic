package arkenterprises.garage_o_matic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


// TODO: Add security
// TODO: Add notifications
// TODO: Add customization
// TODO: Make it close automatically at night
// TODO: update status in background, all the time
// TODO: close activities correctly

public class MainActivity extends AppCompatActivity {

    private static TextView statusTextView;
    private Handler handler;
    private RequestQueue queue;
    private String username;
    private String password;
    private SharedPreferences settingsPrefs;
    private String baseURL;
    boolean statusCheckError;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "MainActivity";
//    private static TextView userNameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        Intent intent = getIntent();
//        String savedUsername = intent.getStringExtra(LoginActivity.EXTRA_USERNAME);
//        String savedPassword = intent.getStringExtra(LoginActivity.EXTRA_PASSWORD);

        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.preference_file_key),
                Context.MODE_PRIVATE);
        username = sharedPref.getString(getString(R.string.username_hint), null);
        password = sharedPref.getString(getString(R.string.password_hint), null);
        System.out.println("Main Activity check 1: " + username);
        System.out.println("Main Activity check 1: " + password);

        settingsPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        baseURL = settingsPrefs.getString(getString(R.string.pref_key_pi_address), "test");
//        baseURL = settingsPrefs.getString(getString(R.string.pref_key_pi_address), null);
//        GPIOStatusURL = baseURL + "/GPIO/8/value";
//        System.out.println("baseURL 1: " + baseURL);

        // Register with GCM, if we have the right play services installed
        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }

        statusTextView = (TextView) findViewById(R.id.statusText);
//        handler = new Handler();
        queue = Volley.newRequestQueue(this);
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

//        Response.Listener statusListener = new Response.Listener<String>() {
//            @Override
//            public void onResponse(String response) {
////            System.out.println(response);
//                // TODO: Make background color change
//                String statusText;
//                if (response.equals("1")) {
//                    statusText = "Closed";
//                }
//                else {
//                    statusText = "Open";
//                }
//                statusTextView.setText(statusText);
//                queue.add(getRelayStatus);
//            }
//        };
//
//        Response.ErrorListener statusErrorListener = new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError error) {
//                System.out.println("Something Went Wrong");
//                error.printStackTrace();
//            }
//        };


        final StringRequest getDoorStatus = new StringRequest(Request.Method.GET, GPIOStatusURL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                System.out.println(response);
                // TODO: Make background color change
                String statusText;
                if (response.equals("1")) {
                    statusText = "Closed";
                }
                else {
                    statusText = "Open";
                }
                statusTextView.setText(statusText);
//                System.out.println(getRelayStatus);
//                queue.add(getDoorStatus);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println("Something Went Wrong");
                error.printStackTrace();
                statusCheckError = true;
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return createBasicAuthHeader(username, password);
            }
        };

//        do {
            System.out.println(getDoorStatus);
            queue.add(getDoorStatus);
//            try {
//                Thread.sleep(1000);
//            }
//            catch(InterruptedException e) {
//                System.out.println(e);
//            }
//        } while (!statusCheckError);

//        final Runnable updateDoorStatus = new Runnable() {
//            @Override
//            public void run() {
////            GPIOStatusURL = baseURL + "/GPIO/8/value";
////            System.out.println("GPIOStatusURL: " + GPIOStatusURL);
//                System.out.println(getDoorStatus);
//                queue.add(getDoorStatus);
//                handler.postDelayed(updateDoorStatus, 1000);
//            }
//        };
//
//        handler.postDelayed(updateDoorStatus, 1000);
    }

    public void toggleDoor(View view) {

        String postURL = baseURL + "/GPIO/17/sequence/100,101";

        Response.Listener postListener = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                System.out.println(response);
            }
        };

        Response.ErrorListener postErrorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println("Something Went Wrong");
                error.printStackTrace();
            }
        };

        StringRequest postRelay = new StringRequest(Request.Method.POST, postURL, postListener,
                postErrorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return createBasicAuthHeader(username, password);
            }
        };

        System.out.println("PostRelay: " + postRelay);
        queue.add(postRelay);
    }


    public void sendOpenNotification(View view) {
        // TODO: send when door actually opens
        DoorOpenNotification don = new DoorOpenNotification();
//        String nowString = new DateFormat.getDateTimeInstance().format(new Date());
        String nowString = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss").format(new Date());
//        String nowString = "Now";
        don.notify(this, nowString, 1);
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

                String savedUsername = sharedPref.getString(getString(R.string.username_hint), null);
                String savedPassword = sharedPref.getString(getString(R.string.password_hint), null);
                System.out.println("Logout clicked 1: " + savedUsername);
                System.out.println("Logout clicked 1: " + savedPassword);

                editor.remove(getString(R.string.username_hint));
                editor.remove(getString(R.string.password_hint));
                editor.commit();

                savedUsername = sharedPref.getString(getString(R.string.username_hint), null);
                savedPassword = sharedPref.getString(getString(R.string.password_hint), null);
                System.out.println("Logout clicked 2: " + savedUsername);
                System.out.println("Logout clicked 2: " + savedPassword);

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
}
