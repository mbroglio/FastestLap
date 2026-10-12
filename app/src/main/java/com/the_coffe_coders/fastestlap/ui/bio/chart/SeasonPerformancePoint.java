package com.the_coffe_coders.fastestlap.ui.bio.chart;

public class SeasonPerformancePoint {
    private final int round;
    private final String roundLabel;
    private final int position; // 1 to 20, or 21 for DNF
    private final String positionText; // "1", "2", "DNF", "DNS", etc.
    private final boolean isDnf;
    private final boolean isSprint;
    private final String raceName;
    private final String entityId;
    private final String entityName;

    public SeasonPerformancePoint(int round, String roundLabel, int position, String positionText,
                                  boolean isDnf, boolean isSprint, String raceName,
                                  String entityId, String entityName) {
        this.round = round;
        this.roundLabel = roundLabel;
        this.position = position;
        this.positionText = positionText;
        this.isDnf = isDnf;
        this.isSprint = isSprint;
        this.raceName = raceName;
        this.entityId = entityId;
        this.entityName = entityName;
    }

    public int getRound() {
        return round;
    }

    public String getRoundLabel() {
        return roundLabel;
    }

    public int getPosition() {
        return position;
    }

    public String getPositionText() {
        return positionText;
    }

    public boolean isDnf() {
        return isDnf;
    }

    public boolean isSprint() {
        return isSprint;
    }

    public String getRaceName() {
        return raceName;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getEntityName() {
        return entityName;
    }
}
