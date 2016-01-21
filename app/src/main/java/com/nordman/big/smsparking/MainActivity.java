package com.nordman.big.smsparking;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, PopupMenu.OnMenuItemClickListener {
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 5;
    public static final int STATUS_INITIAL = 1;
    public static final int STATUS_CONFIRM = 2;
    int appStatus = STATUS_INITIAL;

    Button getZoneButton;
    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    GeoManager geo = new GeoManager(this);
    SmsManager smsMgr = new SmsManager(this);
    Timer timer = null;
    ProgressBar progressBar = null;

    String sms = null;
    String regNum = "________";
    ParkZone currentZone = null;
    String hours = "1";

    boolean waitForSms = false;
    Date sendDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // обработчик клика "Определить паркинг"
        getZoneButton = (Button) this.findViewById(R.id.getZoneButton);

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }
    @Override
    protected void onStart() {
        Log.d("LOG", "onStart...");
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.d("LOG", "onStop...");
        super.onStop();
        if(mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onResume() {
        Log.d("LOG", "onResume...");
        super.onResume();
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
        regNum = prefs.getString("regnum", "________");
        updateSms();

        // если произошло возвращение из смс-приложения, то проверим, была ли отослана смс
        if (waitForSms){

            int smsNumber;
            if (appStatus==STATUS_INITIAL) smsNumber=R.string.smsNumber;
            else smsNumber=R.string.smsNumberBack;

            Log.d("LOG", "check for outgoing sms...");
            waitForSms=false;
            TextView sendMessageText = (TextView) this.findViewById(R.id.sendMessage);
            if(smsMgr.IsSent(sendDate,getResources().getString(smsNumber))) {
                Log.d("LOG", "sms was sent...");
                sendMessageText.setText(getResources().getString(R.string.sendSmsWaiting));
                sendMessageText.setTextColor(Color.BLACK);
                checkSms();
            } else {
                Log.d("LOG", "sms wasn't sent...");
                sendMessageText.setText(getResources().getString(R.string.sendSmsFailed));
                sendMessageText.setTextColor(Color.RED);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent i = new Intent(this, SettingsActivity.class);
            i.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.GeneralPreferenceFragment.class.getName());
            i.putExtra(SettingsActivity.EXTRA_NO_HEADERS, true);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("LOG", "onConnected...");
        createLocationRequest();
        getZoneButton.setEnabled(true);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("LOG", "onConnectionSuspended...");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("LOG", "onConnectionFailed...");
    }

    @Override
    public void onLocationChanged(Location location) {
        //Log.d("LOG", location.toString());
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        startLocationUpdates();
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    private void updateSms(){
        // формируем строку sms
        sms = "p66*";
        TextView zoneDesc = (TextView) this.findViewById(R.id.zoneDesc);

        if (currentZone==null) {
            sms += "___*";
            zoneDesc.setText("Паркинг не определен");
            zoneDesc.setTextColor(Color.RED);
        } else {
            sms += currentZone.getZoneNumber().toString() + "*";
            zoneDesc.setText(currentZone.getZoneDesc());
            zoneDesc.setTextColor(Color.BLACK);
        }
        sms += regNum + "*" + hours;

        // выводим sms на экран
        ((TextView) this.findViewById(R.id.smsText)).setText(sms);

        // выводим часы
        String hourDesc;
        if (hours.equals("1")) hourDesc = hours + " час";
        else hourDesc = hours + " часа";
        ((TextView) this.findViewById(R.id.hourDesc)).setText(hourDesc);

        Button payButton = (Button) findViewById(R.id.payButton);
        LinearLayout confirmLinearLayout = (LinearLayout) findViewById(R.id.confirmLinearLayout);

        if (appStatus==STATUS_INITIAL) {
            payButton.setVisibility(View.VISIBLE);
            confirmLinearLayout.setVisibility(View.INVISIBLE);
            // энаблим/дизаблим кнопку "оплатить"
            if (!regNum.equals("________") & currentZone != null)
                this.findViewById(R.id.payButton).setEnabled(true);
            else this.findViewById(R.id.payButton).setEnabled(false);
        }

        if (appStatus==STATUS_CONFIRM) {
            payButton.setVisibility(View.INVISIBLE);
            confirmLinearLayout.setVisibility(View.VISIBLE);
        }
    }

    public void getZoneButtonOnClick(View v) {
        Log.d("LOG", geo.getCoordinates(mGoogleApiClient));
        Toast.makeText(v.getContext(), geo.getCoordinates(mGoogleApiClient), Toast.LENGTH_LONG).show();

        currentZone = geo.getParkZone(mGoogleApiClient);
        updateSms();
    }

    public void setZoneButtonOnClick(View v) {
        PopupMenu popup = new PopupMenu(this,v);
        Menu mnu = popup.getMenu();

        // заполняем меню из xml с парковочными зонами
        ArrayList<ParkZone> zones = geo.getParkZoneList();

        for(ParkZone zone : zones){
            mnu.add(0,zone.getZoneNumber(),zone.getZoneNumber(),zone.getZoneNumber().toString());
        }

        popup.setOnMenuItemClickListener(this);
        popup.getMenuInflater().inflate(R.menu.menu_zone, mnu);

        popup.show();
    }


    @Override
    public boolean onMenuItemClick(MenuItem item) {
        //Log.d("LOG", "MenuItemId = " + String.valueOf(item.getItemId()));
        currentZone = geo.getParkZone(item.getItemId());
        updateSms();

        return true;
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("sms", sms);
        if (currentZone!=null) outState.putInt("currentZoneNumber", currentZone.getZoneNumber());
        outState.putString("hours", hours);

        Log.d("LOG", "onSaveInstanceState");
    }

    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        sms = savedInstanceState.getString("sms");
        currentZone = geo.getParkZone(savedInstanceState.getInt("currentZoneNumber"));
        hours = savedInstanceState.getString("hours");

        Log.d("LOG", "onRestoreInstanceState");
    }

    public void oneHourButtonOnClick(View view) {
        hours = "1";
        updateSms();
    }

    public void twoHourButtonOnClick(View view) {
        hours = "2";
        updateSms();
    }

    public void threeHourButtonOnClick(View view) {
        hours = "3";
        updateSms();
    }

    public void payButtonOnClick(View view) {
        Uri uri = Uri.parse("smsto:" + getResources().getString(R.string.smsNumber));
        Intent it = new Intent(Intent.ACTION_SENDTO, uri);
        it.putExtra("sms_body", sms);
        startActivity(it);
        waitForSms = true;
        sendDate = new Date();
    }

    public void confirmButtonOnClick(View view) {
    }

    public void cancelButtonOnClick(View view) {
        Uri uri = Uri.parse("smsto:" + getResources().getString(R.string.smsNumberBack));
        Intent it = new Intent(Intent.ACTION_SENDTO, uri);
        it.putExtra("sms_body", "0");   // послать 0, если хотим отменить
        startActivity(it);
        waitForSms = true;
        sendDate = new Date();
    }

    final Handler h = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            String aResponse = msg.getData().getString("message");
            if ((null != aResponse)){
                ((TextView) findViewById(R.id.sendMessage)).setText(aResponse);
                if (aResponse.indexOf("Сумма:")==0){    // если смс именно с подтверждением суммы, а не какое-то другое, то меняем интерфейс на "подтверждение"
                    appStatus=STATUS_CONFIRM;
                    updateSms();
                }
            }
            if (progressBar!=null) progressBar.setVisibility(View.INVISIBLE);
            return false;
        }
    });

    public void checkSms() {
        if (timer==null){
            progressBar = (ProgressBar) findViewById(R.id.progressBar);
            progressBar.setVisibility(View.VISIBLE);

            timer = new Timer();
            timer.schedule(new UpdateTimeTask(), 0, 5000); //тикаем каждые 5 секунд
        }
    }


    private class UpdateTimeTask extends TimerTask {
        int tickCount = 0;
        String smsText;
        Message msgObj;
        Bundle b;
        public void run() {
            tickCount++;
            Log.d("LOG", "timer tick! " + String.valueOf(tickCount) );
            smsText = smsMgr.GetIncomingSms(sendDate,getResources().getString(R.string.smsNumberBack));
            if (smsText!=null){
                timer.cancel();
                timer = null;

                try {
                    smsText = smsText.substring(smsText.indexOf("Сумма:"),smsText.indexOf("Для подтверждения платежа")-1);
                } catch (Exception ignored){}

                msgObj = h.obtainMessage();
                b = new Bundle();
                b.putString("message", smsText);
                msgObj.setData(b);
                h.sendMessage(msgObj);
            }
            if(tickCount>=6) {
                timer.cancel();
                timer = null;

                msgObj = h.obtainMessage();
                b = new Bundle();
                b.putString("message", getResources().getString(R.string.incomingSmsFailed));
                msgObj.setData(b);
                h.sendMessage(msgObj);
            }
        }
    }

    public void qClick(View view) {
        /*
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 2016);
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.DAY_OF_MONTH, 16);
        cal.set(Calendar.HOUR_OF_DAY, 15);


        Date sendDate = cal.getTime();
        String smsText = smsMgr.GetIncomingSms(sendDate, getResources().getString(R.string.smsNumberBack));
        try {
            smsText = smsText.substring(smsText.indexOf("Сумма:"),smsText.indexOf("Для подтверждения платежа")-1);
        } catch (Exception ignored){}
        */
        final TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        Log.d("LOG", "NetworkOperatorName = " + telephonyManager.getNetworkOperatorName());
        Log.d("LOG", "NetworkOperator = " + telephonyManager.getNetworkOperator());
    }

}
