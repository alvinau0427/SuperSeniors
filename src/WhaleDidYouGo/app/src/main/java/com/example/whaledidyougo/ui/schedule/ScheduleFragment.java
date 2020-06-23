package com.example.whaledidyougo.ui.schedule;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.example.whaledidyougo.R;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.example.whaledidyougo.ui.notification.DatabaseHelper.DATABASE_NAME;

public class ScheduleFragment extends Fragment {

    private Calendar beginTime = Calendar.getInstance();
    private Calendar endTime = Calendar.getInstance();
    private int targetDay = 0;
    private int targetMonth = 0;
    private int targetYear = 0;
    private String targetCalendar;
    private String targetCalendarName;
    private List<Integer> calendarIdList = new ArrayList<>();
    private List<String> calendarNameList = new ArrayList<>();
    private String[] calendarSelectionArgs;
    private String[] eventSelectionArgs;
    private TextView calendar_text;
    private Button option_button;
    private DatePicker datePicker;
    private ListView event_listview;
    private boolean picked;
    private int[] eventIDArray;
    private String[] eventTitleArray;
    private Long[] eventBeginArray;
    private Long[] eventEndArray;
    private String[] eventDesArray;
    private int listPosition;

    final String DATABASE_NAME = "elderly_care.db";
    final String TABLE_NAME = "contact_book";
    private String[] contact_name, contact_phone, contact_image;
    private int total_rec;

