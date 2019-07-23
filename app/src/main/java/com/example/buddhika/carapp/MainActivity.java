package com.example.buddhika.carapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.widget.TextView;

import android.util.Log;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements LocationListener {
    protected LocationManager locationManager;
    protected LocationListener locationListener;
    protected Context context;
    TextView txtLat;
    String lat;
    String provider;
    protected String latitude, longitude;
    protected boolean gps_enabled, network_enabled;

    protected boolean stopped = true;
    Timer timer;
    TimerTask timerTask;

    static final String APP_TO_CONTROL = "com.happyconz.blackbox";
    static final int MIN_DISTANCE = 50;
    static final int MIN_TIME = 900*1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtLat = (TextView) findViewById(R.id.textview1);
        timer = new Timer();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(MainActivity.this, "Location permission is not given", Toast.LENGTH_LONG).show();
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, this);
    }
    @Override
    public void onLocationChanged(Location location) {
        txtLat = (TextView) findViewById(R.id.textview1);
        txtLat.setText("Latitude:" + location.getLatitude() + ", Longitude:" + location.getLongitude());
        if (stopped) {
            stopped = false;
            startApplication();
        }
        watchForCarStop();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("Latitude","disable");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("Latitude","enable");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("Latitude","status");
    }

    protected void startApplication() {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(APP_TO_CONTROL);
        if (launchIntent != null) {
            startActivity(launchIntent);
        }
    }

    protected void killApplication() {
        Process suProcess = null;
        try {
            suProcess = Runtime.getRuntime().exec("su");
            try (DataOutputStream os = new DataOutputStream(suProcess.getOutputStream())) {
                os.writeBytes("adb shell" + "\n");
                os.flush();
                os.writeBytes("am force-stop " + (APP_TO_CONTROL) + "\n");
                os.flush();
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Root privileges are needed for closing app", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, "Root privileges are needed for closing app", Toast.LENGTH_LONG).show();
        }
    }

    protected void watchForCarStop() {
        if (timerTask != null) {
            timerTask.cancel();
        }
        timerTask = new TimerTask() {
            @Override
            public void run() {
                stopped = true;
                killApplication();
            }
        };
        timer.schedule(timerTask, MIN_TIME);
    }

}

