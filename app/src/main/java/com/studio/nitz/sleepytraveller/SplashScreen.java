package com.studio.nitz.sleepytraveller;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by nitinpoddar on 4/29/16.
 */
public class SplashScreen extends Activity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        Thread timer= new Thread()
        {
            public void run()
            {
                try
                {
                    sleep(700);
                }
                catch (InterruptedException e)
                {
                }
                finally
                {
                    Intent intent = new Intent(getBaseContext(), MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        };
        timer.start();
    }
}
