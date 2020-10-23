package com.cs360.campsitelocator;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class SearchActivity extends AppCompatActivity {

    // Used in checking for runtime permissions.
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    // The BroadcastReceiver used to listen from broadcasts from the service.
    private MyReceiver myReceiver;

    // A reference to the service used to get location updates.
    private LocationUpdatesService mService = null;

    // Tracks the bound state of the service.
    private boolean mBound = false;

    Spinner searchOptions;
    EditText searchDistance;
    EditText searchText;
    View distanceBlock;
    EditText locationText;
    View locationBlock;
    double latitude = 0;
    double longitude = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        searchOptions = findViewById(R.id.searchSpinner);
        searchText = findViewById(R.id.searchText);
        locationText = findViewById(R.id.locationText);
        locationBlock = findViewById(R.id.locationBlock);
        searchDistance = findViewById(R.id.searchDistance);
        distanceBlock = findViewById(R.id.distanceBlock);

        myReceiver = new MyReceiver();

        // Show the relevant input fields for the selected search option
        searchOptions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                switch (i) {
                    case 0: // by Campsite Name
                    case 1: // by City
                    case 2: // by Feature
                        searchText.setVisibility(View.VISIBLE);
                        locationBlock.setVisibility(View.GONE);
                        distanceBlock.setVisibility(View.GONE);
                        break;
                    case 3: // near Me
                        searchText.setVisibility(View.GONE);
                        locationBlock.setVisibility(View.GONE);
                        distanceBlock.setVisibility(View.VISIBLE);
                        // Start location service
                        if (checkPermissions()) {
                            requestPermissions();
                        } else {
                            mService.requestLocationUpdates();
                        }
                        break;
                    case 4: // near Location
                        searchText.setVisibility(View.GONE);
                        locationBlock.setVisibility(View.VISIBLE);
                        distanceBlock.setVisibility(View.VISIBLE);
                        break;
                    case 5: // all Campsites
                        searchText.setVisibility(View.GONE);
                        locationBlock.setVisibility(View.GONE);
                        distanceBlock.setVisibility(View.GONE);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Bind to the service. If the service is in foreground mode, this signals to the service
        // that since this activity is in the foreground, the service can exit foreground mode.
        bindService(new Intent(this, LocationUpdatesService.class), mServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(myReceiver,
                new IntentFilter(LocationUpdatesService.ACTION_BROADCAST));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver);
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (mBound) {
            // Unbind from the service. This signals to the service that this activity is no longer
            // in the foreground, and the service can respond by promoting itself to a foreground
            // service.
            unbindService(mServiceConnection);
            mBound = false;
        }
        super.onStop();
    }

    /**
     * Called when the user taps one of the buttons.
     */
    public void onClick(View view) {
        switch (view.getId()) {
            // Search campsites
            case R.id.searchButton:
                int option = searchOptions.getSelectedItemPosition();
                String text = searchText.getText().toString();
                String location = locationText.getText().toString();

                // Validate search text
                if (option == 0 || option == 1 || option == 2) {
                    if (text.equals("")) {
                        Toast.makeText(SearchActivity.this, "Please enter a search term.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                // Validate location text
                if (option == 4) {
                    if (location.equals("")) {
                        Toast.makeText(SearchActivity.this, "Please enter a search location.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                // Convert from miles to meters
                float meters = Float.parseFloat(searchDistance.getText().toString()) * 1609.344f;

                final Intent intent1 = new Intent(this, CampsiteListActivity.class);
                intent1.putExtra("option", option);
                intent1.putExtra("text", text);
                intent1.putExtra("distance", meters);
                intent1.putExtra("location", location);

                // Search near me
                if (searchOptions.getSelectedItemPosition() == 3) {
                    if (latitude == 0 && longitude == 0) {
                        Toast.makeText(SearchActivity.this, "Could not find your location, please try again.", Toast.LENGTH_SHORT).show();
                    } else {
                        intent1.putExtra("latitude", latitude);
                        intent1.putExtra("longitude", longitude);
                        startActivity(intent1);
                    }
                    break;
                }

                // Search near location
                if (searchOptions.getSelectedItemPosition() == 4) {
                    // Look up latitude and longitude before submitting
                    GeocodeLocationTaskRunner geocodeTask = new GeocodeLocationTaskRunner(this, intent1);
                    geocodeTask.execute(locationText.getText().toString());
                    break;
                }

                startActivity(intent1);
                break;

            // Open Google Maps API
            case R.id.mapButton:
                Intent intent2 = new Intent(this, MapsActivity.class);
                intent2.putExtra("location", locationText.getText().toString());
                intent2.putExtra("edit", Constants.EDIT_MAP_REQUEST);
                startActivityForResult(intent2, Constants.EDIT_MAP_REQUEST);
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
                    locationText.setText(location);
                }
                break;
        }
    }

    /**
     * Background task to fetch latitude and longitude for the new location
     */
    private static class GeocodeLocationTaskRunner extends AsyncTask<String, Void, Address> {
        private WeakReference<AppCompatActivity> activityReference;
        private Intent myIntent;

        // Only retain a weak reference to the activity
        // https://stackoverflow.com/questions/44309241/
        GeocodeLocationTaskRunner(AppCompatActivity context, Intent intent) {
            activityReference = new WeakReference<>(context);
            myIntent = intent;
        }

        // Assign latitude and longitude to campsite
        @Override
        protected Address doInBackground(String... params) {
            Geocoder gc = new Geocoder(activityReference.get());
            try {
                List<Address> list = gc.getFromLocationName(params[0], 1);
                Address address = list.get(0);
                myIntent.putExtra("latitude", address.getLatitude());
                myIntent.putExtra("longitude", address.getLongitude());
                activityReference.get().startActivity(myIntent);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    // Monitors the state of the connection to the service.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationUpdatesService.LocalBinder binder = (LocationUpdatesService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
        }
    };

    /**
     * Returns the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        return PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private void requestPermissions() {
        Log.i("SearchActivity", "Requesting permission");
        // Request permission. It's possible this can be auto answered if device policy
        // sets the permission in a given state or the user denied the permission
        // previously and checked "Never ask again".
        ActivityCompat.requestPermissions(SearchActivity.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_PERMISSIONS_REQUEST_CODE);
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i("SearchActivity", "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i("SearchActivity", "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted.
                mService.requestLocationUpdates();
            }
        }
    }

    /**
     * Receiver for broadcasts sent by {@link LocationUpdatesService}.
     */
    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Location location = intent.getParcelableExtra(LocationUpdatesService.EXTRA_LOCATION);
            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            }
        }
    }

}