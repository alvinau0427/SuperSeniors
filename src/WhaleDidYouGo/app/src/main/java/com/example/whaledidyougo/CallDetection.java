package com.example.whaledidyougo;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.lang.reflect.Method;

import static com.example.whaledidyougo.MainActivity.FALL_DETECT_SWITCH;
import static com.example.whaledidyougo.MainActivity.REDIAL_REMINDER;
import static com.example.whaledidyougo.MainActivity.SHARED_PREFS;
import static com.example.whaledidyougo.MainActivity.SHOULD_REDIAL;

public class CallDetection extends AccessibilityService {
    public int count = 3;   //重撥次數  set count = 3 -> reset, count will --/time
    public String curr_calling_no;
    public CountDownTimer c;
    public MyBroadcastReceiver myBroadcastReceiver = new MyBroadcastReceiver();
    public IntentFilter intentFilter = new IntentFilter(Intent.ACTION_NEW_OUTGOING_CALL);
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        //acquireLock(this);

        Log.d("myaccess","after lock");
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            Log.d("myaccess","in window changed");
            AccessibilityNodeInfo info = event.getSource();
            //Log.i("DABUG",""+info);

            if (info != null && info.getText() != null) {
                String duration = info.getText().toString();
                String zeroSeconds = String.format("%02d:%02d", new Object[]{Integer.valueOf(0), Integer.valueOf(0)});
                String firstSecond = String.format("%02d:%02d", new Object[]{Integer.valueOf(0), Integer.valueOf(1)});
                Log.d("myaccess","after calculation - "+ zeroSeconds + " --- "+ firstSecond + " --- " + duration);
                if (zeroSeconds.equals(duration) || firstSecond.equals(duration)) {
                    //Toast.makeText(getApplicationContext(),"Call answered",Toast.LENGTH_SHORT).show();
                    // Your Code goes here
                    Log.d("DEBUG: ","screen change");
                    //Toast.makeText(getApplicationContext(),"screen change",Toast.LENGTH_SHORT).show();
                    //from CALL_STATE_OFFHOOK to active
                    c.cancel();
                    editor.putBoolean(REDIAL_REMINDER, false);
                    editor.commit();
                    count = 3;

                }
                info.recycle();
            }
        }
    }


    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Toast.makeText(this,"Service connected", Toast.LENGTH_SHORT).show();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 0;
        info.packageNames = null;
        setServiceInfo(info);
        registerReceiver(myBroadcastReceiver, intentFilter);
        sharedPreferences = getApplicationContext().getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

    }

    @Override
    public void onInterrupt() {

    }

    public class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, final Intent intent) {

            //如果是撥打電話
            if(intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {

                curr_calling_no = getResultData();
                Log.i("calldet: 你而家打緊電話比", "" + curr_calling_no);

                //ready
                //TODO:開始計時, 計20秒, 20秒後仍未接聽->自動cut線
                c = new CountDownTimer(20000, 1000) {

                    @Override
                    public void onTick(long millisUntilFinished) {
                        Log.i("倒數", "" + millisUntilFinished / 1000);

                    }

                    //TODO:如果夠鐘->cut線
                    @Override
                    public void onFinish() {
                        try {
                            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                            Class classTelephony = Class.forName(telephonyManager.getClass().getName());
                            Method methodGetITelephony = classTelephony.getDeclaredMethod("getITelephony");
                            methodGetITelephony.setAccessible(true);
                            Object telephonyInterface = methodGetITelephony.invoke(telephonyManager);
                            Class telephonyInterfaceClass = Class.forName(telephonyInterface.getClass().getName());
                            Method methodEndCall = telephonyInterfaceClass.getDeclaredMethod("endCall");
                            methodEndCall.invoke(telephonyInterface);

                            count--;
                            editor.putBoolean(REDIAL_REMINDER, true);
                            editor.commit();

                        } catch (Exception e) {
                        }
                    }
                }.start();
            }

        }
    }

}
