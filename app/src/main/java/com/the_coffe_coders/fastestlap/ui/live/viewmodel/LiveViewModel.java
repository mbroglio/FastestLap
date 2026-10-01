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

    public LiveViewModel(LiveTimingRepository liveTimingRepository) {
        this.liveTimingRepository = liveTimingRepository;
    }

    /**
     * Avvia (o ripristina) il fetch dei messaggi Race Control.
     */
    public LiveData<Result> getRaceControlMessages() {
        return liveTimingRepository.fetchRaceControlMessages();
    }

    /**
     * Avvia (o ripristina) il fetch delle registrazioni Team Radio.
     */
    public LiveData<Result> getTeamRadioMessages() {
        return liveTimingRepository.fetchTeamRadioMessages();
    }

    /**
     * Avvia il polling automatico di Race Control e Team Radio ogni 2 secondi (2000 ms).
     */
    public void startPolling() {
        if (isPolling) return;
        isPolling = true;

        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isPolling) return;
                Log.d(TAG, "[POLLING] Invio richieste periodiche a Race Control e Team Radio (intervallo 2s)...");
                liveTimingRepository.fetchRaceControlMessages();
                liveTimingRepository.fetchTeamRadioMessages();
                handler.postDelayed(this, 2000);
            }
        };
        handler.post(pollingRunnable);
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
