package com.cs360.campsitelocator;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;

/**
 * Handler for connection errors
 */
class ConnectionErrorHandler implements Response.ErrorListener {
    Context ctx;

    protected ConnectionErrorHandler() {}

    public ConnectionErrorHandler(Context context) {
        ctx = context;
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        String errorMessage;
        if (error instanceof AuthFailureError) {
            errorMessage = "Invalid username or password.";
        }
        else if (error instanceof NetworkError) {
                errorMessage = "Error: Connection to server failed.";
        }
        else if (error instanceof TimeoutError) {
            errorMessage = "Error: Connection to server timed out.";
        }
        else {
            errorMessage = "Error: There was a server error.";
        }
        Toast.makeText(ctx, errorMessage, Toast.LENGTH_SHORT).show();
    }
}

/**
 * Handle authentication errors by returning to the main page
 */
class AuthenticationErrorHandler extends ConnectionErrorHandler {

    public AuthenticationErrorHandler(Context context) {
        ctx = context;
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        if (error instanceof AuthFailureError) {
            // Return to main menu
            GlobalVariables user = (GlobalVariables) ctx.getApplicationContext();
            user.token = "";
            user.id = "";
            user.name = "";
            Intent intent = new Intent(ctx, MainActivity.class);
            ctx.startActivity(intent);
            return;
        }
        super.onErrorResponse(error);
    }
}