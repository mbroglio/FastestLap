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

    private WebView webView;
    private ProgressBar progressBar;
    private String circuitId = "baku";

    public LiveTrackMapFragment() {
        // Required empty public constructor
    }

    public static LiveTrackMapFragment newInstance(String circuitId) {
        LiveTrackMapFragment fragment = new LiveTrackMapFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CIRCUIT_ID, circuitId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null && getArguments().containsKey(ARG_CIRCUIT_ID)) {
            circuitId = getArguments().getString(ARG_CIRCUIT_ID, "baku");
        } else if (getActivity() != null && getActivity().getIntent() != null) {
            String eventTitle = getActivity().getIntent().getStringExtra("EVENT_TITLE");
            if (eventTitle != null) {
                String lower = eventTitle.toLowerCase();
                if (lower.contains("sepang") || lower.contains("malaysia")) {
                    circuitId = "sepang";
                } else if (lower.contains("baku") || lower.contains("azerbaijan") || lower.contains("azerbaigian")) {
                    circuitId = "baku";
                }
            }
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
                return circuitId;
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

        // Carica la dashboard web unificata del live timing con il circuito della sessione corrente
        String url = "file:///android_asset/live_timing/index.html?circuit=" + circuitId;
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
