package com.cs360.campsitelocator;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private EditText location;
    private Marker marker;
    private View buttonBlock;
    private String campsiteName;
    private int edit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        buttonBlock = findViewById(R.id.buttonBlock);

        // Set the default location
        location = findViewById(R.id.editLocation);
        location.setText(getIntent().getStringExtra("location"));

        // Listen for user typing a new location
        location.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View view, int keyCode, KeyEvent keyevent) {
                if (keyevent.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    if (edit == Constants.EDIT_MAP_REQUEST) {
                        moveMarkerToLocation(location.getText().toString());
                        return true;
                    }
                }
                return false;
            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Hide buttons if it is not an edit request
        Intent caller = getIntent();
        edit = caller.getIntExtra("edit", 0);
        if (edit == Constants.EDIT_MAP_REQUEST) {
            buttonBlock.setVisibility(View.VISIBLE);
            location.setFocusable(true);
        } else {
            buttonBlock.setVisibility(View.GONE);
            location.setFocusable(false);
        }

        campsiteName = caller.getStringExtra("name");
        if (campsiteName == null) {
            campsiteName = "Campsite";
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker at the current location
        moveMarkerToLocation(location.getText().toString());

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (edit == Constants.EDIT_MAP_REQUEST) {
                    moveMarkerToPoint(latLng);
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Back button saves changes
                saveChanges();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Move the marker to a new address
     *
     * @param query Address of the new marker
     */
    public void moveMarkerToLocation(String query) {
        // Remove old marker
        if (marker != null) {
            marker.remove();
        }

        // Convert address to latitude and longitude
        List<Address> address = null;
        Geocoder coder = new Geocoder(getApplicationContext());
        try {
            address = coder.getFromLocationName(query, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Add new marker
        if (address != null && address.size() >= 1) {
            Address location = address.get(0);
            LatLng p1 = new LatLng(location.getLatitude(), location.getLongitude());
            marker = mMap.addMarker(new MarkerOptions().position(p1).title(campsiteName));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(p1));
        }
    }

    /**
     * Move the marker to a new point
     *
     * @param point Location of the new marker
     */
    public void moveMarkerToPoint(LatLng point) {
        // Remove old marker
        if (marker != null) {
            marker.remove();
        }

        // Add new marker
        marker = mMap.addMarker(new MarkerOptions().position(point).title("Campsite"));

        // Convert latitude and longitude to address
        List<Address> addressList = null;
        Geocoder coder = new Geocoder(getApplicationContext());
        try {
            addressList = coder.getFromLocation(point.latitude, point.longitude, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Update location
        StringBuilder addressText = new StringBuilder();
        if (addressList != null && addressList.size() >= 1) {
            // Use address
            Address address = addressList.get(0);
            for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                addressText.append(address.getAddressLine(i)).append(" ");
            }
        } else {
            // Use latitude and longitude
            addressText.append(point.latitude);
            addressText.append(", ");
            addressText.append(point.longitude);
        }
        location.setText(addressText.toString());
    }

    /**
     * Called when the user taps one of the buttons.
     */
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.saveButton:
                // Save changes
                saveChanges();
                break;
            case R.id.cancelButton:
                // Exit without saving changes
                finish();
                break;
        }
    }

    /**
     * Return the location to the calling activity
     */
    public void saveChanges() {
        Intent intent = new Intent();
        intent.putExtra("location", location.getText().toString());
        setResult(RESULT_OK, intent);
        finish();
    }
}
