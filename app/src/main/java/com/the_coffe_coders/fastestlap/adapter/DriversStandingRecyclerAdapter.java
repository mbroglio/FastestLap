package com.the_coffe_coders.fastestlap.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandingsElement;
import com.the_coffe_coders.fastestlap.util.LoadingScreen;

import java.util.List;

public class DriversStandingRecyclerAdapter extends RecyclerView.Adapter<DriversStandingRecyclerAdapter.DriverViewHolder>{

    private final Context context;
    private final List<DriverStandingsElement> driversStandingList;
    private final List<Driver> driversList;
    private final String driverId;
    private final LoadingScreen loadingScreen;
    private int counter;

    public DriversStandingRecyclerAdapter(Context context, List<DriverStandingsElement> driversStandingList, List<Driver> driversList, String driverId, LoadingScreen loadingScreen) {
        this.context = context;
        this.driversStandingList = driversStandingList;
        this.driversList = driversList;
        this.driverId = driverId;
        this.loadingScreen = loadingScreen;
        this.counter = 0;
    }

    @NonNull
    @Override
    public DriversStandingRecyclerAdapter.DriverViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull DriversStandingRecyclerAdapter.DriverViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        if(driversList != null){
            return driversList.size();
        }else if(driversStandingList != null){
            return driversStandingList.size();
        }else{
            return 0;
        }
    }

    public static class DriverViewHolder extends RecyclerView.ViewHolder {

        public DriverViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
