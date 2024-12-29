package com.the_coffe_coders.fastestlap.mapper;

import com.the_coffe_coders.fastestlap.domain.grand_prix.Circuit;
import com.the_coffe_coders.fastestlap.dto.CircuitDTO;

public class CircuitMapper {

    public static Circuit toCircuit(CircuitDTO circuitDTO) {
        Circuit circuit = new Circuit();
        circuit.setCircuitId(circuitDTO.getCircuitId());
        circuit.setCircuitName(circuitDTO.getCircuitName());
        circuit.setUrl(circuitDTO.getUrl());
        circuit.setLocation(LocationMapper.toLocation(circuitDTO.getLocation()));
        return circuit;
    }
}
