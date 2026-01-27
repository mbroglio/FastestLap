package com.the_coffe_coders.fastestlap.adapter.junior;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.junior.calendar.JuniorCalendar;
import com.the_coffe_coders.fastestlap.domain.junior.calendar.JuniorCalendarElement;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

public class JuniorCalendarRecyclerAdapter extends RecyclerView.Adapter<JuniorCalendarRecyclerAdapter.JuniorCalendarViewHolder> {

    private final Context context;
    ;
    private final JuniorCalendar calendar;

    public JuniorCalendarRecyclerAdapter(Context context, JuniorCalendar calendar) {
        this.context = context;
        this.calendar = calendar;
    }

    @NonNull
    @Override
    public JuniorCalendarRecyclerAdapter.JuniorCalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.junior_event_calendar_card, parent, false);
        return new JuniorCalendarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JuniorCalendarRecyclerAdapter.JuniorCalendarViewHolder holder, int position) {
        Log.i("JuniorCalendarRecyclerAdapter", "calendar: " + calendar.getEvents());
        JuniorCalendarElement element = calendar.getEvents().get(position);
        holder.eventRound.setText(context.getString(R.string.round_upper_case_plus_value, element.getRound()));
        holder.eventName.setText(element.getCircuit());

        UIUtils.loadImageWithGlide(context, element.getNation_flag_url(), holder.nationFlag,
                () -> setDates(holder, element));
    }

    private void setDates(JuniorCalendarViewHolder holder, JuniorCalendarElement element) {
        String sprintDay = element.getSprint_date().split(" ")[0];
        String sprintMonth = element.getSprint_date().split(" ")[1].substring(0, 3);

        String featureDay = element.getFeature_date().split(" ")[0];
        String featureMonth = element.getFeature_date().split(" ")[1].substring(0, 3);

        holder.sprintDateDay.setText(sprintDay);
        holder.sprintDateMonth.setText(sprintMonth);
        holder.featureDateDay.setText(featureDay);
        holder.featureDateMonth.setText(featureMonth);
    }

    @Override
    public int getItemCount() {
        return calendar.getEvents().size();
    }

    public static class JuniorCalendarViewHolder extends RecyclerView.ViewHolder {

        final TextView eventRound, eventName, sprintDateDay, sprintDateMonth, featureDateDay, featureDateMonth;
        final ImageView nationFlag;

        public JuniorCalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            eventRound = itemView.findViewById(R.id.gp_round_number);
            eventName = itemView.findViewById(R.id.gp_name);
            sprintDateDay = itemView.findViewById(R.id.sprint_date);
            sprintDateMonth = itemView.findViewById(R.id.sprint_month);
            featureDateDay = itemView.findViewById(R.id.feature_date);
            featureDateMonth = itemView.findViewById(R.id.feature_month);
            nationFlag = itemView.findViewById(R.id.event_nation_flag);
        }
    }
}
