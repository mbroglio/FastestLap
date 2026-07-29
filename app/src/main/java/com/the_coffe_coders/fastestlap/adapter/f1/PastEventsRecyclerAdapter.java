package com.the_coffe_coders.fastestlap.adapter.f1;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Race;
import com.the_coffe_coders.fastestlap.domain.f1.result.RaceResult;
import com.the_coffe_coders.fastestlap.domain.f1.track.Track;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.TrackViewModel;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.ui.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ui.NavigationUtils;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import org.threeten.bp.LocalDateTime;

import java.util.List;

public class PastEventsRecyclerAdapter extends RecyclerView.Adapter<PastEventsRecyclerAdapter.PastEventViewHolder> {

    private final List<Race> races;
    private final Context context;
    private final TrackViewModel trackViewModel;
    private final LifecycleOwner lifecycleOwner;
    private final LoadingScreen loadingScreen;

    private boolean[] loadedPositions;
    private int targetLoadCount = 0;
    private int currentLoadedCount = 0;

    public PastEventsRecyclerAdapter(Context context, List<Race> races, TrackViewModel trackViewModel, LifecycleOwner lifecycleOwner, LoadingScreen loadingScreen) {
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
            Race race = races.get(pos);
            if (race.getTrack() != null && race.getTrack().getTrack_minimal_layout_url() != null) {
                UIUtils.preloadImage(context, race.getTrack().getTrack_minimal_layout_url(), () -> endLoading(pos));
            } else {
                androidx.lifecycle.LiveData<Result> trackLd = trackViewModel.getTrack(race.getTrack().getTrackId());
                @SuppressWarnings("unchecked")
                androidx.lifecycle.Observer<Result>[] selfRef = new androidx.lifecycle.Observer[1];
                selfRef[0] = result -> {
                    if (result instanceof Result.Loading) {
                        return;
                    }
                    trackLd.removeObserver(selfRef[0]);
                    if (result.isSuccess()) {
                        Track track = ((Result.TrackSuccess) result).getData();
                        race.setTrack(track);
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
                Log.i("PastEventsAdapter", "Item " + position + " loaded (" + currentLoadedCount + "/" + targetLoadCount + ")");
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
    public PastEventsRecyclerAdapter.PastEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.past_event_card, parent, false);
        return new PastEventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PastEventsRecyclerAdapter.PastEventViewHolder holder, int position) {
        Race race = races.get(position);
        Log.i("PastEventsAdapter", "onBindViewHolder: " + race);

        // Imposta subito i dati di base che sono già disponibili
        LocalDateTime raceDateTime = race.getStartDateTime();
        UIUtils.multipleSetTextViewText(
                new String[]{raceDateTime.getDayOfMonth() + "", raceDateTime.getMonth().toString().substring(0, 3)},
                new TextView[]{holder.pastDateTextView, holder.pastMonthTextView});

        UIUtils.multipleSetTextViewText(
                new String[]{
                        context.getString(R.string.round_plus_value, race.getRound()),
                        race.getRaceName()},
                new TextView[]{
                        holder.pastRoundTextView,
                        holder.pastGPTextView});

        // Prepara il podium subito
        generatePodium(holder, race, position);

        // Carica il track subito se già disponibile (da preloadAllItems) oppure osserva il LiveData
        if (race.getTrack() != null && race.getTrack().getTrack_minimal_layout_url() != null) {
            UIUtils.loadImageWithGlide(context, race.getTrack().getTrack_minimal_layout_url(), holder.trackOutline, () -> endLoading(position));
            holder.pastEventCard.setOnClickListener(v ->
                    NavigationUtils.navigateToEventPage(context, race.getTrack().getTrackId()));
        } else {
            androidx.lifecycle.LiveData<Result> trackLd = trackViewModel.getTrack(race.getTrack().getTrackId());
            @SuppressWarnings("unchecked")
            androidx.lifecycle.Observer<Result>[] selfRef = new androidx.lifecycle.Observer[1];
            selfRef[0] = result -> {
                if (result instanceof Result.Loading) {
                    return;
                }
                trackLd.removeObserver(selfRef[0]);
                if (result.isSuccess()) {
                    Track track = ((Result.TrackSuccess) result).getData();
                    race.setTrack(track);
                    UIUtils.loadImageWithGlide(context, track.getTrack_minimal_layout_url(), holder.trackOutline, () -> endLoading(position));
                    holder.pastEventCard.setOnClickListener(v ->
                            NavigationUtils.navigateToEventPage(context, race.getTrack().getTrackId()));
                } else {
                    Log.e("PastEventsAdapter", "Failed to load track for position: " + position);
                    endLoading(position);
                }
            };
            trackLd.observe(lifecycleOwner, selfRef[0]);
        }
    }

    private void generatePodium(@NonNull PastEventsRecyclerAdapter.PastEventViewHolder holder, Race race, int position) {

        if (race.getResults() == null || race.getResults().isEmpty()) {
            setPendingPodium(holder);
        } else {
            for (int i = 0; i < 3 && i < race.getResults().size(); i++) {
                RaceResult raceResult = race.getResults().get(i);
                UIUtils.singleSetTextViewText(
                        raceResult.getDriver().getFullName(),
                        holder.pastEventCard.findViewById(Constants.PAST_RACE_DRIVER_NAME.get(i)));
            }
        }
    }

    private void setPendingPodium(@NonNull PastEventViewHolder holder) {
        View pendingResults = holder.pastEventCard.findViewById(R.id.pending_results_text);
        View podium = holder.pastEventCard.findViewById(R.id.race_podium);
        View arrow = holder.pastEventCard.findViewById(R.id.past_event_card_arrow);

        pendingResults.setVisibility(View.VISIBLE);
        podium.setVisibility(View.GONE);
        arrow.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return races.size();
    }

    public static class PastEventViewHolder extends RecyclerView.ViewHolder {

        final MaterialCardView pastEventCard;
        final TextView pastDateTextView;
        final TextView pastMonthTextView;
        final TextView pastRoundTextView;
        final TextView pastGPTextView;
        final ImageView trackOutline;

        public PastEventViewHolder(@NonNull View itemView) {
            super(itemView);

            pastEventCard = itemView.findViewById(R.id.past_event_card_layout);
            pastDateTextView = itemView.findViewById(R.id.past_date);
            pastMonthTextView = itemView.findViewById(R.id.past_month);
            trackOutline = itemView.findViewById(R.id.past_track_outline);
            pastRoundTextView = itemView.findViewById(R.id.past_round_number);
            pastGPTextView = itemView.findViewById(R.id.past_gp_name);
        }
    }
}