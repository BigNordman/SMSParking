package com.nordman.big.smsparking;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.preference.PreferenceManager;
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

import java.util.ArrayList;

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
    String hours = "_";

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

        ((TextView) this.findViewById(R.id.smsText)).setText(sms);
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
}
