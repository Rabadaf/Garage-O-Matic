package arkenterprises.garage_o_matic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Get preferences from specific file
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.preference_file_key),
                Context.MODE_PRIVATE);

        String savedUsername = sharedPref.getString(getString(R.string.username_hint), null);
        String savedPassword = sharedPref.getString(getString(R.string.password_hint), null);
        Log.i(TAG, "savedUsername: " + savedUsername);
        Log.i(TAG, "savedPassword: " + savedPassword);

        // If both username and password were retrieved, move on immediately
        if (savedUsername != null && savedPassword != null) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }

        EditText editText = findViewById(R.id.password_text);
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
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.preference_file_key),
                Context.MODE_PRIVATE);

        EditText usernameText = findViewById(R.id.username_text);
        EditText passwordText = findViewById(R.id.password_text);
        String username = usernameText.getText().toString();
        String password = passwordText.getText().toString();

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.username_hint), username);
        editor.putString(getString(R.string.password_hint), password);
        editor.apply();

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
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

//                String savedUsername = sharedPref.getString(getString(R.string.username_hint), null);
//                String savedPassword = sharedPref.getString(getString(R.string.password_hint), null);

                editor.remove(getString(R.string.username_hint));
                editor.remove(getString(R.string.password_hint));
                editor.apply();

                Intent loginIntent = new Intent(this, LoginActivity.class);
                startActivity(loginIntent);
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

}
