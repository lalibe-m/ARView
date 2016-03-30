package com.raw.arview;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;

import com.raw.utils.PaintUtils;


public class RadarView implements LocationListener {
    /**
     * The screen
     */
    public DataView view;
    /**
     * The radar's range
     */
    float range;
    /**
     * Radius in pixel on screen
     */
    public static float RADIUS = 90;
    /**
     * Position on screen
     */
    static float originX = 0, originY = 0;

    /**
     * You can change the radar color from here.
     */
    static int radarColor = Color.argb(100, 0, 0, 0);

    /**
     * Your current location is defined later
     */
    Location currentLocation;
    Location destinedLocation = new Location("provider");

    /*
     * pass the same set of coordinates to plot POI's on radar
     * */
    double[] latitudes = new double[]{50.622647, 50.62209};
    double[] longitudes = new double[]{3.039774, 3.045477};
    protected LocationManager locationManager;

    public float[][] coordinateArray = new float[latitudes.length][2];

    float angleToShift;
    public float degreetopixel;
    public float bearing;
    public float circleOriginX;
    public float circleOriginY;
    private float mscale;

    public float x = 0;
    public float y = 0;
    public float z = 0;

    float yaw = 0;
    double[] bearings;
    ARView arView = new ARView();

    public RadarView(Context context, DataView dataView, double[] bearings) {
        this.bearings = bearings;
        calculateMetrics();

        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
        currentLocation = locationManager.getLastKnownLocation(locationManager.NETWORK_PROVIDER);
    }

    public void calculateMetrics() {
        circleOriginX = originX + RADIUS;
        circleOriginY = originY + RADIUS;

        /**
         * Range of the RadarView
         */
        range = (float) arView.convertToPix(10) * 30;
        mscale = range / arView.convertToPix((int) RADIUS);
    }

    public void paint(PaintUtils dw, float yaw) {

//		circleOriginX = originX + RADIUS;
//		circleOriginY = originY + RADIUS;
        this.yaw = yaw;
//		range = arView.convertToPix(10) * 1000;		/** Draw the radar */
        dw.setFill(true);
        dw.setColor(radarColor);
        dw.paintCircle(originX + RADIUS, originY + RADIUS, RADIUS);

        /** put the markers in it */
//		float scale = range / arView.convertToPix((int)RADIUS);

        /**
         * Draw dots for each POI
         */
        for (int i = 0; i < latitudes.length; i++) {
            destinedLocation.setLatitude(latitudes[i]);
            destinedLocation.setLongitude(longitudes[i]);
            convLocToVec(currentLocation, destinedLocation);
            float x = this.x / mscale;
            float y = this.z / mscale;

            if (x * x + y * y < RADIUS * RADIUS) {
                dw.setFill(true);
                dw.setColor(Color.rgb(255, 255, 255));
                dw.paintRect(x + RADIUS, y + RADIUS, 2, 2);
            }
        }
    }

    public void calculateDistances(PaintUtils dw, float yaw) {
        /**
         * Calculate the distance from currentLocation to each POI's one
         */
        for (int i = 0; i < latitudes.length; i++) {
            if (bearings[i] < 0) {
                bearings[i] = 360 - bearings[i];
            }
            if (Math.abs(coordinateArray[i][0] - yaw) > 3) {
                angleToShift = (float) bearings[i] - this.yaw;
                coordinateArray[i][0] = this.yaw;
            } else {
                angleToShift = (float) bearings[i] - coordinateArray[i][0];
            }

            destinedLocation.setLatitude(latitudes[i]);
            destinedLocation.setLongitude(longitudes[i]);
            float[] z = new float[1];
            z[0] = 0;

            Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), destinedLocation.getLatitude(), destinedLocation.getLongitude(), z);
            bearing = currentLocation.bearingTo(destinedLocation);

            this.x = (float) (circleOriginX + 40 * (Math.cos(angleToShift)));
            this.y = (float) (circleOriginY + 40 * (Math.sin(angleToShift)));


            if (x * x + y * y < RADIUS * RADIUS) {
                dw.setFill(true);
                dw.setColor(Color.rgb(255, 255, 255));
                dw.paintRect(x + RADIUS - 1, y + RADIUS - 1, 2, 2);
            }
        }
    }

    /**
     * Width on screen
     */
    public float getWidth() {
        return RADIUS * 2;
    }

    /**
     * Height on screen
     */
    public float getHeight() {
        return RADIUS * 2;
    }


    public void set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void convLocToVec(Location source, Location destination) {
        float[] z = new float[1];
        z[0] = 0;
        Location.distanceBetween(source.getLatitude(), source.getLongitude(), destination
                .getLatitude(), source.getLongitude(), z);

        float[] x = new float[1];
        Location.distanceBetween(source.getLatitude(), source.getLongitude(), source
                .getLatitude(), destination.getLongitude(), x);
        if (source.getLatitude() < destination.getLatitude())
            z[0] *= -1;
        if (source.getLongitude() > destination.getLongitude())
            x[0] *= -1;

        set(x[0], (float) 0, z[0]);
    }

    @Override
    public void onLocationChanged(Location location) {

        /**
         *  Your current location coordinate here.
         * */
        currentLocation.setLatitude(location.getLatitude());
        currentLocation.setLongitude(location.getLongitude());
        currentLocation.setAltitude(location.getAltitude());
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("Latitude", "disable");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("Latitude", "enable");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        String newStatus = "";
        switch (status) {
            case LocationProvider.OUT_OF_SERVICE:
                newStatus = "OUT_OF_SERVICE";
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                newStatus = "TEMPORARILY_UNAVAILABLE";
                break;
            case LocationProvider.AVAILABLE:
                newStatus = "AVAILABLE";
                break;
            default:
                break;
        }
    }
}