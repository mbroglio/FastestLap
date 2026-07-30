package com.the_coffe_coders.fastestlap.util.ui;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.the_coffe_coders.fastestlap.R;

import java.util.Locale;

/**
 * Controller UI lato ViewHolder per la riproduzione dei Team Radio.
 *
 * <p>Non gestisce direttamente il {@link android.media.MediaPlayer}: delega tutta
 * la logica audio a {@link TeamRadioPlayerManager} e si limita ad aggiornare
 * le view in risposta ai callback del manager.</p>
 *
 * <h3>Utilizzo tipico da un {@code RecyclerView.ViewHolder}:</h3>
 * <pre>
 *   // Costruzione (una volta sola nel costruttore del ViewHolder):
 *   TeamRadioPlayer controller = new TeamRadioPlayer(manager, ...views...);
 *
 *   // Binding (in onBindViewHolder):
 *   controller.bind(title, url);
 *
 *   // Riciclo (in onViewRecycled):
 *   controller.detach();
 * </pre>
 */
public class TeamRadioPlayer {

    // ── Views ────────────────────────────────────────────────────

    private final LinearLayout infoPanel;
    private final LinearLayout playerPanel;

    private final ImageView   playButton;
    private final ProgressBar loadingBar;
    private final TextView    playerDriverLabel;

    private final SeekBar   seekBar;
    private final TextView  timeElapsed;
    private final TextView  timeTotal;
    private final ImageView pauseButton;
    private final ImageView restartButton;
    private final ImageView closeButton;

    // ── Manager reference ────────────────────────────────────────

    private final TeamRadioPlayerManager manager;

    /** URL dell'item attualmente legato a questo controller. */
    private String boundUrl;

