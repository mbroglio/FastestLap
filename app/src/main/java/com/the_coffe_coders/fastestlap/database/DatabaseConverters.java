package com.the_coffe_coders.fastestlap.database;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandingsElement;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandingsElement;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Practice;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Qualifying;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;
import com.the_coffe_coders.fastestlap.domain.grand_prix.RaceResult;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Session;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Sprint;
import com.the_coffe_coders.fastestlap.domain.grand_prix.SprintQualifying;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Track;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.domain.nation.Nation;

import org.threeten.bp.LocalDateTime;

import java.lang.reflect.Type;
import java.util.List;

public class DatabaseConverters {

    public static Gson gson = new Gson();//TODO MOVE TO SERVICE LOCATOR

    //Driver Standings Element List
    @TypeConverter
    public static String fromDriverStandingsElementList(List<DriverStandingsElement> list) {
        return gson.toJson(list);
    }

    @TypeConverter
    public static List<DriverStandingsElement> toDrvierStandingsElementList(String json) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<DriverStandingsElement>>() {
        }.getType();
        return gson.fromJson(json, listType);
    }

    @TypeConverter
    public static List<RaceResult> toRaceResultList(String json) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<RaceResult>>() {
        }.getType();
        return gson.fromJson(json, listType);
    }

    @TypeConverter
    public static String fromRaceResultList(List<RaceResult> results) {
        return gson.toJson(results);
    }

    @TypeConverter
    public static String fromConstructorDrivers(List<String> drivers){
        return gson.toJson(drivers);
    }

    @TypeConverter
    public static List<String> toConstructorDrivers(String json){
        Type listType = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(json, listType);
    }

    //Constructor Standings Element List
    @TypeConverter
    public static String fromConstructorStandingsElementList(List<ConstructorStandingsElement> list) {
        return gson.toJson(list);
    }

    @TypeConverter
    public static List<ConstructorStandingsElement> toConstructorStandingsElementList(String json) {
        Type listType = new TypeToken<List<ConstructorStandingsElement>>() {
        }.getType();
        return gson.fromJson(json, listType);
    }

    //Driver
    @TypeConverter
    public static String fromDriver(Driver driver) {
        return gson.toJson(driver);
    }

    @TypeConverter
    public static Driver toDriver(String json) {
        Type listType = new TypeToken<Driver>() {
        }.getType();
        return gson.fromJson(json, listType);
    }

    //Contructor
    @TypeConverter
    public static String fromConstructor(Constructor constructor) {
        return gson.toJson(constructor);
    }

    @TypeConverter
    public static Constructor toConstructor(String json) {
        Type listType = new TypeToken<Constructor>() {
        }.getType();
        return gson.fromJson(json, listType);
    }

    //Track
    @TypeConverter
    public static String fromTrack(Track track) {
        return gson.toJson(track);
    }

    @TypeConverter
    public static Track toTrack(String json) {
        Type listType = new TypeToken<Track>() {
        }.getType();
        return gson.fromJson(json, listType);
    }

    //Nation
    @TypeConverter
    public static String fromNation(Nation nation) {
        return gson.toJson(nation);
    }

    @TypeConverter
    public static Nation toNation(String json) {
        Type listType = new TypeToken<Nation>() {
        }.getType();
        return gson.fromJson(json, listType);
    }


    //Contructor List
    @TypeConverter
    public static String fromConstructorList(List<Constructor> list) {
        return gson.toJson(list);
    }

    @TypeConverter
    public static List<Constructor> toConstructorList(String json) {
        Type listType = new TypeToken<List<Constructor>>() {
        }.getType();
        return gson.fromJson(json, listType);
    }

    //Practice
    @TypeConverter
    public static String fromPractice(Practice practice) {
        return gson.toJson(practice);
    }

    @TypeConverter
    public static Practice toPractice(String json) {
        return gson.fromJson(json, Practice.class);
    }

    //Qualifying
    @TypeConverter
    public static String fromQualifying(Qualifying qualifying) {
        return gson.toJson(qualifying);
    }

    @TypeConverter
    public static Qualifying toQualifying(String json) {
        return gson.fromJson(json, Qualifying.class);
    }

    //SprintQualifying
    @TypeConverter
    public static String fromSprintQualifying(SprintQualifying sprintQualifying) {
        return gson.toJson(sprintQualifying);
    }

    @TypeConverter
    public static SprintQualifying toSprintQualifying(String json) {
        return gson.fromJson(json, SprintQualifying.class);
    }

    //LocalDateTime
    @TypeConverter
    public static String fromLocalDateTime(LocalDateTime localDateTime) {
        return gson.toJson(localDateTime);
    }

    @TypeConverter
    public static LocalDateTime toLocalDateTime(String value) {
        return gson.fromJson(value, LocalDateTime.class);
    }

    //Race
    @TypeConverter
    public static String fromRace(Race race) {
        return gson.toJson(race);
    }

    @TypeConverter
    public static Race toRace(String json) {
        return gson.fromJson(json, Race.class);
    }

    //Sprint converter
    @TypeConverter
    public static String fromSprint(Sprint sprint) {
        return gson.toJson(sprint);
    }

    @TypeConverter
    public static Sprint toSprint(String json) {
        return gson.fromJson(json, Sprint.class);
    }

    //WeekelyRace converter
    @TypeConverter
    public static String fromWeeklyRace(WeeklyRace weeklyRace) {
        return gson.toJson(weeklyRace);
    }

    @TypeConverter
    public static WeeklyRace toWeeklyRace(String json) {
        return gson.fromJson(json, WeeklyRace.class);
    }

    //Session
    @TypeConverter
    public static String fromSession(Session session) {
        return gson.toJson(session);
    }

    @TypeConverter
    public static Session toSession(String json) {
        return gson.fromJson(json, Session.class);
    }
}
