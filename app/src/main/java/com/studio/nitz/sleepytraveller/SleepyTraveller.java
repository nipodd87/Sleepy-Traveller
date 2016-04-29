package com.studio.nitz.sleepytraveller;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;

/**
 * Created by nitinpoddar on 4/29/16.
 */
public class SleepyTraveller extends Application {
    public static void showErrorDialog(String title, String message, Context mContext){
        new AlertDialog.Builder(mContext)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setIcon(R.drawable.icon_alert)
                .show();
    }
}
