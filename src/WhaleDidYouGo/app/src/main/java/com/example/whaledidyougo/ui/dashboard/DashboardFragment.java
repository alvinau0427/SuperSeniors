package com.example.whaledidyougo.ui.dashboard;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.util.Calendar;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.whaledidyougo.R;

import static com.example.whaledidyougo.MainActivity.ALREADY_FALLDOWN;
import static com.example.whaledidyougo.MainActivity.SHARED_PREFS;
import static com.example.whaledidyougo.MainActivity.SHOULD_REDIAL;
import static com.example.whaledidyougo.MainActivity.REDIAL_REMINDER;

public class DashboardFragment extends Fragment {

    private ImageButton call_people1, call_people2, call_people3;
    private TextView people_1, people_2, people_3;
    public ImageButton goingOut, shopping, urgent, question;

    private int total_rec;

    public String date, time, activiry, smsTo;

    // SQLite
    private String[] contact_name, contact_phone, contact_image;

    final String DATABASE_NAME = "elderly_care.db";
    final String TABLE_NAME = "contact_book";

    public MyPhoneStateListener phoneStateListener;
    public TelephonyManager telephonyManager;

    public BroadcastReceiver myBroadcastReceiver;
    public IntentFilter intentFilter = new IntentFilter();

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public int count = 3;   //重撥次數  set count = 3 -> reset, count will --/time
    public String curr_calling_no;

    public CountDownTimer c;

/*    public BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("Hello", "BroadcastReceiver");

            //如果是撥打電話
            if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {

                curr_calling_no = getResultData();
                Log.i("call: 你而家打緊電話比", "" + curr_calling_no);
                count--;
                Log.i("call: count", "" + count);

            } else {
                Log.i("Hello", "not ACTION_NEW_OUTGOING_CALL");
            }
        }
    };
*/
    //find outgoing call number
    final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            //如果是撥打電話
            if (action.equals(Intent.ACTION_NEW_OUTGOING_CALL)) {

                curr_calling_no = getResultData();
                Log.i("call: 你而家打緊電話比", "" + curr_calling_no);
                count--;
                Log.i("call: count", "" + count);

            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LayoutInflater lf = getActivity().getLayoutInflater();
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);

        call_people1 = (ImageButton) root.findViewById(R.id.call_people1);
        call_people2 = (ImageButton) root.findViewById(R.id.call_people2);
        call_people3 = (ImageButton) root.findViewById(R.id.call_people3);

        people_1 = (TextView) root.findViewById(R.id.people1_name);
        people_2 = (TextView) root.findViewById(R.id.people2_name);
        people_3 = (TextView) root.findViewById(R.id.people3_name);

        goingOut = (ImageButton) root.findViewById(R.id.goingOut);
        shopping = (ImageButton) root.findViewById(R.id.shopping);
        urgent = (ImageButton) root.findViewById(R.id.urgent);
        question = (ImageButton) root.findViewById(R.id.question);

        question.setVisibility(View.INVISIBLE);
        //後加
        sharedPreferences = requireActivity().getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        //後加

