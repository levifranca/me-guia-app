package edu.levifranca.meguia;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class MainActivity extends AppCompatActivity implements BeaconConsumer, SensorEventListener {
    protected static final String TAG = "MainActivity";
    private BeaconManager beaconManager;
    private SensorManager sensorManager;
    private LocationManager locationManager;

    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private static final String IBEACON_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.bind(this);

        beaconManager.getBeaconParsers().add(new BeaconParser("ALTBEACON").setBeaconLayout(BeaconParser.ALTBEACON_LAYOUT));
        beaconManager.getBeaconParsers().add(new BeaconParser("EDDYSTONE_TLM").setBeaconLayout(BeaconParser.EDDYSTONE_TLM_LAYOUT));
        beaconManager.getBeaconParsers().add(new BeaconParser("EDDYSTONE_UID").setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));
        beaconManager.getBeaconParsers().add(new BeaconParser("EDDYSTONE_URL").setBeaconLayout(BeaconParser.EDDYSTONE_URL_LAYOUT));
        beaconManager.getBeaconParsers().add(new BeaconParser("URI").setBeaconLayout(BeaconParser.URI_BEACON_LAYOUT));

        beaconManager.getBeaconParsers().add(new BeaconParser("IBEACON").setBeaconLayout(IBEACON_LAYOUT));

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

    }

    public void repeatLastMessage(View view) {
        // Toast.makeText(this, "Última mensagem", Toast.LENGTH_LONG).show();
    }

    /*
    try {
            beaconManager.startMonitoringBeaconsInRegion(new Region("myMonitoringUniqueId", null, null, null));
        } catch (RemoteException e) {
            Log.e(TAG, " - ", e);
        }
     */

    private boolean isRanging = false;
    private Region region = new Region("myRangingUniqueId", null, null, null);

    private void startRanging() {
        try {
            if (!hasPermissionsNeeded()) {
                return;
            }
            beaconManager.startRangingBeaconsInRegion(region);
            isRanging = true;
            Log.d(TAG, "BeaconRanging - START");
        } catch (RemoteException e) {
            Log.e(TAG, " - ", e);
        }
    }

    private boolean hasPermissionsNeeded() {

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isAPI23OrGreater = Build.VERSION.SDK_INT >= 23;
        if (isAPI23OrGreater && !isGPSEnabled) {
            Intent gpsOptionsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            Toast.makeText(this, "Por Favor, habilite a localização.", Toast.LENGTH_LONG).show();
            startActivityForResult(gpsOptionsIntent, 1);
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        0);
            }
            return false;
        }
        return true;
    }

    private void stopRanging() {
        try {
            beaconManager.stopRangingBeaconsInRegion(region);
            isRanging = false;
            Log.d(TAG, "BeaconRanging - STOP");
        } catch (RemoteException e) {
            Log.e(TAG, " - ", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0 && isRanging) {
                    //Beacon firstBeacon = beacons.iterator().next();
                    Log.i(TAG,"I see " + beacons.size() + " beacon(s)");
                    Collections.sort((ArrayList<Beacon>) beacons, new NearestBeaconComparator());
                    Beacon firstBeacon = ((ArrayList<Beacon>) beacons).get(0);
                    Log.i(TAG,"The first beacon " + firstBeacon.toString() + " is about " + firstBeacon.getDistance() + " meters away." +
                            " Its MAC Address is " + firstBeacon.getBluetoothAddress() + ".");

                    stopRanging();
                }
            }

        });
/*
        beaconManager.addMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {
                Log.i(TAG, "I just saw an beacon for the first time!");r
                startRanging();
            }

            @Override
            public void didExitRegion(Region region) {
                Log.i(TAG, "I no longer see an beacon");
            }

            @Override
            public void didDetermineStateForRegion(int state, Region region) {
                Log.i(TAG, "I have just switched from seeing/not seeing beacons: " + state);
                if (MonitorNotifier.INSIDE == state) {
                    startRanging();
                }
            }
        });
  */
    }

    private float lastX;
    private float lastY;
    private float lastZ;
    private float lastTime;

    int THRESHOLD = 15;
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isRanging) {
            return;
        }

        float currTime =  event.timestamp;
        if ((currTime - lastTime) < 1000) { // has it passed 1s?
            return;
        }

        lastTime = currTime;
        float xChange = lastX - event.values[0];
        float yChange = lastY - event.values[1];
        float zChange = lastZ - event.values[2];

        lastX = event.values[0];
        lastY = event.values[1];
        lastZ = event.values[2];

        double changeAbs = Math.abs(xChange) + Math.abs(yChange) + Math.abs(zChange);

        if(changeAbs > THRESHOLD) {
            startRanging();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    // TODO: Check Location permission (only for 6.0+)
    // TODO: Check Bluetooth
}