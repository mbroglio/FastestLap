package com.the_coffe_coders.fastestlap.adapter;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ConstructorStandingsRecyclerAdapter extends RecyclerView.Adapter<ConstructorStandingsRecyclerAdapter.ConstructorViewHolder> {
    @NonNull
    @Override
    public ConstructorStandingsRecyclerAdapter.ConstructorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull ConstructorStandingsRecyclerAdapter.ConstructorViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    public static class ConstructorViewHolder extends RecyclerView.ViewHolder {

        public ConstructorViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
