package com.the_coffe_coders.fastestlap.data;

public class DriverRepositoryTest extends TestCase {

    public void testFindAll() {
        List<Driver> driverList = new ArrayList<>();
        driverList.add(new Driver("Max Pera", "ds", "ITA", 90, 900, 200, "20", "Ferrari", null));
        driverList.add(new Driver("Max Sroll", "ds", "ITA", 90, 900, 200, "20", "Ferrari", null));
        DriverRepository repository = new DriverRepository();
        IDriverRepository driverRepository = new DriverRepositoryProxy(repository);
        assertEquals(driverList, driverRepository.findAll());
    }
}