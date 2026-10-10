package com.the_coffe_coders.fastestlap.ui.live.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.the_coffe_coders.fastestlap.R;

public class LiveTrackMapFragment extends Fragment {

    public static final String ARG_CIRCUIT_ID = "arg_circuit_id";
    public static final String ARG_SESSION_TYPE = "arg_session_type";
    public static final String ARG_SESSION_PART = "arg_session_part";
    public static final String ARG_TOTAL_LAPS = "arg_total_laps";

    private WebView webView;
    private ProgressBar progressBar;
    private String circuitId = null;
    private String sessionType = "race";
    private String sessionPart = "";
    private String totalLaps = "";

    public LiveTrackMapFragment() {
        // Required empty public constructor
    }

    public static LiveTrackMapFragment newInstance(String circuitId) {
        return newInstance(circuitId, "race", "", "");
    }

    public static LiveTrackMapFragment newInstance(String circuitId, String sessionType, String sessionPart) {
        return newInstance(circuitId, sessionType, sessionPart, "");
    }

    public static LiveTrackMapFragment newInstance(String circuitId, String sessionType, String sessionPart, String totalLaps) {
        LiveTrackMapFragment fragment = new LiveTrackMapFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CIRCUIT_ID, circuitId);
        args.putString(ARG_SESSION_TYPE, sessionType);
        args.putString(ARG_SESSION_PART, sessionPart);
        args.putString(ARG_TOTAL_LAPS, totalLaps);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            circuitId = getArguments().getString(ARG_CIRCUIT_ID, null);
            sessionType = getArguments().getString(ARG_SESSION_TYPE, "race");
            sessionPart = getArguments().getString(ARG_SESSION_PART, "");
            totalLaps = getArguments().getString(ARG_TOTAL_LAPS, "");
        } else if (getActivity() != null && getActivity().getIntent() != null) {
            circuitId = getActivity().getIntent().getStringExtra("CIRCUIT_ID");
            sessionType = getActivity().getIntent().getStringExtra("SESSION_TYPE");
            if (sessionType == null) sessionType = "race";
            sessionPart = getActivity().getIntent().getStringExtra("SESSION_PART");
            if (sessionPart == null) sessionPart = "";
            totalLaps = getActivity().getIntent().getStringExtra("TOTAL_LAPS");
            if (totalLaps == null) totalLaps = "";
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_live_track_map, container, false);
        webView = view.findViewById(R.id.live_track_map_webview);
        progressBar = view.findViewById(R.id.track_map_loading_bar);

        setupWebView();
        return view;
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

        // JavaScript Bridge for bi-directional native <-> web communication
        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public String getCircuitId() {
                return circuitId != null ? circuitId : "";
            }

            @JavascriptInterface
            public String getSessionType() {
                return sessionType != null ? sessionType : "race";
            }

            @JavascriptInterface
            public String getSessionPart() {
                return sessionPart != null ? sessionPart : "";
            }

            @JavascriptInterface
            public String getTotalLaps() {
                return totalLaps != null ? totalLaps : "";
            }

            @JavascriptInterface
            public boolean isSessionLive() {
                return true;
            }

            @JavascriptInterface
            public String getCircuitTimeZone() {
                return "";
            }

            @JavascriptInterface
            public void onDriverSelected(String driverNumber) {
                // Future extension: notify native Android fragments
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
                return false;
            }
        });

        // Carica la dashboard web del live timing
        StringBuilder queryParams = new StringBuilder();
        boolean hasParam = false;
        if (circuitId != null && !circuitId.isEmpty()) {
            queryParams.append("?circuit_id=").append(android.net.Uri.encode(circuitId));
            hasParam = true;
        }
        if (sessionType != null && !sessionType.isEmpty()) {
            queryParams.append(hasParam ? "&" : "?").append("session_type=").append(android.net.Uri.encode(sessionType));
            hasParam = true;
        }
        if (sessionPart != null && !sessionPart.isEmpty()) {
            queryParams.append(hasParam ? "&" : "?").append("session_part=").append(android.net.Uri.encode(sessionPart));
            hasParam = true;
        }
        if (totalLaps != null && !totalLaps.isEmpty()) {
            queryParams.append(hasParam ? "&" : "?").append("total_laps=").append(android.net.Uri.encode(totalLaps));
        }
        String url = "file:///android_asset/live_timing/track_map.html" + queryParams;
        webView.loadUrl(url);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
        }
    }

    @Override
    public void onDestroyView() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroyView();
    }
}
