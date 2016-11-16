package edu.levifranca.meguia;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.annotation.NonNull;
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
import com.android.volley.toolbox.JsonArrayRequest;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class MainActivity extends AppCompatActivity implements BeaconConsumer {

    protected static final String TAG = "MainActivity";

    public static final String ME_GUIA_SERVER_PROTOCOL = "http://";
    public static final String ME_GUIA_SERVER_DOMAIN = "172.16.26.43";
    public static final String ME_GUIA_SERVER_PORT = ":8080";
    public static final String ME_GUIA_SERVER_CONTEXT_PATH = "/me-guia-server";

    public static final String ME_GUIA_SERVER_HOST = ME_GUIA_SERVER_PROTOCOL + ME_GUIA_SERVER_DOMAIN + ME_GUIA_SERVER_PORT + ME_GUIA_SERVER_CONTEXT_PATH;

    public static final int LOCATION_PERMISSION_REQUEST_CODE = 0;
    public static final int BLUETOOTH_ENABLE_REQUEST_CODE = 1;
    public static final int LOCATION_ENABLE_REQUEST_CODE = 2;

    private BeaconManager beaconManager;
    private LocationManager locationManager;
    private Vibrator vibrator;

    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private static final String IBEACON_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";

    private boolean isRanging = false;
    private Region region = new Region("myRangingUniqueId", null, null, null);

    private RequestQueue queue;

    private BeaconInfo lastBeaconInfo;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private MovementDetector mMovementDetector;

    private boolean willBeRightBack = false;

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

        Log.d(TAG, "START - LocationManager initialization");
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Log.d(TAG, "END - LocationManager initialization");

        Log.d(TAG, "START - Volley queue");
        queue = Volley.newRequestQueue(this);
        Log.d(TAG, "END - Volley queue");

        Log.d(TAG, "START - Vibrator");
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        Log.d(TAG, "END - Vibrator");

        Log.d(TAG, "START - ");
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMovementDetector = new MovementDetector();
        mMovementDetector.setOnMovementListener(new OnMovementListener() {
            @Override
            public void onMovement() {
                triggerRanging();
            }
        });

        Log.d(TAG, "END - MainActivity onCreate");
    }

    @Override
    protected void onResume() {
        super.onResume();

        willBeRightBack = false;
        beaconManager.bind(this);
        mSensorManager.registerListener(mMovementDetector, mAccelerometer,	SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        if (willBeRightBack) {
            super.onPause();
            return;
        }

        mSensorManager.unregisterListener(mMovementDetector);
        stopRanging();
        beaconManager.unbind(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "START - onDestroy");
        super.onDestroy();

        stopRanging();
        beaconManager.unbind(this);

        Log.d(TAG, "END - onDestroy");
    }

    public void repeatLastMessage(View view) {
        Log.d(TAG, "START - repeatLastMessage");
        playMessage();
        Log.d(TAG, "END - repeatLastMessage");
    }

    private void triggerRanging() {
        Log.d(TAG, "START - triggerRanging");
        if (isRanging) {
            Log.v(TAG, "Sensor changed but we are already ranging.");
            return;
        }

        checkPermissionAndHardwareState();

    }

    private void checkPermissionAndHardwareState() {
        Log.d(TAG, "START - checkPermissionAndHardwareState");

        checkBluetooth();

        Log.d(TAG, "END - checkPermissionAndHardwareState");
    }

    private void checkBluetooth() {
        Log.d(TAG, "START - checkBluetooth");
        if(mBluetoothAdapter.isEnabled()) {
            checkLocationPermission();
        } else {
            Log.d(TAG, "Requesting to enable Bluetooth");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            willBeRightBack = true;
            startActivityForResult(enableBtIntent, BLUETOOTH_ENABLE_REQUEST_CODE);
        }
        Log.d(TAG, "END - checkBluetooth");
    }

    private void checkLocationPermission() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Requesting Location Permission.");
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                showLongToast(R.string.message_allow_location);
            }
            willBeRightBack = true;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);

        } else {
            checkLocationEnabledForAPI23orHigher();
        }

    }

    private void checkLocationEnabledForAPI23orHigher() {

        boolean isAPI23OrGreater = Build.VERSION.SDK_INT >= 23;
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (isAPI23OrGreater && !isGPSEnabled) {

            Log.d(TAG, "API is greater than 23. Need Location Enabled");
            Intent gpsOptionsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            showLongToast(R.string.message_enable_location);
            willBeRightBack = true;
            startActivityForResult(gpsOptionsIntent, LOCATION_ENABLE_REQUEST_CODE);

        } else {
            startRanging();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(BLUETOOTH_ENABLE_REQUEST_CODE == requestCode) {

            if (RESULT_OK == resultCode) {
                Log.d(TAG, "Bluetooth was enabled.");
                checkLocationPermission();
            }
            if (RESULT_CANCELED == resultCode) {
                Log.d(TAG, "Bluetooth was NOT enabled.");
                showLongToast(R.string.message_enable_bluetooth);
            }

        }

        if (LOCATION_ENABLE_REQUEST_CODE == requestCode) {
            // Result code won't matter here as the user needs to tap the back button
            boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (isGPSEnabled) {
                Log.d(TAG, "Location was enabled.");
                startRanging();
            } else {
                Log.d(TAG, "Location was NOT enabled.");
                showLongToast(R.string.message_enable_location);
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);


        if (LOCATION_PERMISSION_REQUEST_CODE == requestCode) {
            if (permissions.length == 0) {
                Log.e(TAG, "Permission request interrupted.");
                return;
            }
            int resultCode = grantResults[0];
            if (PackageManager.PERMISSION_GRANTED == resultCode) {
                Log.d(TAG, "Location was allowed.");
                checkLocationEnabledForAPI23orHigher();
            }
            if (PackageManager.PERMISSION_DENIED == resultCode) {
                Log.d(TAG, "Location was NOT allowed.");
                showLongToast(R.string.message_allow_location);
            }
        }
    }

    private void startRanging() {
        if (!beaconManager.isBound(this)) {
            beaconManager.bind(this);
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

    private void stopRanging() {
        Log.d(TAG, "START - stopRanging");
        if(!isRanging) {
            Log.d(TAG, "We were not ranging.");
            return;
        }
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

    private void getBeaconInfoFor(String macAddress) {
        Log.d(TAG, "START - getBeaconInfoFor");
        final String url = ME_GUIA_SERVER_HOST + "/api/beacon_info?mac_address=" + macAddress;
        JsonArrayRequest jsonRequest = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
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
                        Log.e(TAG, "Error on response - " + e);
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

        String audioPath = lastBeaconInfo.getAudio();
        if (audioPath == null || audioPath.length() == 0) {
            showLongToast(lastBeaconInfo.getMensagem());
        } else {

            downloadAndPlayAudio(audioPath);

        }


        if (lastBeaconInfo.getVibrar()) {
            Log.d(TAG, "Beacon is set to vibrate device. Vibrating...");
            long[] pattern = {0, 1000, 500, 1000, 500, 1000};
            vibrator.vibrate(pattern, -1);
        }
        Log.d(TAG, "END - playMessage");
    }

    private void downloadAndPlayAudio(String audioPath) {

        AsyncTask<String, Void, File> asyncTask = new AsyncTask<String, Void, File>() {

            @Override
            protected File doInBackground(String... id) {
                File audioFile = null;

                FileOutputStream f = null;
                try {
                    URL u = new URL(ME_GUIA_SERVER_HOST + "/api/beacon/" + id[0] + "/audio.mp3");
                    HttpURLConnection c = (HttpURLConnection) u.openConnection();
                    c.setRequestMethod("GET");
                    //c.setDoOutput(true);
                    c.connect();

                    //audioFile = new File("beacons-audio/" + id + ".mp3");
                    //f = new FileOutputStream(audioFile);
                    f = openFileOutput("beacons-audio-" + id[0] + ".mp3", MODE_PRIVATE);

                    Log.d(TAG, new Integer(c.getResponseCode()).toString());
                    InputStream in = c.getInputStream();

                    byte[] buffer = new byte[1024];
                    int len1 = 0;
                    while ((len1 = in.read(buffer)) > 0) {
                        f.write(buffer, 0, len1);
                    }
                    f.close();

                    audioFile = getFileStreamPath("beacons-audio-" + id[0] + ".mp3");

                } catch (Exception e) {
                    Log.e(TAG, "Error on get and play audio file.", e);
                } finally {
                    if (f != null) {
                        try {
                            f.close();
                        } catch (IOException e) {}
                    }
                }

                return audioFile;
            }

            @Override
            protected void onPostExecute(File audioFile) {
                if (audioFile == null) return;

                playAudio(audioFile);
            }
        };
        asyncTask.execute(lastBeaconInfo.getId().toString());
    }

    private void playAudio(File audioFile) {

        MediaPlayer mp = MediaPlayer.create(this, Uri.fromFile(audioFile));
        mp.start();

    }

    private BeaconInfo parseJsonForGetByMacAddress(JSONArray response) throws JSONException {
        Log.d(TAG, "START - parseJsonForGetByMacAddress");

        JSONObject beaconJson = response.getJSONObject(0);

        BeaconInfo bInfo = BeaconInfo.getInstanceFromJSON(beaconJson);
        Log.d(TAG, "END - parseJsonForGetByMacAddress");
        return bInfo;
    }

    private void showLongToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showLongToast(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_LONG).show();
    }

}