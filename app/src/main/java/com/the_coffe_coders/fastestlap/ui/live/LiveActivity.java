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
import com.google.gson.Gson;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.f1.livetiming.RaceControlMessage;
import com.the_coffe_coders.fastestlap.ui.live.viewmodel.LiveViewModel;
import com.the_coffe_coders.fastestlap.ui.live.viewmodel.LiveViewModelFactory;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import java.util.List;

import androidx.lifecycle.ViewModelProvider;

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
    public static final String EXTRA_SESSION_TYPE = "SESSION_TYPE";
    public static final String EXTRA_SESSION_PART = "SESSION_PART";
    public static final String EXTRA_CIRCUIT_ID = "CIRCUIT_ID";
    public static final String EXTRA_CIRCUIT_IMAGE = "CIRCUIT_IMAGE";
    public static final String EXTRA_TOTAL_LAPS = "TOTAL_LAPS";
    public static final String EXTRA_IS_LIVE = "IS_LIVE";

    private MaterialToolbar toolbar;
    private WebView webView;
    private ProgressBar progressBar;

    private LiveViewModel liveViewModel;
    private String raceControlJson = "[]";

    private String eventTitle = "LIVE TIMING";
    private String sessionName = "";
    private String sessionType = null;
    private String sessionPart = null;
    private String circuitId = null;
    private String circuitImageUrl = null;
    private String totalLaps = null;
    private boolean isLive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Permette la visualizzazione iniziale della sola classifica sia in portrait che in landscape
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
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
        setupLiveViewModel();
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

        if (intent.hasExtra(EXTRA_SESSION_PART)) {
            String sp = intent.getStringExtra(EXTRA_SESSION_PART);
            if (sp != null && !sp.trim().isEmpty()) {
                sessionPart = sp.trim().toUpperCase();
            }
        }

        if (intent.hasExtra(EXTRA_SESSION_TYPE)) {
            String st = intent.getStringExtra(EXTRA_SESSION_TYPE);
            if (st != null && !st.trim().isEmpty()) {
                sessionType = st.trim().toLowerCase();
            }
        } else if (sessionName != null && !sessionName.trim().isEmpty()) {
            String s = sessionName.toLowerCase().trim();
            if (s.contains("practice") || s.contains("prova") || s.contains("prove")
                    || s.contains("libera") || s.contains("libere")
                    || s.matches(".*\\bfp[1-3]?\\b.*") || s.startsWith("fp")) {
                sessionType = "practice";
            } else if (s.contains("qualif") || s.contains("shootout")
                    || s.matches(".*\\bq[1-3]\\b.*") || s.startsWith("q")) {
                sessionType = "qualifying";
            } else if (s.contains("sprint")) {
                sessionType = "sprint";
            } else {
                sessionType = "race";
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
        }
        
        if (intent.hasExtra(EXTRA_CIRCUIT_IMAGE)) {
            circuitImageUrl = intent.getStringExtra(EXTRA_CIRCUIT_IMAGE);
        }

        if (intent.hasExtra(EXTRA_TOTAL_LAPS)) {
            totalLaps = intent.getStringExtra(EXTRA_TOTAL_LAPS);
        }
    }

    private void loadCircuitImageIfNeeded() {
        if (circuitImageUrl != null && !circuitImageUrl.isEmpty()) {
            runOnUiThread(() -> {
                if (webView != null) {
                    String safeUrl = circuitImageUrl.replace("\\", "\\\\").replace("'", "\\'");
                    webView.evaluateJavascript("if (window.setCircuitImage) window.setCircuitImage('" + safeUrl + "');", null);
                }
            });
        }
    }

    private void setupLiveViewModel() {
        LiveViewModelFactory factory = new LiveViewModelFactory(getApplication());
        liveViewModel = new ViewModelProvider(this, factory).get(LiveViewModel.class);

        liveViewModel.getRaceControlMessages().observe(this, result -> {
            if (result instanceof Result.RaceControlSuccess) {
                List<RaceControlMessage> messages = ((Result.RaceControlSuccess) result).getData();
                if (messages != null && !messages.isEmpty()) {
                    raceControlJson = new Gson().toJson(messages);
                    injectRaceControlMessagesToWebView();
                }
            }
        });

        liveViewModel.startPolling();
    }

    private void injectRaceControlMessagesToWebView() {
        if (webView != null && raceControlJson != null && !raceControlJson.equals("[]")) {
            runOnUiThread(() -> {
                if (webView != null) {
                    webView.evaluateJavascript(
                        "if (window.updateRaceControlMessages) window.updateRaceControlMessages(" + raceControlJson + ");",
                        null
                    );
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
    @SuppressWarnings("deprecation")
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
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // Hardware acceleration per canvas a 60 FPS
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // JavaScript Bridge bidirezionale per passare info del tracciato al modulo web
        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public boolean isTestMode() {
                return false;
            }

            @JavascriptInterface
            public String getTestCircuit() {
                return "";
            }

            @JavascriptInterface
            public boolean isSessionLive() {
                // Sempre true all'apertura della LiveActivity per permettere il rendering del tracciato e della telemetria
                return true;
            }

            @JavascriptInterface
            public String getSessionName() {
                return (sessionName != null && !sessionName.trim().isEmpty())
                        ? sessionName
                        : "Sessione Live";
            }

            @JavascriptInterface
            public String getSessionType() {
                if (sessionType != null && !sessionType.trim().isEmpty()) {
                    return sessionType.trim().toLowerCase();
                }
                return "race";
            }

            @JavascriptInterface
            public String getSessionPart() {
                if (sessionPart != null && !sessionPart.trim().isEmpty()) {
                    return sessionPart.trim().toUpperCase();
                }
                if (sessionName == null) return "";
                String s = sessionName.toLowerCase().trim();
                // English keywords
                if (s.contains("q1")) return "Q1";
                if (s.contains("q2")) return "Q2";
                if (s.contains("q3")) return "Q3";
                if (s.contains("fp1") || s.contains("practice 1") || s.contains("prova libera 1") || s.contains("libere 1")) return "FP1";
                if (s.contains("fp2") || s.contains("practice 2") || s.contains("prova libera 2") || s.contains("libere 2")) return "FP2";
                if (s.contains("fp3") || s.contains("practice 3") || s.contains("prova libera 3") || s.contains("libere 3")) return "FP3";
                // Italian Qualifying Sprint
                if (s.contains("qualifica sprint") || s.contains("sprint qualifying") || s.contains("sq")) return "SQ";
                return "";
            }

            @JavascriptInterface
            public String getEventTitle() {
                return (eventTitle != null && !eventTitle.trim().isEmpty() && !eventTitle.equalsIgnoreCase("live timing"))
                        ? eventTitle
                        : "FORMULA 1 GRAND PRIX";
            }

            @JavascriptInterface
            public String getCircuitId() {
                return circuitId != null ? circuitId : "";
            }
            
            @JavascriptInterface
            public String getCircuitImageUrl() {
                return circuitImageUrl != null ? circuitImageUrl : "";
            }

            @JavascriptInterface
            public String getTotalLaps() {
                return totalLaps != null ? totalLaps : "";
            }

            @JavascriptInterface
            public String getRaceControlMessagesJson() {
                return raceControlJson != null ? raceControlJson : "[]";
            }

            @JavascriptInterface
            public void onTabChanged(String tabId) {
                runOnUiThread(() -> {
                    if ("view-track".equals(tabId)) {
                        // La seconda pagina (Mappa & Pista) è visualizzabile unicamente in modalità landscape
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                    } else {
                        // La pagina della sola classifica (view-standings) e race control sono visualizzabili sia in landscape che in portrait
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                    }
                });
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
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                loadCircuitImageIfNeeded();
                injectRaceControlMessagesToWebView();
            }

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
        StringBuilder queryParams = new StringBuilder("?");
        queryParams.append("session_type=").append(Uri.encode(sessionType != null ? sessionType : "race"));
        if (sessionPart != null && !sessionPart.isEmpty()) {
            queryParams.append("&session_part=").append(Uri.encode(sessionPart));
        }
        if (circuitId != null && !circuitId.isEmpty()) {
            queryParams.append("&circuit_id=").append(Uri.encode(circuitId));
        }
        if (circuitImageUrl != null && !circuitImageUrl.isEmpty()) {
            queryParams.append("&circuit_image=").append(Uri.encode(circuitImageUrl));
        }
        queryParams.append("&is_live=").append(isLive ? "true" : "true");
        String url = "file:///android_asset/live_timing/track_map.html" + queryParams;
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
        if (liveViewModel != null) {
            liveViewModel.stopPolling();
        }
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}