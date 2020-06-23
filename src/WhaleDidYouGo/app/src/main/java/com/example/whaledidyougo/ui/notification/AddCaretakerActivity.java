package com.example.whaledidyougo.ui.notification;

import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.whaledidyougo.R;

public class AddCaretakerActivity extends AppCompatActivity {
    DatabaseHelper myDB;
    EditText caretakerName, caretakerNumber;
    Button saveBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_caretaker);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.title_caretaker_set);

        // SQLite database setup
        myDB = new DatabaseHelper(this);

        // Caretaker name and phone number input box
        caretakerName = findViewById(R.id.edt_name);
        caretakerNumber = findViewById(R.id.edt_phone);
        PhoneNumberUtils.formatNumber(caretakerNumber.getText().toString());
        saveBtn = findViewById(R.id.btn_save);
        saveCaretaker();
    }

    public void saveCaretaker() {
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = caretakerName.getText().toString().trim();
                String phone = caretakerNumber.getText().toString().trim();

                if (name.isEmpty()) {
                    caretakerName.setError(getString(R.string.add_caretaker_empty_name_message));
                    caretakerName.requestFocus();
                    return;
                }

                if (phone.isEmpty()) {
                    caretakerNumber.setError(getString(R.string.add_caretaker_empty_phone_message));
                    caretakerNumber.requestFocus();
                    return;
                }

                boolean isInserted = myDB.insertData(name, phone);

                if (isInserted) {
                    Toast.makeText(AddCaretakerActivity.this, R.string.add_caretaker_success_message, Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(AddCaretakerActivity.this, R.string.add_caretaker_error_message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
