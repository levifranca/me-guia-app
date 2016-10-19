package edu.levifranca.meguia;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * Created by levifranca on 17/09/16.
 */
public class MovementDetector implements SensorEventListener {

    private static final float MOVEMENT_THRESHOLD_GRAVITY = 1.3F;
    private static final int MOVEMENT_SLOP_TIME_MS = 500;

    private OnMovementListener mListener;
    private long mShakeTimestamp;

    public void setOnMovementListener(OnMovementListener listener) {
        this.mListener = listener;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // do nothing
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (mListener != null) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            float gX = x / SensorManager.GRAVITY_EARTH;
            float gY = y / SensorManager.GRAVITY_EARTH;
            float gZ = z / SensorManager.GRAVITY_EARTH;

            float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

            if (gForce > MOVEMENT_THRESHOLD_GRAVITY) {
                final long now = System.currentTimeMillis();
                if (mShakeTimestamp + MOVEMENT_SLOP_TIME_MS > now) {
                    return;
                }

                Log.d("MovementDetector", "Movement detected. G-Force is " + gForce);
                mShakeTimestamp = now;

                mListener.onMovement();
            }
        }
    }
}
