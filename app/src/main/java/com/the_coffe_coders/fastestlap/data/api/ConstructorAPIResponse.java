package com.the_coffe_coders.fastestlap.data.api;

import com.google.gson.annotations.SerializedName;
import com.the_coffe_coders.fastestlap.data.dto.ConstructorTableDTO;

public class ConstructorAPIResponse extends APIResponse {
    @SerializedName("ConstructorTable")
    private ConstructorTableDTO constructorTableDTO;

    public ConstructorAPIResponse(String xmlns, String series, String url, String limit, String offset, String total, ConstructorTableDTO constructorTableDTO) {
        super(xmlns, series, url, limit, offset, total);
        this.constructorTableDTO = constructorTableDTO;
    }

    public ConstructorTableDTO getConstructorTableDTO() {
        return constructorTableDTO;
    }

    public void setConstructorTableDTO(ConstructorTableDTO constructorTableDTO) {
        this.constructorTableDTO = constructorTableDTO;
    }

    @Override
    public String toString() {
        return "ConstructorAPIResponse{" + super.toString() + "ConstructorTable=" + constructorTableDTO + "}";
    }
}
