package com.the_coffe_coders.fastestlap.ui.live;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.Inflater;

/**
 * LiveActivity
 *
 * Esegue il modulo Live Timing a schermo intero mantenendo il layout activity_live.xml.
 * Fornisce un bridge JavaScript ad alte prestazioni con decompressore DEFLATE nativo
 * in C++/Java (Inflater) per CarData.z e Position.z da SignalR.
 */
public class LiveActivity extends AppCompatActivity {

    private static final String TAG = "LiveActivity";

    public static final String EXTRA_EVENT_TITLE = "EVENT_TITLE";
    public static final String EXTRA_SESSION_NAME = "SESSION_NAME";
    public static final String EXTRA_SESSION_TYPE = "SESSION_TYPE";
    public static final String EXTRA_SESSION_PART = "SESSION_PART";
    public static final String EXTRA_CIRCUIT_ID = "CIRCUIT_ID";
    public static final String EXTRA_CIRCUIT_IMAGE = "CIRCUIT_IMAGE";
    public static final String EXTRA_TOTAL_LAPS = "TOTAL_LAPS";
    public static final String EXTRA_IS_LIVE = "IS_LIVE";
    public static final String EXTRA_CIRCUIT_TIMEZONE = "CIRCUIT_TIMEZONE";
    public static final String EXTRA_SESSION_KEY = "SESSION_KEY";
    public static final String EXTRA_SESSION_ELAPSED_SECONDS = "SESSION_ELAPSED_SECONDS";

    private MaterialToolbar toolbar;
    private WebView webView;
    private ProgressBar progressBar;
    private LiveSignalRService signalRService;

    private String eventTitle = "LIVE TIMING";
    private String sessionName = "";
    private String sessionType = null;
    private String sessionPart = null;
    private String circuitId = null;
    private String circuitImageUrl = null;
    private String totalLaps = null;
    private String circuitTimeZone = null;
    private String sessionKey = null;
    private long sessionElapsedSeconds = 0;
    private boolean isLive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        setupBackNavigation();
        loadCircuitImageIfNeeded();

