package com.studio.nitz.sleepytraveller;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.ResultReceiver;
import java.io.IOException;
import java.util.List;

/**
 * Created by nitinpoddar on 4/26/16.
 */
public class GeocoderIntentService extends IntentService {

    public GeocoderIntentService(){
        super("Fetch Address");
    }
    public GeocoderIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Geocoder geocoder=new Geocoder(this);
        Bundle bundle=intent.getBundleExtra("bundle");
        ResultReceiver resultReceiver=bundle.getParcelable("receiver");
        String address=bundle.getString("address");
        List<Address> listAddress;

        try {
            listAddress=geocoder.getFromLocationName(address,1);
            if (listAddress==null || listAddress.size() == 0){
                resultReceiver.send(0, null);
            } else {
                Address location = listAddress.get(0);
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                Bundle resultBundle = new Bundle();
                resultBundle.putDouble("latitude", latitude);
                resultBundle.putDouble("longitude", longitude);
                resultReceiver.send(1, resultBundle);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
