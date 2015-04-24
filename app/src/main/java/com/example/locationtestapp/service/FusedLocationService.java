package com.example.locationtestapp.service;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.example.locationtestapp.AppConstant;
import com.example.locationtestapp.stack.HttpStack;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by whdcn_000 on 2015-02-09.
 */

public class FusedLocationService extends Service implements Response.Listener, Response.ErrorListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = FusedLocationService.class.getSimpleName();

    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10 * 60 * 1000;  // 10분

    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;     // 5분

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    protected Boolean mRequestingLocationUpdates;


    @Override
    public void onStart(Intent intent, int startId) {
        Log.i(TAG, "FusedLocationService onStart");
        super.onStart(intent, startId);
        mGoogleApiClient.connect();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "FusedLocationService onCreate");
        mRequestingLocationUpdates = false;
        unregisterRestartAlarm();
        buildGoogleApiClient();

        //startLocationUpdates();
    }

    // support persistent of Service
    public void registerRestartAlarm() {
        Log.d(TAG, "registerRestartAlarm");
        Intent intent = new Intent(FusedLocationService.this, RestartService.class);
        intent.setAction("ACTION.RESTART.FusedLocationService");
        PendingIntent sender = PendingIntent.getBroadcast(FusedLocationService.this, 0, intent, 0);
        long firstTime = SystemClock.elapsedRealtime();
        firstTime += 1000;                                               // 10초 후에 알람이벤트 발생
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime, 1000, sender);
    }

    public void unregisterRestartAlarm() {
        Log.d(TAG, "unregisterRestartAlarm");
        Intent intent = new Intent(FusedLocationService.this, RestartService.class);
        intent.setAction("ACTION.RESTART.FusedLocationService");
        PendingIntent sender = PendingIntent.getBroadcast(FusedLocationService.this, 0, intent, 0);
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.cancel(sender);
    }

    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        createLocationRequest();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        // 앱이 위치 업데이트를받을 수있는 가장 빠른 속도를 제어한다
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        //Set the priority of the request.
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //Set the minimum displacement between location updates in meters
        mLocationRequest.setSmallestDisplacement(30);
    }

    protected void startLocationUpdates() {
        Log.i(TAG, "startLocationUpdates");
        if (!mRequestingLocationUpdates) {
            mRequestingLocationUpdates = true;
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    protected void stopLocationUpdates() {
        Log.i(TAG, "stopLocationUpdates");
        if (mRequestingLocationUpdates) {
            mRequestingLocationUpdates = false;
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
        mGoogleApiClient.disconnect();
        registerRestartAlarm();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (lastLocation != null) {
            Log.i(TAG, "onConnected lastLocation is not null");
            sendLocationUsingBroadCast(lastLocation);
        }

        if (!mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());

        if (connectionResult.getErrorCode() == ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED) {
            Intent locationBroadcast = new Intent(AppConstant.GPS_INFO_BROADCAST);
            locationBroadcast.putExtra("OLD_GOOGLE_PLAY_VERSION", String.valueOf(ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED));
            LocalBroadcastManager.getInstance(this).sendBroadcast(locationBroadcast);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location == null) {
            return;
        }
        Log.i(TAG, "onLocationChanged : " + String.valueOf(location.getLatitude()) + " " + String.valueOf(location.getLongitude()));
        sendLocationUsingBroadCast(location);

    }

    private void sendLocationUsingBroadCast(final Location location) {
        Intent locationBroadcast = new Intent(AppConstant.GPS_INFO_BROADCAST);
        locationBroadcast.putExtra("LOCATION", location);
        LocalBroadcastManager.getInstance(this).sendBroadcast(locationBroadcast);

        StringRequest postRequest = new StringRequest(Request.Method.POST, "http://bold-meridian-91808.appspot.com/gps/add", this, this) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("userId", Build.MODEL + " / " + Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
                params.put("time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                params.put("latitude", String.valueOf(location.getLatitude()));
                params.put("longitude", String.valueOf(location.getLongitude()));

                return params;
            }
        };
        HttpStack.getInstance(this).addToRequestQueue(postRequest);
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        Log.d("Error.Response", "error.getMessage()");
    }

    @Override
    public void onResponse(Object response) {
        Log.d("Response", (String) response);
    }
}