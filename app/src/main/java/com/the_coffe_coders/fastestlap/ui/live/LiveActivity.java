package com.the_coffe_coders.fastestlap.ui.live;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

/**
 * LiveActivity
 *
 * Avvia ed esegue il modulo Live Timing su Web (formato track_map.html),
 * configurandolo in base alle informazioni ricevute (titolo sessione, ID circuito,
 * immagine del circuito, giri totali, stato sessione live).
 */
public class LiveActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_TITLE = "EVENT_TITLE";
    public static final String EXTRA_SESSION_NAME = "SESSION_NAME";
    public static final String EXTRA_CIRCUIT_ID = "CIRCUIT_ID";
    public static final String EXTRA_CIRCUIT_IMAGE = "CIRCUIT_IMAGE";
    public static final String EXTRA_TOTAL_LAPS = "TOTAL_LAPS";
    public static final String EXTRA_IS_LIVE = "IS_LIVE";

    private MaterialToolbar toolbar;
    private WebView webView;
    private ProgressBar progressBar;

    private String eventTitle = "LIVE TIMING";
    private String sessionName = "";
    private String circuitId = "sepang";
    private String circuitImageUrl = null;
    private String totalLaps = "55";
    private boolean isLive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_live);

        toolbar = findViewById(R.id.top_app_bar);
        webView = findViewById(R.id.live_timing_webview);
        progressBar = findViewById(R.id.live_web_loading_bar);

        View rootLayout = findViewById(R.id.main);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.systemBars()
                            | androidx.core.view.WindowInsetsCompat.Type.displayCutout()
            );
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        extractIntentData();
        setupToolbar();
        setupWebView();
        setupBackNavigation();
        loadCircuitImageIfNeeded();
    }

    private void extractIntentData() {
        Intent intent = getIntent();
        if (intent == null) return;

        if (intent.hasExtra(EXTRA_IS_LIVE)) {
            isLive = intent.getBooleanExtra(EXTRA_IS_LIVE, false);
        }

        if (intent.hasExtra(EXTRA_SESSION_NAME)) {
            String sess = intent.getStringExtra(EXTRA_SESSION_NAME);
            if (sess != null && !sess.trim().isEmpty()) {
                sessionName = sess.trim();
            }
        }

        if (intent.hasExtra(EXTRA_EVENT_TITLE)) {
            String title = intent.getStringExtra(EXTRA_EVENT_TITLE);
            if (title != null && !title.trim().isEmpty()) {
                eventTitle = title.trim();
            }
        }

        if (intent.hasExtra(EXTRA_CIRCUIT_ID)) {
            String id = intent.getStringExtra(EXTRA_CIRCUIT_ID);
            if (id != null && !id.trim().isEmpty()) {
                circuitId = id.trim().toLowerCase();
            }
        } else if (eventTitle != null) {
            String lower = eventTitle.toLowerCase();
            if (lower.contains("baku") || lower.contains("azerbaijan") || lower.contains("azerbaigian")) {
                circuitId = "baku";
            } else if (lower.contains("sepang") || lower.contains("malaysia") || lower.contains("malesia")) {
                circuitId = "sepang";
            }
        }

        if (intent.hasExtra(EXTRA_CIRCUIT_IMAGE)) {
            circuitImageUrl = intent.getStringExtra(EXTRA_CIRCUIT_IMAGE);
        }

        if (intent.hasExtra(EXTRA_TOTAL_LAPS)) {
            String laps = intent.getStringExtra(EXTRA_TOTAL_LAPS);
            if (laps != null && !laps.trim().isEmpty()) {
                totalLaps = laps.trim();
            }
        } else {
            totalLaps = "baku".equalsIgnoreCase(circuitId) ? "51" : "55";
        }
    }

    private void loadCircuitImageIfNeeded() {
        if (circuitImageUrl == null || circuitImageUrl.isEmpty()) {
            com.the_coffe_coders.fastestlap.source.track.FirebaseTrackDataSource.getInstance()
                    .getTrack(circuitId, new com.the_coffe_coders.fastestlap.repository.track.TrackCallback() {
                        @Override
                        public void onTrackLoaded(com.the_coffe_coders.fastestlap.domain.f1.track.Track t) {
                            if (t != null && t.getTrack_minimal_layout_url() != null && !t.getTrack_minimal_layout_url().isEmpty()) {
                                circuitImageUrl = t.getTrack_minimal_layout_url();
                                runOnUiThread(() -> {
                                    if (webView != null) {
                                        webView.evaluateJavascript("if (window.setCircuitImage) window.setCircuitImage('" + circuitImageUrl + "');", null);
                                    }
                                });
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            // Fallback to local asset
                        }
                    });
        }
    }

    private void setupToolbar() {
        if (toolbar == null) return;
        toolbar.setTitle(eventTitle);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        if (webView == null) return;

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(true);

        // Hardware acceleration per canvas a 60 FPS
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // JavaScript Bridge bidirezionale per passare info del tracciato al modulo web
        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public boolean isSessionLive() {
                // Momentaneamente forzato a true per far partire la simulazione di Sepang una volta aperta la pagina
                return true;
            }

            @JavascriptInterface
            public String getSessionName() {
                return (sessionName != null && !sessionName.trim().isEmpty())
                        ? sessionName
                        : "Gara (Simulazione)";
            }

            @JavascriptInterface
            public String getEventTitle() {
                return (eventTitle != null && !eventTitle.trim().isEmpty() && !eventTitle.equalsIgnoreCase("live timing"))
                        ? eventTitle
                        : "FORMULA 1 PETRONAS MALAYSIAN GP";
            }

            @JavascriptInterface
            public String getCircuitId() {
                // Momentaneamente garantisce Sepang per la simulazione richiesta
                return "sepang";
            }

            @JavascriptInterface
            public String getTotalLaps() {
                return totalLaps != null ? totalLaps : "55";
            }

            @JavascriptInterface
            public String getCircuitImageUrl() {
                return circuitImageUrl != null ? circuitImageUrl : "";
            }

            @JavascriptInterface
            public void closeLive() {
                runOnUiThread(LiveActivity.this::finish);
            }
        }, "FastestLapBridge");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (progressBar != null) {
                    if (newProgress < 100) {
                        progressBar.setVisibility(View.VISIBLE);
                        progressBar.setProgress(newProgress);
                    } else {
                        progressBar.setVisibility(View.GONE);
                    }
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri url = request.getUrl();
                if (url != null && "file".equalsIgnoreCase(url.getScheme())) {
                    return false; // Carica internamente gli asset HTML5/JS
                }
                // Link esterni
                if (url != null) {
                    try {
                        Intent externalIntent = new Intent(Intent.ACTION_VIEW, url);
                        startActivity(externalIntent);
                        return true;
                    } catch (Exception ignored) {
                    }
                }
                return false;
            }
        });

        // Carica la pagina generale di Live Timing (adattata per landscape e smartphone)
        String url = "file:///android_asset/live_timing/track_map.html";
        webView.loadUrl(url);
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView != null && webView.canGoBack()) {
                    webView.goBack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    // Metodi stub per compatibilità con eventuali chiamate legacy
    public void openLiveTrackMap() {
        // Il modulo web è già a schermo intero
    }

    public void setFullTelemetryMode(boolean full) {
        // Gestito nativamente nel modulo web
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}