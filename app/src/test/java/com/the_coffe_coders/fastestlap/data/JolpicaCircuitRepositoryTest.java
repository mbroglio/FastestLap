package com.the_coffe_coders.fastestlap.data;

import junit.framework.TestCase;

import com.the_coffe_coders.fastestlap.domain.grand_prix.Circuit;

import java.util.List;

public class JolpicaCircuitRepositoryTest extends TestCase {

    public void testFindAll() {
        JolpicaCircuitRepository circuitRepository = new JolpicaCircuitRepository();
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