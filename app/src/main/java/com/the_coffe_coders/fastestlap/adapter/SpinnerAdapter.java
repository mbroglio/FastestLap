package com.the_coffe_coders.fastestlap.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.utils.Constants;

public class SpinnerAdapter extends ArrayAdapter<String> {

    private final Context context;
    private final String[] keys;
    private final boolean isDriver;

    public SpinnerAdapter(Context context, String[] keys, boolean isDriver) {
        super(context, R.layout.spinner_item, keys);
        this.context = context;
        this.keys = keys;
        this.isDriver = isDriver;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.spinner_item, parent, false);
        }

        TextView textView = convertView.findViewById(R.id.spinner_item_text);
        ImageView imageView = convertView.findViewById(R.id.spinner_item_image);

        String key = keys[position];
        if (isDriver) {
            textView.setText(context.getString(Constants.DRIVER_FULLNAME.get(key)));
            imageView.setImageResource(Constants.DRIVER_IMAGE.get(key));
        } else {
            textView.setText(context.getString(Constants.TEAM_FULLNAME.get(key)));
            imageView.setImageResource(Constants.TEAM_LOGO_DRIVER_CARD.get(key));
        }

        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent);
    }
}