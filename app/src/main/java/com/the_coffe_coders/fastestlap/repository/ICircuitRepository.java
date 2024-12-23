package com.the_coffe_coders.fastestlap.repository;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Circuit;
import java.util.List;

public interface ICircuitRepository {
    List<Circuit> findAll();

    Circuit find(String circuitId);
}
