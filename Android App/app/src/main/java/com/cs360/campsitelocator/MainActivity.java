package com.cs360.campsitelocator;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    TextView loginStatusView;
    Button loginButton;
    Button logoutButton;
    Button searchButton;
    Button favoritesButton;
    Button createCampsiteButton;
    Button createAccountButton;
    View contentView;
    GlobalVariables user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        user = (GlobalVariables) getApplicationContext();
        contentView = findViewById(R.id.contentView);
        loginStatusView = findViewById(R.id.loginStatus);
        loginButton = findViewById(R.id.loginButton);
        logoutButton = findViewById(R.id.logoutButton);
        searchButton = findViewById(R.id.searchButton);
        favoritesButton = findViewById(R.id.favoritesButton);
        createCampsiteButton = findViewById(R.id.createCampsiteButton);
        createAccountButton = findViewById(R.id.createAccountButton);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMenu();
        checkLogin();
    }

    /**
     * Called when the user taps one of the buttons.
     */
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.searchButton:
                startActivity(new Intent(this, SearchActivity.class));
                break;
            case R.id.favoritesButton:
                Intent intent1 = new Intent(this, CampsiteListActivity.class);
                intent1.putExtra("favorite", true);
                startActivity(intent1);
                break;
            case R.id.createCampsiteButton:
                startActivity(new Intent(this, CampsiteEditorActivity.class));
                break;
            case R.id.createAccountButton:
                startActivity(new Intent(this, CreateAccountActivity.class));
                break;
            case R.id.aboutButton:
                startActivity(new Intent(this, AboutActivity.class));
                break;
            case R.id.loginButton:
                startActivity(new Intent(this, LoginActivity.class));
                break;
            case R.id.logoutButton:
                logout();

                // Refresh login status
                checkLogin();
                break;
        }
    }

    /**
     * Check for valid login
     */
    public void checkLogin() {
        // Read stored credentials
        SharedPreferences sharedPreferences = getSharedPreferences(Constants.CREDENTIALS_FILE, MODE_PRIVATE);
        final String username = sharedPreferences.getString(Constants.USERNAME_KEY, null);
        final String password = sharedPreferences.getString(Constants.PASSWORD_KEY, null);

        if (username == null || password == null) {
            // Not logged in
            return;
        }

        if (!user.token.equals("")) {
            // Logged in
            return;
        }

        // We have stored credentials, but no token
        // Try to log in

        // Build POST request body
        JSONObject body = new JSONObject();
        try {
            body.put("username", username);
            body.put("password", password);
        } catch (JSONException e) {
            logout();
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
                        // Ignore
                    }
                    updateMenu();
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    if (error instanceof AuthFailureError) {
                        // Delete bad login credentials
                        logout();
                    }
                    updateMenu();
                }
            }
        );
        ConnectionHandler.getInstance(user).addRequest(request);
    }

    /**
     * Refresh menu based on user's login status
     */
    void updateMenu() {
        if (user.token.equals("")) {
            // Invalid login
            loginStatusView.setText(R.string.not_logged_in);
            loginButton.setVisibility(View.VISIBLE);
            createAccountButton.setVisibility(View.VISIBLE);
            logoutButton.setVisibility(View.GONE);
            searchButton.setVisibility(View.GONE);
            favoritesButton.setVisibility(View.GONE);
            createCampsiteButton.setVisibility(View.GONE);
        } else {
            // Valid login
            String loginString = getResources().getString(R.string.logged_in);
            loginStatusView.setText(String.format(loginString, user.name));
            loginButton.setVisibility(View.GONE);
            createAccountButton.setVisibility(View.GONE);
            logoutButton.setVisibility(View.VISIBLE);
            searchButton.setVisibility(View.VISIBLE);
            favoritesButton.setVisibility(View.VISIBLE);
            createCampsiteButton.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Remove stored credentials
     */
    void logout() {
        user.token = "";
        user.id = "";
        user.name = "";
        SharedPreferences preferences = getSharedPreferences(Constants.CREDENTIALS_FILE, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(Constants.USERNAME_KEY);
        editor.remove(Constants.PASSWORD_KEY);
        editor.apply();
        updateMenu();
    }

    /**
     * Installer script.
     */
    void setup() {
        // Copy images to local storage
        for (int i = 0; i < 20; i++) {
            createImage(i+1, Constants.STARTER_IMAGES[i]);
        }
    }

    /**
     * Copy an image from the app package to private storage.
     *
     * @param campsiteId Campsite ID, used as the file name.
     * @param resourceId Resource ID inside the app package.
     * @return true if successful, false otherwise.
     */
    boolean createImage(int campsiteId, int resourceId) {
        // Read input file
        Uri uri = Uri.parse("android.resource://com.cs360.campsitelocator/" + resourceId);
        InputStream source;
        try {
            source = getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            return false;
        }
        if (source == null) {
            return false;
        }

        // Create output file
        FileOutputStream destination;
        try {
            destination = openFileOutput(Long.toString(campsiteId), Context.MODE_PRIVATE);
        } catch (FileNotFoundException e) {
            return false;
        }

        // Copy from input to output
        try {
            byte[] buffer = new byte[source.available()];
            int bytesRead;
            while ((bytesRead = source.read(buffer)) != -1) {
                destination.write(buffer, 0, bytesRead);
            }
            source.close();
            destination.write(buffer);
            destination.close();
        } catch (IOException e) {
            return false;
        }

        return true;
    }

}
