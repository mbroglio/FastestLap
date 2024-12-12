package com.the_coffe_coders.fastestlap.domain.grand_prix;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CircuitTable {
    private String season;
    @SerializedName("Circuits")
    private List<Circuit> circuits;

    public List<Circuit> getCircuits() {
        return circuits;
    }

    public void setCircuits(List<Circuit> circuits) {
        this.circuits = circuits;
    }

    @NonNull
    @Override
    public String toString() {
        return "CircuitTable " + circuits;
    }
}
