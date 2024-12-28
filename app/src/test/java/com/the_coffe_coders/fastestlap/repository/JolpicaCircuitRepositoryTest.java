package com.the_coffe_coders.fastestlap.repository;

import junit.framework.TestCase;

import com.the_coffe_coders.fastestlap.domain.grand_prix.Circuit;
import com.the_coffe_coders.fastestlap.repository.circuit.CircuitRepository;

import java.util.List;

public class JolpicaCircuitRepositoryTest extends TestCase {

    public void testFindAll() {
        CircuitRepository circuitRepository = new CircuitRepository();
        List<Circuit> circuits = circuitRepository.findAll();

        System.out.println("Circuits:");

        if(circuits.isEmpty()){
            System.out.println("Nessun circuito trovato");
        }

        for (Circuit circuit:circuits) {
            System.out.println(circuit.getCircuitName());
        }
        assertTrue(true);
    }

    public void testFind() {

    }
}