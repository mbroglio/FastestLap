package com.the_coffe_coders.fastestlap.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.grand_prix.QualifyingResult;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.UIUtils;

import java.util.List;
import java.util.Objects;

public class QualifyingResultsRecyclerAdapter extends RecyclerView.Adapter<QualifyingResultsRecyclerAdapter.ResultViewHolder>{

    private final List<QualifyingResult> qualifyingResultList;
    private final Context context;
    public QualifyingResultsRecyclerAdapter(Context context, List<QualifyingResult> qualifyingResultList){
        this.context = context;
        this.qualifyingResultList = qualifyingResultList;
    }

    @NonNull
    @Override
    public ResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
       View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.qualifying_result_item, parent, false);
       return new ResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResultViewHolder holder, int position) {
        QualifyingResult qualifyingResult = qualifyingResultList.get(position);

        UIUtils.multipleSetTextViewText(
                new String[]{
                        String.valueOf(position + 1),
                        qualifyingResult.getDriver().getFamilyName()},
                new TextView[]{
                        holder.positionText,
                        holder.driverName});

        UIUtils.setTextViewTextWithCondition(qualifyingResult.getQ1()!=null && !qualifyingResult.getQ1().isEmpty(),
                qualifyingResult.getQ1(),
                context.getString(R.string.separator_high_dash),
                holder.q1Time);

        UIUtils.setTextViewTextWithCondition(qualifyingResult.getQ2()!=null && !qualifyingResult.getQ2().isEmpty(),
                qualifyingResult.getQ2(),
                context.getString(R.string.separator_high_dash),
                holder.q2Time);

        UIUtils.setTextViewTextWithCondition(qualifyingResult.getQ3()!=null && !qualifyingResult.getQ3().isEmpty(),
                qualifyingResult.getQ3(),
                context.getString(R.string.separator_high_dash),
                holder.q3Time);

        String teamId = qualifyingResult.getConstructor().getConstructorId();
        Integer teamColorObj = Constants.TEAM_COLOR.get(teamId);
        int teamColor = ContextCompat.getColor(context,
                Objects.requireNonNullElseGet(teamColorObj, () -> R.color.white));
        holder.teamColorIndicator.setBackgroundColor(teamColor);
    }

    @Override
    public int getItemCount() {
        return qualifyingResultList.size();
    }

    public static class ResultViewHolder extends RecyclerView.ViewHolder{
        final TextView positionText;
        final TextView driverName;
        final TextView q1Time;
        final TextView q2Time;
        final TextView q3Time;
        final View teamColorIndicator;

        ResultViewHolder(View itemView){
            super(itemView);
            positionText = itemView.findViewById(R.id.position_text);
            driverName = itemView.findViewById(R.id.driver_name);
            q1Time = itemView.findViewById(R.id.q1_time);
            q2Time = itemView.findViewById(R.id.q2_time);
            q3Time = itemView.findViewById(R.id.q3_time);
            teamColorIndicator = itemView.findViewById(R.id.team_color_indicator);
        }
    }
}
