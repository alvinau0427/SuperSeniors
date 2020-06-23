package com.example.whaledidyougo;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.example.whaledidyougo.ui.notification.NotificationFragment;

import java.text.DecimalFormat;


import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import static com.example.whaledidyougo.MainActivity.ALREADY_FALLDOWN;
import static com.example.whaledidyougo.MainActivity.IN_BACKEND;
import static com.example.whaledidyougo.MainActivity.KILL_APP;
import static com.example.whaledidyougo.MainActivity.SHARED_PREFS;
import static com.example.whaledidyougo.MainActivity.FALL_DETECT_SWITCH;
import static com.example.whaledidyougo.MainActivity.IS_FALL_DOWN;

public class FallDownDetection extends AccessibilityService implements SensorEventListener {

    SensorManager mSensorManager;
    Sensor mAccelerometerSensor;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    final String packageName = "com.example.whaledidyougo";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        //acquireLock(this);
        Log.d("myaccess","after lock");
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            Log.d("myaccess","in window changed");
            AccessibilityNodeInfo info = event.getSource();
            //Log.i("DEBUG","" + info);
        }
    }


    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Toast.makeText(this,"Accessibility Service Started", Toast.LENGTH_SHORT).show();

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 0;
        info.packageNames = null;
        setServiceInfo(info);

        sharedPreferences = getApplicationContext().getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);



    }

    @Override
    public void onInterrupt() {
        // Not in use
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            double sum = Math.sqrt( Math.pow(event.values[0], 2) + Math.pow(event.values[1], 2) + Math.pow(event.values[2], 2) );
            DecimalFormat precision = new DecimalFormat("0.00");
            double accRound = Double.parseDouble(precision.format(sum));

            boolean b = sharedPreferences.getBoolean(ALREADY_FALLDOWN, false);
            //Log.i("", "" + b);

            if (accRound <= 2.0) {
                Toast.makeText(getApplicationContext(), "Detect fall down by AccessibilityService", Toast.LENGTH_SHORT).show();
                //Log.i("", "donw");

                //sharedPreferences.getBoolean(IS_FALL_DOWN, true);
                editor.putBoolean(IS_FALL_DOWN, true);
                editor.apply();

                if (!sharedPreferences.getBoolean(KILL_APP, false)) {       //如果app開左
                    // Enforce to open application
                    if (sharedPreferences.getBoolean(IN_BACKEND, true)) {
                        if(!b) {    //如果是初次跌倒
                            //onResume

                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_MAIN);
                            intent.addCategory(Intent.CATEGORY_LAUNCHER);
                            intent.setComponent(new ComponentName(getApplicationContext(), MainActivity.class));
                            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                                    | Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                            getApplicationContext().startActivity(intent);

                        } else { return; }
                        //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        //getApplicationContext().startActivity(intent);

                    Log.i("ZZZZZZZZZZ", "fall detect in backend");
                    } else if (!sharedPreferences.getBoolean(IN_BACKEND, true)) {   //如果app係
                        //fragment to fragment
                        Log.i("ZZZZZZZZZZ", "fall detect in frontend");

                        if(!b) {    //如果是初次跌倒

                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_MAIN);
                            intent.addCategory(Intent.CATEGORY_LAUNCHER);
                            intent.setComponent(new ComponentName(getApplicationContext(), MainActivity.class));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //FLAG_ACTIVITY_NEW_TASK
                            getApplicationContext().startActivity(intent);

                        } else {
                            return;
                        }

                    }
                }else {
                    //Enforce to open application
                    Log.i("ZZZZZZZZZZ", "fall detect in kill app");
                    if(!b) {    //如果是初次跌倒

                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getApplicationContext().startActivity(intent);

                    } else { return; }
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not in use
    }
}
