package com.the_coffe_coders.fastestlap.adapter.junior;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.junior.standings.JuniorConstructorStandings;
import com.the_coffe_coders.fastestlap.domain.junior.standings.JuniorConstructorStandingsElement;
import com.the_coffe_coders.fastestlap.domain.junior.standings.JuniorDriverStandings;
import com.the_coffe_coders.fastestlap.domain.junior.standings.JuniorDriverStandingsElement;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import java.util.Objects;

public class JuniorStandingsRecyclerAdapter extends RecyclerView.Adapter<JuniorStandingsRecyclerAdapter.JuniorStandingsViewHolder> {
    private static final String TAG = "JuniorStandingsRecyclerAdapter";

    private final Context context;
    private final JuniorDriverStandings driverStandings;
    private final JuniorConstructorStandings constructorStandings;
    private final int contentType;

    public JuniorStandingsRecyclerAdapter(Context context, JuniorDriverStandings driverStandings) {
        this.context = context;
        this.driverStandings = driverStandings;
        this.constructorStandings = null;
        this.contentType = 0;
    }

    public JuniorStandingsRecyclerAdapter(Context context, JuniorConstructorStandings constructorStandings) {
        this.context = context;
        this.driverStandings = null;
        this.constructorStandings = constructorStandings;
        this.contentType = 1;
    }

    @NonNull
    @Override
    public JuniorStandingsRecyclerAdapter.JuniorStandingsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.junior_standings_card, parent, false);
        return new JuniorStandingsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JuniorStandingsRecyclerAdapter.JuniorStandingsViewHolder holder, int position) {
        switch (contentType) {
            case 0:
                manageDriverStandings(holder, position);
                break;
            case 1:
                manageConstructorStandings(holder, position);
                break;
        }
    }

    private void manageDriverStandings(JuniorStandingsViewHolder holder, int position) {
        JuniorDriverStandingsElement element = driverStandings.getDriverStandings().get(position);
        UIUtils.multipleSetTextViewText(
                new String[]{
                        element.getPosition(),
                        element.getDriver(),
                        element.getPoints()
                },
                new TextView[]{
                        holder.elementPosition,
                        holder.elementName,
                        holder.elementPoints});

        String matchedKey = UIUtils.findMatchingKeyInMap(element.getTeam(), Constants.JUNIOR_TEAM_GRADIENT_COLOR);

        holder.innerLayout.setBackground(AppCompatResources
                .getDrawable(context, Objects.requireNonNull(Constants.JUNIOR_TEAM_GRADIENT_COLOR.get(matchedKey))));

    }

    private void manageConstructorStandings(JuniorStandingsViewHolder holder, int position) {
        JuniorConstructorStandingsElement element = constructorStandings.getConstructorStandings().get(position);
        UIUtils.multipleSetTextViewText(
                new String[]{
                        element.getPosition(),
                        element.getTeam(),
                        element.getPoints()
                },
                new TextView[]{
                        holder.elementPosition,
                        holder.elementName,
                        holder.elementPoints});

        String matchedKey = UIUtils.findMatchingKeyInMap(element.getTeam(), Constants.JUNIOR_TEAM_GRADIENT_COLOR);

        holder.innerLayout.setBackground(AppCompatResources
                .getDrawable(context, Objects.requireNonNull(Constants.JUNIOR_TEAM_GRADIENT_COLOR.get(matchedKey))));
    }

    @Override
    public int getItemCount() {
        switch (contentType) {
            case 0:
                return driverStandings.getDriverStandings().size();
            case 1:
                return constructorStandings.getConstructorStandings().size();
        }
        return 0;
    }

    public static class JuniorStandingsViewHolder extends RecyclerView.ViewHolder {
        private final TextView elementPosition, elementName, elementPoints;
        private final RelativeLayout innerLayout;

        public JuniorStandingsViewHolder(@NonNull View itemView) {
            super(itemView);
            innerLayout = itemView.findViewById(R.id.small_card);
            elementPosition = itemView.findViewById(R.id.element_position);
            elementName = itemView.findViewById(R.id.element_name);
            elementPoints = itemView.findViewById(R.id.element_points);
        }
    }
}
