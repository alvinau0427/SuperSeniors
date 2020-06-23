package com.example.whaledidyougo.ui.schedule;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.whaledidyougo.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class UpdateEvent extends AppCompatActivity {

    private Button select_startdate;
    private Button select_starttime;
    private Button select_enddate;
    private Button select_endtime;
    private Button update_event;
    private TimePickerDialog picker;
    private EditText title;
    private TextView selected_startdate;
    private TextView selected_starttime;
    private TextView selected_enddate;
    private TextView selected_endtime;
    private EditText des;
    private int starthour;
    private int startminute;
    private String starttimeforshow;
    private int endhour;
    private int endminute;
    private int targetEvent;
    private String endtimeforshow;
    private int startDay;
    private int startMonth;
    private int startYear;
    private int endDay;
    private int endMonth;
    private int endYear;
    private String startformat;
    private String endformat;

    private DatePickerDialog.OnDateSetListener onStartDateSetListener;
    private DatePickerDialog.OnDateSetListener onEndDateSetListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_event);

        title = (EditText)findViewById(R.id.title);
        selected_startdate = (TextView)findViewById(R.id.selected_startdate);
        selected_starttime = (TextView)findViewById(R.id.selected_starttime);
        selected_enddate = (TextView)findViewById(R.id.selected_enddate);
        selected_endtime = (TextView)findViewById(R.id.selected_endtime);
        des = (EditText)findViewById(R.id.des);
        select_startdate = (Button)findViewById(R.id.select_startdate);
        select_starttime = (Button)findViewById(R.id.select_starttime);
        select_enddate = (Button)findViewById(R.id.select_enddate);
        select_endtime = (Button)findViewById(R.id.select_endtime);
        update_event = (Button)findViewById(R.id.update_event);

        Intent getUpdate = getIntent();
        targetEvent = getUpdate.getIntExtra("targetEvent", 0);
        title.setText(getUpdate.getStringExtra("title"));
        SimpleDateFormat simpleyearformat = new SimpleDateFormat("yyyy");
        startYear = Integer.valueOf(simpleyearformat.format(getUpdate.getLongExtra("begin", 0)));
        endYear = Integer.valueOf(simpleyearformat.format(getUpdate.getLongExtra("end", 0)));
        SimpleDateFormat simplemonthformat = new SimpleDateFormat("MM");
        startMonth = Integer.valueOf(simplemonthformat.format(getUpdate.getLongExtra("begin", 0))) - 1;
        endMonth = Integer.valueOf(simplemonthformat.format(getUpdate.getLongExtra("end", 0))) - 1;
        SimpleDateFormat simpledayformat = new SimpleDateFormat("dd");
        startDay = Integer.valueOf(simpledayformat.format(getUpdate.getLongExtra("begin", 0)));
        endDay = Integer.valueOf(simpledayformat.format(getUpdate.getLongExtra("end", 0)));
        SimpleDateFormat simplehourformat = new SimpleDateFormat("HH");
        starthour = Integer.valueOf(simplehourformat.format(getUpdate.getLongExtra("begin", 0)));
        endhour = Integer.valueOf(simplehourformat.format(getUpdate.getLongExtra("end", 0)));
        SimpleDateFormat simpleminuteformat = new SimpleDateFormat("mm");
        startminute = Integer.valueOf(simpleminuteformat.format(getUpdate.getLongExtra("begin", 0)));
        endminute = Integer.valueOf(simpleminuteformat.format(getUpdate.getLongExtra("end", 0)));
        SimpleDateFormat simpletimeforshowformat = new SimpleDateFormat("a h:mm");
        starttimeforshow = simpletimeforshowformat.format(getUpdate.getLongExtra("begin", 0));
        endtimeforshow = simpletimeforshowformat.format(getUpdate.getLongExtra("end", 0));

        selected_startdate.setText(getString(R.string.event_selected_startdate) + " " + startYear + "/" + (startMonth + 1) + "/" + startDay);
        selected_starttime.setText(getString(R.string.event_selected_starttime) + " " + starttimeforshow);
        selected_enddate.setText(getString(R.string.event_selected_enddate) + " " + endYear + "/" + (endMonth + 1) + "/" + endDay);
        selected_endtime.setText(getString(R.string.event_selected_endtime) + " " + endtimeforshow);
        des.setText(getUpdate.getStringExtra("des"));

        final Calendar cldr = Calendar.getInstance();
        final int hour = cldr.get(Calendar.HOUR_OF_DAY);
        final int minutes = cldr.get(Calendar.MINUTE);

        select_startdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog dialog = new DatePickerDialog(UpdateEvent.this,
                        android.R.style.Theme_Holo_Light_Dialog_MinWidth, onStartDateSetListener,
                        startYear, startMonth, startDay);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.show();
            }
        });
        onStartDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                startYear = year;
                startMonth = month;
                startDay = dayOfMonth;
                selected_startdate.setText(getString(R.string.event_selected_startdate) + " " + startYear + "/" + (startMonth + 1) + "/" + startDay);
            }
        };

        select_starttime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                picker = new TimePickerDialog(UpdateEvent.this,
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay,
                                                  int minute) {
                                starthour = hourOfDay;
                                startminute = minute;
                                if (hourOfDay == 0) {
                                    hourOfDay += 12;
                                    startformat = "AM";
                                }
                                else if (hourOfDay == 12) {
                                    startformat = "PM";
                                }
                                else if (hourOfDay > 12) {
                                    hourOfDay -= 12;
                                    startformat = "PM";
                                }
                                else {
                                    startformat = "AM";
                                }
                                if(startformat == "AM") {
                                    selected_starttime.setText(getString(R.string.event_selected_startdate) + " " +
                                            getString(R.string.event_time_am) + " " + hourOfDay + ":" + String.format(Locale.getDefault(), "%02d", minute));
                                }else if(startformat == "PM"){
                                    selected_starttime.setText(getString(R.string.event_selected_startdate) + " " +
                                            getString(R.string.event_time_pm) + " " + hourOfDay + ":" + String.format(Locale.getDefault(), "%02d", minute));
                                }
                            }
                        }, hour, minutes, false);
                picker.show();
            }
        });

        select_enddate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog dialog = new DatePickerDialog(UpdateEvent.this,
                        android.R.style.Theme_Holo_Light_Dialog_MinWidth, onEndDateSetListener,
                        endYear, endMonth, endDay);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.show();
            }
        });
        onEndDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                endYear = year;
                endMonth = month;
                endDay = dayOfMonth;
                selected_enddate.setText(getString(R.string.event_selected_enddate)+ " " + endYear + "/" + (endMonth + 1) + "/" + endDay);
            }
        };

        select_endtime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                picker = new TimePickerDialog(UpdateEvent.this,
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay,
                                                  int minute) {
                                endhour = hourOfDay;
                                endminute = minute;
                                if (hourOfDay == 0) {
                                    hourOfDay += 12;
                                    endformat = "AM";
                                }
                                else if (hourOfDay == 12) {
                                    endformat = "PM";
                                }
                                else if (hourOfDay > 12) {
                                    hourOfDay -= 12;
                                    endformat = "PM";
                                }
                                else {
                                    endformat = "AM";
                                }
                                if(endformat == "AM") {
                                    selected_endtime.setText(getString(R.string.event_selected_enddate) + " " +
                                            getString(R.string.event_time_am) + " " + hourOfDay + ":" + String.format(Locale.getDefault(), "%02d", minute));
                                }else if(endformat == "PM"){
                                    selected_endtime.setText(getString(R.string.event_selected_enddate) + " " +
                                            getString(R.string.event_time_pm) + " " + hourOfDay + ":" + String.format(Locale.getDefault(), "%02d", minute));
                                }
                            }
                        }, hour, minutes, false);
                picker.show();
            }
        });

        update_event.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (startYear > endYear || startMonth > endMonth || startDay > endDay || (startDay == endDay && starthour > endhour) ||
                        (startDay == endDay && starthour == endhour && startminute > endminute)) {
                    timeerror();
                } else {
                    long eventId = Long.valueOf(targetEvent);
                    String updatedTitle = ((EditText) findViewById(R.id.title)).getText().toString();
                    long startMillis = 0;
                    long endMillis = 0;
                    Calendar startTime = Calendar.getInstance();
                    startTime.set(startYear, startMonth, startDay, starthour, startminute);
                    startMillis = startTime.getTimeInMillis();
                    Calendar endTime = Calendar.getInstance();
                    endTime.set(endYear, endMonth, endDay, endhour, endminute);
                    endMillis = endTime.getTimeInMillis();
                    String updatedDes = ((EditText) findViewById(R.id.des)).getText().toString();
                    ContentResolver cr = getContentResolver();
                    ContentValues values = new ContentValues();
                    values.put(CalendarContract.Events.TITLE, updatedTitle);
                    values.put(CalendarContract.Events.DTSTART, startMillis);
                    values.put(CalendarContract.Events.DTEND, endMillis);
                    values.put(CalendarContract.Events.DESCRIPTION, updatedDes);
                    int permissionCheck = ContextCompat.checkSelfPermission(UpdateEvent.this,
                            Manifest.permission.WRITE_CALENDAR);
                    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                        Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId);
                        cr.update(uri, values, null, null);
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(UpdateEvent.this);
                    builder.setMessage(R.string.event_update_success)
                            .setPositiveButton(R.string.event_ok_button, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    finish();
                                }
                            });
                    builder.show();

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

    private void timeerror(){
        Toast toast = Toast.makeText(UpdateEvent.this, R.string.datetime_error, Toast.LENGTH_LONG);
        toast.show();
    }

}
