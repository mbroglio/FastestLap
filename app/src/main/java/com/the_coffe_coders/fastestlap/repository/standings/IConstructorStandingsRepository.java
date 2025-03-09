package com.the_coffe_coders.fastestlap.repository.standings;

import com.the_coffe_coders.fastestlap.domain.Result;

import java.util.concurrent.CompletableFuture;

public interface IConstructorStandingsRepository {
    CompletableFuture<Result> fetchConstructorStandings(long lastUpdate);
}
