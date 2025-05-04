package com.the_coffe_coders.fastestlap.adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Track;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.TrackViewModel;
import com.the_coffe_coders.fastestlap.ui.event.EventActivity;
import com.the_coffe_coders.fastestlap.util.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.UIUtils;

import java.util.List;

public class UpcomingEventsRecyclerAdapter extends RecyclerView.Adapter<UpcomingEventsRecyclerAdapter.UpcomingEventViewHolder>{

    private final List<WeeklyRace> races;
    private final Context context;
    private final TrackViewModel trackViewModel;
    private final LifecycleOwner lifecycleOwner;
    private final LoadingScreen loadingScreen;
    private View eventCard;
    private int counter;

    public UpcomingEventsRecyclerAdapter(Context context, List<WeeklyRace> races, TrackViewModel trackViewModel, LifecycleOwner lifecycleOwner, LoadingScreen loadingScreen) {
        this.context = context;
        this.races = races;
        this.trackViewModel = trackViewModel;
        this.lifecycleOwner = lifecycleOwner;
        this.loadingScreen = loadingScreen;
        this.counter = 0;
    }

    @NonNull
    @Override
    public UpcomingEventsRecyclerAdapter.UpcomingEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if (viewType == 1) {
            eventCard = LayoutInflater.from(parent.getContext()).inflate(R.layout.upcoming_event_live_card, parent, false);

            ImageView liveIcon = eventCard.findViewById(R.id.upcoming_event_icon);
            Animation pulse = AnimationUtils.loadAnimation(parent.getContext(), R.anim.pulse_dynamic);
            liveIcon.startAnimation(pulse);
        } else if (viewType == 0) {
            eventCard = LayoutInflater.from(parent.getContext()).inflate(R.layout.upcoming_event_card, parent, false);
        }

        return new UpcomingEventViewHolder(eventCard);

    }

    @Override
    public void onBindViewHolder(@NonNull UpcomingEventsRecyclerAdapter.UpcomingEventViewHolder holder, int position) {
        WeeklyRace weeklyRace = races.get(position);
        Log.i("UpcomingEventsAdapter", "onBindViewHolder: " + weeklyRace);

        trackViewModel.getTrack(weeklyRace.getTrack().getTrackId()).observe(lifecycleOwner, result -> {
            if(result instanceof Result.Loading) {
                return;
            }
            if(result.isSuccess()){
                Track track = ((Result.TrackSuccess) result).getData();

                UIUtils.loadImageWithGlide(context, track.getTrack_minimal_layout_url(), holder.trackOutline, () -> {

                    UIUtils.multipleSetTextViewText(
                            new String[]{
                                    context.getString(R.string.round_upper_case_plus_value, weeklyRace.getRound()),
                                    weeklyRace.getRaceName(),
                                    weeklyRace.getFirstPractice().getStartDateTime().getDayOfMonth() + " - " + weeklyRace.getDateTime().getDayOfMonth()},

                            new TextView[]{
                                    holder.roundTextView,
                                    holder.gpTextView,
                                    holder.dateTextView});

                    UIUtils.translateMonth(weeklyRace.getDateTime().getMonth().toString().substring(0, 3).toUpperCase(),
                            holder.monthTextView);

                    eventCard.setOnClickListener(v -> {
                        Intent intent = new Intent(context, EventActivity.class);
                        intent.putExtra("CIRCUIT_ID", weeklyRace.getTrack().getTrackId());
                        context.startActivity(intent);
                    });

                    counter++;
                    Log.i("UpcomingEvent", "onBindViewHolder " + counter + "/" + getItemCount());
                    loadingScreen.hideLoadingScreenWithCondition(counter == getItemCount() - 1);
                });

            }
        });
    }

    @Override
    public int getItemViewType(int position) {
        return races.get(position).isUnderway(true) ? 1 : 0;
    }

    @Override
    public int getItemCount() {
        return races.size();
    }

    public static class UpcomingEventViewHolder extends RecyclerView.ViewHolder {

        ImageView trackOutline;
        TextView roundTextView, gpTextView, dateTextView, monthTextView;

        public UpcomingEventViewHolder(@NonNull View itemView) {
            super(itemView);

            trackOutline = itemView.findViewById(R.id.upcoming_track_outline);
            roundTextView = itemView.findViewById(R.id.upcoming_round_number);
            gpTextView = itemView.findViewById(R.id.upcoming_gp_name);
            dateTextView = itemView.findViewById(R.id.upcoming_date);
            monthTextView = itemView.findViewById(R.id.upcoming_month);
        }
    }
}
