package com.the_coffe_coders.fastestlap.adapter.junior;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.junior.result.FeatureRace;
import com.the_coffe_coders.fastestlap.domain.junior.result.JuniorRaceSession;
import com.the_coffe_coders.fastestlap.domain.junior.result.JuniorResult;
import com.the_coffe_coders.fastestlap.domain.junior.result.JuniorResultElement;
import com.the_coffe_coders.fastestlap.domain.junior.result.JuniorSessionResultElement;
import com.the_coffe_coders.fastestlap.domain.junior.result.SprintRace;
import com.the_coffe_coders.fastestlap.util.ui.NavigationUtils;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import java.util.List;

public class JuniorResultsRecyclerAdapter extends RecyclerView.Adapter<JuniorResultsRecyclerAdapter.JuniorResultsViewHolder> {

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
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == 0) {
            View view = inflater.inflate(R.layout.junior_event_result_card, parent, false);
            return new EventResultViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.junior_race_result_item, parent, false);
            return new SessionOrderViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull JuniorResultsViewHolder holder, int position) {
        if (holder instanceof EventResultViewHolder) {
            manageJuniorResult((EventResultViewHolder) holder, position);
        } else if (holder instanceof SessionOrderViewHolder) {
            manageSessionOrder((SessionOrderViewHolder) holder, position);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return contentType;
    }

    @Override
    public int getItemCount() {
        switch (contentType) {
            case 0:
                return (juniorResult != null && juniorResult.getResults() != null) ? juniorResult.getResults().size() : 0;
            case 1:
                return (featureRace != null && featureRace.getOrder() != null) ? featureRace.getOrder().size() : 0;
            case 2:
                return (sprintRace != null && sprintRace.getOrder() != null) ? sprintRace.getOrder().size() : 0;
            default:
                return 0;
        }
    }

    private void manageSessionOrder(SessionOrderViewHolder holder, int position) {
        JuniorSessionResultElement element = null;
        if (contentType == 1 && featureRace != null && featureRace.getOrder() != null) {
            element = featureRace.getOrder().get(position);
        } else if (contentType == 2 && sprintRace != null && sprintRace.getOrder() != null) {
            element = sprintRace.getOrder().get(position);
        }

        if (element != null) {
            if (holder.position != null) holder.position.setText(element.getPosition());
            if (holder.driverName != null) holder.driverName.setText(element.getDriver());
        }
    }

    private void manageJuniorResult(EventResultViewHolder holder, int position) {
        if (juniorResult == null || juniorResult.getResults() == null || position >= juniorResult.getResults().size()) {
            return;
        }

        JuniorResultElement element = juniorResult.getResults().get(position);
        if (element == null) return;

        // Dynamic border color matching category topbar color
        int topBarColor = ContextCompat.getColor(context, categoryType == 0 ? R.color.formula_2 : R.color.app_primary_red);
        if (holder.cardView != null) {
            holder.cardView.setStrokeColor(topBarColor);
        }

        // Round number with category color accent
        if (holder.roundNumber != null) {
            holder.roundNumber.setText(context.getString(R.string.round_plus_value, String.valueOf(element.getRound())));
            holder.roundNumber.setTextColor(topBarColor);
        }

        if (holder.gpName != null) {
            holder.gpName.setText(element.getCircuit());
        }

        // Flag loading
        if (holder.eventNationFlag != null) {
            UIUtils.loadImageWithGlide(context, element.getNationFlagUrl(), holder.eventNationFlag, null);
        }

        boolean isDouble = element.isEventDouble();

        // 1. Row 1 - Left: Sprint (always run)
        bindSessionSlot(holder.sprintSlot, context.getString(R.string.sprint), element.getSprint(), element.getCircuit(), 0, topBarColor);

        // 2. Row 1 - Right: Feature 1 (in double feature) or Feature (in standard weekend)
        String feature1Title = isDouble ? context.getString(R.string.feature_1) : context.getString(R.string.feature);
        FeatureRace feature1Race = isDouble ? element.getFeature1() : element.getFeature();
        bindSessionSlot(holder.feature1Slot, feature1Title, feature1Race, element.getCircuit(), isDouble ? 1 : 0, topBarColor);

        // 3. Row 2: Feature 2 (only displayed if double feature weekend)
        if (isDouble) {
            if (holder.feature2RowLayout != null) {
                holder.feature2RowLayout.setVisibility(View.VISIBLE);
            }
            bindSessionSlot(holder.feature2Slot, context.getString(R.string.feature_2), element.getFeature2(), element.getCircuit(), 2, topBarColor);
        } else {
            if (holder.feature2RowLayout != null) {
                holder.feature2RowLayout.setVisibility(View.GONE);
            }
        }
    }

    private void bindSessionSlot(SessionSlotViewHolder slot, String titleText, JuniorRaceSession session, String circuit, int raceType, int color) {
        if (slot == null || slot.itemView == null) return;

        if (slot.title != null) {
            slot.title.setText(titleText);
            slot.title.setTextColor(color);
        }

        if (session == null) {
            showSlotState(slot, SlotState.ERROR);
            slot.itemView.setOnClickListener(null);
            slot.itemView.setClickable(false);
            return;
        }

        if (session.isCompleted()) {
            showSlotState(slot, SlotState.COMPLETED);
            List<JuniorSessionResultElement> podium = session.getPodium();
            if (podium != null) {
                if (slot.firstDriver != null) slot.firstDriver.setText(podium.size() > 0 ? podium.get(0).getDriver() : "-");
                if (slot.secondDriver != null) slot.secondDriver.setText(podium.size() > 1 ? podium.get(1).getDriver() : "-");
                if (slot.thirdDriver != null) slot.thirdDriver.setText(podium.size() > 2 ? podium.get(2).getDriver() : "-");
            }
            slot.itemView.setClickable(true);
            slot.itemView.setOnClickListener(v -> {
                if (session instanceof FeatureRace) {
                    NavigationUtils.showFullResultsDialogFeature(circuit, (FeatureRace) session, fragmentManager, categoryType, raceType);
                } else if (session instanceof SprintRace) {
                    NavigationUtils.showFullResultsDialogSprint(circuit, (SprintRace) session, fragmentManager, categoryType, raceType);
                }
            });
        } else if (session.isCancelled()) {
            showSlotState(slot, SlotState.CANCELLED);
            slot.itemView.setOnClickListener(null);
            slot.itemView.setClickable(false);
        } else if (session.isYetToStart()) {
            showSlotState(slot, SlotState.YET_TO_START);
            slot.itemView.setOnClickListener(null);
            slot.itemView.setClickable(false);
        } else {
            showSlotState(slot, SlotState.ERROR);
            slot.itemView.setOnClickListener(null);
            slot.itemView.setClickable(false);
        }
    }

    private enum SlotState { COMPLETED, CANCELLED, YET_TO_START, ERROR }

    private void showSlotState(SessionSlotViewHolder slot, SlotState state) {
        if (slot.podiumLayout != null) slot.podiumLayout.setVisibility(state == SlotState.COMPLETED ? View.VISIBLE : View.GONE);
        if (slot.cancelledLayout != null) slot.cancelledLayout.setVisibility(state == SlotState.CANCELLED ? View.VISIBLE : View.GONE);
        if (slot.yetToStartLayout != null) slot.yetToStartLayout.setVisibility(state == SlotState.YET_TO_START ? View.VISIBLE : View.GONE);
        if (slot.errorLayout != null) slot.errorLayout.setVisibility(state == SlotState.ERROR ? View.VISIBLE : View.GONE);
    }

    public static abstract class JuniorResultsViewHolder extends RecyclerView.ViewHolder {
        public JuniorResultsViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public static class EventResultViewHolder extends JuniorResultsViewHolder {
        final MaterialCardView cardView;
        final TextView roundNumber;
        final TextView gpName;
        final ImageView eventNationFlag;
        final View feature2RowLayout;
        final SessionSlotViewHolder sprintSlot;
        final SessionSlotViewHolder feature1Slot;
        final SessionSlotViewHolder feature2Slot;

        public EventResultViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.junior_event_result_card_layout);
            roundNumber = itemView.findViewById(R.id.round_number);
            gpName = itemView.findViewById(R.id.gp_name);
            eventNationFlag = itemView.findViewById(R.id.event_nation_flag);
            feature2RowLayout = itemView.findViewById(R.id.feature2_row_layout);

            sprintSlot = new SessionSlotViewHolder(
                    itemView.findViewById(R.id.sprint_race_layout),
                    R.id.sprint_title,
                    R.id.sprint_podium,
                    R.id.sprint_cancelled,
                    R.id.sprint_yet_to_start,
                    R.id.sprint_error
            );

            feature1Slot = new SessionSlotViewHolder(
                    itemView.findViewById(R.id.feature1_race_layout),
                    R.id.feature1_title,
                    R.id.feature1_podium,
                    R.id.feature1_cancelled,
                    R.id.feature1_yet_to_start,
                    R.id.feature1_error
            );

            feature2Slot = new SessionSlotViewHolder(
                    itemView.findViewById(R.id.feature2_race_layout),
                    R.id.feature2_title,
                    R.id.feature2_podium,
                    R.id.feature2_cancelled,
                    R.id.feature2_yet_to_start,
                    R.id.feature2_error
            );
        }
    }

    public static class SessionOrderViewHolder extends JuniorResultsViewHolder {
        final TextView position;
        final TextView driverName;

        public SessionOrderViewHolder(@NonNull View itemView) {
            super(itemView);
            position = itemView.findViewById(R.id.position_text);
            driverName = itemView.findViewById(R.id.driver_name);
        }
    }

    static class SessionSlotViewHolder {
        final View itemView;
        final TextView title;
        final View podiumLayout;
        final View cancelledLayout;
        final View yetToStartLayout;
        final View errorLayout;
        final TextView firstDriver;
        final TextView secondDriver;
        final TextView thirdDriver;

        SessionSlotViewHolder(View root, int titleId, int podiumLayoutId, int cancelledId, int yetToStartId, int errorId) {
            itemView = root;
            if (root != null) {
                title = root.findViewById(titleId);
                podiumLayout = root.findViewById(podiumLayoutId);
                cancelledLayout = root.findViewById(cancelledId);
                yetToStartLayout = root.findViewById(yetToStartId);
                errorLayout = root.findViewById(errorId);

                if (podiumLayout != null) {
                    firstDriver = podiumLayout.findViewById(R.id.first_driver);
                    secondDriver = podiumLayout.findViewById(R.id.second_driver);
                    thirdDriver = podiumLayout.findViewById(R.id.third_driver);
                } else {
                    firstDriver = null;
                    secondDriver = null;
                    thirdDriver = null;
                }
            } else {
                title = null;
                podiumLayout = null;
                cancelledLayout = null;
                yetToStartLayout = null;
                errorLayout = null;
                firstDriver = null;
                secondDriver = null;
                thirdDriver = null;
            }
        }
    }
}
