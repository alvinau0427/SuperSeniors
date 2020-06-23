package com.example.whaledidyougo.ui.notification;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.whaledidyougo.R;

import java.util.List;

public class CaretakerAdapter extends ArrayAdapter<Caretaker> {
    private Context cContext;
    private int resourceL;
    private List<Caretaker> caretakerList;

    private SQLiteDatabase myDB;

    public CaretakerAdapter(@NonNull Context cContext, int resourceL, @NonNull List<Caretaker> caretakerList, SQLiteDatabase myDB) {
        super(cContext, resourceL, caretakerList);
        this.cContext = cContext;
        this.resourceL = resourceL;
        this.caretakerList = caretakerList;
        this.myDB = myDB;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // return super.getView(position, convertView, parent);
        LayoutInflater inflater = LayoutInflater.from(cContext);

        @SuppressLint("ViewHolder")
        View view = inflater.inflate(resourceL, null);

        TextView name = view.findViewById(R.id.txt_name);
        TextView phone = view.findViewById(R.id.txt_phone);

        final Caretaker caretaker = caretakerList.get(position);

        name.setText(caretaker.getName());
        phone.setText(caretaker.getPhone());

        Button deleteBtn = view.findViewById(R.id.btn_delete);

        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteCaretaker(caretaker);
            }
        });
        return view;
    }

    private void deleteCaretaker(final Caretaker caretaker) {
        AlertDialog.Builder builder = new AlertDialog.Builder(cContext);
        builder.setTitle(R.string.delete_caretaker_confirm_message);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String sql = "DELETE FROM caretaker_table WHERE id = ?";
                myDB.execSQL(sql, new Integer[] {caretaker.getId()});
                reloadCaretaker();
            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Not in use
            }
        });

        final AlertDialog alertDialog = builder.create();

        // Alert Dialog UI Setting
        /*
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onShow(DialogInterface dialog) {
                Button negButton = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE);
                // alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.RED);
                // alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
                // alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(Color.rgb(62, 194, 134));
                // alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 20, 0);
                negButton.setLayoutParams(params);
            }
        });
        */
        alertDialog.show();
    }

    private void reloadCaretaker() {
        Cursor cursorCaretakers = myDB.rawQuery("SELECT * FROM caretaker_table", null);
        if (cursorCaretakers.moveToFirst()) {
            caretakerList.clear();
            do {
                caretakerList.add(new Caretaker(
                        cursorCaretakers.getInt(0),
                        cursorCaretakers.getString(1),
                        cursorCaretakers.getString(2)
                ));
            } while (cursorCaretakers.moveToNext());
            notifyDataSetChanged();
        }
    }
}
