package com.the_coffe_coders.fastestlap.domain.f1.result;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Modello di dominio per le informazioni sulla sosta del pit stop restituite dall'endpoint /v1/pit di OpenF1.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PitStopInfo {
    private int lapNumber;
    private double duration;
}
