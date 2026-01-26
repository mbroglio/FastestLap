package com.the_coffe_coders.fastestlap.adapter.junior;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.junior.result.FeatureRace;
import com.the_coffe_coders.fastestlap.domain.junior.result.JuniorResult;
import com.the_coffe_coders.fastestlap.domain.junior.result.JuniorResultElement;
import com.the_coffe_coders.fastestlap.domain.junior.result.JuniorSessionResultElement;
import com.the_coffe_coders.fastestlap.domain.junior.result.SprintRace;
import com.the_coffe_coders.fastestlap.util.ui.NavigationUtils;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import java.util.List;

public class JuniorResultsRecyclerAdapter extends RecyclerView.Adapter<JuniorResultsRecyclerAdapter.JuniorResultsViewHolder> {
    private static final String TAG = "JuniorResultsRecyclerAdapter";

    private final Context context;
    private final JuniorResult juniorResult;
    private final FragmentManager fragmentManager;
    private final int categoryType;
    private final FeatureRace featureRace;
    private final SprintRace sprintRace;
    private final int contentType;


    public JuniorResultsRecyclerAdapter(Context context, JuniorResult juniorResult, FragmentManager fragmentManager, int categoryType) {
        this.context = context;
        this.juniorResult = juniorResult;
        this.fragmentManager = fragmentManager;
        this.categoryType = categoryType;
        this.featureRace = null;
        this.sprintRace = null;
        this.contentType = 0;
    }

    public JuniorResultsRecyclerAdapter(Context context, FeatureRace featureRace, FragmentManager fragmentManager) {
        this.context = context;
        this.juniorResult = null;
        this.fragmentManager = fragmentManager;
        this.categoryType = -1;
        this.featureRace = featureRace;
        this.sprintRace = null;
        this.contentType = 1;
    }

    public JuniorResultsRecyclerAdapter(Context context, SprintRace sprintRace, FragmentManager fragmentManager) {
        this.context = context;
        this.juniorResult = null;
        this.fragmentManager = fragmentManager;
        this.categoryType = -1;
        this.featureRace = null;
        this.sprintRace = sprintRace;
        this.contentType = 2;
    }

    @NonNull
    @Override
    public JuniorResultsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = null;

        switch (contentType) {
            case 0:
                view = LayoutInflater.from(context).inflate(R.layout.junior_event_result_card, parent, false);
                break;
            case 1:
            case 2:
                view = LayoutInflater.from(context).inflate(R.layout.junior_race_result_item, parent, false);
                break;
        }

        if (view == null)
            throw new IllegalArgumentException("Invalid content type");

        return new JuniorResultsViewHolder(view, contentType);
    }

    @Override
    public void onBindViewHolder(@NonNull JuniorResultsViewHolder holder, int position) {
        switch (contentType) {
            case 0:
                manageJuniorResult(holder, position);
                break;
            case 1:
                manageJuniorSessionResultFeature(holder, position);
                break;
            case 2:
                manageJuniorSessionResultSprint(holder, position);
                break;
        }
    }

    private void manageJuniorSessionResultSprint(JuniorResultsViewHolder holder, int position) {
        JuniorSessionResultElement element = sprintRace.getOrder().get(position);
        UIUtils.multipleSetTextViewText(
                new String[]{
                        element.getPosition(),
                        element.getDriver()
                },
                new TextView[]{
                        holder.position,
                        holder.driverName});
    }

    private void manageJuniorSessionResultFeature(JuniorResultsViewHolder holder, int position) {
        JuniorSessionResultElement element = featureRace.getOrder().get(position);
        UIUtils.multipleSetTextViewText(
                new String[]{
                        element.getPosition(),
                        element.getDriver()
                },
                new TextView[]{
                        holder.position,
                        holder.driverName});
    }

    private void manageJuniorResult(JuniorResultsViewHolder holder, int position) {
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

            holder.featureRaceLayout.setOnClickListener(v -> NavigationUtils.showFullResultsDialogFeature(
                    element.getCircuit(), element.getFeature(), fragmentManager, categoryType, 0));
        } else {
            showFeatureCancelled(holder);
        }

        setSprintResults(holder, element);
    }

    private void setSprintResults(JuniorResultsViewHolder holder, JuniorResultElement element) {
        if (element.getSprint() != null && element.getSprint().getOrder() != null &&
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

            holder.sprintRaceLayout.setOnClickListener(v -> NavigationUtils.showFullResultsDialogSprint(
                    element.getCircuit(), element.getSprint(), fragmentManager, categoryType, 0));
        } else {
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
        switch (contentType) {
            case 0:
                return juniorResult.getResults().size();
            case 1:
                return featureRace.getOrder().size();
            case 2:
                return sprintRace.getOrder().size();
        }
        return 0;
    }

    public static class JuniorResultsViewHolder extends RecyclerView.ViewHolder {

        private final TextView roundNumber, gpName,
                firstDriverFeature, secondDriverFeature, thirdDriverFeature,
                firstDriverSprint, secondDriverSprint, thirdDriverSprint,
                featureCancelledLayout, sprintCancelledLayout;
        private final LinearLayout sprintResultsLayout, featureResultsLayout, sprintRaceLayout, featureRaceLayout;
        private final ImageView eventNationFlag;

        private final TextView position, driverName;

        public JuniorResultsViewHolder(@NonNull View itemView, int contentType) {
            super(itemView);

            if(contentType == 0){
                //junior_event_result_card
                roundNumber = itemView.findViewById(R.id.round_number);
                gpName = itemView.findViewById(R.id.gp_name);
                sprintResultsLayout = itemView.findViewById(R.id.sprint_podium);
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
                position = null;
                driverName = null;
            }else{
                roundNumber = null;
                gpName = null;
                sprintResultsLayout = null;
                featureResultsLayout = null;
                featureCancelledLayout = null;
                sprintCancelledLayout = null;
                sprintRaceLayout = null;
                featureRaceLayout = null;
                firstDriverFeature = null;
                secondDriverFeature = null;
                thirdDriverFeature = null;
                firstDriverSprint = null;
                secondDriverSprint = null;
                thirdDriverSprint = null;
                eventNationFlag = null;

                //junior_race_result_item
                position = itemView.findViewById(R.id.position_text);
                driverName = itemView.findViewById(R.id.driver_name);
            }


        }
    }
}
