package com.nordman.big.smsparking;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.util.Log;

import java.util.Date;

/**
 * Created by s_vershinin on 15.01.2016.
 */
public class SmsManager {
    public static final long MILLIS_IN_HOUR = 3600000;
    private static final long MILLIS_IN_MINUTE = 60000;

    Context context;
    Date sendDate;
    Date startParkingDate;

    String sms = null;
    String regNum = "________";
    ParkZone currentZone = null;
    String hours = "1";

    public SmsManager(Context context) {
        this.context = context;
    }

    public void updateSms() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        sms = "p66*";
        regNum = prefs.getString("regnum", "________");

        if (currentZone==null) {
            sms += "___*";
        } else {
            sms += currentZone.getZoneNumber().toString() + "*";
        }
        sms += regNum + "*" + hours;
    }

    public String hourDesc(){
        if (hours.equals("1")) return hours + " час";
        else return hours + " часа";
    }

    public boolean smsComplete(){
        return !regNum.equals("________") & currentZone != null;
    }

    public int getProgress(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        long lh = Long.parseLong(prefs.getString("LastHours", "1"));
        long lpt = Long.parseLong(prefs.getString("LastParkTime", "0"));
        long current = (new Date()).getTime();
        return (int) (100 * (lh*MILLIS_IN_HOUR - (current - lpt) )/(lh*MILLIS_IN_HOUR));
    }

    public int getMinutes(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        long lh = Long.parseLong(prefs.getString("LastHours", "1"));
        long lpt = Long.parseLong(prefs.getString("LastParkTime", "0"));
        long current = (new Date()).getTime();
        return (int) ((lh*MILLIS_IN_HOUR - (current - lpt))/MILLIS_IN_MINUTE);
    }

    public boolean parkingActive(){
        return (getProgress()>0);
    }

    /// отправлена ли смс указанному адресату
    public boolean IsSent(String toWhom){
        boolean result = false;

        ContentResolver cr = context.getContentResolver();

        Cursor c = cr.query(Telephony.Sms.Sent.CONTENT_URI, // Official CONTENT_URI from docs
                new String[]{Telephony.Sms.Sent.DATE, Telephony.Sms.Sent.ADDRESS, Telephony.Sms.Sent.BODY}, // Select body text
                Telephony.Sms.Sent.ADDRESS + " = '" + toWhom + "'",
                null,
                Telephony.Sms.Sent.DEFAULT_SORT_ORDER); // Default sort order

        assert c != null;
        int totalSMS = c.getCount();

        if (c.moveToFirst()) {
            for (int i = 0; i < totalSMS; i++) {
                if (Long.parseLong(c.getString(0)) > sendDate.getTime()) {
                    result=true;
                    break;
                }
                c.moveToNext();
            }
        }

        c.close();

        return result;
    }

    public String GetIncomingSms(String fromWhom){
        String result = null;

        ContentResolver cr = context.getContentResolver();

        Cursor c = cr.query(Telephony.Sms.Inbox.CONTENT_URI, // Official CONTENT_URI from docs
                new String[] { Telephony.Sms.Inbox.DATE, Telephony.Sms.Inbox.ADDRESS, Telephony.Sms.Inbox.BODY }, // Select body text
                Telephony.Sms.Inbox.ADDRESS + " = '" + fromWhom + "'",
                null,
                Telephony.Sms.Inbox.DEFAULT_SORT_ORDER); // Default sort order

        assert c != null;
        int totalSMS = c.getCount();

        if (c.moveToFirst()) {
            for (int i = 0; i < totalSMS; i++) {
                if (Long.parseLong(c.getString(0)) > sendDate.getTime()) {
                    result=c.getString(2);
                    break;
                }
                c.moveToNext();
            }
        }

        c.close();

        return result;
    }

    public void startParking() {
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(context);
        if (startParkingDate==null) startParkingDate=new Date();
        SharedPreferences.Editor ed = prefs.edit();
        ed.putString("LastParkTime", String.valueOf(startParkingDate.getTime()));
        ed.putString("LastHours", hours);
        ed.apply();

        showParkingScreen();
    }

    public void stopParking() {
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(context);
        startParkingDate=null;
        SharedPreferences.Editor ed = prefs.edit();
        ed.putString("LastParkTime", "0");
        ed.putString("LastHours", hours);
        ed.apply();

        showMainScreen();
    }

    public  void showParkingScreen() {
        Intent intent = new Intent(context, ParkingActivity.class);
        context.startActivity(intent);
    }

    public  void showMainScreen() {
        Intent intent = new Intent(context, MainActivity.class);
        context.startActivity(intent);
    }

}