        signalRService = new LiveSignalRService(webView);
        signalRService.startStream();
        fetchTrackFallbackIfNeeded();
    }

    public static boolean isSprintQualifying(String type, String part, String name) {
        String t = (type != null) ? type.toLowerCase().trim() : "";
        String p = (part != null) ? part.toUpperCase().trim() : "";
        String n = (name != null) ? name.toLowerCase().trim() : "";
        return p.startsWith("SQ")
                || t.equals("sq")
                || (t.contains("sprint") && (t.contains("qualif") || t.contains("shootout")))
                || t.contains("sprint_qualifying")
                || t.contains("shootout")
                || (n.contains("sprint") && (n.contains("qualif") || n.contains("shootout") || n.contains("sq")))
                || n.contains("shootout")
                || n.contains("qualifica sprint");
    }

    public static String normalizeSessionType(String type, String part, String name) {
        if (isSprintQualifying(type, part, name)) {
            return "qualifying";
        }
        String t = (type != null) ? type.toLowerCase().trim() : "";
        String n = (name != null) ? name.toLowerCase().trim() : "";
        String combined = t + " " + n;
        if (combined.contains("practice") || combined.contains("prova") || combined.contains("prove")
                || combined.contains("libera") || combined.contains("libere")
                || combined.matches(".*\\bfp[1-3]?\\b.*") || t.startsWith("fp")) {
            return "practice";
        }
        if (combined.contains("qualif") || combined.matches(".*\\bq[1-3]\\b.*") || t.startsWith("q")) {
            return "qualifying";
        }
        if (combined.contains("sprint")) {
            return "sprint";
        }
        return "race";
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

        String rawType = intent.hasExtra(EXTRA_SESSION_TYPE) ? intent.getStringExtra(EXTRA_SESSION_TYPE) : null;
        if (isSprintQualifying(rawType, sessionPart, sessionName)) {
            sessionType = "qualifying";
            if (sessionPart == null || sessionPart.isEmpty()) {
                sessionPart = "SQ";
            }
        } else {
            sessionType = normalizeSessionType(rawType, sessionPart, sessionName);
            if (sessionPart == null || sessionPart.isEmpty()) {
                if ("sprint".equals(sessionType)) {
                    sessionPart = "SPRINT";
                } else if ("practice".equals(sessionType)) {
                    sessionPart = "FP1";
                } else if ("qualifying".equals(sessionType)) {
                    sessionPart = "Q";
                } else {
                    sessionPart = "RACE";
                }
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

        if (intent.hasExtra(EXTRA_CIRCUIT_TIMEZONE)) {
            circuitTimeZone = intent.getStringExtra(EXTRA_CIRCUIT_TIMEZONE);
        }

        if (intent.hasExtra(EXTRA_SESSION_KEY)) {
            sessionKey = intent.getStringExtra(EXTRA_SESSION_KEY);
        }

        if (intent.hasExtra(EXTRA_SESSION_ELAPSED_SECONDS)) {
            sessionElapsedSeconds = intent.getLongExtra(EXTRA_SESSION_ELAPSED_SECONDS, 0);
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

    private void fetchTrackFallbackIfNeeded() {
        if ((circuitImageUrl == null || circuitImageUrl.isEmpty()) && circuitId != null && !circuitId.isEmpty()) {
            com.the_coffe_coders.fastestlap.repository.track.TrackRepository trackRepo =
                    com.the_coffe_coders.fastestlap.repository.track.TrackRepository.getInstance(
                            com.the_coffe_coders.fastestlap.database.AppRoomDatabase.getDatabase(getApplicationContext()),
                            getApplicationContext()
                    );
            trackRepo.getTrack(circuitId).observe(this, res -> {
                if (res instanceof com.the_coffe_coders.fastestlap.domain.Result.TrackSuccess) {
                    com.the_coffe_coders.fastestlap.domain.f1.track.Track t =
                            ((com.the_coffe_coders.fastestlap.domain.Result.TrackSuccess) res).getData();
                    if (t != null) {
                        String url = null;
                        if (t.getTrack_minimal_layout_url() != null && !t.getTrack_minimal_layout_url().trim().isEmpty()) {
                            url = t.getTrack_minimal_layout_url().trim();
                        } else if (t.getTrack_full_layout_url() != null && !t.getTrack_full_layout_url().trim().isEmpty()) {
                            url = t.getTrack_full_layout_url().trim();
                        } else if (t.getTrack_pic_url() != null && !t.getTrack_pic_url().trim().isEmpty()) {
                            url = t.getTrack_pic_url().trim();
                        }
                        if (url != null) {
                            circuitImageUrl = url;
                            loadCircuitImageIfNeeded();
                        }
                    }
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
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // Accelerazione hardware GPU per rendering canvas a 60 FPS
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // JavaScript Bridge bidirezionale con decompressore DEFLATE nativo
        webView.addJavascriptInterface(new Object() {

            @JavascriptInterface
            public String inflateRaw(String base64Data) {
                if (base64Data == null || base64Data.isEmpty()) return "{}";
                try {
                    byte[] compressed = Base64.decode(base64Data, Base64.DEFAULT);
                    Inflater inflater = new Inflater(true); // nowrap = true per raw DEFLATE
                    inflater.setInput(compressed);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buf = new byte[4096];
                    while (!inflater.finished()) {
                        int count = inflater.inflate(buf);
                        if (count == 0 && inflater.needsInput()) break;
                        bos.write(buf, 0, count);
                    }
                    inflater.end();
                    return new String(bos.toByteArray(), StandardCharsets.UTF_8);
                } catch (Exception e) {
                    Log.e(TAG, "Errore decompressione Inflater nativo: " + e.getMessage());
                    return "{}";
                }
            }

            @JavascriptInterface
            public boolean isSessionLive() {
                return isLive;
            }

            @JavascriptInterface
            public long getSessionElapsedSeconds() {
                return sessionElapsedSeconds;
            }

            @JavascriptInterface
            public String getCircuitTimeZone() {
                return circuitTimeZone != null ? circuitTimeZone : "";
            }

            @JavascriptInterface
            public String getCircuitLocalTime() {
                if (circuitTimeZone != null && !circuitTimeZone.trim().isEmpty()) {
                    try {
                        return UIUtils.getCurrentTime(circuitTimeZone.trim());
                    } catch (Exception ignored) {}
                }
                return "";
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
            public void onTabChanged(String tabId) {
                runOnUiThread(() -> {
                    if ("view-track".equals(tabId)) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                    } else {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                    }
                });
            }

            @JavascriptInterface
            public void startSignalRStream() {
                if (signalRService != null) {
                    signalRService.startStream();
                }
            }

            @JavascriptInterface
            public void stopSignalRStream() {
                if (signalRService != null) {
                    signalRService.stopStream();
                }
            }

            @JavascriptInterface
            public boolean isSignalRConnected() {
                return signalRService != null && signalRService.isConnected();
            }

            @JavascriptInterface
            public String getRaceControlMessagesJson() {
                if (signalRService != null) {
                    String json = signalRService.getCachedFeedJson("RaceControlMessages");
                    if (json != null) return json;
                }
                return "[]";
            }

            @JavascriptInterface
            public void requestInitialFeeds() {
                if (signalRService != null) {
                    signalRService.requestInitialFeeds();
                }
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
                if (signalRService != null) {
                    signalRService.requestInitialFeeds();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri url = request.getUrl();
                if (url != null && "file".equalsIgnoreCase(url.getScheme())) {
                    return false;
                }
                if (url != null) {
                    try {
                        Intent externalIntent = new Intent(Intent.ACTION_VIEW, url);
                        startActivity(externalIntent);
                        return true;
                    } catch (Exception ignored) {}
                }
                return false;
            }
        });

        // Query parameters per modulo web
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
        if (totalLaps != null && !totalLaps.trim().isEmpty()) {
            queryParams.append("&total_laps=").append(Uri.encode(totalLaps.trim()));
        }
        if (circuitTimeZone != null && !circuitTimeZone.trim().isEmpty()) {
            queryParams.append("&circuit_timezone=").append(Uri.encode(circuitTimeZone.trim()));
        }
        if (sessionElapsedSeconds > 0) {
            queryParams.append("&session_elapsed_sec=").append(sessionElapsedSeconds);
        }
        queryParams.append("&is_live=").append(isLive ? "true" : "false");

        String url = "file:///android_asset/live_timing/track_map.html" + queryParams;
        Log.d(TAG, "Caricamento URL Live: " + url);
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

    // Metodi stub per compatibilità con eventuali frammenti legacy
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
        if (signalRService != null) {
            signalRService.stopStream();
            signalRService = null;
        }
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}