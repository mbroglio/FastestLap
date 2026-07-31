package com.the_coffe_coders.fastestlap.util.ui;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Manager globale per la riproduzione audio dei Team Radio.
 *
 * <p>Garantisce che <strong>un solo audio alla volta</strong> sia in riproduzione:
 * ogni chiamata a {@link #play} ferma l'audio corrente (notificando il listener
 * precedente tramite {@link Listener#onStopped()}) e avvia quello nuovo.</p>
 *
 * <p>Il {@link MediaPlayer} sopravvive al riciclo del ViewHolder: quando un
 * ViewHolder scorre fuori dallo schermo chiama {@link #detachListener} (il player
 * continua a suonare); quando torna visibile chiama {@link #attachIfPlaying} per
 * ricollegarsi allo stato corrente.</p>
 *
 * <p>Il ciclo di vita del manager è legato all'adapter; rilasciarlo in
 * {@code onDetachedFromRecyclerView} e in {@code Fragment.onDestroyView}.</p>
 */
public class TeamRadioPlayerManager {

    private static final String TAG = "TeamRadioPlayerManager";

    // ─────────────────────────────────────────────────────────────
    // Listener interface
    // ─────────────────────────────────────────────────────────────

    public interface Listener {
        /** Buffering in corso. */
        void onBuffering();
        /** Riproduzione avviata o ripresa. */
        void onStarted(int durationMs, int currentMs);
        /** Aggiornamento della posizione (ogni ~500 ms). */
        void onProgress(int currentMs, int durationMs);
        /** Messo in pausa. */
        void onPaused(int currentMs, int durationMs);
        /** Riproduzione terminata normalmente. */
        void onCompleted(int durationMs);
        /** Un altro URL ha preso il controllo del player: resettare la UI. */
        void onStopped();
        /** Errore irreversibile. */
        void onError();
    }

    // ─────────────────────────────────────────────────────────────
    // State
    // ─────────────────────────────────────────────────────────────

    private MediaPlayer activePlayer;    // player già avviato
    private MediaPlayer preparingPlayer; // player in attesa di onPrepared
    private String      activeUrl;
    private Listener    activeListener;

    private final Handler  progressHandler  = new Handler(Looper.getMainLooper());
    private       Runnable progressRunnable;

    // ─────────────────────────────────────────────────────────────
    // Public API – playback control
    // ─────────────────────────────────────────────────────────────

    /**
     * Avvia la riproduzione di {@code url}.
     * Se un URL diverso era già in riproduzione, viene fermato e il suo listener
     * notificato con {@link Listener#onStopped()} prima di avviare il nuovo.
     */
    @SuppressWarnings("deprecation")
    public void play(String url, Listener listener) {
        if (url == null || url.isEmpty()) return;

        // Stesso URL già in corso: aggiorna solo il listener e notifica lo stato
        if (url.equals(activeUrl) && activePlayer != null) {
            activeListener = listener;
            if (activePlayer.isPlaying()) {
                listener.onStarted(activePlayer.getDuration(), activePlayer.getCurrentPosition());
            } else {
                listener.onPaused(activePlayer.getCurrentPosition(), activePlayer.getDuration());
            }
            return;
        }

        // URL diverso: avvisa il vecchio listener e ferma il player corrente
        if (activeListener != null) {
            activeListener.onStopped();
        }
        stopInternal();

        activeUrl      = url;
        activeListener = listener;

        listener.onBuffering();

        MediaPlayer mp = new MediaPlayer();
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            mp.setDataSource(url);
        } catch (Exception e) {
            Log.e(TAG, "setDataSource failed: " + url, e);
            activeUrl      = null;
            activeListener = null;
            listener.onError();
            return;
        }

        // Tracciamo il player PRIMA di prepareAsync, così stopInternal() può
        // rilasciarlo anche se onPrepared non è ancora scattato.
        preparingPlayer = mp;

        mp.setOnPreparedListener(player -> {
            // Ignorare se il player è stato sostituito da una chiamata stop()/play() successiva
            if (player != preparingPlayer) {
                player.release();
                return;
            }
            preparingPlayer = null;
            activePlayer = player;
            player.start();
            startProgressUpdater();
            if (activeListener != null) {
                activeListener.onStarted(player.getDuration(), 0);
            }
        });

        mp.setOnCompletionListener(player -> {
            stopProgressUpdater();
            int dur = player.getDuration();
            if (activeListener != null) activeListener.onCompleted(dur);
            activePlayer = null;
            activeUrl    = null;
        });

        mp.setOnErrorListener((player, what, extra) -> {
            Log.e(TAG, "Error: what=" + what + " extra=" + extra);
            if (activeListener != null) activeListener.onError();
            stopInternal();
            return true;
        });

        mp.prepareAsync();
    }

    public void pause() {
        if (activePlayer != null && activePlayer.isPlaying()) {
            activePlayer.pause();
            stopProgressUpdater();
            if (activeListener != null) {
                activeListener.onPaused(
                        activePlayer.getCurrentPosition(),
                        activePlayer.getDuration());
            }
        }
    }

    public void resume() {
        if (activePlayer != null && !activePlayer.isPlaying()) {
            activePlayer.start();
            startProgressUpdater();
            if (activeListener != null) {
                activeListener.onStarted(
                        activePlayer.getDuration(),
                        activePlayer.getCurrentPosition());
            }
        }
    }

    public void restart() {
        if (activePlayer != null) {
            activePlayer.seekTo(0);
            if (!activePlayer.isPlaying()) {
                activePlayer.start();
                startProgressUpdater();
            }
            if (activeListener != null) {
                activeListener.onStarted(activePlayer.getDuration(), 0);
            }
        }
    }

    public void seekTo(int ms) {
        if (activePlayer != null) activePlayer.seekTo(ms);
    }

    // ─────────────────────────────────────────────────────────────
    // Public API – listener lifecycle (ViewHolder bind/recycle)
    // ─────────────────────────────────────────────────────────────

    /**
     * Tenta di collegare {@code listener} all'URL attualmente attivo.
     * Chiamato quando un ViewHolder ritorna visibile.
     *
     * @return {@code true} se l'URL è attivo e il listener è stato collegato.
     */
    public boolean attachIfPlaying(String url, Listener listener) {
        if (url == null || !url.equals(activeUrl)) return false;
        // L'URL corrisponde: ci siamo in buffering (preparingPlayer != null) o in riproduzione
        activeListener = listener;
        if (activePlayer != null) {
            if (activePlayer.isPlaying()) {
                listener.onStarted(activePlayer.getDuration(), activePlayer.getCurrentPosition());
            } else {
                listener.onPaused(activePlayer.getCurrentPosition(), activePlayer.getDuration());
            }
        } else {
            // Ancora in buffering
            listener.onBuffering();
        }
        return true;
    }

    /**
     * Scollega il listener <em>senza</em> fermare la riproduzione.
     * Chiamato quando il ViewHolder scorre fuori dallo schermo.
     */
    public void detachListener(Listener listener) {
        if (activeListener == listener) {
            activeListener = null;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Public API – stato
    // ─────────────────────────────────────────────────────────────

    public boolean isActive(String url) {
        return url != null && url.equals(activeUrl) && activePlayer != null;
    }

    public boolean isPlaying() {
        return activePlayer != null && activePlayer.isPlaying();
    }

    public int getCurrentPosition() {
        return activePlayer != null ? activePlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return activePlayer != null ? activePlayer.getDuration() : 0;
    }

    /**
     * Ferma la riproduzione corrente e rilascia il player, senza distruggere il manager.
     * Il manager rimane disponibile per successive chiamate a {@link #play}.
     */
    public void stop() {
        if (activeListener != null) {
            activeListener.onStopped();
        }
        stopInternal();
        activeListener = null;
        activeUrl      = null;
    }

    /**
     * Ferma tutto e rilascia il player. Da chiamare in
     * {@code onDetachedFromRecyclerView} e in {@code Fragment.onDestroyView}.
     */
    public void release() {
        stopInternal();
        activeListener = null;
        activeUrl      = null;
    }

    // ─────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────

    private void stopInternal() {
        stopProgressUpdater();
        // Rilascia il player in fase di preparazione (zombie durante prepareAsync)
        if (preparingPlayer != null) {
            try { preparingPlayer.release(); } catch (Exception ignored) { }
            preparingPlayer = null;
        }
        if (activePlayer != null) {
            try {
                if (activePlayer.isPlaying()) activePlayer.stop();
                activePlayer.release();
            } catch (Exception ignored) { }
            activePlayer = null;
        }
    }

    private void startProgressUpdater() {
        stopProgressUpdater();
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (activePlayer != null && activePlayer.isPlaying()) {
                    int current  = activePlayer.getCurrentPosition();
                    int duration = activePlayer.getDuration();
                    if (activeListener != null) {
                        activeListener.onProgress(current, duration);
                    }
                    progressHandler.postDelayed(this, 500);
                }
            }
        };
        progressHandler.post(progressRunnable);
    }

    private void stopProgressUpdater() {
        if (progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
            progressRunnable = null;
        }
    }
}
