package com.example.buddhika.carapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    protected static boolean DEVICE_FOUND = false;
    private BroadcastReceiver blueReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setApplicationList();
        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            blueReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                        if (state == BluetoothAdapter.STATE_ON) {
                            DEVICE_FOUND = false;
                            bluetoothAdapter.startDiscovery();
                        } else if (state == BluetoothAdapter.STATE_OFF) {
                            bluetoothAdapter.enable();
                        }
                    }
                    else if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        String deviceName = device.getName();
                        if (Arrays.asList(((EditText) findViewById(R.id.deviceName)).getText().toString().split("\\s*,\\s*")).contains(deviceName)) {
                            DEVICE_FOUND = true;
                            startApplication();
                        }
                    } else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                        setLookupTimeout(bluetoothAdapter);
                    }
                }
            };
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(blueReceiver, filter);
        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        } else {
            bluetoothAdapter.startDiscovery();
        }
    }

    protected void startApplication() {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(((AutoCompleteTextView) findViewById(R.id.applicationsAutoComplete)).getText().toString());
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
                os.writeBytes("am force-stop " + ((AutoCompleteTextView) findViewById(R.id.applicationsAutoComplete)).getText().toString() + "\n");
                os.flush();
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Root privileges are needed for closing app", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, "Root privileges are needed for closing app", Toast.LENGTH_LONG).show();
        }

    }

    protected void setLookupTimeout(final BluetoothAdapter bluetoothAdapter) {
        Integer timeout;
        try {
            timeout = (Integer.parseInt(((EditText) findViewById(R.id.checkInterval)).getText().toString()));
        } catch (Exception e) {
            timeout = 60;
        }

        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {

                        if (!bluetoothAdapter.isEnabled()) {
                            bluetoothAdapter.enable();
                        } else {
                            if (!DEVICE_FOUND) {
                                killApplication();
                            }
                            DEVICE_FOUND = false;
                            bluetoothAdapter.startDiscovery();
                        }
                    }
                },timeout * 1000);
    }

    protected void setApplicationList() {
        final PackageManager pm = getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        List<String> packageNames = new ArrayList<String>();
        for (ApplicationInfo packageInfo : packages) {
            packageNames.add(packageInfo.packageName);
        }
        ArrayAdapter<String> applicationSelectorAdaptor = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_selectable_list_item, packageNames);
        AutoCompleteTextView applicationSelector = (AutoCompleteTextView) findViewById(R.id.applicationsAutoComplete);
        applicationSelector.setAdapter(applicationSelectorAdaptor);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(blueReceiver);
    }
}