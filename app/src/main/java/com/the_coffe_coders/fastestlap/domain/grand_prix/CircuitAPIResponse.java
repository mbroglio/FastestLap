package com.the_coffe_coders.fastestlap.domain.grand_prix;

import com.google.gson.annotations.SerializedName;
import com.the_coffe_coders.fastestlap.domain.constructor.StandingsTable;

import java.util.List;

public class CircuitAPIResponse {
    private String xmlns;
    private String series;
    private String url;
    private String limit;
    private String offset;
    private String total;

    @SerializedName("CircuitTable")
    private CircuitTable circuitTable;
    public CircuitAPIResponse(String xmlns, String series, String url, String limit, String offset, String total, CircuitTable circuitTable) {
        this.xmlns = xmlns;
        this.series = series;
        this.url = url;
        this.limit = limit;
        this.offset = offset;
        this.total = total;
        this.circuitTable = circuitTable;
    }

    public CircuitTable getCircuitTable() {
        return circuitTable;
    }

    public void setCircuitTable(CircuitTable circuitTable) {
        this.circuitTable = circuitTable;
    }

    public String getXmlns() {
        return xmlns;
    }

    public void setXmlns(String xmlns) {
        this.xmlns = xmlns;
    }

    public String getSeries() {
        return series;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLimit() {
        return limit;
    }

    public void setLimit(String limit) {
        this.limit = limit;
    }

    public String getOffset() {
        return offset;
    }

    public void setOffset(String offset) {
        this.offset = offset;
    }

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }
}