    /**
     * Listener implementato come campo per permettere a {@link #detach()} di
     * passare l'istanza esatta a {@link TeamRadioPlayerManager#detachListener}.
     */
    private final TeamRadioPlayerManager.Listener uiListener = new TeamRadioPlayerManager.Listener() {

        @Override
        public void onBuffering() {
            playButton.setVisibility(View.INVISIBLE);
            loadingBar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onStarted(int durationMs, int currentMs) {
            loadingBar.setVisibility(View.GONE);
            playButton.setVisibility(View.VISIBLE);

            if (seekBar != null) {
                seekBar.setMax(Math.max(durationMs, 1));
                seekBar.setProgress(currentMs);
            }
            setTime(timeElapsed, currentMs);
            setTime(timeTotal,   durationMs);

            pauseButton.setImageResource(R.drawable.pause_64);
            pauseButton.setOnClickListener(v -> manager.pause());
            restartButton.setOnClickListener(v -> manager.restart());

            showPlayerPanel();
        }

        @Override
        public void onProgress(int currentMs, int durationMs) {
            if (seekBar != null) seekBar.setProgress(currentMs);
            setTime(timeElapsed, currentMs);
        }

        @Override
        public void onPaused(int currentMs, int durationMs) {
            if (seekBar != null) seekBar.setProgress(currentMs);
            setTime(timeElapsed, currentMs);
            pauseButton.setImageResource(R.drawable.play_64);
            pauseButton.setOnClickListener(v -> manager.resume());
            showPlayerPanel();
        }

        @Override
        public void onCompleted(int durationMs) {
            if (seekBar != null) seekBar.setProgress(seekBar.getMax());
            setTime(timeElapsed, durationMs);
            // Riprendi dall'inizio con click sul pulsante
            pauseButton.setImageResource(R.drawable.play_64);
            pauseButton.setOnClickListener(v -> manager.restart());
        }

        @Override
        public void onStopped() {
            // Un altro URL ha preso il controllo: torna al pannello info
            resetToInfoPanel();
        }

        @Override
        public void onError() {
            resetToInfoPanel();
        }
    };

    // ─────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────

    public TeamRadioPlayer(
            TeamRadioPlayerManager manager,
            LinearLayout infoPanel,
            LinearLayout playerPanel,
            ImageView    playButton,
            ProgressBar  loadingBar,
            TextView     playerDriverLabel,
            SeekBar      seekBar,
            TextView     timeElapsed,
            TextView     timeTotal,
            ImageView    pauseButton,
            ImageView    restartButton,
            ImageView    closeButton) {

        this.manager           = manager;
        this.infoPanel         = infoPanel;
        this.playerPanel       = playerPanel;
        this.playButton        = playButton;
        this.loadingBar        = loadingBar;
        this.playerDriverLabel = playerDriverLabel;
        this.seekBar           = seekBar;
        this.timeElapsed       = timeElapsed;
        this.timeTotal         = timeTotal;
        this.pauseButton       = pauseButton;
        this.restartButton     = restartButton;
        this.closeButton       = closeButton;
    }

    // ─────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────

    /**
     * Lega questo controller all'item specificato.
     * Se l'URL è attualmente in riproduzione, mostra direttamente il player panel
     * con lo stato corrente; altrimenti mostra il pannello info con il pulsante play.
     */
    public void bind(String title, String url) {
        // Prima scollega dal manager (senza fermare la riproduzione)
        manager.detachListener(uiListener);

        boundUrl = url;
        if (playerDriverLabel != null) {
            playerDriverLabel.setText(title != null ? title : "");
        }

        // Tenta di ricollegarsi a un URL già in riproduzione (item ritornato visibile)
        if (url != null && !url.isEmpty() && manager.attachIfPlaying(url, uiListener)) {
            // il listener riceve subito onStarted o onPaused → il pannello viene aggiornato
            return;
        }

        // Nessuna riproduzione attiva per questo URL: mostra il pannello info
        resetToInfoPanel();

        if (url != null && !url.isEmpty()) {
            playButton.setAlpha(1f);
            playButton.setImageResource(R.drawable.play_64);
            playButton.setOnClickListener(v -> manager.play(url, uiListener));
        } else {
            playButton.setAlpha(0.3f);
            playButton.setOnClickListener(null);
        }

        // Pulsante chiudi: torna al pannello info senza fermare la riproduzione
        closeButton.setOnClickListener(v -> showInfoPanel());
    }

    /**
     * Scollega il listener dal manager <em>senza</em> fermare la riproduzione.
     * Da chiamare in {@code onViewRecycled}.
     */
    public void detach() {
        manager.detachListener(uiListener);
    }

    // ─────────────────────────────────────────────────────────────
    // Panel switching & helpers
    // ─────────────────────────────────────────────────────────────

    private void showInfoPanel() {
        infoPanel.setVisibility(View.VISIBLE);
        playerPanel.setVisibility(View.GONE);
    }

    private void showPlayerPanel() {
        infoPanel.setVisibility(View.GONE);
        playerPanel.setVisibility(View.VISIBLE);
    }

    private void resetToInfoPanel() {
        loadingBar.setVisibility(View.GONE);
        playButton.setVisibility(View.VISIBLE);
        playButton.setImageResource(R.drawable.play_64);
        playButton.setAlpha(1f);

        if (seekBar != null) seekBar.setProgress(0);
        setTime(timeElapsed, 0);
        setTime(timeTotal,   0);

        showInfoPanel();
    }

    private static void setTime(TextView tv, int ms) {
        if (tv == null) return;
        int totalSec = Math.max(ms, 0) / 1000;
        tv.setText(String.format(Locale.US, "%d:%02d", totalSec / 60, totalSec % 60));
    }

    // ─────────────────────────────────────────────────────────────
    // Static utility
    // ─────────────────────────────────────────────────────────────

    public static String formatTime(int millis) {
        if (millis < 0) millis = 0;
        int totalSec = millis / 1000;
        return String.format(Locale.US, "%d:%02d", totalSec / 60, totalSec % 60);
    }
}
