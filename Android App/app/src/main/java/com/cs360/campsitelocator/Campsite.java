package com.cs360.campsitelocator;

/**
 * A campsite.
 */
public class Campsite {
    // Instance variables
    private String _id;
    private String _name;
    private String _location;
    private String _features;
    private String _twitter;
    private float _averageRating;
    private double _latitude;
    private double _longitude;

    // Default constructor
    public Campsite() { }

    // Setters (mutators)
    public void setID(String id) { this._id = id; }
    public void setName(String name) { this._name = name; }
    public void setLocation(String location) { this._location = location; }
    public void setLat(double latitude) { this._latitude = latitude; }
    public void setLong(double longitude) { this._longitude = longitude; }
    public void setFeatures(String features) { this._features = features; }
    public void setTwitter(String twitter) {
        this._twitter = twitter;
        if (this._twitter.length() < 1) {
            return;
        }
        // Remove '@' symbol
        char firstChar = this._twitter.charAt(0);
        if (firstChar == '@' || firstChar == '＠') {
            this._twitter = this._twitter.substring(1);
        }
    }
    public void setAverageRating(float rating) { this._averageRating = rating; }

    // Getters (accessors)
    public String getID() { return this._id; }
    public String getName() { return this._name; }
    public String getLocation() { return this._location; }
    public double getLat() { return this._latitude; }
    public double getLong() { return this._longitude; }
    public String getFeatures() { return this._features; }
    public String getTwitter() { return this._twitter; }
    public float getAverageRating() { return this._averageRating; }

}
