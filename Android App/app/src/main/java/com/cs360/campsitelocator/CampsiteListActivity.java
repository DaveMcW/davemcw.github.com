package com.cs360.campsitelocator;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class CampsiteListActivity extends AppCompatActivity  implements CampsiteListAdapter.ItemClickListener {

    CampsiteListAdapter adapter;
    RecyclerView recyclerView;
    TextView statusView;
    GlobalVariables user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campsite_list);
        user = (GlobalVariables) getApplicationContext();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        statusView = findViewById(R.id.statusView);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CampsiteListAdapter(CampsiteListActivity.this);
        adapter.setClickListener(CampsiteListActivity.this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Search for new campsites
        statusView.setText(R.string.searching);
        search();
    }

    @Override
    public void onItemClick(View view, int position) {
        // View this campsite
        Intent intent = new Intent(this, CampsiteViewActivity.class);
        intent.putExtra("id", adapter.getItem(position).getID());
        startActivity(intent);
    }

    /**
     * Search the campsite database using the intent parameters
     */
    public void search() {
        int option = getIntent().getIntExtra("option", 0);
        float distance = getIntent().getFloatExtra("distance", 0);
        double latitude = getIntent().getDoubleExtra("latitude", 0);
        double longitude = getIntent().getDoubleExtra("longitude", 0);
        boolean favorite = getIntent().getBooleanExtra("favorite", false);

        // Encode user input
        String userInput = getIntent().getStringExtra("text");
        if (userInput == null) {
            userInput = "";
        }
        try {
            URLEncoder.encode(userInput, StandardCharsets.UTF_8.toString());
        } catch(UnsupportedEncodingException e) {
            userInput = "";
        }

        String api;
        if (favorite) {
            // Search user favorites
            api = "/search-campsite-favorite";
        }
        else switch (option) {
            case 0: // Search by name
                api = "/search-campsite-name/" + userInput;
                break;

            case 1: // Search by city
                api = "/search-campsite-location/" + userInput;
                break;

            case 2: // Search by feature
                api = "/search-campsite-feature/" + userInput;
                break;

            case 3: // Search near me
            case 4: // Search near location
                api = "/search-campsite-near/" + latitude + "/" + longitude + "/" + distance;
                break;

            case 5: // Search all
            default:
                api = "/get-all-campsites";
                break;
        }

        // Send API request
        JsonArrayRequest request = new JsonArrayRequest(
            Request.Method.GET,
            getResources().getString(R.string.WEB_SERVER) + api,
            null,
            new Response.Listener<JSONArray>() {
                @Override
                public void onResponse(JSONArray response) {
                    // Parse the list of campsites
                    ArrayList<Campsite> list = new ArrayList<>();
                    for (int i = 0; i < response.length(); i++){
                        try {
                            JSONObject object = response.getJSONObject(i);
                            Campsite campsite = new Campsite();
                            campsite.setID(object.getString("id"));
                            campsite.setName(object.getString("name"));
                            list.add(campsite);
                        } catch (JSONException e) {
                            // Ignore
                        }
                    }

                    // Fill the RecyclerView
                    adapter.setData(list);

                    // Change status message, only visible if there are no campsites
                    statusView.setText(R.string.no_campsites_found);
                }
            },
            new AuthenticationErrorHandler(this)
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return Collections.singletonMap("Authorization", user.token);
            }
        };
        ConnectionHandler.getInstance(user).addRequest(request);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Return to parent activity
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

