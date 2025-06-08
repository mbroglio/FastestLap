package com.the_coffe_coders.fastestlap.api;

import com.google.gson.annotations.SerializedName;
import com.the_coffe_coders.fastestlap.dto.ConstructorTableDTO;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
@Setter
public class ConstructorAPIResponse extends APIResponse {
    @SerializedName("ConstructorTable")
    private ConstructorTableDTO constructorTableDTO;

    public ConstructorAPIResponse(String xmlns, String series, String url, String limit, String offset, String total, ConstructorTableDTO constructorTableDTO) {
        super(xmlns, series, url, limit, offset, total);
        this.constructorTableDTO = constructorTableDTO;
    }
}
