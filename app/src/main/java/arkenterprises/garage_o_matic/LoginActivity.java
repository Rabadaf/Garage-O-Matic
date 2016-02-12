package arkenterprises.garage_o_matic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;

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

        String savedUsername = sharedPref.getString(getString(R.string.username_hint), "Not found");
        String savedPassword = sharedPref.getString(getString(R.string.password_hint), "Not found");
        System.out.println(savedUsername);
        System.out.println(savedPassword);

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(EXTRA_USERNAME, savedUsername);
        intent.putExtra(EXTRA_PASSWORD, savedPassword);
        startActivity(intent);
    }

}
