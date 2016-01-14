package com.nordman.big.smsparking;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, PopupMenu.OnMenuItemClickListener {
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 5;

    Button getZoneButton;
    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    GeoManager geo = new GeoManager(this);
    String sms = null;
    String regNum = "________";
    ParkZone currentZone = null;
    String hours = "1";

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
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
        regNum = prefs.getString("regnum", "________");
        updateSms();
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
            i.putExtra( SettingsActivity.EXTRA_NO_HEADERS, true );
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
        Log.d("LOG", location.toString());
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

        // энаблим/дизаблим кнопку "оплатить"
        if (!regNum.equals("________") & currentZone!=null) this.findViewById(R.id.payButton).setEnabled(true);
        else this.findViewById(R.id.payButton).setEnabled(false);
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
    }

    public void checkSms(View view) {
        ContentResolver cr = this.getContentResolver();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Cursor c = cr.query(Telephony.Sms.Inbox.CONTENT_URI, // Official CONTENT_URI from docs
                new String[] { Telephony.Sms.Inbox.DATE, Telephony.Sms.Inbox.BODY }, // Select body text
                null,
                null,
                Telephony.Sms.Inbox.DEFAULT_SORT_ORDER); // Default sort order

        assert c != null;
        int totalSMS = c.getCount();

        if (c.moveToFirst()) {
            for (int i = 0; i < totalSMS; i++) {
                Date dt = new Date(Long.parseLong(c.getString(0)));
                Log.d("LOG", "sms date= " + dateFormat.format(dt));
                Log.d("LOG", "sms text= " + c.getString(1));
                c.moveToNext();
            }
        } else {
            throw new RuntimeException("You have no SMS in Inbox");
        }
        c.close();
    }
}
