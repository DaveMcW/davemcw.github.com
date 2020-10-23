package com.cs360.campsitelocator;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class CreateAccountActivity extends AppCompatActivity {

    private EditText nameView;
    private EditText passwordView;
    private EditText passwordView2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Load fields
        nameView = findViewById(R.id.editName);
        passwordView = findViewById(R.id.editPassword);
        passwordView2 = findViewById(R.id.editPassword2);
    }

    /**
     * Called when the user taps one of the buttons.
     */
    public void onClick(View view) {
        switch (view.getId()) {
            // Create new account
            case R.id.createAccount:
                createAccount();
                break;
        }
    }

    /**
     * Create new account
     */
    public void createAccount() {
        // Check for valid username
        String username = nameView.getText().toString();
        if (username.equals("")) {
            Toast.makeText(getApplicationContext(),"Username is required.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Check for valid password
        String password = passwordView.getText().toString();
        if (password.equals("")) {
            Toast.makeText(getApplicationContext(),"Password is required.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(passwordView2.getText().toString())) {
            Toast.makeText(getApplicationContext(),"Password does not match.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build POST request body
        JSONObject body = new JSONObject();
        try {
            body.put("username", username);
            body.put("password", password);
        } catch (JSONException e) {
            Toast.makeText(getApplicationContext(),"Error creating account.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Send API request
        JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.POST,
            getResources().getString(R.string.WEB_SERVER) + "/create-user",
            body,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    // Print any error messages
                    String errorMessage = null;
                    try {
                        errorMessage = response.getString("error");
                    } catch (JSONException e) {
                        // Ignore
                    }
                    if (errorMessage != null) {
                        Toast.makeText(getApplicationContext(),errorMessage, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Success! We can close this activity now
                    setResult(RESULT_OK);
                    finish();
                }
            },
            new ConnectionErrorHandler(this)
        );
        ConnectionHandler.getInstance(this).addRequest(request);
    }

}