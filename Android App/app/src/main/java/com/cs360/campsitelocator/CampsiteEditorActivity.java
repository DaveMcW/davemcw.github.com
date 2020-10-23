package com.cs360.campsitelocator;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class CampsiteEditorActivity extends AppCompatActivity {

    EditText nameView;
    EditText locationView;
    EditText featuresView;
    EditText twitterView;
    ImageView photoView;
    Uri imageLocation;
    GlobalVariables user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campsite_editor);
        user = (GlobalVariables) getApplicationContext();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Load fields
        nameView = findViewById(R.id.editName);
        locationView = findViewById(R.id.editLocation);
        featuresView = findViewById(R.id.editFeatures);
        twitterView = findViewById(R.id.editTwitter);
        photoView = findViewById(R.id.photoPreview);
    }

    /**
     * Called when the user taps one of the buttons.
     */
    public void onClick(View view) {
        switch (view.getId()) {

            // Open Google Maps API
            case R.id.mapButton:
                Intent intent1 = new Intent(this, MapsActivity.class);
                intent1.putExtra("location", locationView.getText().toString());
                intent1.putExtra("edit", Constants.EDIT_MAP_REQUEST);
                startActivityForResult(intent1, Constants.EDIT_MAP_REQUEST);
                break;

            // Open Photo Gallery API
            case R.id.photoPreview:
                Intent intent2 = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent2, Constants.GET_PHOTO_REQUEST);
                break;

            // Add campsite to database
            case R.id.createCampsiteButton:
                if (nameView.getText().toString().equals("")) {
                    Toast.makeText(getApplicationContext(),"Name is required", Toast.LENGTH_SHORT).show();
                    break;
                }
                createCampsite();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case Constants.EDIT_MAP_REQUEST:
                // Update the location
                String location = data.getStringExtra("location");
                if (location != null) {
                    locationView.setText(location);
                }
                break;
            case Constants.GET_PHOTO_REQUEST:
                // Update the image
                imageLocation = data.getData();
                if (!loadImage()) {
                    // Image is missing, set placeholder image
                    photoView.setImageResource(R.drawable.add_photo);
                }
                break;
        }
    }


    /**
     * Save changes as a new campsite
     */
    void createCampsite() {
        // Build POST request body
        JSONObject body = new JSONObject();
        try {
            body.put("name", nameView.getText().toString());
            body.put("location", locationView.getText().toString());
            body.put("features", featuresView.getText().toString());
            body.put("twitter", twitterView.getText().toString());
        } catch (JSONException e) {
            Toast.makeText(getApplicationContext(),"Submission failed.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Send API request
        JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.POST,
            getResources().getString(R.string.WEB_SERVER) + "/create-campsite",
            body,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    // Build campsite object
                    String campsite_id;
                    try {
                        campsite_id = response.getString("id");
                    } catch (JSONException e) {
                        Toast.makeText(getApplicationContext(),"Submission failed.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Campsite campsite = new Campsite();
                    campsite.setID(campsite_id);
                    campsite.setName(nameView.getText().toString());
                    campsite.setLocation(locationView.getText().toString());
                    campsite.setFeatures(featuresView.getText().toString());
                    campsite.setTwitter(twitterView.getText().toString());

                    // Look up latitude and longitude
                    GeocodeDBTaskRunner geocodeTask = new GeocodeDBTaskRunner(user);
                    geocodeTask.execute(campsite);

                    // Close page
                    setResult(RESULT_OK);
                    finish();
                }
            },
            new AuthenticationErrorHandler(this)
        ) {
            @Override
            public Map<String, String> getHeaders() {
                // Headers
                Map<String, String> params = new HashMap<>();
                params.put("Content-Type", "application/json");
                params.put("Authorization", user.token);
                return params;
            }
        };
        ConnectionHandler.getInstance(user).addRequest(request);

        // Save image
        //createImage(campsite.getID());
    }

    /**
     * Copy an image from an external source to private storage.
     *
     * @param id Campsite ID, used as the file name
     * @return true if successful, false otherwise
     */
    boolean createImage(long id) {
        // Check Uri
        if (imageLocation == null) {
            return false;
        }

        // Read input file
        InputStream source;
        try {
            source = getContentResolver().openInputStream(imageLocation);
        } catch (FileNotFoundException e) {
            return false;
        }
        if (source == null) {
            return false;
        }

        // Create output file
        FileOutputStream destination;
        try {
            destination = openFileOutput(Long.toString(id), Context.MODE_PRIVATE);
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

    /**
     * Load image from external source
     *
     */
    boolean loadImage() {
        // Check Uri
        if (imageLocation == null) {
            return false;
        }

        // Read input file
        InputStream source;
        try {
            source = getContentResolver().openInputStream(imageLocation);
        } catch (FileNotFoundException e) {
            return false;
        }

        // Read local image
        Bitmap bitmap = BitmapFactory.decodeStream(source);
        photoView.setImageBitmap(bitmap);
        return true;
    }


    /**
     * Background task to fetch latitude and longitude for the new campsite
     */
    private static class GeocodeDBTaskRunner extends AsyncTask<Campsite, Void, Address> {
        private GlobalVariables user;

        GeocodeDBTaskRunner(GlobalVariables u) {
            user = u;
        }

        // Assign latitude and longitude to campsite
        @Override
        protected Address doInBackground(Campsite... params) {
            Geocoder gc = new Geocoder(user);
            Campsite campsite = params[0];
            JSONObject body = new JSONObject();
            try {
                // Look up geo coordinates
                List<Address> list = gc.getFromLocationName(campsite.getLocation(), 1);
                Address address = list.get(0);

                // Build PUT request body
                body.put("lat", address.getLatitude());
                body.put("long", address.getLongitude());
            } catch (IOException | JSONException e)  {
                e.printStackTrace();
                return null;
            }

            // Send API request
            JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.PUT,
                user.getResources().getString(R.string.WEB_SERVER) + "/update-campsite/" + campsite.getID(),
                body,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {}
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError e) {
                        e.printStackTrace();
                    }
                }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    // Headers
                    Map<String, String> params = new HashMap<>();
                    params.put("Content-Type", "application/json");
                    params.put("Authorization", user.token);
                    return params;
                }
            };
            ConnectionHandler.getInstance(user).addRequest(request);

            return null;
        }
    }
}

