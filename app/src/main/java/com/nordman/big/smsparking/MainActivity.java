package com.nordman.big.smsparking;

import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, PopupMenu.OnMenuItemClickListener {
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 5;

    public static final int MAX_TICK_WAITING = 60;
    public static final int STATUS_INITIAL = 1;
    public static final int STATUS_WAITING_OUT = 2;
    public static final int STATUS_WAITING_IN = 3;
    public static final int STATUS_SMS_SENT = 4;
    public static final int STATUS_SMS_NOT_SENT = 5;
    int appStatus = STATUS_INITIAL;

    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    GeoManager geoMgr = new GeoManager(this);
    SmsManager smsMgr = new SmsManager(this);
    Timer tick = null;
    int tickWaiting = 0;
    String sendMessage = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        if (tick==null){
            tick = new Timer();
            tick.schedule(new UpdateTickTask(), 0, 1000); //тикаем каждую секунду
        }

        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.d("LOG", "onStop...");
        super.onStop();
        if(mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        if (tick!=null) {
            tick.cancel();
            tick = null;
        }
    }

    @Override
    protected void onResume() {
        Log.d("LOG", "onResume...");

        if (smsMgr.parkingActive()){
            Log.d("LOG", "smsMgr.parkingActive...");
            smsMgr.showParkingScreen();
        }

        super.onResume();
        updateView();
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
        this.findViewById(R.id.getZoneButton).setEnabled(true);
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
        Log.d("LOG", "onLocationChanged...");
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

    private void updateView(){
        smsMgr.updateSms();
        TextView zoneDesc = (TextView) this.findViewById(R.id.zoneDesc);

        if (smsMgr.currentZone==null) {
            zoneDesc.setText("Паркинг не определен");
            zoneDesc.setTextColor(Color.RED);
        } else {
            zoneDesc.setText(smsMgr.currentZone.getZoneDesc());
            zoneDesc.setTextColor(Color.BLACK);
        }
        // выводим sms на экран
        ((TextView) this.findViewById(R.id.smsText)).setText(smsMgr.sms);
        // выводим часы
        ((TextView) this.findViewById(R.id.hourDesc)).setText(smsMgr.hourDesc());

        // формируем строку sms
        // энаблим/дизаблим кнопку "оплатить"
        if (smsMgr.smsComplete()) this.findViewById(R.id.payButton).setEnabled(true);
        else this.findViewById(R.id.payButton).setEnabled(false);

        switch (appStatus) {
            case STATUS_INITIAL:
                findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
                ((TextView) this.findViewById(R.id.sendMessage)).setText(sendMessage);
                ((TextView) this.findViewById(R.id.sendMessage)).setTextColor(Color.RED);
                break;
            case STATUS_WAITING_OUT:
                Log.d("LOG", "waiting outgoing sms...");
                findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
                ((TextView) this.findViewById(R.id.sendMessage)).setText(getResources().getString(R.string.outgoingSmsWaiting));
                ((TextView) this.findViewById(R.id.sendMessage)).setTextColor(Color.BLACK);
                break;
            case STATUS_WAITING_IN:
                Log.d("LOG", "waiting incoming sms...");
                findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
                ((TextView) this.findViewById(R.id.sendMessage)).setText(getResources().getString(R.string.incomingSmsWaiting));
                ((TextView) this.findViewById(R.id.sendMessage)).setTextColor(Color.BLACK);
                break;
            case STATUS_SMS_SENT:
                Log.d("LOG", "sms was sent...");
                findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
                ((TextView) this.findViewById(R.id.sendMessage)).setText(getResources().getString(R.string.sendSmsWaiting));
                ((TextView) this.findViewById(R.id.sendMessage)).setTextColor(Color.BLACK);
                break;
            case STATUS_SMS_NOT_SENT:
                Log.d("LOG", "sms wasn't sent...");
                findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
                ((TextView) this.findViewById(R.id.sendMessage)).setText(getResources().getString(R.string.sendSmsFailed));
                ((TextView) this.findViewById(R.id.sendMessage)).setTextColor(Color.RED);
                break;
        }

    }

    public void getZoneButtonOnClick(View v) {
        Log.d("LOG", geoMgr.getCoordinates(mGoogleApiClient));
        Toast.makeText(v.getContext(), geoMgr.getCoordinates(mGoogleApiClient), Toast.LENGTH_LONG).show();

        smsMgr.currentZone = geoMgr.getParkZone(mGoogleApiClient);
        updateView();
    }

    public void setZoneButtonOnClick(View v) {
        PopupMenu popup = new PopupMenu(this,v);
        Menu mnu = popup.getMenu();

        // заполняем меню из xml с парковочными зонами
        ArrayList<ParkZone> zones = geoMgr.getParkZoneList();

        for(ParkZone zone : zones){
            mnu.add(0,zone.getZoneNumber(),zone.getZoneNumber(),zone.getZoneNumber().toString());
        }

        popup.setOnMenuItemClickListener(this);
        popup.getMenuInflater().inflate(R.menu.menu_zone, mnu);

        popup.show();
    }


    @Override
    public boolean onMenuItemClick(MenuItem item) {
        smsMgr.currentZone = geoMgr.getParkZone(item.getItemId());
        updateView();

        return true;
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("sms", smsMgr.sms);
        if (smsMgr.currentZone!=null) outState.putInt("currentZoneNumber", smsMgr.currentZone.getZoneNumber());
        outState.putString("hours", smsMgr.hours);
        if (smsMgr.sendDate !=null) outState.putLong("sendDate", smsMgr.sendDate.getTime());
        outState.putInt("status", appStatus);

        Log.d("LOG", "onSaveInstanceState");
    }

    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        smsMgr.sms = savedInstanceState.getString("sms");
        smsMgr.currentZone = geoMgr.getParkZone(savedInstanceState.getInt("currentZoneNumber"));
        smsMgr.hours = savedInstanceState.getString("hours");
        smsMgr.sendDate = new Date(savedInstanceState.getLong("sendDate"));
        appStatus = savedInstanceState.getInt("status");

        Log.d("LOG", "onRestoreInstanceState");
    }

    public void oneHourButtonOnClick(View view) {
        smsMgr.hours = "1";
        updateView();
    }

    public void twoHourButtonOnClick(View view) {
        smsMgr.hours = "2";
        updateView();
    }

    public void threeHourButtonOnClick(View view) {
        smsMgr.hours = "3";
        updateView();
    }

    public void payButtonOnClick(View view) {
        Uri uri = Uri.parse("smsto:" + getResources().getString(R.string.smsNumber));
        Intent it = new Intent(Intent.ACTION_SENDTO, uri);
        it.putExtra("sms_body", smsMgr.sms);
        startActivity(it);
        smsMgr.sendDate = new Date();
        appStatus = STATUS_WAITING_OUT;
        tickWaiting = 0;
    }

    private class UpdateTickTask extends TimerTask {
        public void run() {
            tickHandler.sendEmptyMessage(0);
        }
    }

    final Handler tickHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            // обрабатываем сообщение таймера
            Log.d("LOG", "***tick! ");

            if(appStatus==STATUS_WAITING_OUT){
                // ждем исходящее смс
                Log.d("LOG", "***ждем исходящее смс");
                tickWaiting++;

                if (smsMgr.IsSent(getResources().getString(R.string.smsNumber))){
                    // обнаружили, что смс отправлена. Меняем статус на ожидание входящего смс
                    appStatus=STATUS_WAITING_IN;
                    tickWaiting = 0;
                }
                if (tickWaiting==MAX_TICK_WAITING){
                    // время ожидания исходящего смс истекло
                    appStatus=STATUS_SMS_NOT_SENT;
                }
            }

            if(appStatus==STATUS_WAITING_IN){
                // ждем входящее смс
                Log.d("LOG", "***ждем входящее смс");
                tickWaiting++;

                String smsText = smsMgr.GetIncomingSms(getResources().getString(R.string.smsNumber));
                if (smsText!=null){
                    // какая-то смс с искомого номера пришла...
                    if (smsText.indexOf(getResources().getString(R.string.smsOrderPaid))==0){
                        // если смс именно с подтверждением оплаты, то меняем интерфейс на "припарковано"
                        smsMgr.startParking();
                    } else {
                        // если какая-то другая смс - просто выводим ее содержимое
                        sendMessage = smsText;
                        appStatus = STATUS_INITIAL;
                    }

                }
                if (tickWaiting==MAX_TICK_WAITING){
                    // время ожидания исходящего смс истекло
                    appStatus=STATUS_SMS_NOT_SENT;
                }

            }

            updateView();
            return false;
        }
    });


    public void qClick(View view) {
        smsMgr.startParking();
    }
}
