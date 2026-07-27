package com.the_coffe_coders.fastestlap.adapter.f1;

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

    private boolean[] loadedPositions;
    private int targetLoadCount = 0;
    private int currentLoadedCount = 0;

    public UpcomingEventsRecyclerAdapter(Context context, List<WeeklyRace> races, TrackViewModel trackViewModel, LifecycleOwner lifecycleOwner, LoadingScreen loadingScreen) {
        this.context = context;
        this.races = races;
        this.trackViewModel = trackViewModel;
        this.lifecycleOwner = lifecycleOwner;
        this.loadingScreen = loadingScreen;
        updateTargetLoadCount();
    }

    public void updateTargetLoadCount() {
        synchronized (this) {
            this.targetLoadCount = getItemCount();
            this.loadedPositions = new boolean[getItemCount()];
            this.currentLoadedCount = 0;
        }
        preloadAllItems();
    }

    private void preloadAllItems() {
        if (races == null || races.isEmpty()) {
            return;
        }
        for (int i = 0; i < races.size(); i++) {
            final int pos = i;
            WeeklyRace weeklyRace = races.get(pos);
            if (weeklyRace.getTrack() != null && weeklyRace.getTrack().getTrack_minimal_layout_url() != null) {
                UIUtils.preloadImage(context, weeklyRace.getTrack().getTrack_minimal_layout_url(), () -> endLoading(pos));
            } else {
                androidx.lifecycle.LiveData<Result> trackLd = trackViewModel.getTrack(weeklyRace.getTrack().getTrackId());
                @SuppressWarnings("unchecked")
                androidx.lifecycle.Observer<Result>[] selfRef = new androidx.lifecycle.Observer[1];
                selfRef[0] = result -> {
                    if (result instanceof Result.Loading) {
                        return;
                    }
                    trackLd.removeObserver(selfRef[0]);
                    if (result.isSuccess()) {
                        Track track = ((Result.TrackSuccess) result).getData();
                        weeklyRace.setTrack(track);
                        UIUtils.preloadImage(context, track.getTrack_minimal_layout_url(), () -> endLoading(pos));
                    } else {
                        endLoading(pos);
                    }
                };
                trackLd.observe(lifecycleOwner, selfRef[0]);
            }
        }
    }

    private void endLoading(int position) {
        boolean shouldHide = false;
        synchronized (this) {
            if (targetLoadCount > 0 && position >= 0 && position < loadedPositions.length && !loadedPositions[position]) {
                loadedPositions[position] = true;
                currentLoadedCount++;
                Log.i("UpcomingEventsAdapter", "Item " + position + " loaded (" + currentLoadedCount + "/" + targetLoadCount + ")");
                if (currentLoadedCount >= targetLoadCount) {
                    shouldHide = true;
                }
            }
        }
        if (shouldHide && loadingScreen != null) {
            loadingScreen.hideLoadingScreen();
        }
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

        UIUtils.multipleSetTextViewText(
                new String[]{
                        context.getString(R.string.round_plus_value, weeklyRace.getRound()),
                        weeklyRace.getRaceName(),
                        weeklyRace.getFirstPractice().getStartDateTime().getDayOfMonth() + " - " + weeklyRace.getDateTime().getDayOfMonth()},
                new TextView[]{
                        holder.roundTextView,
                        holder.gpTextView,
                        holder.dateTextView});

        UIUtils.translateMonth(weeklyRace.getDateTime().getMonth().toString().substring(0, 3).toUpperCase(java.util.Locale.ROOT),
                holder.monthTextView, true);

        // Carica il track subito se già disponibile (da preloadAllItems) oppure osserva il LiveData
        if (weeklyRace.getTrack() != null && weeklyRace.getTrack().getTrack_minimal_layout_url() != null) {
            UIUtils.loadImageWithGlide(context, weeklyRace.getTrack().getTrack_minimal_layout_url(), holder.trackOutline, () -> endLoading(position));
            holder.upcomingEventCard.setOnClickListener(v ->
                    NavigationUtils.navigateToEventPage(context, weeklyRace.getTrack().getTrackId()));
        } else {
            androidx.lifecycle.LiveData<Result> trackLd = trackViewModel.getTrack(weeklyRace.getTrack().getTrackId());
            @SuppressWarnings("unchecked")
            androidx.lifecycle.Observer<Result>[] selfRef = new androidx.lifecycle.Observer[1];
            selfRef[0] = result -> {
                if (result instanceof Result.Loading) {
                    return;
                }
                trackLd.removeObserver(selfRef[0]);
                if (result.isSuccess()) {
                    Track track = ((Result.TrackSuccess) result).getData();
                    weeklyRace.setTrack(track);
                    UIUtils.loadImageWithGlide(context, track.getTrack_minimal_layout_url(), holder.trackOutline, () -> endLoading(position));
                    holder.upcomingEventCard.setOnClickListener(v ->
                            NavigationUtils.navigateToEventPage(context, weeklyRace.getTrack().getTrackId()));
                } else {
                    Log.e("UpcomingEventsAdapter", "Failed to load track for position: " + position);
                    endLoading(position);
                }
            };
            trackLd.observe(lifecycleOwner, selfRef[0]);
        }
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
