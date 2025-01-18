package com.the_coffe_coders.fastestlap.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Set;

//TODO Questa implementazione e quella di world news bisogna modificarla eventualmente con quello che dobbiamo fare noi
public class SharedPreferencesUtils {

    private final Context context;

    public SharedPreferencesUtils(Context context) {
        this.context = context;
    }

    public void writeStringData(String sharedPreferencesFileName, String key, String value) {
        SharedPreferences sharedPref = context.getSharedPreferences(sharedPreferencesFileName,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public String readStringData(String sharedPreferencesFileName, String key) {
        SharedPreferences sharedPref = context.getSharedPreferences(sharedPreferencesFileName,
                Context.MODE_PRIVATE);
        return sharedPref.getString(key, null);
    }
}