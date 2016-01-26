package com.nordman.big.smsparking;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import com.lylc.widget.circularprogressbar.CircularProgressBar;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


public class ParkingActivity extends Activity {
    public static final long MILLIS_IN_HOUR = 3600000;
    public static final long MILLIS_IN_MINUTE = 60000;
    Timer timer = null;
    Long lh, lpt;
    CircularProgressBar pb;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parking);

        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
        lh = Long.parseLong(prefs.getString("LastHours", "1"));
        lpt = Long.parseLong(prefs.getString("LastParkTime", "0"));
        pb = (CircularProgressBar) findViewById(R.id.circularprogressbar1);
        setProgress();

        if (timer==null){
            timer = new Timer();
            timer.schedule(new UpdateTimeTask(), 0, MILLIS_IN_MINUTE); //тикаем каждую минуту
        }

    }

    private void setProgress() {
        long current = (new Date()).getTime();

        int curProgress = (int) (100 * (lh*MILLIS_IN_HOUR - (current - lpt) )/(lh*MILLIS_IN_HOUR));
        int minutes = (int) ((lh*MILLIS_IN_HOUR - (current - lpt))/MILLIS_IN_MINUTE);
        pb.setProgress(curProgress);
        pb.setTitle(String.valueOf(minutes) + " мин");
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
    }

    public void prolongButtonOnClick(View view) {
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setMessage("Выйти из приложения?")
                    .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int i) {
                            timer.cancel();
                            timer = null;
                            moveTaskToBack(true);
                            finish();
                        }
                    })
                    .setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int i) {
                        }
                    })
                    .show();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }
}
