package com.example.whaledidyougo;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

import cat.ereza.customactivityoncrash.config.CaocConfig;

public class MainActivity extends AppCompatActivity {
    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String FALL_DETECT_SWITCH = "sensorSwitch";
    public static final String IS_FALL_DOWN = "isFallDown";
    public static final String SHOULD_REDIAL = "should_redial";
    public static final String IN_BACKEND = "inBackend";
    public static final String KILL_APP = "killApp";
    public static final String ALREADY_FALLDOWN = "already_falldown";
    public static final String REDIAL_REMINDER = "redial_reminder";

    private GoogleSignInClient mGoogleSignInClient;
    private GoogleSignInOptions gso;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Action bar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Bottom navigation
        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(R.id.navigation_schedule, R.id.navigation_dashboard, R.id.navigation_notification).build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        // Crash Screen
        CaocConfig.Builder.create()
                .backgroundMode(CaocConfig.BACKGROUND_MODE_SHOW_CUSTOM)
                .enabled(true)
                .showErrorDetails(true)
                .showRestartButton(true)
                .logErrorOnRestart(true)
                .trackActivities(false)
                .minTimeBetweenCrashesMs(3000)
                .errorDrawable(null)
                .restartActivity(MainActivity.class)
                .errorActivity(CrashScreen.class)
                .eventListener(null)
                .apply();

        // Setup the information of Google account
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        sharedPreferences = this.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        editor.putBoolean(KILL_APP, false);
        editor.apply();
    }

    @Override
    public void onResume() {
        super.onResume();
        ActivityManager activityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            Log.i("come back ", ""+appProcess.processName);
            if(appProcess.processName.equals("com.example.whaledidyougo")) {
                Log.i("XXXXXXX", "" + "whaledidyougo go to back");

                sharedPreferences = this.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
                editor = sharedPreferences.edit();
                editor.putBoolean(IN_BACKEND, false);
                editor.apply();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        //check application is opened or not
        ActivityManager activityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            Log.i("go back ", "" + appProcess.processName);
            if(appProcess.processName.equals("com.example.whaledidyougo")) {
                Log.i("XXXXXXX", "" + "whaledidyougo go to back");

                sharedPreferences = this.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
                editor = sharedPreferences.edit();
                editor.putBoolean(IN_BACKEND, true);
                editor.apply();
            }
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.i("XXXXXXX", "" + "Killed App");
        sharedPreferences = this.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        editor.putBoolean(KILL_APP, true);
        editor.apply();
    }

    private void signOut() {
        mGoogleSignInClient.signOut().addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                // Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                // startActivity(intent);
                finish();
                LoginActivity.buttonLogout.setVisibility(View.INVISIBLE);
                Toast.makeText(MainActivity.this, getString(R.string.logout_success_message), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void restartAct() {
        finish();
        Intent _Intent = new Intent(this, MainActivity.class);
        startActivity(_Intent);
        overridePendingTransition(0, 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu and adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks actions
        switch (item.getItemId()) {
            case R.id.action_logout:
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int selection) {
                        switch (selection) {
                            case DialogInterface.BUTTON_POSITIVE:
                                signOut();
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                Toast.makeText(MainActivity.this, getString(R.string.action_logout_cancel), Toast.LENGTH_SHORT).show();
                                break;
                        }
                    }
                };

                new AlertDialog.Builder(this)
                        .setMessage(R.string.action_logout_confirm_message)
                        .setPositiveButton(getString(R.string.action_logout_confirm_yes), dialogClickListener)
                        .setNegativeButton(getString(R.string.action_logout_confirm_no), dialogClickListener)
                        .show();
                break;
            case R.id.action_language_english:
                if (LocaleUtils.needUpdateLocale(this, LocaleUtils.LOCALE_ENGLISH)) {
                    LocaleUtils.updateLocale(this, LocaleUtils.LOCALE_ENGLISH);
                    restartAct();
                }
                break;
            case R.id.action_language_chinese:
                if (LocaleUtils.needUpdateLocale(this, LocaleUtils.LOCALE_CHINESE)) {
                    LocaleUtils.updateLocale(this, LocaleUtils.LOCALE_CHINESE);
                    restartAct();
                }
                break;
            case R.id.action_about:
                // new AlertDialog.Builder(this).setTitle(R.string.action_about).setMessage(R.string.action_about_message).setNeutralButton(android.R.string.ok, null).show();
                Intent actionAbout = new Intent(this, AboutActivity.class);
                startActivity(actionAbout);
                break;
        }
        return false;
    }

}
