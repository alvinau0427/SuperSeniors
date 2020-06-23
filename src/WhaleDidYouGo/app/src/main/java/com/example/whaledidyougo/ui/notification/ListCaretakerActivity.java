package com.example.whaledidyougo.ui.notification;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.whaledidyougo.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ListCaretakerActivity extends AppCompatActivity {
    SQLiteDatabase myDB;

    List<Caretaker> caretakerList;
    ListView listView;
    CaretakerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_caretaker);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.title_caretaker_show);

        listView = findViewById(R.id.layout_list_caretaker);
        caretakerList = new ArrayList<>();

        // SQLite database setup
        myDB = openOrCreateDatabase(DatabaseHelper.DATABASE_NAME, MODE_PRIVATE, null);
        loadCareTakersFromDatabase();
    }

    private void loadCareTakersFromDatabase() {
        @SuppressLint("Recycle")
        Cursor cursorCaretakers = myDB.rawQuery("SELECT * FROM caretaker_table", null);

        if(cursorCaretakers.moveToFirst()) {
            do {
                caretakerList.add(new Caretaker(cursorCaretakers.getInt(0),
                        cursorCaretakers.getString(1),
                        cursorCaretakers.getString(2)));
            } while (cursorCaretakers.moveToNext());

            adapter = new CaretakerAdapter(this, R.layout.view_list_caretaker, caretakerList, myDB);
            listView.setAdapter(adapter);
        } else {
            ListView listView = findViewById(R.id.layout_list_caretaker);
            TextView emptyTextView = findViewById(R.id.txt_empty);
            listView.setEmptyView(emptyTextView);
        }
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
