package com.the_coffe_coders.fastestlap.util.chart;

import lombok.Getter;

@Getter
public class SeasonPerformancePoint {
    private final int round;
    private final String roundLabel;
    private final int position; // 1 to 22, or 23 for DNF
    private final String positionText; // "1", "2", "DNF", "DNS", etc.
    private final boolean isDnf;
    private final int sessionType;
    private final String raceName;
    private final String nationAbbr;
    private final String entityId;
    private final String entityName;

    public SeasonPerformancePoint(int round, String roundLabel, int position, String positionText,
                                  boolean isDnf, int sessionType, String raceName,
                                  String nationAbbr, String entityId, String entityName) {
        this.round = round;
        this.roundLabel = roundLabel;
        this.position = position;
        this.positionText = positionText;
        this.isDnf = isDnf;
        this.sessionType = sessionType;
        this.raceName = raceName;
        this.nationAbbr = nationAbbr;
        this.entityId = entityId;
        this.entityName = entityName;
    }

}
