package com.the_coffe_coders.fastestlap.domain.driver;

import java.util.List;

public class DriverAPIResponse {
    private String status;
    private int totalResults;
    private List<Driver> drivers;

    public DriverAPIResponse() {

    }

    public DriverAPIResponse(List<Driver> drivers) {
        this.drivers = drivers;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }

    public List<Driver> getDrivers() {
        return drivers;
    }

    public void setDrivers(List<Driver> drivers) {
        this.drivers = drivers;
    }
}