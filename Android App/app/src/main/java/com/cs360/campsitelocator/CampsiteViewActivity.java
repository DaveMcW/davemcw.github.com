package com.cs360.campsitelocator;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

public class CampsiteViewActivity extends AppCompatActivity {

    private String campsite_id;
    private TextView nameView;
    private TextView locationView;
    private TextView featuresView;
    private TextView twitterView;
    private ImageView photoView;
    private TextView averageRatingView;
    private View photoBlock;
    private View twitterBlock;
    private SeekBar yourRatingView;
    private CustomSwitch favoriteSwitch;
    TweetTaskRunner tweetTask;
    GlobalVariables user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campsite_view);
        user = (GlobalVariables) getApplicationContext();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Load fields
        nameView = findViewById(R.id.editName);
        locationView = findViewById(R.id.editLocation);
        featuresView = findViewById(R.id.editFeatures);
        twitterBlock = findViewById(R.id.twitterBlock);
        twitterView = findViewById(R.id.editTwitter);
        photoBlock = findViewById(R.id.photoBlock);
        photoView = findViewById(R.id.photoPreview);
        averageRatingView = findViewById(R.id.averageRatingView);
        yourRatingView = findViewById(R.id.yourRatingView);
        favoriteSwitch = findViewById(R.id.favoriteSwitch);

        yourRatingView.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekBar.getThumb().mutate().setAlpha(255);
                if (fromUser) {
                    submitNewRating(progress);
                }
            }
        });

        favoriteSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                submitFavorite(isChecked);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Read the ID passed from the previous activity
        campsite_id = getIntent().getStringExtra("id");

        // Send API request
        JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.GET,
            getResources().getString(R.string.WEB_SERVER) + "/get-campsite/" + campsite_id,
            null,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    readResponse(response);
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

    /**
     * Populate the page with data
     */
    void readResponse(JSONObject response) {
        try {
            nameView.setText(response.getString("name"));
        } catch (JSONException e) {
            // Ignore
        }
        try {
            locationView.setText(response.getString("location"));
        } catch (JSONException e) {
            // Ignore
        }
        try {
            featuresView.setText(response.getString("features"));
        } catch (JSONException e) {
            // Ignore
        }
        try {
            setAverageRating(response.getDouble("average_rating"));
        } catch (JSONException e) {
            // Ignore
        }

        int userRating = -1;
        try {
            userRating = response.getInt("rating");
        } catch (JSONException e) {
            // Ignore
        }
        if (userRating >= 0) {
            // Set rating
            yourRatingView.setProgress(userRating);
            yourRatingView.getThumb().mutate().setAlpha(255);
        } else {
            // Hide selection
            yourRatingView.getThumb().mutate().setAlpha(0);
        }

        // Load favorite
        boolean isChecked;
        try {
            isChecked = response.getInt("favorite") == 1;
        } catch (JSONException e) {
            isChecked = false;
        }
        favoriteSwitch.setCheckedSilent(isChecked);

        // Load twitter
        String twitterUsername;
        try {
            twitterUsername = response.getString("twitter");
        } catch (JSONException e) {
            twitterUsername = "";
        }
        if (twitterUsername.length() > 0) {
            tweetTask = new TweetTaskRunner(this);
            tweetTask.execute(twitterUsername);
            twitterBlock.setVisibility(View.VISIBLE);
        } else {
            twitterBlock.setVisibility(View.GONE);
        }

        // Load photo
//        try {
//            Bitmap bitmap = BitmapFactory.decodeStream(openFileInput(Long.toString(campsiteId)));
//            photoView.setImageBitmap(bitmap);
//            photoBlock.setVisibility(View.VISIBLE);
//        } catch (Exception e) {
//            photoBlock.setVisibility(View.GONE);
//        }

    }

    /**
     * Called when the user taps one of the buttons.
     */
    public void onClick(View view) {
        switch (view.getId()) {
            // Open Google Maps API
            case R.id.mapButton:
                Intent intent = new Intent(this, MapsActivity.class);
                intent.putExtra("location", locationView.getText().toString());
                intent.putExtra("name", nameView.getText().toString());
                startActivity(intent);
                break;
        }
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

    private static class TweetTaskRunner extends AsyncTask<String, Void, Status> {
        private WeakReference<CampsiteViewActivity> activityReference;

        // Only retain a weak reference to the activity
        // https://stackoverflow.com/questions/44309241/
        TweetTaskRunner(CampsiteViewActivity context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected twitter4j.Status doInBackground(String... params) {
            ConfigurationBuilder cb = new ConfigurationBuilder();
            cb.setDebugEnabled(true)
                    .setOAuthConsumerKey(Constants.TWITTER_CONSUMER_KEY)
                    .setOAuthConsumerSecret(Constants.TWITTER_CONSUMER_SECRET)
                    .setOAuthAccessToken(Constants.TWITTER_ACCESS_TOKEN)
                    .setOAuthAccessTokenSecret(Constants.TWITTER_ACCESS_SECRET);
            TwitterFactory tf = new TwitterFactory(cb.build());
            Twitter twitter = tf.getInstance();

            try {
                ResponseList<twitter4j.Status> timeline = twitter.getUserTimeline(params[0], new Paging(1, 1));
                for (twitter4j.Status tweet : timeline) {
                    return tweet;
                }
            } catch (TwitterException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(twitter4j.Status tweet) {
            if (tweet == null) {
                activityReference.get().twitterBlock.setVisibility(View.GONE);
            } else {
                String text = String.format("%s\n%s", tweet.getText(), tweet.getCreatedAt());
                activityReference.get().twitterView.setText(text);
                activityReference.get().twitterBlock.setVisibility(View.VISIBLE);
            }
        }
    }

    void setAverageRating(double rating) {
        if (rating >= 0) {
            averageRatingView.setText(String.format(Locale.US, "%.1f", rating));
        } else {
            averageRatingView.setText(R.string.no_ratings_yet);
        }
    }

    void submitNewRating(int i) {
        // Send API request
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.PUT,
                getResources().getString(R.string.WEB_SERVER) + "/set-campsite-rating/" + campsite_id + "/" + i,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Read new average rating
                        double newRating;
                        try {
                            newRating = response.getDouble("average_rating");
                        } catch (JSONException e) {
                            return;
                        }

                        // Update displayed rating
                        setAverageRating(newRating);
                    }
                },
                new AuthenticationErrorHandler(this)
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return Collections.singletonMap("Authorization", user.token);
            }
        };
        ConnectionHandler.getInstance(this).addRequest(request);
    }

    void submitFavorite(boolean isFavorite) {
        int favorite = isFavorite ? 1 : 0;
        // Send API request
        JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.PUT,
            getResources().getString(R.string.WEB_SERVER) + "/set-campsite-favorite/" + campsite_id + "/" + favorite,
            null,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {}
            },
            new AuthenticationErrorHandler(this)
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return Collections.singletonMap("Authorization", user.token);
            }
        };

        ConnectionHandler.getInstance(this).addRequest(request);

    }

}
