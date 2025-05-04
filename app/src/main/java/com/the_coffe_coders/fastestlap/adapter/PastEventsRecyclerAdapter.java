package com.the_coffe_coders.fastestlap.adapter;

import android.content.Context;
import android.content.Intent;
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
import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;
import com.the_coffe_coders.fastestlap.domain.grand_prix.RaceResult;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Track;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.TrackViewModel;
import com.the_coffe_coders.fastestlap.ui.event.EventActivity;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.UIUtils;

import org.threeten.bp.LocalDateTime;

import java.util.List;

public class PastEventsRecyclerAdapter extends RecyclerView.Adapter<PastEventsRecyclerAdapter.PastEventViewHolder>{

    private final List<Race> races;
    private final Context context;
    private final TrackViewModel trackViewModel;
    private final LifecycleOwner lifecycleOwner;
    private final LoadingScreen loadingScreen;
    private int counter;

    public PastEventsRecyclerAdapter(Context context, List<Race> races, TrackViewModel trackViewModel, LifecycleOwner lifecycleOwner, LoadingScreen loadingScreen) {
        this.context = context;
        this.races = races;
        this.trackViewModel = trackViewModel;
        this.lifecycleOwner = lifecycleOwner;
        this.loadingScreen = loadingScreen;
        this.counter = 1;
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

        trackViewModel.getTrack(race.getTrack().getTrackId()).observe(lifecycleOwner, result -> {
            if(result instanceof Result.Loading){
                return;
            }

            if(result.isSuccess()){
                Track track = ((Result.TrackSuccess) result).getData();

                LocalDateTime raceDateTime = race.getStartDateTime();

                UIUtils.multipleSetTextViewText(
                        new String[]{raceDateTime.getDayOfMonth() + "", raceDateTime.getMonth().toString().substring(0, 3)},

                        new TextView[]{holder.pastDateTextView, holder.pastMonthTextView});

                UIUtils.loadImageWithGlide(context, track.getTrack_minimal_layout_url(), holder.trackOutline, () -> {

                    UIUtils.multipleSetTextViewText(
                            new String[]{
                                    context.getString(R.string.round_upper_case_plus_value,race.getRound()),
                                    race.getRaceName()},

                            new TextView[]{
                                    holder.pastRoundTextView,
                                    holder.pastGPTextView});

                    generatePodium(holder, race);

                    holder.pastEventCard.setOnClickListener(v -> {
                        Intent intent = new Intent(context, EventActivity.class);
                        intent.putExtra("CIRCUIT_ID", race.getTrack().getTrackId());
                        context.startActivity(intent);
                    });
                });
            }
        });
    }

    private void generatePodium(@NonNull PastEventsRecyclerAdapter.PastEventViewHolder holder, Race race) {
        if(race.getResults() == null){
            setPendingPodium(holder);
        } else{
            for(int i = 0; i < 3; i++) {
                RaceResult raceResult = race.getResults().get(i);
                UIUtils.singleSetTextViewText(
                        raceResult.getDriver().getFullName(),
                        holder.pastEventCard.findViewById(Constants.PAST_RACE_DRIVER_NAME.get(i)));
            }
        }

        loadingScreen.updateProgress(counter * 100 / getItemCount());

        Log.i("PastEventsAdapter", "onBindViewHolder: " + counter + " / " + getItemCount());
        loadingScreen.hideLoadingScreenWithCondition(counter == getItemCount() - 1);

        counter++;
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

        MaterialCardView pastEventCard;
        TextView pastDateTextView, pastMonthTextView, pastRoundTextView, pastGPTextView;
        ImageView trackOutline;


        public PastEventViewHolder(@NonNull View itemView) {
            super(itemView);

            pastEventCard = itemView.findViewById(R.id.past_event_card_layout);
            pastDateTextView= itemView.findViewById(R.id.past_date);
            pastMonthTextView = itemView.findViewById(R.id.past_month);
            trackOutline = itemView.findViewById(R.id.past_track_outline);
            pastRoundTextView = itemView.findViewById(R.id.past_round_number);
            pastGPTextView = itemView.findViewById(R.id.past_gp_name);
        }
    }
}