    public ScrollView sv;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_schedule, container, false);

        calendar_text = root.findViewById(R.id.calendar_text);
        datePicker = (DatePicker)root.findViewById(R.id.datepicker);
        event_listview = (ListView) root.findViewById(R.id.event_list);
        sv = (ScrollView)root.findViewById(R.id.sv);
        option_button = (Button)root.findViewById(R.id.option_button);

        getContact();

        String[] EVENT_PROJECTION = new String[]{
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.OWNER_ACCOUNT,
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
        };

        int PROJECTION_ID_INDEX = 0;
        int PROJECTION_ACCOUNT_NAME_INDEX = 1;
        int PROJECTION_DISPLAY_NAME_INDEX = 2;
        int PROJECTION_OWNER_ACCOUNT_INDEX = 3;
        int PROJECTION_CALENDAR_ACCESS_LEVEL = 4;

        String targetAccount = requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE).getString("Email", "Null");
        Cursor cur;
        ContentResolver cr = getActivity().getContentResolver();
        Uri uri = CalendarContract.Calendars.CONTENT_URI;
        String selection = "((" + CalendarContract.Calendars.ACCOUNT_NAME + " = ?) AND ("
                + CalendarContract.Calendars.ACCOUNT_TYPE + " = ?) AND ("
                + CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + " = ?))";
        calendarSelectionArgs = new String[]{targetAccount,
                "com.google",
                Integer.toString(CalendarContract.Calendars.CAL_ACCESS_OWNER)};

        int permissionCheck = ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.READ_CALENDAR);

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            cur = cr.query(uri, EVENT_PROJECTION, selection, calendarSelectionArgs, null);
            if (cur != null) {
                while (cur.moveToNext()) {
                    long calendarId = 0;
                    String accountName = null;
                    String displayName = null;
                    String ownerAccount = null;
                    int accessLevel = 0;
                    calendarId = cur.getLong(PROJECTION_ID_INDEX);
                    accountName = cur.getString(PROJECTION_ACCOUNT_NAME_INDEX);
                    displayName = cur.getString(PROJECTION_DISPLAY_NAME_INDEX);
                    ownerAccount = cur.getString(PROJECTION_OWNER_ACCOUNT_INDEX);
                    accessLevel = cur.getInt(PROJECTION_CALENDAR_ACCESS_LEVEL);
                    Log.i("query_calendar", String.format("calendarId=%s", calendarId));
                    Log.i("query_calendar", String.format("accountName=%s", accountName));
                    Log.i("query_calendar", String.format("displayName=%s", displayName));
                    Log.i("query_calendar", String.format("ownerAccount=%s", ownerAccount));
                    Log.i("query_calendar", String.format("accessLevel=%s", accessLevel));
                    calendarNameList.add(displayName);
                    calendarIdList.add((int) calendarId);
                }
                cur.close();
            }
            else {
                Toast toast = Toast.makeText(getActivity(), R.string.cannot_find_calendar, Toast.LENGTH_LONG);
                toast.show();
            }
        }
        else {
            Toast toast = Toast.makeText(getActivity(), R.string.no_permission, Toast.LENGTH_LONG);
            toast.show();
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        datePicker.init(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker datePicker, int year, int month, int dayOfMonth) {
                picked = true;
                setevent();
            }
        });

        option_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                androidx.appcompat.app.AlertDialog.Builder adb = new androidx.appcompat.app.AlertDialog.Builder(getContext());
                String items[] = {getString(R.string.change_calendar), getString(R.string.title_insert), getString(R.string.refresh)};
                adb.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(which == 0) {
                                if (calendarIdList.size() != 0) {
                                    androidx.appcompat.app.AlertDialog.Builder adb = new androidx.appcompat.app.AlertDialog.Builder(getContext());
                                    CharSequence items[] = calendarNameList.toArray(new CharSequence[calendarNameList.size()]);
                                    adb.setItems(items, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            targetCalendar = String.format("%s", calendarIdList.get(which));
                                            targetCalendarName = calendarNameList.get(which);
                                            dialog.dismiss();
                                            setevent();
                                        }
                                    });
                                    adb.setNegativeButton(R.string.event_cancel_button, null);
                                    adb.show();
                                }
                            }else if(which == 1){
                                Intent intent_insert = new Intent(getActivity(), InsertEvent.class);
                                intent_insert.putExtra("targetDay", targetDay);
                                intent_insert.putExtra("targetMonth", targetMonth);
                                intent_insert.putExtra("targetYear", targetYear);
                                intent_insert.putExtra("targetCalendar", targetCalendar);
                                startActivity(intent_insert);
                            }else if (which == 2){
                                setevent();
                            }else{}
                        }
                    });
                    adb.setNegativeButton(R.string.event_cancel_button, null);
                    adb.show();
            }
        });

        setevent();

        return root;

    }

    private void setevent(){
        if(picked == true){
            targetYear = datePicker.getYear();
            targetMonth = datePicker.getMonth();
            targetDay = datePicker.getDayOfMonth();
        }else if(targetDay == 0 && targetMonth == 0 && targetYear == 0) {
            targetDay = beginTime.get(Calendar.DAY_OF_MONTH);
            targetMonth = beginTime.get(Calendar.MONTH);
            targetYear = beginTime.get(Calendar.YEAR);
        }

        String[] INSTANCE_PROJECTION = new String[]{
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.END,
                CalendarContract.Instances.DESCRIPTION
        };


        int PROJECTION_ID_INDEX = 0;
        int PROJECTION_BEGIN_INDEX = 1;
        int PROJECTION_TITLE_INDEX = 2;
        int PROJECTION_END_INDEX = 3;
        int PROJECTION_DESCRIPTION_INDEX = 4;

        long startMillis = 0;
        long endMillis = 0;
        beginTime.set(targetYear, targetMonth, targetDay,
                beginTime.get(0), beginTime.get(0));
        startMillis = beginTime.getTimeInMillis();
        endTime.set(targetYear, targetMonth, targetDay + 1,
                endTime.get(0), endTime.get(0));
        endMillis = endTime.getTimeInMillis();

        Cursor cur = null;
        ContentResolver cr = getActivity().getContentResolver();
        Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        String selection = CalendarContract.Events.CALENDAR_ID + " = ?";
        if(targetCalendar == null){
            targetCalendar = String.format("%s", calendarIdList.get(0));
            targetCalendarName = calendarNameList.get(0);
        }
        eventSelectionArgs = new String[]{targetCalendar};
        ContentUris.appendId(builder, startMillis);
        ContentUris.appendId(builder, endMillis);
        int permissionCheck = ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.READ_CALENDAR);
        final List<String> eventList = new ArrayList<>();
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            cur = cr.query(builder.build(),
                    INSTANCE_PROJECTION,
                    selection,
                    eventSelectionArgs,
                    CalendarContract.Events.DTSTART + " ASC");
            if (cur != null) {
                int eventcount = 0;
                eventIDArray = new int[cur.getCount() + 1];
                eventTitleArray = new String[cur.getCount() + 1];
                eventBeginArray = new Long[cur.getCount() + 1];
                eventEndArray = new Long[cur.getCount() + 1];
                eventDesArray = new String[cur.getCount() + 1];
                while (cur.moveToNext()) {
                    long eventID = 0;
                    String beginVal = null;
                    String title = null;
                    String endVal = null;
                    String description = null;
                    eventID = cur.getLong(PROJECTION_ID_INDEX);
                    beginVal = DateFormat.getDateTimeInstance().format(cur.getLong(PROJECTION_BEGIN_INDEX));
                    title = cur.getString(PROJECTION_TITLE_INDEX);
                    endVal = DateFormat.getDateTimeInstance().format(cur.getLong(PROJECTION_END_INDEX));
                    description = cur.getString(PROJECTION_DESCRIPTION_INDEX);
                    eventIDArray[eventcount] = (int)eventID;
                    eventTitleArray[eventcount] = title;
                    eventBeginArray[eventcount] = cur.getLong(PROJECTION_BEGIN_INDEX);
                    eventEndArray[eventcount] = cur.getLong(PROJECTION_END_INDEX);
                    eventDesArray[eventcount] = description;
                    eventcount = eventcount + 1;
                    eventList.add(getString(R.string.event_title) + " " + title + "\n" + getString(R.string.event_starttime)
                            + " " + beginVal + "\n" + getString(R.string.event_endtime) + " " + endVal);
                }
                cur.close();
            }

            calendar_text.setText(getString(R.string.title_schedule) + ": " + targetCalendarName);

            //try
            int totalHeight = 0;
            //try
            ArrayAdapter<String> arrayAdapter1 = new ArrayAdapter<String>(
                    getActivity(),
                    android.R.layout.simple_list_item_1,
                    eventList);
            event_listview.setAdapter(arrayAdapter1);

            //try
            for(int i = 0; i < arrayAdapter1.getCount(); i++) {
                View listItem = arrayAdapter1.getView(i, null, event_listview);
                listItem.measure(0,0);
                totalHeight += listItem.getMeasuredHeight();
            }

            ViewGroup.LayoutParams params = event_listview.getLayoutParams();
            params.height = totalHeight + (event_listview.getDividerHeight() * (arrayAdapter1.getCount() -1));
            event_listview.setLayoutParams(params);
            //

            event_listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    listPosition = position;
                    final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
                    builder.setMessage(getString(R.string.event_des) + "\n" + eventDesArray[listPosition])
                            .setPositiveButton(R.string.event_delete_button, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    final android.app.AlertDialog.Builder delete_builder = new android.app.AlertDialog.Builder(getContext());
                                    delete_builder.setMessage(R.string.delete_question).setPositiveButton(R.string.event_yes_button, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            long eventId = new Long(eventIDArray[listPosition]);
                                            ContentResolver cr = getActivity().getContentResolver();
                                            int permissionCheck = ContextCompat.checkSelfPermission(getContext(),
                                                    Manifest.permission.WRITE_CALENDAR);
                                            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                                Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId);
                                                cr.delete(uri, null, null);
                                            }

                                            AlertDialog.Builder delete_success_builder = new AlertDialog.Builder(getContext());
                                            delete_success_builder.setMessage(R.string.event_delete_success)
                                                    .setPositiveButton(R.string.event_ok_button, new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int id) {
                                                        }
                                                    });
                                            delete_success_builder.show();
                                            setevent();
                                        }
                                    });
                                    delete_builder.setNeutralButton(R.string.event_No_button, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                        }
                                    });
                                    delete_builder.show();
                                }
                            });
                    builder.setNegativeButton(R.string.event_update_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent eventlisttoupdate = new Intent(getActivity(), UpdateEvent.class);
                            eventlisttoupdate.putExtra("targetEvent", eventIDArray[listPosition]);
                            eventlisttoupdate.putExtra("title", eventTitleArray[listPosition]);
                            eventlisttoupdate.putExtra("begin", eventBeginArray[listPosition]);
                            eventlisttoupdate.putExtra("end", eventEndArray[listPosition]);
                            eventlisttoupdate.putExtra("des", eventDesArray[listPosition]);
                            startActivity(eventlisttoupdate);
                        }
                    });
                    builder.setNeutralButton(R.string.event_done_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (!eventTitleArray[listPosition].contains("(done)")) {
                                final android.app.AlertDialog.Builder done_builder = new android.app.AlertDialog.Builder(getContext());
                                done_builder.setMessage(R.string.done_question).setPositiveButton(R.string.event_yes_button, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (contact_phone != null) {
                                            long eventId = Long.valueOf(eventIDArray[listPosition]);
                                            String doneTitle = eventTitleArray[listPosition] + "(done)";
                                            ContentResolver cr = getActivity().getContentResolver();
                                            ContentValues values = new ContentValues();
                                            values.put(CalendarContract.Events.TITLE, doneTitle);
                                            int permissionCheck = ContextCompat.checkSelfPermission(getContext(),
                                                    Manifest.permission.WRITE_CALENDAR);
                                            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                                Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId);
                                                cr.update(uri, values, null, null);
                                            }

                                            String starttimeformsn = DateFormat.getDateTimeInstance().format(eventBeginArray[listPosition]);
                                            String endtimeformsn = DateFormat.getDateTimeInstance().format(eventEndArray[listPosition]);
                                            for (int i = 0; i < contact_phone.length; i++) {
                                                String message = getString(R.string.done_sms) + ":\n" + eventTitleArray[listPosition];
                                                String smsTo = contact_phone[i];
                                                SmsManager smsManager = SmsManager.getDefault();
                                                smsManager.sendTextMessage(smsTo, null, message, null, null);
                                            }

                                            AlertDialog.Builder welldone_builder = new AlertDialog.Builder(getContext());
                                            welldone_builder.setMessage(R.string.done_message)
                                                    .setPositiveButton(R.string.event_ok_button, new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int id) {
                                                        }
                                                    });
                                            welldone_builder.show();
                                        } else {
                                            Toast toast = Toast.makeText(getActivity(), R.string.no_contact_person, Toast.LENGTH_LONG);
                                            toast.show();
                                        }
                                    }
                                });
                                done_builder.setNeutralButton(R.string.event_No_button, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                });
                                done_builder.show();
                            } else {
                                Toast toast = Toast.makeText(getActivity(), R.string.event_already_done, Toast.LENGTH_LONG);
                                toast.show();
                            }
                        }
                    });

                    builder.show();
                }
            });

            if (eventList.size() == 0) {
                Toast toast = Toast.makeText(getActivity(), R.string.no_event, Toast.LENGTH_LONG);
                toast.show();
            }
        }
        else {
            Toast toast = Toast.makeText(getActivity(), R.string.no_permission, Toast.LENGTH_LONG);
            toast.show();
        }

        picked = false;
    }

    public void getContact() {
        final SQLiteDatabase db = getActivity().openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null);
        String createTable = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(Name VARCHAR(255), " + "Phone VARCHAR(255), " + "Image BLOB);";
        db.execSQL(createTable);

        //get record
        String sql = "SELECT * FROM " + TABLE_NAME;
        Cursor cursor = db.rawQuery(sql, null);
        int k = 0;
        if (cursor.getCount() > 0) {
            total_rec = cursor.getCount();
            contact_name = new String[cursor.getCount()];
            contact_phone = new String[cursor.getCount()];
            contact_image = new String[cursor.getCount()];
            cursor.moveToFirst();
            Log.i("THERE ARE", "" + cursor.getCount() + "ITEM");
            do {
                //temp_name = cursor.getString(0);
                contact_name[k] = cursor.getString(0);
                contact_phone[k] = cursor.getString(1);
                contact_image[k] = cursor.getString(2);
                //Log.i("RECORD-1 ", "" + contact_name[k]);
                //Log.i("RECORD-2 ", "" + contact_phone[k]);
                //Log.i("RECORD-3 ", "" + contact_image[k]);
                k++;
            } while (cursor.moveToNext());
        } else if (cursor.getCount() <= 0) {
            total_rec = 0;
        }
    }


}