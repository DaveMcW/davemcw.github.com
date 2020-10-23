package com.cs360.campsitelocator;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private TextView usernameView;
    private TextView passwordView;
    private GlobalVariables user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        usernameView = findViewById(R.id.usernameText);
        passwordView = findViewById(R.id.passwordText);
        user = (GlobalVariables) getApplicationContext();
    }

    /**
     * Called when the user taps the login button
     */
    public void login(View view) {
        final String username = usernameView.getText().toString();
        final String password = passwordView.getText().toString();

        // Build POST request body
        JSONObject body = new JSONObject();
        try {
            body.put("username", username);
            body.put("password", password);
        } catch (JSONException e) {
            Toast.makeText(this,"Login failed.", Toast.LENGTH_SHORT).show();
        }

        // Send API request
        JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.POST,
            getResources().getString(R.string.WEB_SERVER) + "/login",
            body,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        // Read authentication token
                        user.id = response.getString("id");
                        user.token = response.getString("token");
                        user.name = username;
                    } catch (JSONException e) {
                        Toast.makeText(getApplicationContext(),"Login failed.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Update stored credentials
                    SharedPreferences sharedPreferences = getSharedPreferences(Constants.CREDENTIALS_FILE, MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(Constants.USERNAME_KEY, username);
                    editor.putString(Constants.PASSWORD_KEY, password);
                    editor.apply();

                    // Close this activity
                    finish();
                }
            },
            new ConnectionErrorHandler(this)
        );
        ConnectionHandler.getInstance(user).addRequest(request);
    }
}
