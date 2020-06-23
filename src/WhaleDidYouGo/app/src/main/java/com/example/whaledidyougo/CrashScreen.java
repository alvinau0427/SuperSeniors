package com.example.whaledidyougo;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import cat.ereza.customactivityoncrash.CustomActivityOnCrash;
import cat.ereza.customactivityoncrash.config.CaocConfig;

public class CrashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_crash_screen);

        // CustomActivityOnCrash.getStackTraceFromIntent(getIntent()); // gets the stack trace as a string
        // CustomActivityOnCrash.getActivityLogFromIntent(getIntent()); // gets the activity log as a string
        // CustomActivityOnCrash.getAllErrorDetailsFromIntent(context, getIntent()); // returns all error details including stacktrace as a string
        // CustomActivityOnCrash.getConfigFromIntent(getIntent()); // returns the config of the library when the error happened

        TextView errorDetailsText = findViewById(R.id.error_details);
        errorDetailsText.setText(CustomActivityOnCrash.getStackTraceFromIntent(getIntent()));

        Button restartButton = findViewById(R.id.restart_button);

        final CaocConfig config = CustomActivityOnCrash.getConfigFromIntent(getIntent());

        if (config == null) {
            // This should never happen, just finish the activity to avoid a recursive crash
            finish();
            return;
        }

        if (config.isShowRestartButton() && config.getRestartActivityClass() != null) {
            restartButton.setText(R.string.restart_app);
            restartButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CustomActivityOnCrash.restartApplication(CrashScreen.this, config);
                }
            });
        } else {
            restartButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CustomActivityOnCrash.closeApplication(CrashScreen.this, config);
                }
            });
        }
    }
}
