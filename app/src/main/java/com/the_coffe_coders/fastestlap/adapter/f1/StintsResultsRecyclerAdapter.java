package com.the_coffe_coders.fastestlap.adapter.f1;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.f1.result.RaceResult;
import com.the_coffe_coders.fastestlap.domain.f1.result.Stint;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter per la visualizzazione degli stint dei pneumatici per ciascun pilota,
 * ordinati per posizione di arrivo e con grafica a tutta larghezza proporzionale ai giri dello stint.
 */
public class StintsResultsRecyclerAdapter extends RecyclerView.Adapter<StintsResultsRecyclerAdapter.StintViewHolder> {

    private final Context context;
    private final List<DriverStints> driverStintsList = new ArrayList<>();
    private List<RaceResult> raceResults;

    private int maxRaceLaps = 1;
    private final Map<Integer, RaceResult> driverToResultMap = new HashMap<>();

    public StintsResultsRecyclerAdapter(Context context, List<Stint> stints, List<RaceResult> raceResults) {
        this.context = context;
        this.raceResults = raceResults;
        groupAndSortStints(stints, raceResults);
    }

    public StintsResultsRecyclerAdapter(Context context, List<Stint> stints) {
        this(context, stints, null);
    }

    public void updateStints(List<Stint> stints, List<RaceResult> raceResults) {
        this.raceResults = raceResults;
        groupAndSortStints(stints, raceResults);
        notifyDataSetChanged();
    }

