package com.studio.nitz.sleepytraveller;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
        , LocationListener, ResultCallback<Status> {

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
        mGeofenceAdded = mSharedPreferences.getBoolean(Constants.GEOFENCE_ADDED_KEY, false);
        setButtonsEnabledState();
        map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        buildGoogleApiClient();
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
        if (mGoogleApiClient.isConnected()) {
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
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, "Address not located or No Internet Connection", Toast.LENGTH_LONG).show();
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Toast.makeText(this, "No Internet Connection or Location Permission not granted", Toast.LENGTH_LONG).show();
            return;
        }

        LocationServices.GeofencingApi.addGeofences(mGoogleApiClient,
                getGeofencingRequest(), getGeofencePendingIntent()).setResultCallback(this);
    }

    public void stopGeofenceTrack(View view){
        isAddressLocated=false;
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, "Address not located or No Internet Connection", Toast.LENGTH_LONG).show();
            return;
        }
        map.clear();
        LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, getGeofencePendingIntent()).setResultCallback(this);
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
        if (mLastLocation == null) {
            getUserCurrentLocation();
            startLocationUpdates();
        }
        else {
            startLocationUpdates();
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
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        updateUI(mLastLocation);
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            return;
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
        }
    }

    private void locateOnMap(double latitude, double longitude) {
        LatLng latLng = new LatLng(latitude, longitude);
        map.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        map.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        map.animateCamera(CameraUpdateFactory.zoomTo(14), 3000, null);
        CircleOptions circleOptions = new CircleOptions()
                                            .center(latLng)
                                            .fillColor(0x40ff0000)
                                            .strokeColor(Color.TRANSPARENT)
                                            .radius(1000);
        map.addCircle(circleOptions);
    }

    protected void populateGeofence(double latitude, double longitude){
        mGeofenceList.add(new Geofence.Builder().setRequestId(Constants.PACKAGE_NAME)
                .setCircularRegion(latitude, longitude, 3000) //building it for 3000 mts
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
            btnStop.setEnabled(true);
        }else {
            btnStart.setEnabled(true);
            btnStop.setEnabled(false);
        }
    }

    @Override
    public void onResult(Status status) {
        if (status.isSuccess()){
            mGeofenceAdded = !mGeofenceAdded;
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
    // @Override
  //  protected void onSaveInstanceState(Bundle savedInstanceState) {
    //    savedInstanceState.putParcelable("LOCATION-KEY", mLastLocation);
      //  super.onSaveInstanceState(savedInstanceState);
  //  }
}
