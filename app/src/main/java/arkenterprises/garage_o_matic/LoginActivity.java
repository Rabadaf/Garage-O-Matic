package arkenterprises.garage_o_matic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    public final static String EXTRA_USERNAME = "arkenterprises.garage_o_matic.USERNAME";
    public final static String EXTRA_PASSWORD = "arkenterprises.garage_o_matic.PASSWORD";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
//
//        String savedUsername = sharedPref.getString(getString(R.string.username_hint), null);
//        String savedPassword = sharedPref.getString(getString(R.string.password_hint), null);
//
//        if (savedUsername != null && savedPassword != null) {
//            Intent intent = new Intent(this, MainActivity.class);
//            intent.putExtra(EXTRA_USERNAME, savedUsername);
//            intent.putExtra(EXTRA_PASSWORD, savedPassword);
//            startActivity(intent);
//        }

        EditText editText = (EditText) findViewById(R.id.password_text);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    login(v);
                    handled = true;
                }
                return handled;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_appbar, menu);
        return true;
    }

    public void login(View view) {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);

        EditText usernameText = (EditText) findViewById(R.id.username_text);
        EditText passwordText = (EditText) findViewById(R.id.password_text);
        String username = usernameText.getText().toString();
        String password = passwordText.getText().toString();

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.username_hint), username);
        editor.putString(getString(R.string.password_hint), password);
        editor.apply();

//        String savedUsername = sharedPref.getString(getString(R.string.username_hint), "Not found");
//        String savedPassword = sharedPref.getString(getString(R.string.password_hint), "Not found");
//        System.out.println(savedUsername);
//        System.out.println(savedPassword);

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(EXTRA_USERNAME, username);
        intent.putExtra(EXTRA_PASSWORD, password);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                // User chose the "Settings" item, show the app settings UI...
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

}
