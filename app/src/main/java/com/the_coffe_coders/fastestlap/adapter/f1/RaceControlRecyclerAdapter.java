package com.the_coffe_coders.fastestlap.adapter.f1;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter della RecyclerView di {@code RaceControlFragment}.
 *
 * <p>Gestisce due tipi di item distinti tramite {@link #getItemViewType}:</p>
 * <ul>
 *   <li>{@link #VIEW_TYPE_RACE_CONTROL} – usa {@code race_control_view_entry.xml},
 *       che raggruppa tutti i messaggi di Race Control dello **stesso giro** in un unico item.</li>
 *   <li>{@link #VIEW_TYPE_TEAM_RADIO} – usa {@code race_control_team_radio_item.xml}.</li>
 * </ul>
 */
public class RaceControlRecyclerAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "RaceControlAdapter";

    public static final int VIEW_TYPE_RACE_CONTROL = 0;
    public static final int VIEW_TYPE_TEAM_RADIO   = 1;

    private final Context context;
    private final List<Object> items;

    public RaceControlRecyclerAdapter(Context context) {
        this.context = context;
        this.items   = new ArrayList<>();
    }

    // ─────────────────────────────────────────────────────────────
    // Domain Group Model – raggruppa i messaggi dello stesso giro
    // ─────────────────────────────────────────────────────────────

    public static class RaceControlLapGroup {
        private final Integer lapNumber;
        private final List<RaceControlMessage> messages;
        private final String latestDate;

        public RaceControlLapGroup(Integer lapNumber, List<RaceControlMessage> messages) {
            this.lapNumber = lapNumber;
            this.messages = messages;
            this.latestDate = (messages != null && !messages.isEmpty()) ? messages.get(0).getDate() : null;
        }

        public Integer getLapNumber() {
            return lapNumber;
        }

        public List<RaceControlMessage> getMessages() {
            return messages;
        }

        public String getLatestDate() {
            return latestDate;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Public API – chiamato dal Fragment per aggiornare i dati
    // ─────────────────────────────────────────────────────────────

    /**
     * Raggruppa i messaggi per numero di giro (lapNumber). Tutti i messaggi
     * appartenenti allo stesso giro vengono mostrati in un unico item.
     */
    public void submitRaceControlMessages(List<RaceControlMessage> rawMessages) {
        items.removeIf(o -> o instanceof RaceControlLapGroup);

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
                items.add(new RaceControlLapGroup(entry.getKey(), entry.getValue()));
            }

            if (!nullLapMessages.isEmpty()) {
                items.add(new RaceControlLapGroup(null, nullLapMessages));
            }
        }

        sortAndNotify();
    }

    /**
     * Sostituisce i messaggi Team Radio preservando i gruppi Race Control.
     */
    public void submitTeamRadioMessages(List<TeamRadioMessage> messages) {
        items.removeIf(o -> o instanceof TeamRadioMessage);
        if (messages != null) {
            items.addAll(messages);
        }
        sortAndNotify();
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
            View view = inflater.inflate(R.layout.race_control_team_radio_item, parent, false);
            return new TeamRadioViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);
        if (holder instanceof RaceControlViewHolder) {
            ((RaceControlViewHolder) holder).bind((RaceControlLapGroup) item);
        } else if (holder instanceof TeamRadioViewHolder) {
            ((TeamRadioViewHolder) holder).bind((TeamRadioMessage) item);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ─────────────────────────────────────────────────────────────
    // Helper ordinamento cronologico inverso
    // ─────────────────────────────────────────────────────────────

    private void sortAndNotify() {
        items.sort((a, b) -> {
            String dateA = getDate(a);
            String dateB = getDate(b);
            if (dateA == null && dateB == null) return 0;
            if (dateA == null) return 1;
            if (dateB == null) return -1;
            return dateB.compareTo(dateA); // ordine cronologico inverso
        });
        notifyDataSetChanged();
    }

    private String getDate(Object obj) {
        if (obj instanceof RaceControlLapGroup) return ((RaceControlLapGroup) obj).getLatestDate();
        if (obj instanceof TeamRadioMessage)   return ((TeamRadioMessage) obj).getDate();
        return null;
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
                View cardView;

                if (msg.getDriverNumber() != null) {
                    cardView = inflater.inflate(R.layout.race_control_flag_driver_message, messagesContainer, false);

                    TextView driverName = cardView.findViewById(R.id.driver_name);
                    if (driverName != null) {
                        driverName.setText("Car #" + msg.getDriverNumber());
                    }

                    TextView messageText = cardView.findViewById(R.id.message_text);
                    if (messageText != null) {
                        messageText.setText(msg.getMessage() != null ? msg.getMessage() : "");
                    }
                } else {
                    cardView = inflater.inflate(R.layout.race_control_stewards_message, messagesContainer, false);

                    TextView messageTitle = cardView.findViewById(R.id.message_title);
                    if (messageTitle != null) {
                        String category = msg.getCategory() != null ? msg.getCategory().toUpperCase() : "RACE CONTROL";
                        messageTitle.setText(category);
                    }

                    TextView messageText = cardView.findViewById(R.id.message_text);
                    if (messageText != null) {
                        String text = msg.getMessage();
                        if (text == null || text.isEmpty()) {
                            text = (msg.getFlag() != null ? msg.getFlag().toUpperCase() : "")
                                 + (msg.getScope() != null ? " – " + msg.getScope() : "");
                        }
                        messageText.setText(text);
                    }
                }

                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) cardView.getLayoutParams();
                if (params == null) {
                    params = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                }
                params.bottomMargin = dpToPx(itemView.getContext(), 6);
                cardView.setLayoutParams(params);

                messagesContainer.addView(cardView);
            }
        }

        private int dpToPx(Context context, int dp) {
            return Math.round(dp * context.getResources().getDisplayMetrics().density);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ViewHolder – Team Radio
    // ─────────────────────────────────────────────────────────────

    public static class TeamRadioViewHolder extends RecyclerView.ViewHolder {

        private final TextView driverText;
        private final TextView timeText;
        private final ImageView playButton;

        public TeamRadioViewHolder(@NonNull View itemView) {
            super(itemView);
            driverText = itemView.findViewById(R.id.team_radio_driver);
            timeText   = itemView.findViewById(R.id.team_radio_time);
            playButton = itemView.findViewById(R.id.team_radio_play_button);
        }

        public void bind(TeamRadioMessage msg) {
            driverText.setText("TEAM RADIO – Driver #" + msg.getDriverNumber());

            String date = msg.getDate();
            if (date != null && date.length() >= 19) {
                timeText.setText(date.substring(11, 19));
            } else {
                timeText.setText(date != null ? date : "");
            }

            String url = msg.getRecordingUrl();
            if (url != null && !url.isEmpty()) {
                playButton.setAlpha(1f);
                playButton.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        v.getContext().startActivity(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to open team radio URL: " + url, e);
                    }
                });
            } else {
                playButton.setAlpha(0.3f);
                playButton.setOnClickListener(null);
            }
        }
    }
}
