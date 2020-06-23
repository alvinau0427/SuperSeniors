package com.example.whaledidyougo.ui.notification;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.whaledidyougo.MainActivity;
import com.example.whaledidyougo.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import android.Manifest;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static com.example.whaledidyougo.MainActivity.ALREADY_FALLDOWN;
import static com.example.whaledidyougo.MainActivity.SHARED_PREFS;
import static com.example.whaledidyougo.MainActivity.FALL_DETECT_SWITCH;
import static com.example.whaledidyougo.MainActivity.IS_FALL_DOWN;

public class NotificationFragment extends Fragment implements SensorEventListener, LocationListener {
    private final int SMS_REQUEST_CODE = 0;
    private final int LOCATION_REQUEST_CODE = 1;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationManager mLocationManager;
    private Geocoder geocoder;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private TextView x;
    private TextView y;
    private TextView z;
    private TextView sensorReader;
    private TextView lat;
    private TextView lng;
    private TextView currentAddress;
    private CountDownTimer timer;
    private List<Address> addresses;
    private boolean switchState = false;

    public boolean dialog_is_show = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification, container, false);

        // Assign TextView
        x = view.findViewById(R.id.txt_x);
        y = view.findViewById(R.id.txt_y);
        z = view.findViewById(R.id.txt_z);
        sensorReader = view.findViewById(R.id.txt_sensorReader);
        lat = view.findViewById(R.id.txt_lat);
        lng = view.findViewById(R.id.txt_lng);
        currentAddress = view.findViewById(R.id.txt_address);
        Switch sensorSwitch = view.findViewById(R.id.btn_notification_switch);
        Button setContactBtn = view.findViewById(R.id.btn_add_caretaker);
        Button getContactBtn = view.findViewById(R.id.btn_list_caretaker);

        /*
        // Request SMS Permission
        if (!checkSmsPermission()) {
            requestSmsPermission();
        }
         */

        // Assign Sensor
        mSensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        assert mSensorManager != null;
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // Assign Location
        mLocationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        getLastLocation();

        // Change location to address
        geocoder = new Geocoder(requireContext(), Locale.getDefault());

        // Fall detection alert activation state
        sharedPreferences = requireActivity().getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        sensorSwitch.setChecked(sharedPreferences.getBoolean(FALL_DETECT_SWITCH, true));
        switchState = sensorSwitch.isChecked();

        sensorSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled
                    switchState = true;
                    editor.putBoolean(FALL_DETECT_SWITCH, true);
                    editor.apply();
                } else {
                    // The toggle is disabled
                    switchState = false;
                    editor.putBoolean(FALL_DETECT_SWITCH, false);
                    editor.apply();
                }
            }
        });

        setContactBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent setContentView = new Intent(getActivity(), AddCaretakerActivity.class);
                startActivity(setContentView);
            }
        });

        getContactBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent getContactView = new Intent(getActivity(), ListCaretakerActivity.class);
                startActivity(getContactView);
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        if (checkLocationPermission()) {
            getLastLocation();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // mSensorManager.unregisterListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    // Request Permission Area : START
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case SMS_REQUEST_CODE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    sendMessage();
                    Toast.makeText(getActivity(), R.string.notification_sms_permission_accept, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getActivity(), R.string.notification_sms_permission_decline, Toast.LENGTH_LONG).show();
                }
                break;
            case LOCATION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLastLocation();
                }
                break;
        }
    }

    private boolean checkSmsPermission() {
        return (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED);
    }

    private void requestSmsPermission() {
        ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.SEND_SMS}, SMS_REQUEST_CODE);
    }

    private boolean checkLocationPermission() {
        return (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
    }
    // Request Permission Area : END

    // Fall Detection Area : START
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not in use
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onSensorChanged(final SensorEvent event){
        x.setText("" + event.values[0]);
        y.setText("" + event.values[1]);
        z.setText("" + event.values[2]);
        //Log.i("DEBUG: ", "X " + x + "Y " + y + "Z " + z);

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            double sum = Math.sqrt( Math.pow(event.values[0], 2) + Math.pow(event.values[1], 2) + Math.pow(event.values[2], 2) );
            DecimalFormat precision = new DecimalFormat("0.00");
            double accRound = Double.parseDouble(precision.format(sum));
            sensorReader.setText("" + accRound);
            //Log.i("DEBUG: ", "accRound " + accRound);
            //Log.i("XXXXXXXX",""+dialog_is_show);

            if(switchState) {
                if(sharedPreferences.getBoolean(IS_FALL_DOWN, false)) {//if (accRound <= 2.0) {
                    if (isAdded()) {
                        editor.putBoolean(IS_FALL_DOWN, true);
                        editor.apply();
                        mSensorManager.unregisterListener(this);
                        //Toast.makeText(getActivity(), R.string.notification_fall_detected_message, Toast.LENGTH_SHORT).show();
                        //requireActivity().startService(new Intent(getActivity(), AlertService.class));
                        if(!sharedPreferences.getBoolean(ALREADY_FALLDOWN, false)) {//if(!dialog_is_show) {
                            requireActivity().startService(new Intent(getActivity(), AlertService.class));
                            showTimerDialog();
                            //Log.i("XXXXXXXX",""+dialog_is_show);
                        } else { }
                    }
                }
            }
        }
    }

    private void showTimerDialog() {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
        alertDialogBuilder.setTitle(R.string.notification_timer_confirm_message);
        alertDialogBuilder.setMessage("").setPositiveButton(R.string.notification_timer_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        dialog_is_show = false;
                        editor.putBoolean(IS_FALL_DOWN, false);
                        editor.apply();
                        editor.putBoolean(ALREADY_FALLDOWN, false);
                        editor.commit();
                    }
        });

        final AlertDialog alertDialog = alertDialogBuilder.create();
        dialog_is_show = true;
        editor.putBoolean(ALREADY_FALLDOWN, true);
        editor.commit();
        alertDialog.show();
        timer = new CountDownTimer(15000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                int minutes = seconds / 60;
                // seconds = seconds % 60;
                alertDialog.setMessage(getString(R.string.notification_timer_message) + " " + String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
                alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        timer.cancel();
                        requireActivity().stopService(new Intent(getActivity(),AlertService.class));
                        onResume();
                    }
                });
            }

            @Override
            public void onFinish() {
                try {
                    //editor.putBoolean(IS_FALL_DOWN, false);
                    //editor.apply();
                    //editor.putBoolean(ALREADY_FALLDOWN, false);
                    //editor.commit();
                    sendMessage();
                    dialog_is_show = false;
                    editor.putBoolean(IS_FALL_DOWN, false);
                    editor.apply();
                    editor.putBoolean(ALREADY_FALLDOWN, false);
                    editor.commit();
                    // alertDialog.dismiss();
                    // onResume();
                    // Toast.makeText(getActivity(), "Sent sms", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    requireActivity().stopService(new Intent(getActivity(), AlertService.class));
                }
            }
        }.start();

    }

    private void sendMessage() {
        String strLat = lat.getText().toString();
        String strLng = lng.getText().toString();
        //String message = getString(R.string.notification_sms_message) + strLat + "," + strLng;
        String name = requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE).getString("Name", "Null");
        //String address = currentAddress.getText().toString();
        String message = getString(R.string.notification_sms_message1) + name + getString(R.string.notification_sms_message2) + strLat + "," + strLng;

        if (checkSmsPermission()) {
            SQLiteDatabase myDB = requireActivity().openOrCreateDatabase(DatabaseHelper.DATABASE_NAME, Context.MODE_PRIVATE, null);
            Cursor phoneNumbers = myDB.rawQuery("SELECT * FROM caretaker_table", null);
            if (phoneNumbers.getCount() > 0) {
                for (phoneNumbers.moveToFirst(); !phoneNumbers.isAfterLast(); phoneNumbers.moveToNext()) {
                    String smsTo = phoneNumbers.getString(2).trim();

                    SmsManager smsManager = SmsManager.getDefault();

                    smsManager.sendTextMessage(smsTo, null, message, null, null);
                    // Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                    Toast.makeText(getContext(), R.string.notification_sms_success, Toast.LENGTH_LONG).show();
                }
            } else {
                Log.d(getString(R.string.notification_sms_no_phone), "null");
                Toast.makeText(getActivity(), R.string.notification_sms_no_phone_message, Toast.LENGTH_LONG).show();
            }
            phoneNumbers.close();
        } else {
            requestSmsPermission();
        }
    }
    // Fall Detection Area : END

    // Location Detection Area : START
    private boolean isLocationEnabled() {
        return mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        if (checkLocationPermission()) {
            if (isLocationEnabled()) {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
                mFusedLocationClient.getLastLocation().addOnCompleteListener(
                        new OnCompleteListener<Location>() {
                            @SuppressLint("SetTextI18n")
                            @Override
                            public void onComplete(@NonNull Task<Location> task) {
                                Location location = task.getResult();
                                if (location == null) {
                                    requestNewLocationData();
                                } else {
                                    lat.setText("" + location.getLatitude());
                                    lng.setText("" + location.getLongitude());
                                    try {
                                        addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                        String address = addresses.get(0).getAddressLine(0);
                                        String country = addresses.get(0).getCountryName();
                                        currentAddress.setText(address + ", " + country);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                );
            } else {
                Toast.makeText(getActivity(), R.string.notification_location_on, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            requestLocationPermission();
        }
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationData() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(0);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setNumUpdates(1);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
    }

    private LocationCallback mLocationCallback = new LocationCallback() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();
            lat.setText("" + mLastLocation.getLatitude());
            lng.setText("" + mLastLocation.getLongitude());
            try {
                addresses = geocoder.getFromLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude(), 1);
                String address = addresses.get(0).getAddressLine(0);
                String country = addresses.get(0).getCountryName();
                currentAddress.setText(address + ", " + country);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    @SuppressLint("SetTextI18n")
    @Override
    public void onLocationChanged(Location location) {
        lat.setText("" + location.getLatitude());
        lng.setText("" + location.getLongitude());
        try {
            addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            String address = addresses.get(0).getAddressLine(0);
            String country = addresses.get(0).getCountryName();
            currentAddress.setText(address + ", " + country);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Not in use
    }

    @Override
    public void onProviderEnabled(String provider) {
        // Not in use
    }

    @Override
    public void onProviderDisabled(String provider) {
        // Not in use
    }
    // Location Detection Area : END
}