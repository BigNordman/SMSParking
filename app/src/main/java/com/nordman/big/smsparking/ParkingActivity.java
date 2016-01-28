package com.nordman.big.smsparking;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;

import com.lylc.widget.circularprogressbar.CircularProgressBar;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


public class ParkingActivity extends Activity {
    private static final long MILLIS_IN_MINUTE = 60000;
    private static final int STATUS_INITIAL = 1;
    private static final int STATUS_WAITING_SMS = 3;

    int appStatus = STATUS_INITIAL;

    SmsManager smsMgr = new SmsManager(this);
    Timer timer = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parking);

        setProgress();

        if (timer==null){
            timer = new Timer();
            timer.schedule(new UpdateTimeTask(), 0, MILLIS_IN_MINUTE); //тикаем каждую минуту
        }
    }

    protected void onResume() {
        super.onResume();

        // если произошло возвращение из смс-приложения, то проверим, была ли отослана смс
        if (appStatus==STATUS_WAITING_SMS){
            if(smsMgr.IsSent(getResources().getString(R.string.smsNumber))) {
                // смс о досрочном прекращении отослана - возвращаемся на стартовый экран
                smsMgr.stopParking();
                finish();
            }
        }
    }


    private void setProgress() {
        CircularProgressBar pb = (CircularProgressBar) findViewById(R.id.circularprogressbar1);

        pb.setProgress(smsMgr.getProgress());
        pb.setTitle(String.valueOf(smsMgr.getMinutes()) + " мин");
    }

    private class UpdateTimeTask extends TimerTask {
        public void run() {
            h.sendEmptyMessage(0);
        }
    }

    final Handler h = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            // обрабатываем сообщение таймера
            setProgress();
            return false;
        }
    });

    public void stopParkingButtonOnClick(View view) {
        Uri uri = Uri.parse("smsto:" + getResources().getString(R.string.smsNumber));
        Intent it = new Intent(Intent.ACTION_SENDTO, uri);
        it.putExtra("sms_body", "p66*c");
        startActivity(it);

        appStatus = STATUS_WAITING_SMS;
        smsMgr.sendDate = new Date();
    }

    public void prolongButtonOnClick(View view) {
        smsMgr.stopParking();
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            moveTaskToBack(true);
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }
}
