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
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class MainActivity extends AppCompatActivity implements BeaconConsumer, SensorEventListener {

    protected static final String TAG = "MainActivity";

    public static final String ME_GUIA_SERVER_PROTOCOL = "http://";
    public static final String ME_GUIA_SERVER_DOMAIN = "172.16.26.43";
    public static final String ME_GUIA_SERVER_PORT = ":1080";

    public static final String ME_GUIA_SERVER_HOST = ME_GUIA_SERVER_PROTOCOL + ME_GUIA_SERVER_DOMAIN + ME_GUIA_SERVER_PORT;

    private BeaconManager beaconManager;
    private LocationManager locationManager;
    private Vibrator vibrator;

    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private static final String IBEACON_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";

    private Boolean isRanging = false;
    private Region region = new Region("myRangingUniqueId", null, null, null);

    private float lastX;
    private float lastY;
    private float lastZ;

    int THRESHOLD = 15;

    private RequestQueue queue;

    private BeaconInfo lastBeaconInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "START - MainActivity onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "START - BeaconManager initialization");
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.bind(this);

        beaconManager.getBeaconParsers().add(new BeaconParser("ALTBEACON").setBeaconLayout(BeaconParser.ALTBEACON_LAYOUT));
        beaconManager.getBeaconParsers().add(new BeaconParser("EDDYSTONE_TLM").setBeaconLayout(BeaconParser.EDDYSTONE_TLM_LAYOUT));
        beaconManager.getBeaconParsers().add(new BeaconParser("EDDYSTONE_UID").setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));
        beaconManager.getBeaconParsers().add(new BeaconParser("EDDYSTONE_URL").setBeaconLayout(BeaconParser.EDDYSTONE_URL_LAYOUT));
        beaconManager.getBeaconParsers().add(new BeaconParser("URI").setBeaconLayout(BeaconParser.URI_BEACON_LAYOUT));

        beaconManager.getBeaconParsers().add(new BeaconParser("IBEACON").setBeaconLayout(IBEACON_LAYOUT));
        Log.d(TAG, "END - BeaconManager initialization");

        Log.d(TAG, "START - SensorManager initialization");
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        Log.d(TAG, "END - SensorManager initialization");

        Log.d(TAG, "START - LocationManager initialization");
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Log.d(TAG, "END - LocationManager initialization");

        Log.d(TAG, "START - Volley queue");
        queue = Volley.newRequestQueue(this);
        Log.d(TAG, "END - Volley queue");

        Log.d(TAG, "START - Vibrator");
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        Log.d(TAG, "END - Vibrator");

        Log.d(TAG, "END - MainActivity onCreate");
    }

    public void repeatLastMessage(View view) {
        Log.d(TAG, "START - repeatLastMessage");
        playMessage();
        Log.d(TAG, "END - repeatLastMessage");
    }

    private void startRanging() {
        Log.d(TAG, "START - startRanging");
        if (isRanging) {
            Log.v(TAG, "Sensor changed but we are already ranging.");
            return;
        }

        if (!hasPermissionsNeeded()) {
            Log.d(TAG, "Does not have the required permissions.");
            return;
        }
        try {
            beaconManager.startRangingBeaconsInRegion(region);
            isRanging = true;
            Log.d(TAG, "BeaconRanging - START");
        } catch (RemoteException e) {
            Log.e(TAG, "Error when starting ranging for beacons", e);
        }
        Log.d(TAG, "END - startRanging");
    }

    // FIXME
    private boolean hasPermissionsNeeded() {
        Log.d(TAG, "START - hasPermissionsNeeded");

        boolean result;

        checkBluetooth();

        Log.d(TAG, "Checking for location permissions");
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isAPI23OrGreater = Build.VERSION.SDK_INT >= 23;
        if (isAPI23OrGreater && !isGPSEnabled) {
            Log.d(TAG, "API is greater than 23. Need Location Enabled");
            Intent gpsOptionsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            // TODO Move to strings.xml
            Toast.makeText(this, "Por Favor, habilite a localização.", Toast.LENGTH_LONG).show();
            startActivityForResult(gpsOptionsIntent, 1);
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            Log.d(TAG, "Requesting Location Permission.");
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
            } else {
                Log.d(TAG, "????"); //FIXME
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        0);
            }
            result = false;
        }

        result = true;
        Log.d(TAG, "END - hasPermissionsNeeded");
        return result;
    }

    private void checkBluetooth() {
        Log.d(TAG, "START - checkBluetooth");
        if(!mBluetoothAdapter.isEnabled())
        {
            Log.d(TAG, "Requesting to enable Bluetooth");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
        Log.d(TAG, "END - checkBluetooth");
    }

    private void stopRanging() {
        Log.d(TAG, "START - stopRanging");
        try {
            beaconManager.stopRangingBeaconsInRegion(region);
            isRanging = false;
            Log.d(TAG, "BeaconRanging - STOP");
        } catch (RemoteException e) {
            Log.e(TAG, "Error when stoping ranging for beacon.", e);
        }
        Log.d(TAG, "END - stopRanging");
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "START - onDestroy");
        super.onDestroy();
        beaconManager.unbind(this);

        Log.d(TAG, "END - onDestroy");
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0 && isRanging) {
                    Log.d(TAG,"I see " + beacons.size() + " beacon(s)");

                    Collections.sort((ArrayList<Beacon>) beacons, new NearestBeaconComparator());
                    Beacon nearestBeacon = ((ArrayList<Beacon>) beacons).get(0);

                    Log.d(TAG,"The nearest beacon " + nearestBeacon.toString() + " is about " + nearestBeacon.getDistance() + " meters away." +
                            " Its MAC Address is " + nearestBeacon.getBluetoothAddress() + ".");

                    getBeaconInfoFor(nearestBeacon.getBluetoothAddress());

                    stopRanging();
                }
            }

        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.v(TAG, "START - onSensorChanged");

        float xChange = lastX - event.values[0];
        float yChange = lastY - event.values[1];
        float zChange = lastZ - event.values[2];

        lastX = event.values[0];
        lastY = event.values[1];
        lastZ = event.values[2];

        double changeAbs = Math.abs(xChange) + Math.abs(yChange) + Math.abs(zChange);

        if(changeAbs > THRESHOLD) {
            Log.d(TAG, "Change was above the threshold. Change was " + changeAbs);
            startRanging();
        }
        Log.v(TAG, "END - onSensorChanged");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // do nothing
    }

    private void getBeaconInfoFor(String macAddress) {
        Log.d(TAG, "START - getBeaconInfoFor");
        final String url = ME_GUIA_SERVER_HOST + "/beacon_info?mac_address=" + macAddress;
        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, "Response for " + url + " successful.");
                        try {
                            BeaconInfo bInfo = parseJsonForGetByMacAddress(response);
                            if (bInfo.equals(lastBeaconInfo)) {
                                Log.d(TAG, "We have found the same beacon found the last time.");
                                return;
                            }

                            lastBeaconInfo = bInfo;
                            playMessage();
                        } catch (JSONException e) {
                            Log.e(TAG, "Error on parsing json", e);
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError e) {
                        Log.e(TAG, "Error on response - Status Code: " + e.networkResponse.statusCode);
                    }
                });
        queue.add(jsonRequest);
        Log.d(TAG, "END - getBeaconInfoFor");
    }

    private void playMessage() {
        Log.d(TAG, "START - playMessage");
        if(lastBeaconInfo == null) {
            Log.d(TAG, "We haven't found a beacon yet.");
            return;
        }

        Toast.makeText(this, lastBeaconInfo.getMensagem(), Toast.LENGTH_LONG).show();

        if (lastBeaconInfo.getVibrar()) {
            Log.d(TAG, "Beacon is set to vibrate device. Vibrating...");
            long[] pattern = {0, 1000, 500, 1000, 500, 1000};
            vibrator.vibrate(pattern, -1);
        }
        Log.d(TAG, "END - playMessage");
    }

    private BeaconInfo parseJsonForGetByMacAddress(JSONObject response) throws JSONException {
        Log.d(TAG, "START - parseJsonForGetByMacAddress");
        JSONObject sucessoObj = response.getJSONObject("sucesso");

        JSONArray beacons = sucessoObj.getJSONArray("beacons");

        JSONObject beaconJson = beacons.getJSONObject(0);

        BeaconInfo bInfo = BeaconInfo.getInstanceFromJSON(beaconJson);
        Log.d(TAG, "END - parseJsonForGetByMacAddress");
        return bInfo;
    }



    // TODO: Refatorar o código
}