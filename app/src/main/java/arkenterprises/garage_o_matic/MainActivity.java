package arkenterprises.garage_o_matic;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


// TODO: Add security
// TODO: Add notifications
// TODO: Add customization
// TODO: Make it close automatically at night

public class MainActivity extends AppCompatActivity {

    private static TextView statusTextView;
    private Handler handler;
    private RequestQueue queue;
    private static TextView userNameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        statusTextView = (TextView) findViewById(R.id.statusText);
        handler = new Handler();
        queue = Volley.newRequestQueue(this);
        handler.post(updateDoorStatus);

        Intent intent = getIntent();
        String savedUsername = intent.getStringExtra(LoginActivity.EXTRA_USERNAME);
        String savedPassword = intent.getStringExtra(LoginActivity.EXTRA_PASSWORD);
        System.out.println("Main Activity:" + savedUsername);
        System.out.println("Main Activity:" + savedPassword);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_appbar, menu);
        return true;
    }

    String GPIOStatusURL = "http://arkf.duckdns.org:8000/GPIO/8/value";

    Response.Listener statusListener = new Response.Listener<String>() {
        @Override
        public void onResponse(String response) {
//            System.out.println(response);
            // TODO: Make background color change
            String statusText;
            if (response.equals("1")) {
                statusText = "Closed";
            }
            else {
                statusText = "Open";
            }
            statusTextView.setText(statusText);
        }
    };

    Response.ErrorListener statusErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            System.out.println("Something Went Wrong");
            error.printStackTrace();
        }
    };

    StringRequest getRelayStatus = new StringRequest(Request.Method.GET, GPIOStatusURL, statusListener,
            statusErrorListener) {
        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            return createBasicAuthHeader("ArkF", "12345");
        }
    };

    Runnable updateDoorStatus = new Runnable() {
        @Override
        public void run() {
            queue.add(getRelayStatus);
            handler.postDelayed(updateDoorStatus, 1000);
        }
    };

    Map<String, String> createBasicAuthHeader(String username, String password) {
        Map<String, String> headerMap = new HashMap<>();

        String credentials = username + ":" + password;
        String base64EncodedCredentials =
                Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
        headerMap.put("Authorization", "Basic " + base64EncodedCredentials);

        return headerMap;
    }
    //test

    public void toggleDoor(View view) {

        String postURL = "http://arkf.duckdns.org:8000/GPIO/17/sequence/100,101";

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
                return createBasicAuthHeader("ArkF", "12345");
            }
        };

        queue.add(postRelay);
    }


    public void sendOpenNotification(View view) {
        DoorOpenNotification don = new DoorOpenNotification();
//        String nowString = new DateFormat.getDateTimeInstance().format(new Date());
        String nowString = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss").format(new Date());
//        String nowString = "Now";
        don.notify(this, nowString, 1);
    }



}