        final SQLiteDatabase db = getActivity().openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE,null);
        String createTable = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(Name VARCHAR(255), " + "Phone VARCHAR(255), " + "Image BLOB);";
        db.execSQL(createTable);

        //find outgoing call number
        final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                //如果是撥打電話
                if (action.equals(Intent.ACTION_NEW_OUTGOING_CALL)) {

                    curr_calling_no = getResultData();
                    Log.i("call: 你而家打緊電話比", "" + curr_calling_no);
                    count--;
                    Log.i("call: count", "" + count);

                }
            }
        };

        //IntentFilter filter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
        getActivity().registerReceiver(broadcastReceiver, intentFilter);

        //find call state
        phoneStateListener = new MyPhoneStateListener();
        telephonyManager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        getContact();
        setContact();
        ImageButtonListener();
        root.invalidate();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        getContact();
        setContact();
    }

    @Override
    public void onPause() {
        super.onPause();

    }

    //
    public class MyPhoneStateListener extends PhoneStateListener {

        @Override
        public void onCallStateChanged(int state, String phoneNumber) {
            super.onCallStateChanged(state, phoneNumber);

            ////!!! call call 緊cut線會由CALL_STATE_OFFHOOK ->CALL_STATE_IDLE
            //test
            boolean reminder = sharedPreferences.getBoolean(REDIAL_REMINDER, true);
            Log.i("AAAAAA", "" + reminder);

            switch (state) {

                //TODO: 終止通話會入到here
                //電話狀態是閒置的
                case TelephonyManager.CALL_STATE_IDLE:

                    //原先case TelephonyManager.CALL_STATE_IDLE:
                    Log.i("冇打入冇打出","IDLE");

                    if(count > 0 && count < 3 && reminder) {
                        //重撥提示器
                         final AlertDialog alertDialog2 = new AlertDialog.Builder(getContext())
                                .setTitle(R.string.title_recall) //.setTitle("TEST" + curr_calling_no)
                                 .setMessage("")

                                //按下取消
                                .setNegativeButton(R.string.cancel_recall, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        count = 3;
                                        Log.i("已取消重撥", "OK");
                                        dialog.dismiss();
                                    }
                                }).create();

                        //重撥計時器 10s
                        final CountDownTimer countDownTimer = new CountDownTimer(10000, 1000) {

                            @Override
                            public void onTick(final long millisUntilFinished) {
                                Log.i("重撥計時器", "" + millisUntilFinished / 1000);
                                alertDialog2.setTitle(R.string.title_recall);   //TODO:edit here
                                //alertDialog2.setTitle("" + String.valueOf(millisUntilFinished/1000).toString() + " " + "hi" + " " + curr_calling_no);
                                alertDialog2.setMessage(String.valueOf(millisUntilFinished/1000) + " " + getString(R.string.sec_after_recall) + " " + curr_calling_no);
                            }

                            @Override
                            public void onFinish() {
                                Intent intent_1_1 = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + curr_calling_no));
                                startActivity(intent_1_1);
                                alertDialog2.dismiss();
                            }
                        };

                        //如果得知按下取消->取消計時器=取消重撥
                        alertDialog2.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                countDownTimer.cancel();
                                Log.i("計時器已停止", "OK");
                            }
                        });

                        countDownTimer.start();
                        alertDialog2.show();
                    }  else if(count <= 0 || !reminder) { //重撥2次後仍無人接聽
                        count = 3;
                    }

                    //TODO:Check 去電接通與否
                    break;

                //電話狀態是接起的  //打緊出去
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    //原先case TelephonyManager.CALL_STATE_OFFHOOK:
                    //Toast.makeText(call.this, "正接起電話…", Toast.LENGTH_LONG).show();
                    Log.i("TT","正接起電話…");
                    editor.putBoolean(REDIAL_REMINDER, true);
                    editor.commit();

                    break;

                //電話狀態是響起的
                //TODO:唔洗理, 因為冇人會打入黎
                case TelephonyManager.CALL_STATE_RINGING:
                    //Toast.makeText(call.this, phoneNumber + "正打電話來…", Toast.LENGTH_LONG).show();
                    Log.i("TT","正打電話來…");
                    break;

                default:
                    break;
            }
        };
    }

    //TODO: find current outgoing phone number and count 3 times, 重撥係MyPhoneStateListener做
    public class MyBroadcastReceiver extends BroadcastReceiver {
        private static final String TAG = "MyBroadcastReceiver";
        @Override
        public void onReceive(final Context context, Intent intent) {

            //如果是撥打電話
            if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {

                curr_calling_no = getResultData();
                Log.i("call: 你而家打緊電話比", "" + curr_calling_no);
                count--;
                Log.i("call: count", "" + count);

            }
        }
    }

    public void setContact() {
        // get default image from drawable folder
        Resources res = getResources();
        Bitmap def_bitmap = BitmapFactory.decodeResource(res, R.drawable.img_choose_photo);

        //get X and Y
        DisplayMetrics displayMetrics = new DisplayMetrics();
        int w = getActivity().getWindowManager().getDefaultDisplay().getWidth();
        int h = getActivity().getWindowManager().getDefaultDisplay().getHeight();
        int real_W = (3 * (w / 3)) / 4;

        switch (total_rec) {
            case 0:
                // No people1
                people_1.setText(R.string.contact_no_person);
                Bitmap bitmap_0_1 = Bitmap.createScaledBitmap(def_bitmap, real_W, w / 3, true);
                call_people1.setImageBitmap(bitmap_0_1);

                call_people1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(getActivity());
                        myAlertDialog.setTitle(R.string.contact_no_persons);

                        myAlertDialog.setPositiveButton(R.string.title_contact_add,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface arg0, int arg1) {
                                        Intent intent_2_3 = new Intent(getActivity(), AddContactActivity.class);
                                        startActivity(intent_2_3);
                                    }
                                });

                        myAlertDialog.setNegativeButton(R.string.title_contact_cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface arg0, int arg1) {
                                        // Not in use
                                    }
                                });

                        myAlertDialog.show();
                    }
                });

                // No people2
                people_2.setText(R.string.contact_no_person);
                Bitmap bitmap_0_2 = Bitmap.createScaledBitmap(def_bitmap, real_W, w / 3, true);
                call_people2.setImageBitmap(bitmap_0_2);

                call_people2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(getActivity());
                        myAlertDialog.setTitle(R.string.contact_no_person);

                        myAlertDialog.setPositiveButton(R.string.title_contact_add,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface arg0, int arg1) {
                                        Intent intent_2_3 = new Intent(getActivity(), AddContactActivity.class);
                                        startActivity(intent_2_3);
                                    }
                                });

                        myAlertDialog.setNegativeButton(R.string.title_contact_cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface arg0, int arg1) {
                                        // Not in use
                                    }
                                });

                        myAlertDialog.show();
                    }
                });

                // No people 3
                people_3.setText(R.string.contact_no_person);
                Bitmap bitmap_0_3 = Bitmap.createScaledBitmap(def_bitmap, real_W, w / 3, true);
                call_people3.setImageBitmap(bitmap_0_3);

                call_people3.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(getActivity());
                        myAlertDialog.setTitle(R.string.contact_no_persons);

                        myAlertDialog.setPositiveButton(R.string.title_contact_add,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface arg0, int arg1) {
                                        Intent intent_2_3 = new Intent(getActivity(), AddContactActivity.class);
                                        startActivity(intent_2_3);
                                    }
                                });

                        myAlertDialog.setNegativeButton(R.string.title_contact_cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface arg0, int arg1) {
                                        // Not in use
                                    }
                                });

                        myAlertDialog.show();
                    }
                });
                break;

            case 1:
                // People 1
                people_1.setText(contact_name[0]);
                byte[] decode = Base64.decode(contact_image[0], Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decode, 0, decode.length);
                bitmap = Bitmap.createScaledBitmap(bitmap, real_W, w / 3, true);
                call_people1.setImageBitmap(bitmap);

                call_people1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent_1_1 = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + contact_phone[0]));
                        startActivity(intent_1_1);
                    }
                });

                // People 2
                people_2.setText(R.string.contact_no_person);
                Bitmap bitmap_1_2 = Bitmap.createScaledBitmap(def_bitmap, real_W, w / 3, true);
                call_people2.setImageBitmap(bitmap_1_2);

                call_people2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(getActivity());
                        myAlertDialog.setTitle(R.string.contact_no_persons);

                        myAlertDialog.setPositiveButton(R.string.title_contact_add,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface arg0, int arg1) {
                                        Intent intent_2_3 = new Intent(getActivity(), AddContactActivity.class);
                                        startActivity(intent_2_3);
                                    }
                                });

                        myAlertDialog.setNegativeButton(R.string.title_contact_cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface arg0, int arg1) {
                                        // Not in use
                                    }
                                });

                        myAlertDialog.show();
                    }
                });

                // People 3
                people_3.setText(R.string.contact_no_person);
                Bitmap bitmap_1_3 = Bitmap.createScaledBitmap(def_bitmap, real_W, w / 3, true);
                call_people3.setImageBitmap(bitmap_1_3);

                call_people3.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(getActivity());
                        myAlertDialog.setTitle(R.string.contact_no_persons);

                        myAlertDialog.setPositiveButton(R.string.title_contact_add,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface arg0, int arg1) {
                                        Intent intent_2_3 = new Intent(getActivity(), AddContactActivity.class);
                                        startActivity(intent_2_3);
                                    }
                                });

                        myAlertDialog.setNegativeButton(R.string.title_contact_cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface arg0, int arg1) {
                                        // Not in use
                                    }
                                });

                        myAlertDialog.show();
                    }
                });
                break;

            case 2:
                // People 1
                people_1.setText(contact_name[0]);
                byte[] decode_2_1 = Base64.decode(contact_image[0], Base64.DEFAULT);
                Bitmap bitmap_2_1 = BitmapFactory.decodeByteArray(decode_2_1, 0, decode_2_1.length);
                bitmap_2_1 = Bitmap.createScaledBitmap(bitmap_2_1, real_W, w / 3, true);
                call_people1.setImageBitmap(bitmap_2_1);

                call_people1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent_2_1 = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + contact_phone[0]));
                        startActivity(intent_2_1);
                    }
                });

                // People 2
                people_2.setText(contact_name[1]);
                byte[] decode_2_2 = Base64.decode(contact_image[1], Base64.DEFAULT);
                Bitmap bitmap_2_2 = BitmapFactory.decodeByteArray(decode_2_2, 0, decode_2_2.length);
                bitmap_2_2 = Bitmap.createScaledBitmap(bitmap_2_2, real_W, w / 3, true);
                call_people2.setImageBitmap(bitmap_2_2);

                call_people2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent_2_1 = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + contact_phone[1]));
                        startActivity(intent_2_1);
                    }
                });

                // People 3
                people_3.setText(R.string.contact_no_person);
                Bitmap bitmap_2_3 = Bitmap.createScaledBitmap(def_bitmap, real_W, w / 3, true);
                call_people3.setImageBitmap(bitmap_2_3);

                call_people3.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(getActivity());
                        myAlertDialog.setTitle(R.string.contact_no_persons);

                        myAlertDialog.setPositiveButton(R.string.title_contact_add,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface arg0, int arg1) {
                                        Intent intent_2_3 = new Intent(getActivity(), AddContactActivity.class);
                                        startActivity(intent_2_3);
                                    }
                                });

                        myAlertDialog.setNegativeButton(R.string.title_contact_cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface arg0, int arg1) {
                                        // Not in use
                                    }
                                });

                        myAlertDialog.show();
                    }
                });
                break;

            case 3:
                // People 1
                people_1.setText(contact_name[0]);
                byte[] decode_3_1 = Base64.decode(contact_image[0], Base64.DEFAULT);
                Bitmap bitmap_3_1 = BitmapFactory.decodeByteArray(decode_3_1, 0, decode_3_1.length);
                bitmap_3_1 = Bitmap.createScaledBitmap(bitmap_3_1, real_W, w / 3, true);
                call_people1.setImageBitmap(bitmap_3_1);

                call_people1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent_3_1 = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + contact_phone[0]));
                        startActivity(intent_3_1);
                    }
                });

                // People 2
                people_2.setText(contact_name[1]);
                byte[] decode_3_2 = Base64.decode(contact_image[1], Base64.DEFAULT);
                Bitmap bitmap_3_2 = BitmapFactory.decodeByteArray(decode_3_2, 0, decode_3_2.length);
                bitmap_3_2 = Bitmap.createScaledBitmap(bitmap_3_2, real_W, w / 3, true);
                call_people2.setImageBitmap(bitmap_3_2);

                call_people2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent_3_2 = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + contact_phone[1]));
                        startActivity(intent_3_2);
                    }
                });

                // People 3
                people_3.setText(contact_name[2]);
                byte[] decode_3_3 = Base64.decode(contact_image[2], Base64.DEFAULT);
                Bitmap bitmap_3_3 = BitmapFactory.decodeByteArray(decode_3_3, 0, decode_3_3.length);
                bitmap_3_3 = Bitmap.createScaledBitmap(bitmap_3_3, real_W, w / 3, true);
                call_people3.setImageBitmap(bitmap_3_3);

                call_people3.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent_3_3 = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + contact_phone[2]));
                        startActivity(intent_3_3);
                    }
                });
                break;
        }
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

    public void ImageButtonListener() {
        goingOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(getActivity(),"hello",Toast.LENGTH_SHORT).show();
                SendQuickMessageTo();
                activity_goingOut();
                dateTimePicker();
                //activity_goingOut();
                //Toast.makeText(getActivity(),""+date + time,Toast.LENGTH_SHORT).show();
            }
        });

        shopping.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(getActivity(),"hello",Toast.LENGTH_SHORT).show();
                SendQuickMessageTo();
                activity_shopping();
                dateTimePicker();
            }
        });

        urgent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(getActivity(),"hello",Toast.LENGTH_SHORT).show();
                date="";
                time="";
                SendQuickMessageTo();
                activity_urgent();
            }
        });

        question.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //.makeText(getActivity(),"hello",Toast.LENGTH_SHORT).show();
                //SendQuickMessageTo();
            }
        });

    }

    public void dateTimePicker() {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONDAY);
        final int day = c.get(Calendar.DAY_OF_MONTH);
        final int hour = c.get(Calendar.HOUR_OF_DAY);
        final int mins = c.get(Calendar.MINUTE);

        new TimePickerDialog(getContext(), new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                time = String.valueOf(hour) + "點" + String.valueOf(mins) + "分";
                Log.i("DEBUG",""+time);
            }
        }, hour, mins, false).show();

        new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                date = String.valueOf(year) + "年" + String.valueOf(month) + "月" + String.valueOf(day) + "日";
                Log.i("DEBUG",""+date);
            }
        }, year, month, day).show();

    }

    public void activity_goingOut() {

        String[] activity_goingOut2 = {getString(R.string.hospital),
                getString(R.string.community_center), getString(R.string.nursing_home),
                getString(R.string.restaurant), getString(R.string.supermarket)
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.activity)
                .setItems(activity_goingOut2,new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i("DEBUG: ", "" + which);
                        ListView lw = ((AlertDialog)dialog).getListView();
                        String item = lw.getAdapter().getItem(which).toString();
                        Log.i("DEBUG: ", "" + item);
                        activiry = item;
                    }
                });
        builder.show();
    }

    public void activity_shopping() {

        String[] activity_shopping2 = {getString(R.string.daily_necessities),
                getString(R.string.medicines), getString(R.string.mask),
                getString(R.string.clothes), getString(R.string.food)
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.activity)
                .setItems(activity_shopping2,new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i("DEBUG: ", "" + which);
                        ListView lw = ((AlertDialog)dialog).getListView();
                        String item = lw.getAdapter().getItem(which).toString();
                        Log.i("DEBUG: ", "" + item);
                        activiry = item;
                    }
                });
        builder.show();
    }

    public void activity_urgent() {

        String[] activity_urgent2 = {getString(R.string.lost),
                getString(R.string.property), getString(R.string.injured)
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.activity)
                .setItems(activity_urgent2,new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i("DEBUG: ", "" + which);
                        ListView lw = ((AlertDialog)dialog).getListView();
                        String item = lw.getAdapter().getItem(which).toString();
                        Log.i("DEBUG: ", "" + item);
                        activiry = item;
                    }
                });
        builder.show();
    }

    public void SendQuickMessageTo() {

        //Log.i("Debug","" + date + ", " + time + ", " + activiry);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.send_message_to)
                .setItems(contact_name,new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i("DEBUG: ", "" + which);
                        ListView lw = ((AlertDialog)dialog).getListView();
                        String item = lw.getAdapter().getItem(which).toString();
                        Log.i("DEBUG: ", "" + item);
                        //activiry = item;
                        smsTo = contact_phone[which];
                        Log.i("DEBUG: ", "" + smsTo);

                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle(R.string.are_you_sure_send_message_to);
                        builder.setMessage(date + time + activiry);
                        builder.setNegativeButton(R.string.title_contact_cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                        });
                        builder.setPositiveButton(R.string.send, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        /*
                                        Uri uri = Uri.parse("smsto:"+smsTo);
                                        Intent it = new Intent(Intent.ACTION_SENDTO, uri);
                                        it.putExtra("sms_body", date + time + activiry);
                                        startActivity(it);
                                        */
                                        SmsManager smsManager = SmsManager.getDefault();

                                        smsManager.sendTextMessage(smsTo, null, date + time + activiry, null, null);
                                        Toast.makeText(getContext(), R.string.notification_sms_success, Toast.LENGTH_LONG).show();
                                    }
                        });
                        builder.show();
                    }
                });
        builder.show();

    }

}
