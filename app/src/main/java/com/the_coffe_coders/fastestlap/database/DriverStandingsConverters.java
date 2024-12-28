package com.the_coffe_coders.fastestlap.database;
import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandingsElement;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandingsElement;

import java.lang.reflect.Type;
import java.util.List;

public class DriverStandingsConverters {

    @TypeConverter
    public static String fromDriverStandingsElementList(List<DriverStandingsElement> list) {
        Gson gson = new Gson();
        return gson.toJson(list);
    }

    @TypeConverter
    public static List<DriverStandingsElement> toDrvierStandingsElementList(String json) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<DriverStandingsElement>>() {}.getType();
        return gson.fromJson(json, listType);
    }

    @TypeConverter
    public static String fromConstructorStandingsElementList(List<ConstructorStandingsElement> list) {
        Gson gson = new Gson();
        return gson.toJson(list);
    }

    @TypeConverter
    public static List<ConstructorStandingsElement> toConstructorStandingsElementList(String json) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<ConstructorStandingsElement>>() {}.getType();
        return gson.fromJson(json, listType);
    }

    @TypeConverter
    public static String fromDriver(Driver driver) {
        Gson gson = new Gson();
        return gson.toJson(driver);
    }

    @TypeConverter
    public static Driver toDriver(String json) {
        Gson gson = new Gson();
        Type listType = new TypeToken<Driver>() {}.getType();
        return gson.fromJson(json, listType);
    }


    @TypeConverter
    public static String fromConstructorList(List<Constructor> list) {
        Gson gson = new Gson();
        return gson.toJson(list);
    }

    @TypeConverter
    public static List<Constructor> toConstructorList(String json) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Constructor>>() {}.getType();
        return gson.fromJson(json, listType);
    }
}