    private void groupAndSortStints(List<Stint> stints, List<RaceResult> raceResults) {
        driverStintsList.clear();
        driverToResultMap.clear();
        if (stints == null || stints.isEmpty()) return;

        // Raggruppamento per numero di pilota
        Map<Integer, DriverStints> map = new LinkedHashMap<>();
        for (Stint stint : stints) {
            DriverStints ds = map.get(stint.getDriverNumber());
            if (ds == null) {
                ds = new DriverStints(stint.getDriverNumber());
                map.put(stint.getDriverNumber(), ds);
            }
            ds.stints.add(stint);
        }

        // Mappatura numero pilota -> posizione di arrivo e RaceResult
        Map<Integer, String> driverToPosMap = new HashMap<>();
        if (raceResults != null) {
            for (RaceResult res : raceResults) {
                if (res != null && res.getDriver() != null) {
                    if (res.getDriver().getPermanentNumber() != null) {
                        try {
                            int num = Integer.parseInt(res.getDriver().getPermanentNumber());
                            driverToPosMap.put(num, res.getPosition());
                            driverToResultMap.put(num, res);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }

        // Calcolo del numero totale di giri di gara (maxRaceLaps)
        maxRaceLaps = calculateMaxRaceLaps(stints, raceResults);

        List<DriverStints> list = new ArrayList<>(map.values());
        for (DriverStints ds : list) {
            ds.position = driverToPosMap.get(ds.driverNumber);
        }

        // Ordinamento per posizione di arrivo crescente
        list.sort((ds1, ds2) -> {
            int p1 = parsePosition(ds1.position);
            int p2 = parsePosition(ds2.position);
            if (p1 != p2) return Integer.compare(p1, p2);
            return Integer.compare(ds1.driverNumber, ds2.driverNumber);
        });

        driverStintsList.addAll(list);
    }

    private int calculateMaxRaceLaps(List<Stint> stints, List<RaceResult> raceResults) {
        int maxLaps = 1;
        if (stints != null) {
            for (Stint s : stints) {
                if (s.getLapEnd() != null && s.getLapEnd() > maxLaps) {
                    maxLaps = s.getLapEnd();
                } else if (s.getLapStart() != null && s.getLapStart() > maxLaps) {
                    maxLaps = s.getLapStart();
                }
            }
        }
        if (raceResults != null) {
            for (RaceResult res : raceResults) {
                if (res != null && res.getLaps() != null) {
                    try {
                        int laps = Integer.parseInt(res.getLaps());
                        if (laps > maxLaps) {
                            maxLaps = laps;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return maxLaps;
    }

    private int parsePosition(String posStr) {
        if (posStr == null || posStr.isEmpty()) return 999;
        try {
            return Integer.parseInt(posStr);
        } catch (NumberFormatException e) {
            return 999;
        }
    }

    @NonNull
    @Override
    public StintViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.stint_result_item, parent, false);
        return new StintViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StintViewHolder holder, int position) {
        DriverStints ds = driverStintsList.get(position);

        // Posizione di arrivo (al posto del numero di gara)
        String posText = (ds.position != null && !ds.position.isEmpty()) ? ds.position : String.valueOf(position + 1);
        UIUtils.singleSetTextViewText(posText, holder.positionText);

        // Nome Pilota (da Constants.DRIVER_NUMBER_NAME)
        String driverName = Constants.DRIVER_NUMBER_NAME.get(String.valueOf(ds.driverNumber));
        if (driverName == null || driverName.isEmpty()) {
            driverName = "Driver #" + ds.driverNumber;
        }
        UIUtils.singleSetTextViewText(driverName, holder.driverName);

        // Numero di Pit Stop = (numero di stint - 1)
        int pitCount = Math.max(0, ds.stints.size() - 1);
        String pitText = pitCount + " " + (pitCount == 1 ? "PIT" : "PITS");
        UIUtils.singleSetTextViewText(pitText, holder.pitNumber);

        RaceResult driverResult = driverToResultMap.get(ds.driverNumber);
        if (driverResult != null && driverResult.getConstructor() != null && holder.teamColorIndicator != null) {
            String teamId = driverResult.getConstructor().getConstructorId();
            Integer teamColorObj = Constants.TEAM_COLOR.get(teamId);
            int color = androidx.core.content.ContextCompat.getColor(context, java.util.Objects.requireNonNullElseGet(teamColorObj, () -> R.color.mercedes_f1));
            holder.teamColorIndicator.setBackgroundColor(color);
        }

        // Generazione grafica degli stint
        holder.stintsContainer.removeAllViews();

        int driverWeightSum = 0;
        List<Integer> stintWeights = new ArrayList<>();

        for (int i = 0; i < ds.stints.size(); i++) {
            Stint stint = ds.stints.get(i);
            int actualLaps = calculateStintLaps(stint, i, ds.stints, driverResult);
            // Garantisce un peso minimo di 3 giri per ospitare l'icona della mescola ed evitare sovrapposizioni
            int weight = Math.max(3, actualLaps);
            stintWeights.add(weight);
            driverWeightSum += weight;
        }

        // Il weightSum della riga si adatta per garantire che nessuna icona venga tagliata a destra
        float rowWeightSum = Math.max((float) maxRaceLaps, (float) driverWeightSum);
        holder.stintsContainer.setWeightSum(rowWeightSum);

        for (int i = 0; i < ds.stints.size(); i++) {
            Stint stint = ds.stints.get(i);
            int weight = stintWeights.get(i);
            boolean isFirstStint = (i == 0);
            View stintGraphicView = createStintGraphicView(stint, weight, isFirstStint);
            holder.stintsContainer.addView(stintGraphicView);
        }
    }

    private int calculateStintLaps(Stint stint, int stintIndex, List<Stint> allDriverStints, RaceResult raceResult) {
        int lapStart = (stint.getLapStart() != null && stint.getLapStart() > 0) ? stint.getLapStart() : 1;
        int lapEnd;
        if (stint.getLapEnd() != null && stint.getLapEnd() >= lapStart) {
            lapEnd = stint.getLapEnd();
        } else if (stintIndex + 1 < allDriverStints.size() && allDriverStints.get(stintIndex + 1).getLapStart() != null) {
            lapEnd = allDriverStints.get(stintIndex + 1).getLapStart() - 1;
        } else if (raceResult != null && raceResult.getLaps() != null) {
            try {
                lapEnd = Integer.parseInt(raceResult.getLaps());
            } catch (NumberFormatException e) {
                lapEnd = maxRaceLaps;
            }
        } else {
            lapEnd = maxRaceLaps;
        }
        return Math.max(1, lapEnd - lapStart + 1);
    }

    private View createStintGraphicView(Stint stint, int displayWeight, boolean isFirstStint) {
        LinearLayout stintLayout = new LinearLayout(context);
        stintLayout.setOrientation(LinearLayout.HORIZONTAL);
        stintLayout.setGravity(Gravity.TOP);

        int lapStart = (stint.getLapStart() != null) ? stint.getLapStart() : 1;

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, (float) displayWeight
        );
        layoutParams.setMargins(0, 0, dpToPx(1), 0);
        stintLayout.setLayoutParams(layoutParams);

        // Colonna con Icona Mescola + Numero Giro Iniziale dello stint + Tempo Pit Stop
        LinearLayout compoundColumn = new LinearLayout(context);
        compoundColumn.setOrientation(LinearLayout.VERTICAL);
        compoundColumn.setGravity(Gravity.CENTER_HORIZONTAL);

        ImageView compoundIcon = new ImageView(context);
        compoundIcon.setImageResource(getTyreDrawableResource(stint.getCompound()));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                dpToPx(16), dpToPx(16)
        );
        compoundIcon.setLayoutParams(iconParams);

        TextView lapStartTextView = new TextView(context);
        if (isFirstStint) {
            // Per lo stint di partenza (inizio gara), il numero del giro (sempre 1) viene rimosso
            lapStartTextView.setText(" ");
            lapStartTextView.setVisibility(View.INVISIBLE);
        } else {
            lapStartTextView.setText(String.valueOf(lapStart));
            lapStartTextView.setVisibility(View.VISIBLE);
        }
        lapStartTextView.setTextSize(8.5f);
        lapStartTextView.setTextColor(Color.WHITE);
        lapStartTextView.setGravity(Gravity.CENTER);
        lapStartTextView.setSingleLine(true);

        TextView pitDurationTextView = new TextView(context);
        pitDurationTextView.setTextSize(7.5f);
        pitDurationTextView.setGravity(Gravity.CENTER);
        pitDurationTextView.setSingleLine(true);

        if (stint.getPitDuration() != null && stint.getPitDuration() > 0) {
            String durationStr = String.format(java.util.Locale.US, "%.1fs", stint.getPitDuration());
            pitDurationTextView.setText(durationStr);
            pitDurationTextView.setTextColor(Color.parseColor("#FFE800"));
            pitDurationTextView.setVisibility(View.VISIBLE);
        } else {
            // Placeholder invisibile per garantire l'altezza uniforme e l'allineamento orizzontale delle ruote
            pitDurationTextView.setText(" ");
            pitDurationTextView.setVisibility(View.INVISIBLE);
        }

        compoundColumn.addView(compoundIcon);
        compoundColumn.addView(lapStartTextView);
        compoundColumn.addView(pitDurationTextView);

        // Linea orizzontale del colore della mescola (icona 16dp -> centro a 8dp; linea 5dp -> topMargin = 5.5dp)
        View stintLine = new View(context);
        stintLine.setBackgroundColor(getCompoundColor(stint.getCompound()));
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(
                0, dpToPx(5), 1.0f
        );
        lineParams.setMargins(dpToPx(2), dpToPx(6), 0, 0);
        stintLine.setLayoutParams(lineParams);

        stintLayout.addView(compoundColumn);
        stintLayout.addView(stintLine);

        return stintLayout;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    private int getTyreDrawableResource(String compound) {
        if (compound == null) return R.drawable.medium_tyre_icon;
        switch (compound.toUpperCase()) {
            case "SOFT":
                return R.drawable.soft_tyre_icon;
            case "HARD":
                return R.drawable.hard_tyre_icon;
            case "INTERMEDIATE":
                return R.drawable.intermediate_tyre_icon;
            case "WET":
                return R.drawable.wet_tyre_icon;
            case "MEDIUM":
            default:
                return R.drawable.medium_tyre_icon;
        }
    }

    private int getCompoundColor(String compound) {
        if (compound == null) return Color.parseColor("#FFE800");
        switch (compound.toUpperCase()) {
            case "SOFT":
                return Color.parseColor("#FF1801");
            case "HARD":
                return Color.parseColor("#FFFFFF");
            case "INTERMEDIATE":
                return Color.parseColor("#39B54A");
            case "WET":
                return Color.parseColor("#00AEEF");
            case "MEDIUM":
            default:
                return Color.parseColor("#FFE800");
        }
    }

    @Override
    public int getItemCount() {
        return driverStintsList.size();
    }

    public static class StintViewHolder extends RecyclerView.ViewHolder {
        public final TextView positionText;
        public final View teamColorIndicator;
        public final TextView driverName;
        public final TextView pitNumber;
        public final LinearLayout stintsContainer;

        public StintViewHolder(@NonNull View itemView) {
            super(itemView);
            positionText = itemView.findViewById(R.id.position_text);
            teamColorIndicator = itemView.findViewById(R.id.team_color_indicator);
            driverName = itemView.findViewById(R.id.driver_name);
            pitNumber = itemView.findViewById(R.id.pit_number);
            stintsContainer = itemView.findViewById(R.id.stints_container);
        }
    }

    public static class DriverStints {
        public final int driverNumber;
        public String position;
        public final List<Stint> stints = new ArrayList<>();

        public DriverStints(int driverNumber) {
            this.driverNumber = driverNumber;
        }
    }
}
