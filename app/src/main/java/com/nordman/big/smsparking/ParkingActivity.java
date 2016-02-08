package com.nordman.big.smsparking;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.lylc.widget.circularprogressbar.CircularProgressBar;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


public class ParkingActivity extends Activity {
    private static final long MILLIS_IN_MINUTE = 60000;

    SmsManager smsMgr;
    Timer timer = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parking);

        Intent i = getIntent();
        smsMgr = new SmsManager(this);
        smsMgr.restoreState();

        setProgress();

        if (timer==null){
            timer = new Timer();
            timer.schedule(new UpdateTimeTask(), 0, MILLIS_IN_MINUTE); //тикаем каждую минуту
        }
    }

    protected void onResume() {
        super.onResume();

        if (smsMgr.currentZone!=null) {
            ((TextView) this.findViewById(R.id.zoneText)).setText(smsMgr.currentZone.getZoneDesc());
        }
        if (smsMgr.startParkingDate!=null) {
            Log.d("LOG", "smsMgr.startParkingDate = " + smsMgr.startParkingDate);

            DateFormat df = new SimpleDateFormat("kk:mm");
            ((TextView) this.findViewById(R.id.beginText)).setText("Парковка с " + df.format(smsMgr.startParkingDate));

            Log.d("LOG", "smsMgr.startParkingDate formatted = " + df.format(smsMgr.startParkingDate));
        }

        // если произошло возвращение из смс-приложения, то проверим, была ли отослана смс
        if (smsMgr.appStatus==SmsManager.STATUS_WAITING_OUT){
            if(smsMgr.IsSent(getResources().getString(R.string.smsNumber))) {
                // смс о досрочном прекращении отослана - возвращаемся на стартовый экран
                smsMgr.stopParking();
                smsMgr.appStatus=SmsManager.STATUS_INITIAL;
                smsMgr.saveState();
                finish();
            }
        }
    }


    private void setProgress() {
        CircularProgressBar pb = (CircularProgressBar) findViewById(R.id.circularprogressbar1);
        int progress = smsMgr.getProgress();
        pb.setProgress(progress);
        pb.setTitle(String.valueOf(smsMgr.getMinutes()) + " мин");

        if (progress==0) {
            smsMgr.stopParking();
            smsMgr.appStatus=SmsManager.STATUS_INITIAL;
            smsMgr.saveState();
            finish();
        }
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

        smsMgr.appStatus = SmsManager.STATUS_WAITING_OUT;
        smsMgr.sendDate = new Date();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            moveTaskToBack(true);
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    public void qButtonOnClick(View view) {
        smsMgr.stopParking();
        smsMgr.appStatus=SmsManager.STATUS_INITIAL;
        smsMgr.saveState();
        finish();
    }

}
