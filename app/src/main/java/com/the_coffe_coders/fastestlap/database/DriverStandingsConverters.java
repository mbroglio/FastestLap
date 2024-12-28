package com.the_coffe_coders.fastestlap.database;
import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Circuit;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandingsElement;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandingsElement;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Practice;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Qualifying;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Sprint;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;

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

    //Circuit converter
    @TypeConverter
    public static String fromCircuit(Circuit circuit) {
        Gson gson = new Gson();
        return gson.toJson(circuit);
    }

    @TypeConverter
    public static Circuit toCircuit(String json) {
        Gson gson = new Gson();
        Type listType = new TypeToken<Circuit>() {}.getType();
        return gson.fromJson(json, listType);
    }

    //Practice converter
    @TypeConverter
    public static String fromPractice(Practice practice) {
        Gson gson = new Gson();
        return gson.toJson(practice);
    }

    @TypeConverter
    public static Practice toPractice(String json) {
        Gson gson = new Gson();
        Type listType = new TypeToken<Practice>() {}.getType();
        return gson.fromJson(json, listType);
    }

    //Qualifying converter
    @TypeConverter
    public static String fromQualifying(Qualifying qualifying) {
        Gson gson = new Gson();
        return gson.toJson(qualifying);
    }

    @TypeConverter
    public static Qualifying toQualifying(String json) {
        Gson gson = new Gson();
        Type listType = new TypeToken<Qualifying>() {}.getType();
        return gson.fromJson(json, listType);
    }

    //Race converter
    @TypeConverter
    public static String fromRace(Race race) {
        Gson gson = new Gson();
        return gson.toJson(race);
    }

    @TypeConverter
    public static Race toRace(String json) {
        Gson gson = new Gson();
        Type listType = new TypeToken<Race>() {}.getType();
        return gson.fromJson(json, listType);
    }

    //Sprint converter
    @TypeConverter
    public static String fromSprint(Sprint sprint) {
        Gson gson = new Gson();
        return gson.toJson(sprint);
    }

    @TypeConverter
    public static Sprint toSprint(String json) {
        Gson gson = new Gson();
        Type listType = new TypeToken<Sprint>() {}.getType();
        return gson.fromJson(json, listType);
    }

    //WeekelyRace converter
    @TypeConverter
    public static String fromWeeklyRace(WeeklyRace weeklyRace) {
        Gson gson = new Gson();
        return gson.toJson(weeklyRace);
    }

    @TypeConverter
    public static WeeklyRace toWeeklyRace(String json) {
        Gson gson = new Gson();
        Type listType = new TypeToken<WeeklyRace>() {}.getType();
        return gson.fromJson(json, listType);
    }
}
