package com.malikali.compassapp;

import android.content.Context;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.malikali.compassapp.Service.ConnectivityReceiver;

public class Application extends android.app.Application implements LifecycleObserver {

    private static Application multiDexApplication;
    public static Context context;

    public Application(){
        multiDexApplication=this;
    }

    public static Application getMultiDexApplication() {
        return multiDexApplication;
    }
    private static Context currentActivity = null;


    private static Application sInstance = null;


    public static boolean wasInBackground;
    public  static ConnectivityReceiver connectivityReceiver;


    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        sInstance = this;
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);


    }
    public static Context getCurrentActivity() {
        return currentActivity;
    }

    public void setCurrentActivity(Context currentActivity) {
        this.currentActivity = currentActivity;
    }

    public static Application getInstance(){
        return sInstance;
    }
    public static Context getAppContext() {
        return context;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onMoveToForeground() {

        wasInBackground=true;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onMoveToBackground() {

        wasInBackground =false;
    }

    public void setConnectivityListener(ConnectivityReceiver.ConnectivityReceiverListener listener){
        ConnectivityReceiver.connectivityReceiverListener = listener;


    }


    public void unregisterReceiver() {
        unregisterReceiver(connectivityReceiver);//your broadcast
    }
}

