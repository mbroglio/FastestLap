package com.the_coffe_coders.fastestlap.ui.live.viewmodel;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.repository.f1.livetiming.LiveTimingRepository;

/**
 * ViewModel condiviso tra i Fragment della LiveActivity.
 *
 * <p>Espone due stream LiveData (race control e team radio) osservati
 * da {@code RaceControlFragment}. Gestisce inoltre il polling automatico
 * ogni 2 secondi durante l'attivita visibile dell'utente.</p>
 */
public class LiveViewModel extends ViewModel {

    private static final String TAG = "LiveViewModel";

    private final LiveTimingRepository liveTimingRepository;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pollingRunnable;
    private boolean isPolling = false;

    private String sessionKey = null;
    private boolean isSessionLive = false;

    public LiveViewModel(LiveTimingRepository liveTimingRepository) {
        this.liveTimingRepository = liveTimingRepository;
    }

    public void configureSession(String sessionKey, boolean isLive) {
        this.sessionKey = sessionKey;
        this.isSessionLive = isLive;
        liveTimingRepository.configureSession(sessionKey, isLive);
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
        liveTimingRepository.configureSession(sessionKey, this.isSessionLive);
    }

    public void setSessionLive(boolean isLive) {
        this.isSessionLive = isLive;
        liveTimingRepository.configureSession(this.sessionKey, isLive);
    }

    /**
     * Avvia (o ripristina) il fetch dei messaggi Race Control.
     */
    public LiveData<Result> getRaceControlMessages() {
        return liveTimingRepository.fetchRaceControlMessages(sessionKey);
    }

    /**
     * Avvia (o ripristina) il fetch delle registrazioni Team Radio.
     */
    public LiveData<Result> getTeamRadioMessages() {
        return liveTimingRepository.fetchTeamRadioMessages(sessionKey);
    }

    /**
     * Polling disabilitato: gli endpoint OpenF1 race_control e team_radio richiedono licenza a pagamento nel livetiming.
     */
    public void startPolling() {
        Log.d(TAG, "Polling OpenF1 race control e team radio disabilitato nel livetiming (richiede licenza a pagamento)");
    }

    /**
     * Interrompe il polling automatico.
     */
    public void stopPolling() {
        isPolling = false;
        if (pollingRunnable != null) {
            handler.removeCallbacks(pollingRunnable);
            pollingRunnable = null;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopPolling();
    }
}
