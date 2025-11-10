package com.the_coffe_coders.fastestlap.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

/**
 * LiveData\<Boolean\> that reports whether the device currently has a validated internet connection.
 * Observe this from UI (Activity/Fragment) to get reactive updates when connectivity changes.
 */

public class NetworkUtils extends LiveData<Boolean> {
    private final Context context;
    private final ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private BroadcastReceiver connectivityReceiver;

    public NetworkUtils(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.connectivityManager = (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    protected void onActive() {
        super.onActive();
        // Publish current state immediately
        postValue(isConnected());

        if (connectivityManager == null) return;

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                postValue(isConnected());
            }

            @Override
            public void onLost(@NonNull Network network) {
                postValue(isConnected());
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                postValue(isConnected());
            }
        };

        connectivityManager.registerDefaultNetworkCallback(networkCallback);
    }

    @Override
    protected void onInactive() {
        super.onInactive();
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) { }
            networkCallback = null;
        }
        if (connectivityReceiver != null) {
            try {
                context.unregisterReceiver(connectivityReceiver);
            } catch (Exception ignored) { }
            connectivityReceiver = null;
        }
    }

    public boolean isConnected() {
        if (connectivityManager == null) return false;

        Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities nc = connectivityManager.getNetworkCapabilities(network);
        if (nc == null) return false;

        boolean hasTransport = nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                || nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);

        boolean hasInternetCapability = nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        boolean isValidated = nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);

        Log.d("NetworkUtils","connection: " + hasTransport + " " + hasInternetCapability + " " + isValidated);

        return hasTransport && hasInternetCapability && isValidated;
    }


}