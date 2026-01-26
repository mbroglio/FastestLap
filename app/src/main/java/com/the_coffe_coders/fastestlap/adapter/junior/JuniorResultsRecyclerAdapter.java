package com.the_coffe_coders.fastestlap.adapter.junior;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.junior.result.JuniorResult;
import com.the_coffe_coders.fastestlap.domain.junior.result.JuniorResultElement;
import com.the_coffe_coders.fastestlap.domain.junior.result.JuniorSessionResultElement;
import com.the_coffe_coders.fastestlap.util.ui.NavigationUtils;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import java.util.List;

public class JuniorResultsRecyclerAdapter extends RecyclerView.Adapter<JuniorResultsRecyclerAdapter.JuniorResultsViewHolder>{
    private static final String TAG = "JuniorResultsRecyclerAdapter";

    private final Context context;
    private final JuniorResult juniorResult;
    private final FragmentManager fragmentManager;
    private final int categoryType;


    public JuniorResultsRecyclerAdapter(Context context, JuniorResult juniorResult, FragmentManager fragmentManager, int categoryType) {
        this.context = context;
        this.juniorResult = juniorResult;
        this.fragmentManager = fragmentManager;
        this.categoryType = categoryType;
    }

    @NonNull
    @Override
    public JuniorResultsRecyclerAdapter.JuniorResultsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.junior_event_result_card, parent, false);
        return new JuniorResultsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JuniorResultsRecyclerAdapter.JuniorResultsViewHolder holder, int position) {
        JuniorResultElement element = juniorResult.getResults().get(position);

        UIUtils.singleSetTextViewText(context.getString(R.string.round_upper_case_plus_value, Integer.toString(element.getRound())), holder.roundNumber);
        UIUtils.singleSetTextViewText(element.getCircuit(), holder.gpName);

        UIUtils.loadImageWithGlide(context, element.getNationFlagUrl(), holder.eventNationFlag,
                () -> setResults(holder, element));

    }

    private void setResults(JuniorResultsViewHolder holder, JuniorResultElement element) {
        if (element.getFeature() != null && element.getFeature().getOrder() != null &&
                !element.getFeature().getOrder().isEmpty()) {
            showFeatureResults(holder);
            List<JuniorSessionResultElement> podium = element.getFeature().getPodium();

            UIUtils.multipleSetTextViewText(
                    new String[]{
                            podium.get(0).getDriver(),
                            podium.get(1).getDriver(),
                            podium.get(2).getDriver()
                    },
                    new TextView[]{
                            holder.firstDriverFeature,
                            holder.secondDriverFeature,
                            holder.thirdDriverFeature});

            holder.featureRaceLayout.setOnClickListener(v -> NavigationUtils.showFullResultsDialog(juniorResult,
                    fragmentManager, categoryType, 0));
        }else{
            showFeatureCancelled(holder);
        }

        setSprintResults(holder, element);
    }

    private void setSprintResults(JuniorResultsViewHolder holder, JuniorResultElement element) {
        if(element.getSprint() != null && element.getSprint().getOrder() != null &&
                !element.getSprint().getOrder().isEmpty()) {
            showSprintResults(holder);
            List<JuniorSessionResultElement> podium = element.getSprint().getPodium();

            UIUtils.multipleSetTextViewText(
                    new String[]{
                            podium.get(0).getDriver(),
                            podium.get(1).getDriver(),
                            podium.get(2).getDriver()
                    },
                    new TextView[]{
                            holder.firstDriverSprint,
                            holder.secondDriverSprint,
                            holder.thirdDriverSprint});

            holder.sprintRaceLayout.setOnClickListener(v -> NavigationUtils.showFullResultsDialog(juniorResult,
                    fragmentManager, categoryType, 1));
        }else{
            showSprintCancelled(holder);
        }
    }

    private void showFeatureResults(JuniorResultsViewHolder holder) {
        holder.featureResultsLayout.setVisibility(View.VISIBLE);
        holder.featureCancelledLayout.setVisibility(View.GONE);
    }

    private void showSprintResults(JuniorResultsViewHolder holder) {
        holder.sprintResultsLayout.setVisibility(View.VISIBLE);
        holder.sprintCancelledLayout.setVisibility(View.GONE);
    }

    private void showFeatureCancelled(JuniorResultsViewHolder holder) {
        holder.featureCancelledLayout.setVisibility(View.VISIBLE);
        holder.featureResultsLayout.setVisibility(View.GONE);
    }

    private void showSprintCancelled(JuniorResultsViewHolder holder) {
        holder.sprintCancelledLayout.setVisibility(View.VISIBLE);
        holder.sprintResultsLayout.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return juniorResult.getResults().size();
    }

    public static class JuniorResultsViewHolder extends RecyclerView.ViewHolder {

        private final TextView roundNumber, gpName,
                firstDriverFeature, secondDriverFeature, thirdDriverFeature,
                firstDriverSprint, secondDriverSprint, thirdDriverSprint,
                featureCancelledLayout, sprintCancelledLayout;
        private final LinearLayout sprintResultsLayout, featureResultsLayout, sprintRaceLayout, featureRaceLayout;
        private final ImageView eventNationFlag;

        public JuniorResultsViewHolder(@NonNull View itemView) {
            super(itemView);
            roundNumber = itemView.findViewById(R.id.round_number);
            gpName = itemView.findViewById(R.id.gp_name);
            sprintResultsLayout= itemView.findViewById(R.id.sprint_podium);
            featureResultsLayout = itemView.findViewById(R.id.feature_podium);
            featureCancelledLayout = itemView.findViewById(R.id.feature_cancelled);
            sprintCancelledLayout = itemView.findViewById(R.id.sprint_cancelled);
            sprintRaceLayout = itemView.findViewById(R.id.sprint_race_layout);
            featureRaceLayout = itemView.findViewById(R.id.feature_race_layout);
            firstDriverFeature = featureResultsLayout.findViewById(R.id.first_driver);
            secondDriverFeature = featureResultsLayout.findViewById(R.id.second_driver);
            thirdDriverFeature = featureResultsLayout.findViewById(R.id.third_driver);
            firstDriverSprint = sprintResultsLayout.findViewById(R.id.first_driver);
            secondDriverSprint = sprintResultsLayout.findViewById(R.id.second_driver);
            thirdDriverSprint = sprintResultsLayout.findViewById(R.id.third_driver);
            eventNationFlag = itemView.findViewById(R.id.event_nation_flag);
        }
    }
}
