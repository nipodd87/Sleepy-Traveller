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
import com.google.android.gms.location.Geofence;
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
        , LocationListener {

    private EditText addressTxt;
    private String addressInput;
    private Button btnStart;
    private static final String TAG = "com.studio.nitz.sleepytraveller";
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private GoogleMap map;
    private GeocodeReceiver geocodeReceiver = null;
    private boolean mGeofenceAdded;
    private boolean mGeofenceStarted;
    private boolean mGeofenceSearched;
    private SharedPreferences mSharedPreferences;
    private LocationRequest mLocationRequest;
    protected ArrayList<Geofence> mGeofenceList;
    private PendingIntent mGeofencePendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mGeofenceList = new ArrayList<Geofence>();
        mGeofencePendingIntent=null;
        btnStart = (Button) findViewById(R.id.btnStart);
        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        mGeofenceAdded = mSharedPreferences.getBoolean(Constants.GEOFENCE_ADDED_KEY, false);
        setButtonEnabledState();
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

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains("LOCATION-KEY")) {
                mLastLocation = savedInstanceState.getParcelable("LOCATION-KEY");
            }
        }
    }

    private void setButtonEnabledState() {
        if (mGeofenceAdded) {
            btnStart.setText("STOP");
        } else if (!mGeofenceAdded) {
            btnStart.setText("START");
        }
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
        mGeofenceStarted = false;
        mGeofenceSearched = true;
        map.clear();
        addressInput = addressTxt.getText().toString();
        if (addressInput.length() == 0) {
            Toast.makeText(this, "Enter valid Address to search", Toast.LENGTH_LONG).show();
        } else {
            startGeocodingIntent(addressInput);
        }
    }

    public void startGeofenceTrack(View view) {
        mGeofenceStarted = true;
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

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

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
                .build());
    }

    @Override
    public void onLocationChanged(Location location) {
        if (mGeofenceSearched && !mGeofenceStarted){
            //do nothing
        } else{
            updateUI(location);
        }

    }

    protected void updateUI(Location location){
//        mLastLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
        map.clear();
        map.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_current_location)));
        map.animateCamera(cameraUpdate);
    }

   // @Override
  //  protected void onSaveInstanceState(Bundle savedInstanceState) {
    //    savedInstanceState.putParcelable("LOCATION-KEY", mLastLocation);
      //  super.onSaveInstanceState(savedInstanceState);
  //  }
}
