package com.the_coffe_coders.fastestlap.adapter.f1;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.domain.f1.track.Track;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.TrackViewModel;
import com.the_coffe_coders.fastestlap.util.ui.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ui.NavigationUtils;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

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

        if(weeklyRace.getRound() != null){
            showEventConfirmed(holder, weeklyRace);
        } else {
            showEventCancelled(holder, weeklyRace);
        }

        trackViewModel.getTrack(weeklyRace.getTrack().getTrackId()).observe(lifecycleOwner, result -> {
            if (result instanceof Result.Loading) {
                return;
            }
            if (result.isSuccess()) {
                Track track = ((Result.TrackSuccess) result).getData();

                UIUtils.loadImageWithGlide(context, track.getTrack_minimal_layout_url(), holder.trackOutline, () -> {
                    Log.i("UpcomingEventsAdapter", "Image loaded for position: " + position);

                    if (position == getItemCount() - 1) {
                        loadingScreen.hideLoadingScreen();
                    }
                });

                holder.upcomingEventCard.setOnClickListener(v ->
                        NavigationUtils.navigateToEventPage(context, weeklyRace.getTrack().getTrackId()));
            } else {
                Log.e("UpcomingEventsAdapter", "Failed to load track for position: " + position);

                if (position == getItemCount() - 1) {
                    loadingScreen.hideLoadingScreen();
                }
            }
        });
    }

    private void showEventCancelled(UpcomingEventViewHolder holder, WeeklyRace weeklyRace) {
        holder.eventConfirmedLayout.setVisibility(View.GONE);
        holder.eventCancelledLayout.setVisibility(View.VISIBLE);

        UIUtils.singleSetTextViewText(weeklyRace.getRaceName(), holder.gpTextViewCancelled);
    }

    private void showEventConfirmed(UpcomingEventViewHolder holder, WeeklyRace weeklyRace) {
        holder.eventConfirmedLayout.setVisibility(View.VISIBLE);
        holder.eventCancelledLayout.setVisibility(View.GONE);

        UIUtils.multipleSetTextViewText(
                new String[]{
                        context.getString(R.string.round_plus_value, weeklyRace.getRound()),
                        weeklyRace.getRaceName(),
                        weeklyRace.getFirstPractice().getStartDateTime().getDayOfMonth() + " - " + weeklyRace.getDateTime().getDayOfMonth()},
                new TextView[]{
                        holder.roundTextView,
                        holder.gpTextViewConfirmed,
                        holder.dateTextView});

        UIUtils.translateMonth(weeklyRace.getDateTime().getMonth().toString().substring(0, 3).toUpperCase(),
                holder.monthTextView, true);
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
        final ImageView trackOutline, accessEventIcon, liveEventIcon;
        final TextView roundTextView, gpTextViewConfirmed, dateTextView, monthTextView, gpTextViewCancelled;
        final FrameLayout eventInfoLayout;
        final RelativeLayout liveEventIconLayout, eventConfirmedLayout, eventCancelledLayout;

        public UpcomingEventViewHolder(@NonNull View itemView) {
            super(itemView);

            upcomingEventCard = itemView.findViewById(R.id.upcoming_events_card_layout);
            trackOutline = itemView.findViewById(R.id.upcoming_track_outline);
            roundTextView = itemView.findViewById(R.id.upcoming_round_number);
            gpTextViewConfirmed = itemView.findViewById(R.id.upcoming_gp_name);
            dateTextView = itemView.findViewById(R.id.upcoming_date);
            monthTextView = itemView.findViewById(R.id.upcoming_month);
            accessEventIcon = itemView.findViewById(R.id.access_event_icon);
            liveEventIconLayout = itemView.findViewById(R.id.live_event_icon_layout);
            liveEventIcon = itemView.findViewById(R.id.upcoming_event_icon);
            eventInfoLayout = itemView.findViewById(R.id.event_info_layout);
            gpTextViewCancelled = itemView.findViewById(R.id.upcoming_gp_name_cancelled);
            eventConfirmedLayout = itemView.findViewById(R.id.eventConfirmed);
            eventCancelledLayout = itemView.findViewById(R.id.eventCancelled);
        }
    }
}
