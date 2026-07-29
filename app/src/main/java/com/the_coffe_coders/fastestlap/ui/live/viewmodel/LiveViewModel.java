package com.the_coffe_coders.fastestlap.ui.live.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.repository.f1.livetiming.LiveTimingRepository;

/**
 * ViewModel condiviso tra i Fragment della LiveActivity.
 *
 * <p>Espone due stream LiveData (race control e team radio) osservati
 * da {@code RaceControlFragment}. Il ViewModel è legato al ciclo di
 * vita dell'Activity in modo che entrambi i Fragment condividano la
 * stessa istanza e non effettuino chiamate duplicate.</p>
 */
public class LiveViewModel extends ViewModel {

    private final LiveTimingRepository liveTimingRepository;

    public LiveViewModel(LiveTimingRepository liveTimingRepository) {
        this.liveTimingRepository = liveTimingRepository;
    }

    /**
     * Avvia (o ripristina) il fetch dei messaggi Race Control.
     *
     * @return LiveData che emette {@link Result.Loading} →
     *         {@link Result.RaceControlSuccess} oppure {@link Result.Error}
     */
    public LiveData<Result> getRaceControlMessages() {
        return liveTimingRepository.fetchRaceControlMessages();
    }

    /**
     * Avvia (o ripristina) il fetch delle registrazioni Team Radio.
     *
     * @return LiveData che emette {@link Result.Loading} →
     *         {@link Result.TeamRadioSuccess} oppure {@link Result.Error}
     */
    public LiveData<Result> getTeamRadioMessages() {
        return liveTimingRepository.fetchTeamRadioMessages();
    }
}
