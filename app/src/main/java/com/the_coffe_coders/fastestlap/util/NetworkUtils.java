package com.the_coffe_coders.fastestlap.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

/**
 * Utility class to check network connectivity status
 */
public class NetworkUtils {

    /**
     * Checks if the device has an active internet connection
     *
     * @param context Application context
     * @return boolean True if device has internet connection, false otherwise
     */
    public static boolean isNetworkAvailable(Context context) {
        if (context == null) return false;

        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return false;

        // For Android 10 (API 29) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;

            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        }
        // For Android 9 (API 28) and below
        else {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
    }


}