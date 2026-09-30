package com.the_coffe_coders.fastestlap.adapter.f1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.f1.livetiming.RaceControlMessage;
import com.the_coffe_coders.fastestlap.domain.f1.livetiming.TeamRadioMessage;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.teamradio.TeamRadioPlayer;
import com.the_coffe_coders.fastestlap.util.teamradio.TeamRadioPlayerManager;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;

public class RaceControlRecyclerAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "RaceControlAdapter";

    public static final int VIEW_TYPE_RACE_CONTROL = 0;
    public static final int VIEW_TYPE_TEAM_RADIO   = 1;

    private final Context context;
    private final List<Object> items;
    private final List<RaceControlLapGroup> rawRaceControlGroups = new ArrayList<>();
    private final List<TeamRadioMessage> rawTeamRadioMessages = new ArrayList<>();

    /** Manager audio condiviso da tutti i ViewHolder Team Radio di questo adapter.
     * -- GETTER --
     * Espone il manager per il rilascio da Fragment.onDestroyView.
     */
    @Getter
    private final TeamRadioPlayerManager playerManager;

    public RaceControlRecyclerAdapter(Context context) {
        this.context = context;
        this.items         = new ArrayList<>();
        this.playerManager = new TeamRadioPlayerManager();
    }

    // ─────────────────────────────────────────────────────────────
    // Domain Group Models – raggruppano messaggi dello stesso giro / adiacenti
    // ─────────────────────────────────────────────────────────────

    @Getter
    public static class RaceControlLapGroup {
        private final Integer lapNumber;
        private final List<RaceControlMessage> messages;
        private final String latestDate;

        public RaceControlLapGroup(Integer lapNumber, List<RaceControlMessage> messages) {
            this.lapNumber  = lapNumber;
            this.messages   = messages;
            this.latestDate = (messages != null && !messages.isEmpty()) ? messages.get(0).getDate() : null;
        }
    }

    @Getter
    public static class TeamRadioGroup {
        private final List<TeamRadioMessage> messages;
        private final String latestDate;

        public TeamRadioGroup(List<TeamRadioMessage> messages) {
            this.messages   = messages;
            this.latestDate = (messages != null && !messages.isEmpty()) ? messages.get(0).getDate() : null;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────

    public void submitRaceControlMessages(List<RaceControlMessage> rawMessages) {
        rawRaceControlGroups.clear();

        if (rawMessages != null && !rawMessages.isEmpty()) {
            Map<Integer, List<RaceControlMessage>> groupedMap = new LinkedHashMap<>();
            List<RaceControlMessage> nullLapMessages = new ArrayList<>();

            for (RaceControlMessage msg : rawMessages) {
                Integer lap = msg.getLapNumber();
                if (lap != null) {
                    groupedMap.computeIfAbsent(lap, k -> new ArrayList<>()).add(msg);
                } else {
                    nullLapMessages.add(msg);
                }
            }

            for (Map.Entry<Integer, List<RaceControlMessage>> entry : groupedMap.entrySet()) {
                rawRaceControlGroups.add(new RaceControlLapGroup(entry.getKey(), entry.getValue()));
            }

            if (!nullLapMessages.isEmpty()) {
                rawRaceControlGroups.add(new RaceControlLapGroup(null, nullLapMessages));
            }
        }

        rebuildAndSortItems();
    }

    public void submitTeamRadioMessages(List<TeamRadioMessage> messages) {
        rawTeamRadioMessages.clear();
        if (messages != null) {
            rawTeamRadioMessages.addAll(messages);
        }
        rebuildAndSortItems();
    }

    private void rebuildAndSortItems() {
        List<Object> unGroupedList = new ArrayList<>();
        unGroupedList.addAll(rawRaceControlGroups);
        unGroupedList.addAll(rawTeamRadioMessages);

        // Ordina tutti gli elementi in ordine cronologico decrescente (più recente in cima)
        unGroupedList.sort((a, b) -> {
            String dateA = getDate(a);
            String dateB = getDate(b);
            if (dateA == null && dateB == null) return 0;
            if (dateA == null) return 1;
            if (dateB == null) return -1;
            return dateB.compareTo(dateA);
        });

        // Raggruppa i Team Radio adiacenti in un unico item (TeamRadioGroup)
        List<Object> finalItems = new ArrayList<>();
        List<TeamRadioMessage> pendingTRs = new ArrayList<>();

        for (Object item : unGroupedList) {
            if (item instanceof TeamRadioMessage) {
                pendingTRs.add((TeamRadioMessage) item);
            } else {
                if (!pendingTRs.isEmpty()) {
                    finalItems.add(new TeamRadioGroup(new ArrayList<>(pendingTRs)));
                    pendingTRs.clear();
                }
                finalItems.add(item);
            }
        }
        if (!pendingTRs.isEmpty()) {
            finalItems.add(new TeamRadioGroup(new ArrayList<>(pendingTRs)));
            pendingTRs.clear();
        }

        this.items.clear();
        this.items.addAll(finalItems);
        notifyDataSetChanged();
    }

    private String getDate(Object obj) {
        if (obj instanceof RaceControlLapGroup) return ((RaceControlLapGroup) obj).getLatestDate();
        if (obj instanceof TeamRadioGroup)     return ((TeamRadioGroup) obj).getLatestDate();
        if (obj instanceof TeamRadioMessage)   return ((TeamRadioMessage) obj).getDate();
        return null;
    }

    // ─────────────────────────────────────────────────────────────
    // RecyclerView.Adapter overrides
    // ─────────────────────────────────────────────────────────────

    @Override
    public int getItemViewType(int position) {
        return (items.get(position) instanceof RaceControlLapGroup)
                ? VIEW_TYPE_RACE_CONTROL
                : VIEW_TYPE_TEAM_RADIO;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_RACE_CONTROL) {
            View view = inflater.inflate(R.layout.race_control_view_entry, parent, false);
            return new RaceControlViewHolder(view);
        } else {
            LinearLayout container = new LinearLayout(parent.getContext());
            container.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            container.setOrientation(LinearLayout.VERTICAL);
            return new TeamRadioViewHolder(container, playerManager);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);
        if (holder instanceof RaceControlViewHolder) {
            ((RaceControlViewHolder) holder).bind((RaceControlLapGroup) item);
        } else if (holder instanceof TeamRadioViewHolder) {
            ((TeamRadioViewHolder) holder).bind((TeamRadioGroup) item);
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof TeamRadioViewHolder) {
            // Scollega i listener UI senza fermare la riproduzione
            ((TeamRadioViewHolder) holder).detach();
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        playerManager.release();
    }

    // ─────────────────────────────────────────────────────────────
    // ViewHolder – Race Control (Grouped Lap)
    // ─────────────────────────────────────────────────────────────

    public static class RaceControlViewHolder extends RecyclerView.ViewHolder {

        private final TextView lapCount;
        private final LinearLayout messagesContainer;

        public RaceControlViewHolder(@NonNull View itemView) {
            super(itemView);
            lapCount          = itemView.findViewById(R.id.lap_count);
            messagesContainer = itemView.findViewById(R.id.messages_container);
        }

        @android.annotation.SuppressLint("SetTextI18n")
        public void bind(RaceControlLapGroup group) {
            if (group.getLapNumber() != null) {
                lapCount.setText("Lap: " + group.getLapNumber());
                lapCount.setVisibility(View.VISIBLE);
            } else {
                lapCount.setVisibility(View.GONE);
            }

            messagesContainer.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(itemView.getContext());

            for (RaceControlMessage msg : group.getMessages()) {
                View cardView = inflateCardForMessage(inflater, messagesContainer, msg);

                if (cardView != null) {
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    params.bottomMargin = dpToPx(itemView.getContext(), 10);
                    cardView.setLayoutParams(params);

                    messagesContainer.addView(cardView);
                }
            }
        }

        /**
         * Seleziona e gonfia il layout corretto per un singolo {@link RaceControlMessage}
         * in base a category, flag, scope e contenuto del messaggio.
         */
        private View inflateCardForMessage(LayoutInflater inflater, ViewGroup parent, RaceControlMessage msg) {
            String category = msg.getCategory() != null ? msg.getCategory().toUpperCase().trim() : "";
            String flag     = msg.getFlag()     != null ? msg.getFlag().toUpperCase().trim()     : "";
            String scope    = msg.getScope()    != null ? msg.getScope().toUpperCase().trim()    : "";
            String message  = msg.getMessage()  != null ? msg.getMessage()                       : "";

            switch (category) {

                // ──────────────────────────────────────────────
                // FLAG – suddivisione per scope
                // ──────────────────────────────────────────────
                case "FLAG": {
                    switch (scope) {
                        case "SECTOR":
                            return inflateFlagTrackCard(inflater, parent, flag, message);
                        case "DRIVER":
                            return inflateFlagDriverCard(inflater, parent, flag, msg);
                        case "TRACK":{
                            if(msg.isChequeredFlag() || msg.isTrackClear())
                                return inflateFlagTrackCard(inflater, parent, flag, message);
                            if(msg.isPitLane())
                                return inflatePitLaneCard(inflater, parent, flag, message);
                        }

                    }
                }

                // ──────────────────────────────────────────────
                // SAFETYCAR
                // ──────────────────────────────────────────────
                case "SAFETYCAR": {
                    return inflateSafetyCarCard(inflater, parent, message);
                }

                case "SESSIONSTATUS": {
                    return inflateSessionStatusLayout(inflater, parent, message);
                }

                // ──────────────────────────────────────────────
                // STEWARD / OTHER / con driver number
                // ──────────────────────────────────────────────
                case "OTHER": {
                    if(msg.isTrackLimits()){
                        return inflateTrackLimitsCard(inflater, parent, message);
                    } else if(msg.isPitLane()){
                        return inflatePitLaneCard(inflater, parent, flag, message);
                    } else{
                        return inflateStewardsCard(inflater, parent, message);
                    }
                }
                default: {
                    // Fallback per categorie sconosciute
                    return inflateStewardsCard(inflater, parent, message);
                }
            }
        }


        // ── Layout inflaters ────────────────────────────────────
        private View inflateFlagTrackCard(LayoutInflater inflater, ViewGroup parent, String flag, String message) {
            View card = inflater.inflate(R.layout.race_control_flag_track_message, parent, false);
            applyFlagIcons(card, flag);
            if(flag.equalsIgnoreCase("CLEAR")){
                UIUtils.singleSetTextViewText("TRACK CLEAR", card.findViewById(R.id.message_title));
            }else{
                UIUtils.singleSetTextViewText(flag, card.findViewById(R.id.message_title));
            }
            UIUtils.singleSetTextViewText(message, card.findViewById(R.id.message_text));

            return card;
        }

        private View inflateFlagDriverCard(LayoutInflater inflater, ViewGroup parent, String flag, RaceControlMessage msg) {
            View card = inflater.inflate(R.layout.race_control_flag_driver_message, parent, false);
            applyFlagIcons(card, flag);

            TextView driverName = card.findViewById(R.id.driver_name);
            if (driverName != null && msg.getDriverNumber() != null) {
                UIUtils.singleSetTextViewText(Constants.DRIVER_NUMBER_NAME.get(msg.getDriverNumber().toString()), driverName);
            }

            if(flag.equalsIgnoreCase("BLUE")){
                msg.setMessage(msg.getMessage().replaceAll("TIMED AT \\d+:\\d+:\\d+", "").trim());
            }

            UIUtils.multipleSetTextViewText(
                    new String[]{
                            msg.getFlag(),
                            msg.getMessage()},
                    new TextView[]{
                            card.findViewById(R.id.message_title),
                            card.findViewById(R.id.message_text)});

            return card;
        }

        private View inflateSessionStatusLayout(LayoutInflater inflater, ViewGroup parent, String message) {
            View card = inflater.inflate(R.layout.race_control_session_status, parent, false);

            if(message.contains("SESSION STARTED")){
                applyFlagIcons(card, "GREEN");
            }else{
                applyFlagIcons(card, "CHECKERED");
            }

            String newMessage = message.replace(" ", "\n");
            UIUtils.singleSetTextViewText(newMessage, card.findViewById(R.id.message_title));

            return card;
        }

        private View inflatePitLaneCard(LayoutInflater inflater, ViewGroup parent, String flag, String message) {
            View card = inflater.inflate(R.layout.race_control_pit_lane_lights, parent, false);


            ImageView flag1 = card.findViewById(R.id.flag_1);
            ImageView flag2 = card.findViewById(R.id.flag_2);

            if(message.contains("CLOSED")){
                flag1.setImageResource(R.drawable.traffic_light_red_icon);
                flag2.setImageResource(R.drawable.traffic_light_red_icon);

                UIUtils.multipleSetTextViewText(
                        new String[]{
                                "RED LIGHT",
                                message},
                        new TextView[]{
                                card.findViewById(R.id.message_title),
                                card.findViewById(R.id.message_text)});
            }else{
                flag1.setImageResource(R.drawable.traffic_light_green_icon);
                flag2.setImageResource(R.drawable.traffic_light_green_icon);

                String title = message.split(" - ")[0];
                String newMessage = message.split(" - ")[1];

                UIUtils.multipleSetTextViewText(
                        new String[]{
                                title,
                                newMessage},
                        new TextView[]{
                                card.findViewById(R.id.message_title),
                                card.findViewById(R.id.message_text)});
            }

            return card;
        }

        /**
         * race_control_safety_car_message → usato per category=SAFETYCAR
         * IDs: flag_1, flag_2 (ImageView), message_text_layout (FrameLayout con TextView anonimo)
         */
        private View inflateSafetyCarCard(LayoutInflater inflater, ViewGroup parent, String message) {
            View card = inflater.inflate(R.layout.race_control_safety_car_message, parent, false);

            ImageView panel = card.findViewById(R.id.panel);

            if(message.contains("VSC")){
                panel.setImageResource(R.drawable.vsc_panel_icon);
            }else{
                panel.setImageResource(R.drawable.sc_panel_icon);
            }

            UIUtils.singleSetTextViewText(message, card.findViewById(R.id.message_text));

            return card;
        }

        private View inflateTrackLimitsCard(LayoutInflater inflater, ViewGroup parent, String message) {
            View card = inflater.inflate(R.layout.race_control_track_limits_message, parent, false);

            //extract "car 5" from string "car 5 (bor) time deleted"
            String driverName = Constants.DRIVER_NUMBER_NAME.get(message.split(" ")[1]);

            UIUtils.multipleSetTextViewText(
                    new String[]{
                            driverName,
                            message},
                    new TextView[]{
                            card.findViewById(R.id.driver_name),
                            card.findViewById(R.id.message_text)});

            return card;
        }

        private View inflateStewardsCard(LayoutInflater inflater, ViewGroup parent, String message) {
            View card = inflater.inflate(R.layout.race_control_stewards_message, parent, false);
            UIUtils.singleSetTextViewText(message, card.findViewById(R.id.message_text));

            return card;
        }

        private void applyFlagIcons(View card, String flag) {
            int iconRes;
            switch (flag) {
                case "BLUE":
                    iconRes = R.drawable.blue_flag_panel_icon;
                    break;
                case "BLACK AND WHITE":
                    iconRes = R.drawable.black_and_white_flag_panel_icon;
                    break;
                case "GREEN":
                case "CLEAR":
                    iconRes = R.drawable.green_flag_panel_icon;
                    break;
                case "YELLOW":
                case "DOUBLE YELLOW":
                    iconRes = R.drawable.yellow_flag_panel_icon;
                    break;
                case "CHECKERED":
                case "CHEQUERED":
                    iconRes = R.drawable.checkered_flag_panel_icon;
                    break;
                default:
                    iconRes = R.drawable.content_not_found_icon;
                    break;
            }

            ImageView f1 = card.findViewById(R.id.flag_1);
            ImageView f2 = card.findViewById(R.id.flag_2);
            if (f1 != null) f1.setImageResource(iconRes);
            if (f2 != null) f2.setImageResource(iconRes);
        }



        private int dpToPx(Context context, int dp) {
            return Math.round(dp * context.getResources().getDisplayMetrics().density);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ViewHolder – Team Radio Group (delega a TeamRadioPlayer + TeamRadioPlayerManager)
    // ─────────────────────────────────────────────────────────────

    public static class TeamRadioViewHolder extends RecyclerView.ViewHolder {

        private final LinearLayout container;
        private final TeamRadioPlayerManager manager;
        private final List<TeamRadioPlayer> activePlayers = new ArrayList<>();

        public TeamRadioViewHolder(@NonNull View itemView, TeamRadioPlayerManager manager) {
            super(itemView);
            this.manager   = manager;
            this.container = (LinearLayout) itemView;
        }

        public void bind(TeamRadioGroup group) {
            detach();

            activePlayers.clear();
            container.removeAllViews();

            Context ctx = itemView.getContext();
            int verticalPadding = dpToPx(ctx, 12);
            container.setPadding(0, verticalPadding, 0, verticalPadding);

            LayoutInflater inflater = LayoutInflater.from(ctx);

            List<TeamRadioMessage> messages = group.getMessages();
            for (int i = 0; i < messages.size(); i++) {
                TeamRadioMessage msg = messages.get(i);
                View cardView = inflater.inflate(R.layout.race_control_team_radio_item, container, false);

                if (i < messages.size() - 1) {
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    params.bottomMargin = dpToPx(ctx, 10);
                    cardView.setLayoutParams(params);
                }

                TextView driverText     = cardView.findViewById(R.id.team_radio_driver);
                TextView driverNameText = cardView.findViewById(R.id.driver_name);

                String title = ctx.getString(R.string.team_radio_plus_driver_number, msg.getDriverNumber());

                UIUtils.multipleSetTextViewText(
                        new String[]{title, Constants.DRIVER_NUMBER_NAME.get(Integer.toString(msg.getDriverNumber()))},
                        new TextView[]{driverText, driverNameText});

                TeamRadioPlayer radioPlayer = new TeamRadioPlayer(
                        manager,
                        cardView.findViewById(R.id.team_radio_info_panel),
                        cardView.findViewById(R.id.team_radio_player_panel),
                        cardView.findViewById(R.id.team_radio_play_button),
                        cardView.findViewById(R.id.team_radio_loading),
                        cardView.findViewById(R.id.team_radio_player_driver_label),
                        cardView.findViewById(R.id.team_radio_seek_bar),
                        cardView.findViewById(R.id.team_radio_time_elapsed),
                        cardView.findViewById(R.id.team_radio_time_total),
                        cardView.findViewById(R.id.team_radio_pause_button),
                        cardView.findViewById(R.id.team_radio_restart_button),
                        cardView.findViewById(R.id.team_radio_close_button)
                );

                radioPlayer.bind(title, msg.getRecordingUrl());
                activePlayers.add(radioPlayer);

                container.addView(cardView);
            }
        }

        /** Scollega i listener UI dal manager senza fermare la riproduzione. */
        public void detach() {
            for (TeamRadioPlayer player : activePlayers) {
                player.detach();
            }
            activePlayers.clear();
        }

        private int dpToPx(Context context, int dp) {
            return Math.round(dp * context.getResources().getDisplayMetrics().density);
        }
    }
}
