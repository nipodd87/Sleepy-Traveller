package com.studio.nitz.sleepytraveller;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, GeocodeReceiver.Receiver
        , LocationListener, ResultCallback<Status>, ActivityCompat.OnRequestPermissionsResultCallback {

    private EditText addressTxt;
    private String addressInput;
    private Button btnStart;
    private Button btnStop;
    private static final String TAG = "com.studio.nitz.sleepytraveller";
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private GoogleMap map;
    private GeocodeReceiver geocodeReceiver = null;
    public static boolean mGeofenceAdded;
    private boolean mGeofenceStarted;
    private boolean mGeofenceSearched;
    public static SharedPreferences mSharedPreferences;
    private LocationRequest mLocationRequest;
    protected ArrayList<Geofence> mGeofenceList;
    private PendingIntent mGeofencePendingIntent;
    private boolean isAddressLocated;
    private static final int REQUEST_CHECK_SETTINGS = 0x1;
    public int selectedTone;
    private AlertDialog alert;
    private static String[] PERMISSIONS_LOCATION = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
    private static final int REQUEST_LOCATION = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mGeofenceList = new ArrayList<Geofence>();
        mGeofencePendingIntent = null;
        isAddressLocated = false;
        btnStart = (Button) findViewById(R.id.btnStart);
        btnStop = (Button) findViewById(R.id.btnStop);
        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        selectedTone = mSharedPreferences.getInt(Constants.ALARM_TONE_KEY, 0);
        mGeofenceAdded = mSharedPreferences.getBoolean(Constants.GEOFENCE_ADDED_KEY, false);
        setButtonsEnabledState();
        map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        map.getUiSettings().setZoomControlsEnabled(true);
        addressTxt = (EditText) findViewById(R.id.addressTxt);
        addressTxt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                addressTxt.setCursorVisible(true);
                return false;
            }
        });
        geocodeReceiver = new GeocodeReceiver(new Handler());
        geocodeReceiver.setReceiver(this);
        btnStart.setEnabled(false);
        btnStart.setAlpha(.3f);
        btnStart.setClickable(false);
        buildGoogleApiClient();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.alarm_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final String[] selectTone = new String[]{"Digital Alarm Clock", "Kalinka", "Synth dreams", "Final Destination", "Piano Dreams", "Wake Up Song"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose your alarm style");
        builder.setIcon(R.drawable.sleepy_icon);
        builder.setSingleChoiceItems(selectTone, selectedTone, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedTone = which;
                }
        });
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.putInt(Constants.ALARM_TONE_KEY, selectedTone);
                editor.apply();
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedTone = mSharedPreferences.getInt(Constants.ALARM_TONE_KEY, 0);
                dialog.dismiss();
            }
        });
        alert = builder.create();
        alert.show();
        return true;
    }

    protected synchronized void buildGoogleApiClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .addApi(Places.GEO_DATA_API)
                    .build();
            createLocationRequest();
        }
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(Constants.UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(Constants.FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }
    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected() && (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            startLocationUpdates();
        }
    }
    @Override
    protected void onDestroy() {
        mGoogleApiClient.disconnect();
        super.onDestroy();
    }
    public void findAddressOnMap(View view) {
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        isAddressLocated = false;
        mGeofenceStarted = false;
        mGeofenceSearched = true;
        map.clear();
        addressInput = addressTxt.getText().toString();
        if (addressInput.length() == 0) {
            Toast.makeText(this, "Enter valid Address to search", Toast.LENGTH_LONG).show();
            return;
        } else {
            startGeocodingIntent(addressInput);
        }
    }
    public void startGeofenceTrack(View view) {
        isAddressLocated = false;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            // Location Permission not granted, need to request the permission.
            requestLocationPermission();
            return;
        }
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, "Address not located or No Internet Connection", Toast.LENGTH_LONG).show();
            return;

        }
        else{
            LocationServices.GeofencingApi.addGeofences(mGoogleApiClient,
                    getGeofencingRequest(), getGeofencePendingIntent()).setResultCallback(this);
        }
    }
    private void requestLocationPermission(){
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_LOCATION){
            if (grantResults.length ==  1 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                return;
            } else if (grantResults[0] ==  PackageManager.PERMISSION_DENIED){
                //should show and explanation and ask again
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
                    new AlertDialog.Builder(this)
                            .setTitle("Permission Required")
                            .setMessage("Sleepy Traveller is designed to trigger an alert as soon as you enter a chosen region.\n\n " +
                                    "To do so, this app requires permission to monitor your current location, even while it is in background.\n\n" +
                                    "Your location data does not and will never leave this device and is only used for internal calculations. ")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .setIcon(R.drawable.ic_location)
                            .show();
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle("Permission Issue")
                            .setMessage("Pending Location Permission. Go to settings first to make the changes.")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    final Intent i = new Intent();
                                    i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    i.addCategory(Intent.CATEGORY_DEFAULT);
                                    i.setData(Uri.parse("package:" + "com.studio.nitz.sleepytraveller"));
                                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                    i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                                    startActivity(i);
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .setIcon(R.drawable.ic_location)
                            .show();
                }
            }

        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void stopGeofenceTrack(View view){
        isAddressLocated=false;
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, "Address not located or No Internet Connection", Toast.LENGTH_LONG).show();
            return;
        } else {
            map.clear();
            LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, getGeofencePendingIntent()).setResultCallback(this);
        }
    }
    private PendingIntent getGeofencePendingIntent(){
        if (mGeofencePendingIntent != null){
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
    protected void startGeocodingIntent(String address) {
        Intent geoIntent = new Intent(this, GeocoderIntentService.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable("receiver", geocodeReceiver);
        bundle.putString("address", address);
        geoIntent.putExtra("bundle", bundle);
        startService(geoIntent);
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
        } else {
            if (mLastLocation == null) {
                getUserCurrentLocation();
                startLocationUpdates();
            } else {
                startLocationUpdates();
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, "Connection Suspended", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, "Connection Failed", Toast.LENGTH_LONG).show();
    }

    private void getUserCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        updateUI(mLastLocation);
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();

        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onReceiveResult(int code, Bundle data) {
        if (code==0){ //issue in finding address
            addressTxt.setText("");
            Toast.makeText(this, "Incorrect Address. Try Again", Toast.LENGTH_LONG).show();
        } else if (code==1){ //successful finding address
            isAddressLocated=true;
            locateOnMap(data.getDouble("latitude"), data.getDouble("longitude"));
            populateGeofence(data.getDouble("latitude"), data.getDouble("longitude"));
            btnStart.setEnabled(true);
            btnStart.setAlpha(1f);
            btnStart.setClickable(true);
        }
    }

    private void locateOnMap(double latitude, double longitude) {
        LatLng latLng = new LatLng(latitude, longitude);
        map.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        map.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        map.animateCamera(CameraUpdateFactory.zoomTo(13), 2000, null);
        CircleOptions circleOptions = new CircleOptions()
                                            .center(latLng)
                                            .fillColor(0x40ff0000)
                                            .strokeColor(Color.TRANSPARENT)
                                            .radius(1600);
        map.addCircle(circleOptions);
    }

    protected void populateGeofence(double latitude, double longitude){
        mGeofenceList.add(new Geofence.Builder().setRequestId(Constants.PACKAGE_NAME)
                .setCircularRegion(latitude, longitude, 500) //building it for 3000 mts
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .setExpirationDuration(24 * 60 * 60 * 1000)
                .build());
    }

    @Override
    public void onLocationChanged(Location location) {
        if (!isAddressLocated) {
            updateUI(location);
        }
    }

    protected void updateUI(Location location){
        if (location == null){
            SleepyTraveller.showErrorDialog("Network Issue", "Sleepy Traveller needs Network connection and Location to work", MainActivity.this);
            return;
        }
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
        map.clear();
        map.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_current_location)));
        map.animateCamera(cameraUpdate);
    }
    private GeofencingRequest getGeofencingRequest(){
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }

    public void setButtonsEnabledState(){
        if (mGeofenceAdded){
            btnStart.setEnabled(false);
            btnStart.setAlpha(.3f);
            btnStart.setClickable(false);
            btnStop.setEnabled(true);
            btnStop.setAlpha(1f);
            btnStop.setClickable(true);
        }else {
            btnStart.setEnabled(true);
            btnStop.setEnabled(false);
            btnStop.setAlpha(.3f);
            btnStop.setClickable(false);
            btnStart.setAlpha(1f);
            btnStart.setClickable(true);
        }
    }

    @Override
    public void onResult(Status status) {
        if (status.isSuccess()){
            if (mGeofenceAdded){
                mGeofenceAdded=false;
            } else{
                mGeofenceAdded=true;
            }
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putBoolean(Constants.GEOFENCE_ADDED_KEY, mGeofenceAdded);
            editor.apply();
            setButtonsEnabledState();

            Toast.makeText(this, mGeofenceAdded?"Geofence Added Successfully":"Geofence Removed Successfully", Toast.LENGTH_LONG).show();
        } else{
            String errorMsg = GeofenceErrorMessages.getErrorString(this, status.getStatusCode());
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
        }
    }
}
