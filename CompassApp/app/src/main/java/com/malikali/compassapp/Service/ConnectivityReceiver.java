package com.malikali.compassapp.Service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.malikali.compassapp.Application;

public class ConnectivityReceiver extends BroadcastReceiver {

    public static ConnectivityReceiverListener connectivityReceiverListener;

    public ConnectivityReceiver(){
        super();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);


        NetworkInfo activeNetwork  = null;
        if (cm != null) {
            activeNetwork = cm.getActiveNetworkInfo();
        }

        boolean isConnected = false;
        if (activeNetwork != null) {
            isConnected = activeNetwork.isConnected() && activeNetwork.isConnectedOrConnecting();
        }
        //  if (connectivityReceiverListener!=null){
        connectivityReceiverListener.onNetworkConnectionChanged(isConnected);
        // }



    }

    public static boolean isConnected(){
        ConnectivityManager cm = (ConnectivityManager) Application.getInstance()
                .getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork  = null;
        if (cm != null) {
            activeNetwork = cm.getActiveNetworkInfo();

        }
        if (activeNetwork != null) {
            return activeNetwork.isConnected() && activeNetwork.isConnectedOrConnecting();


        }
        return false;
    }

    public interface ConnectivityReceiverListener{
        void onNetworkConnectionChanged(boolean isConnected);
    }
}

