package com.cs360.campsitelocator;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

/**
 * Singleton class to handle http requests
 * https://developer.android.com/training/volley/requestqueue
 */
class ConnectionHandler {
    private static ConnectionHandler instance;
    private RequestQueue requestQueue;
    private ImageLoader imageLoader;
    private static Context ctx;

    private ConnectionHandler(Context context) {
        ctx = context.getApplicationContext();
        requestQueue = getRequestQueue();
        imageLoader = new ImageLoader(requestQueue,
                new ImageLoader.ImageCache() {
                    private final LruCache<String, Bitmap> cache = new LruCache<>(20);

                    @Override
                    public Bitmap getBitmap(String url) {
                        return cache.get(url);
                    }

                    @Override
                    public void putBitmap(String url, Bitmap bitmap) {
                        cache.put(url, bitmap);
                    }
                });
    }

    public static synchronized ConnectionHandler getInstance(Context context) {
        if (instance == null) {
            instance = new ConnectionHandler(context);
        }
        return instance;
    }

    private RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            requestQueue = Volley.newRequestQueue(ctx.getApplicationContext());
        }
        return requestQueue;
    }

    public <T> void addRequest(Request<T> req) {
        // Avoid double requests
        // https://stackoverflow.com/questions/37905223/
        req.setRetryPolicy(new DefaultRetryPolicy(30000, -1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        // Add to queue
        getRequestQueue().add(req);
    }

    public ImageLoader getImageLoader() {
        return imageLoader;
    }

}
