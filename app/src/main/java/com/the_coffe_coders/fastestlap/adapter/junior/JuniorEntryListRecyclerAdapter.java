package com.the_coffe_coders.fastestlap.adapter.junior;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.junior.JuniorEntryList;

public class JuniorEntryListRecyclerAdapter extends RecyclerView.Adapter<JuniorEntryListRecyclerAdapter.JuniorEntryListViewHolder> {

    private final Context context;
    private final JuniorEntryList juniorEntryList;

    public JuniorEntryListRecyclerAdapter(Context context, JuniorEntryList juniorEntryList) {
        this.context = context;
        this.juniorEntryList = juniorEntryList;
    }


    @NonNull
    @Override
    public JuniorEntryListRecyclerAdapter.JuniorEntryListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.junior_standings_card, parent, false);
        return new JuniorEntryListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JuniorEntryListRecyclerAdapter.JuniorEntryListViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    public static class JuniorEntryListViewHolder extends RecyclerView.ViewHolder {

        public JuniorEntryListViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
