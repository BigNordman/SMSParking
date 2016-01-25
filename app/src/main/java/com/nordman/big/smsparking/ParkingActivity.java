package com.nordman.big.smsparking;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import com.lylc.widget.circularprogressbar.CircularProgressBar;


public class ParkingActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parking);

        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
        Log.d("LOG", "LastParkTime = " + prefs.getString("LastParkTime", ""));
        Log.d("LOG", "LastHours = " + prefs.getString("LastHours", ""));

        CircularProgressBar c1 = (CircularProgressBar) findViewById(R.id.circularprogressbar1);
        c1.setProgress(45);
        c1.setTitle("June");
        c1.setSubTitle("2013");

    }

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
