package com.the_coffe_coders.fastestlap.repository.standings.driver;

import com.the_coffe_coders.fastestlap.domain.Result;

import java.util.concurrent.CompletableFuture;

/**
 * Interfaccia per il repository dei piloti che fornisce metodi per recuperare
 * i dati sulle classifiche dei piloti da fonti remote o locali.
 */
public interface IDriverStandingsRepository {
    /**
     * Recupera le classifiche dei piloti.
     *
     * @param lastUpdate timestamp dell'ultimo aggiornamento per determinare se i dati sono freschi
     * @return CompletableFuture che si completerà con il risultato dell'operazione
     */
    CompletableFuture<Result> fetchDriversStandings(long lastUpdate);
}