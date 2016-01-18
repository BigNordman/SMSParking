package com.nordman.big.smsparking;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony;

import java.util.Date;

/**
 * Created by s_vershinin on 15.01.2016.
 */
public class SmsManager {
    Context context;

    public SmsManager(Context context) {
        this.context = context;
    }

    /// проверяем, отправлена ли была смс на оплату (смотрим по адресату и дате/времени отправления)
    public boolean IsSent(Date dt, String toWhom){
        boolean result = false;

        ContentResolver cr = context.getContentResolver();

        Cursor c = cr.query(Telephony.Sms.Sent.CONTENT_URI, // Official CONTENT_URI from docs
                new String[] { Telephony.Sms.Sent.DATE, Telephony.Sms.Sent.ADDRESS, Telephony.Sms.Sent.BODY }, // Select body text
                Telephony.Sms.Sent.ADDRESS + " = '" + toWhom + "'",
                null,
                Telephony.Sms.Sent.DEFAULT_SORT_ORDER); // Default sort order

        assert c != null;
        int totalSMS = c.getCount();

        if (c.moveToFirst()) {
            for (int i = 0; i < totalSMS; i++) {
                if (Long.parseLong(c.getString(0)) > dt.getTime()) {
                    result=true;
                    break;
                }
                c.moveToNext();
            }
        }

        c.close();

        return result;
    }

    public String GetIncomingSms(Date dt, String fromWhom){
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
                if (Long.parseLong(c.getString(0)) > dt.getTime()) {
                    result=c.getString(2);
                    break;
                }
                c.moveToNext();
            }
        }

        c.close();

        return result;
    }

}
