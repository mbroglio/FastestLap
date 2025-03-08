package com.the_coffe_coders.fastestlap.repository.driver;

import com.the_coffe_coders.fastestlap.domain.Result;
import java.util.concurrent.CompletableFuture;

public interface IDriverStandingsRepository {
    CompletableFuture<Result> fetchDriversStandings(long lastUpdate);
}