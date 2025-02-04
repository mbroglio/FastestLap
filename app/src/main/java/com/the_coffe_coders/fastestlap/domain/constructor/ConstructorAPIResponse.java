package com.the_coffe_coders.fastestlap.domain.constructor;


import java.util.List;

public class ConstructorAPIResponse {
    private String status;
    private int totalResults;
    private List<Constructor> constructors;

    public ConstructorAPIResponse() {

    }

    public ConstructorAPIResponse(List<Constructor> constructors) {
        this.constructors = constructors;
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

    public List<Constructor> getConstructors() {
        return constructors;
    }

    public void setConstructors(List<Constructor> constructors) {
        this.constructors = constructors;
    }


}
