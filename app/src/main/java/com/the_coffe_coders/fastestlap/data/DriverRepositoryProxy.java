package com.the_coffe_coders.fastestlap.data;

import com.the_coffe_coders.fastestlap.domain.Driver;

import java.util.List;

public class DriverRepositoryProxy implements IDriverRepository{
    private final DriverRepository driverRepository;


    public DriverRepositoryProxy(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    @Override
    public List<Driver> findAll() {
        System.out.println("Proxy: Recupero di tutti gli utenti con controlli aggiuntivi...");
        return driverRepository.findAll();
    }
}
