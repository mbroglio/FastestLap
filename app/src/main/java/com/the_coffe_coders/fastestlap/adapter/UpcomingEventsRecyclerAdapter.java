package com.the_coffe_coders.fastestlap.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Track;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.TrackViewModel;
import com.the_coffe_coders.fastestlap.util.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.UIUtils;

import java.util.List;

public class UpcomingEventsRecyclerAdapter extends RecyclerView.Adapter<UpcomingEventsRecyclerAdapter.UpcomingEventViewHolder> {

    private final List<WeeklyRace> races;
    private final Context context;
    private final TrackViewModel trackViewModel;
    private final LifecycleOwner lifecycleOwner;
    private final LoadingScreen loadingScreen;

    public UpcomingEventsRecyclerAdapter(Context context, List<WeeklyRace> races, TrackViewModel trackViewModel, LifecycleOwner lifecycleOwner, LoadingScreen loadingScreen) {
        this.context = context;
        this.races = races;
        this.trackViewModel = trackViewModel;
        this.lifecycleOwner = lifecycleOwner;
        this.loadingScreen = loadingScreen;
    }

    @NonNull
    @Override
    public UpcomingEventsRecyclerAdapter.UpcomingEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.upcoming_event_card, parent, false);

        return new UpcomingEventViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull UpcomingEventsRecyclerAdapter.UpcomingEventViewHolder holder, int position) {
        WeeklyRace weeklyRace = races.get(position);
        Log.i("UpcomingEventsAdapter", "onBindViewHolder: " + weeklyRace);

        setupEventCardIcon(weeklyRace, holder);

        holder.roundTextView.setText(context.getString(R.string.round_upper_case_plus_value, weeklyRace.getRound()));
        holder.gpTextView.setText(weeklyRace.getRaceName());
        holder.dateTextView.setText(weeklyRace.getFirstPractice().getStartDateTime().getDayOfMonth() + " - " + weeklyRace.getDateTime().getDayOfMonth());
        UIUtils.translateMonth(weeklyRace.getDateTime().getMonth().toString().substring(0, 3).toUpperCase(),
                holder.monthTextView, true);

        trackViewModel.getTrack(weeklyRace.getTrack().getTrackId()).observe(lifecycleOwner, result -> {
            if (result instanceof Result.Loading) {
                return;
            }
            if (result.isSuccess()) {
                Track track = ((Result.TrackSuccess) result).getData();

                // Carica l'immagine solo se il track è disponibile
                UIUtils.loadImageWithGlide(context, track.getTrack_minimal_layout_url(), holder.trackOutline, () -> {
                    loadingScreen.updateProgress();
                    Log.i("UpcomingEventsAdapter", "Image loaded for position: " + position);
                    loadingScreen.hideLoadingScreenWithCondition(position == getItemCount() - 1);
                });

                holder.upcomingEventCard.setOnClickListener(v ->
                        UIUtils.navigateToEventPage(context, weeklyRace.getTrack().getTrackId()));
            } else {
                // Gestisci il caso di errore
                Log.e("UpcomingEventsAdapter", "Failed to load track for position: " + position);
                loadingScreen.updateProgress();
                loadingScreen.hideLoadingScreenWithCondition(position == getItemCount() - 1);
            }
        });
    }

    private void setupEventCardIcon(WeeklyRace weeklyRace, UpcomingEventViewHolder holder) {

        if (weeklyRace.isUnderway(true)) {
            holder.accessEventIcon.setVisibility(View.GONE);
            holder.liveEventIconLayout.setVisibility(View.VISIBLE);

            Animation pulse = AnimationUtils.loadAnimation(context, R.anim.pulse_dynamic);
            holder.liveEventIcon.startAnimation(pulse);
        } else {
            holder.accessEventIcon.setVisibility(View.VISIBLE);
            holder.liveEventIconLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return races.size();
    }

    public static class UpcomingEventViewHolder extends RecyclerView.ViewHolder {

        final MaterialCardView upcomingEventCard;
        final ImageView trackOutline;
        final ImageView accessEventIcon;
        final ImageView liveEventIcon;
        final TextView roundTextView;
        final TextView gpTextView;
        final TextView dateTextView;
        final TextView monthTextView;
        final RelativeLayout liveEventIconLayout;

        public UpcomingEventViewHolder(@NonNull View itemView) {
            super(itemView);

            upcomingEventCard = itemView.findViewById(R.id.upcoming_events_card_layout);
            trackOutline = itemView.findViewById(R.id.upcoming_track_outline);
            roundTextView = itemView.findViewById(R.id.upcoming_round_number);
            gpTextView = itemView.findViewById(R.id.upcoming_gp_name);
            dateTextView = itemView.findViewById(R.id.upcoming_date);
            monthTextView = itemView.findViewById(R.id.upcoming_month);
            accessEventIcon = itemView.findViewById(R.id.access_event_icon);
            liveEventIconLayout = itemView.findViewById(R.id.live_event_icon_layout);
            liveEventIcon = itemView.findViewById(R.id.upcoming_event_icon);
        }
    }
}
