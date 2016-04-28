package com.studio.nitz.sleepytraveller;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

/**
 * Created by nitinpoddar on 4/26/16.
 */
public class GeocodeReceiver extends ResultReceiver {

    private Receiver receiver;
    public GeocodeReceiver(Handler handler) {
        super(handler);
    }
    public interface Receiver{
        public void onReceiveResult(int code, Bundle data);
    }
    public void setReceiver(Receiver r){
        receiver=r;
    }

    protected void onReceiveResult(int resultCode, Bundle resultData){
        if(receiver!=null){
            receiver.onReceiveResult(resultCode, resultData);
        }
    }
}
