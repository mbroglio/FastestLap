package com.the_coffe_coders.fastestlap;

public class Driver implements Comparable<Driver> {
    private int driverNo;      // Unique identifier for the driver
    private double gapToLeader;   // Interval to the leader (in seconds)
    private double interval;      // Interval to the previous driver (in seconds)

    public Driver(int driverNo, double gapToLeader, double interval) {
        this.driverNo = driverNo;
        this.gapToLeader = gapToLeader;
        this.interval = interval;
    }

    // Getters and setters
    public int getDriverNo() {
        return driverNo;
    }

    public double getGapToLeader() {
        return gapToLeader;
    }

    public void setGapToLeader(double gapToLeader) {
        this.gapToLeader = gapToLeader;
    }

    public double getInterval() {
        return interval;
    }

    public void setInterval(double interval) {
        this.interval = interval;
    }

    public void updateInterval(double interval) {
        setInterval(interval);
    }

    public void updateGapToLeader(double gapToLeader) {
        setGapToLeader(gapToLeader);
    }

    public void updateTimings(double interval, double gapToLeader) {
        updateInterval(interval);
        updateGapToLeader(gapToLeader);
    }

    // Override compareTo for sorting by gapToLeader
    @Override
    public int compareTo(Driver other) {
        return Double.compare(this.gapToLeader, other.gapToLeader);
    }

    // Override equals based on unique driverId
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Driver driver = (Driver) obj;
        return driverNo == driver.driverNo;
    }

    // Override hashCode based on unique driverId
    @Override
    public int hashCode() {
        return driverNo;
    }

    @Override
    public String toString() {
        return "Driver{" +
                "driverId='" + driverNo + '\'' +
                ", gapToLeader=" + gapToLeader +
                ", interval=" + interval +
                '}';
    }
}
