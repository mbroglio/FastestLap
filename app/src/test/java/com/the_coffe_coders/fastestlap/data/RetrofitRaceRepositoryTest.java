package com.the_coffe_coders.fastestlap.data;

import com.the_coffe_coders.fastestlap.domain.race_result.Race;
import com.the_coffe_coders.fastestlap.domain.race_result.Result;
import com.the_coffe_coders.fastestlap.domain.race_week.Circuit;
import com.the_coffe_coders.fastestlap.domain.race_week.Location;

import junit.framework.TestCase;

import org.junit.Test;

import java.util.concurrent.CompletableFuture;

public class RetrofitRaceRepositoryTest extends TestCase {

    @Test
    public void getLastRaceFromServerTest() {
        try {
            RetrofitRaceRepository repository = RetrofitRaceRepository.getInstance();
            CompletableFuture<Race> lastRace = repository.getLastRaceFromServer();
            Race race = new Race("2024", "23", "https://en.wikipedia.org/wiki/2024_Qatar_Grand_Prix", "Qatar Grand Prix", new Circuit("losail", "http://en.wikipedia.org/wiki/Losail_International_Circuit", "Losail", new Location("25.49", "51.4542", "Al Daayen", "Qatar")), "2024-12-01", "17:00:00Z", );
            //assertEquals(race, lastRace.get());
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

}