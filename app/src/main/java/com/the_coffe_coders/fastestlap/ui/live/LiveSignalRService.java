package com.the_coffe_coders.fastestlap.ui.live;

import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.Inflater;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * LiveSignalRService
 *
 * Gestisce la connessione nativa ad alta affidabilità con il feed SignalR Core di Formula 1
 * (https://livetiming.formula1.com/signalrcore).
 *
 * Caratteristiche principali:
 * 1. Negoziazione HTTP POST nativa con OkHttp bypassando le restrizioni CORS della WebView.
 * 2. Gestione dei cookie di sessione AWS ALB (AWSALB, AWSALBCORS) per garantire il corretto routing sticky.
 * 3. Connessione WebSocket e handshake SignalR Core (JSON v1, record separator 0x1e).
 * 4. Decompressione nativa multi-thread in Java/C++ con Inflater raw DEFLATE per CarData.z e Position.z.
 * 5. Inoltro bidirezionale istantaneo verso la WebView Javascript.
 */
public class LiveSignalRService {

    private static final String TAG = "LiveSignalRService";
    private static final String NEGOTIATE_URL = "https://livetiming.formula1.com/signalrcore/negotiate?negotiateVersion=1";
    private static final String WS_BASE_URL = "wss://livetiming.formula1.com/signalrcore?id=";
    private static final String RECORD_SEP = String.valueOf((char) 0x1e);

    private final WebView webView;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private final OkHttpClient httpClient;
    private final java.util.Map<String, FeedItem> cachedFeeds = new java.util.concurrent.ConcurrentHashMap<>();

    private static class FeedItem {
        final String feedName;
        final String jsonPayload;
        final boolean isStringLiteral;

        FeedItem(String feedName, String jsonPayload, boolean isStringLiteral) {
            this.feedName = feedName;
            this.jsonPayload = jsonPayload;
            this.isStringLiteral = isStringLiteral;
        }
    }

    private WebSocket webSocket;
    private boolean isHandshakeComplete = false;
    private boolean isRunning = false;
    private boolean isConnecting = false;

    public LiveSignalRService(WebView webView) {
        this.webView = webView;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS) // WebSocket keepalive infinito
                .retryOnConnectionFailure(true)
                .build();
    }

    public synchronized void startStream() {
        if (isRunning) return;
        isRunning = true;
        connectAsync();
    }

    public synchronized void stopStream() {
        isRunning = false;
        isConnecting = false;
        isHandshakeComplete = false;
        if (webSocket != null) {
            try {
                webSocket.close(1000, "Activity closed");
            } catch (Exception ignored) {}
            webSocket = null;
        }
    }

    public synchronized boolean isConnected() {
        return webSocket != null && isHandshakeComplete;
    }

    private synchronized void connectAsync() {
        if (!isRunning || isConnecting) return;
        isConnecting = true;
        notifyStatus("connecting", "Connessione a SignalR F1 Live...");

        networkExecutor.execute(() -> {
            try {
                performNegotiateAndConnect();
            } catch (Exception e) {
                Log.w(TAG, "Connessione SignalR fallita: " + e.getMessage());
                isConnecting = false;
                isHandshakeComplete = false;
                notifyStatus("simulation", "In attesa feed SignalR Live...");
                scheduleReconnect();
            }
        });
    }

    private void performNegotiateAndConnect() throws Exception {
        // 1. Negoziazione HTTP POST con cookie capture
        Request negotiateReq = new Request.Builder()
                .url(NEGOTIATE_URL)
                .post(RequestBody.create(new byte[0], null))
                .header("User-Agent", "BestHTTP")
                .header("Accept", "application/json")
                .build();

        String connectionToken;
        StringBuilder cookieHeader = new StringBuilder();

        try (Response resp = httpClient.newCall(negotiateReq).execute()) {
            if (!resp.isSuccessful()) {
                throw new IllegalStateException("Negoziazione HTTP fallita con codice " + resp.code());
            }

            List<String> setCookies = resp.headers("Set-Cookie");
            for (String sc : setCookies) {
                String cookieVal = sc.split(";")[0].trim();
                if (cookieHeader.length() > 0) cookieHeader.append("; ");
                cookieHeader.append(cookieVal);
            }

            String body = resp.body() != null ? resp.body().string() : "";
            JSONObject json = new JSONObject(body);
            connectionToken = json.getString("connectionToken");
        }

        if (connectionToken == null || connectionToken.isEmpty()) {
            throw new IllegalStateException("connectionToken mancante nella risposta di negoziazione");
        }

        // 2. Connessione WebSocket SignalR Core con sticky cookie AWS ALB
        String wsUrl = WS_BASE_URL + URLEncoder.encode(connectionToken, "UTF-8");
        Request.Builder wsReqBuilder = new Request.Builder()
                .url(wsUrl)
                .header("User-Agent", "BestHTTP");

        if (cookieHeader.length() > 0) {
            wsReqBuilder.header("Cookie", cookieHeader.toString());
        }

        Request wsReq = wsReqBuilder.build();
        isHandshakeComplete = false;

        webSocket = httpClient.newWebSocket(wsReq, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket ws, Response response) {
                Log.i(TAG, "WebSocket SignalR aperto. Invio handshake protocollo JSON v1...");
                isConnecting = false;
                ws.send("{\"protocol\":\"json\",\"version\":1}" + RECORD_SEP);
            }

            @Override
            public void onMessage(WebSocket ws, String text) {
                handleIncomingMessage(ws, text);
            }

            @Override
            public void onFailure(WebSocket ws, Throwable t, Response response) {
                Log.w(TAG, "Errore WebSocket SignalR: " + t.getMessage());
                isConnecting = false;
                isHandshakeComplete = false;
                notifyStatus("simulation", "Connessione interrotta — Riconnessione...");
                scheduleReconnect();
            }

            @Override
            public void onClosed(WebSocket ws, int code, String reason) {
                Log.i(TAG, "WebSocket SignalR chiuso: " + reason);
                isConnecting = false;
                isHandshakeComplete = false;
                scheduleReconnect();
            }
        });
    }

    private void handleIncomingMessage(WebSocket ws, String text) {
        if (text == null || text.isEmpty()) return;

        String[] frames = text.split(RECORD_SEP);
        for (String frameStr : frames) {
            if (frameStr == null || frameStr.trim().isEmpty()) continue;
            try {
                JSONObject frame = new JSONObject(frameStr);

                // Protocol Handshake
                if (!isHandshakeComplete) {
                    if (frame.has("error")) {
                        Log.e(TAG, "Errore handshake SignalR: " + frame.getString("error"));
                        ws.close(1000, "Handshake error");
                        return;
                    }
                    isHandshakeComplete = true;
                    Log.i(TAG, "Handshake SignalR completato con successo!");
                    notifyStatus("live", "● SIGNALR LIVE");

                    // Sottoscrizione ai canali live F1
                    String sub = "{\"type\":1,\"invocationId\":\"0\",\"target\":\"Subscribe\",\"arguments\":[[\"Heartbeat\",\"CarData.z\",\"Position.z\",\"TimingData\",\"TimingAppData\",\"SessionInfo\",\"SessionStatus\",\"TrackStatus\",\"RaceControlMessages\",\"DriverList\"]]}" + RECORD_SEP;
                    ws.send(sub);
                    continue;
                }

                // Server Ping (type 6) -> rispondi con ping
                if (frame.optInt("type") == 6) {
                    ws.send("{\"type\":6}" + RECORD_SEP);
                    continue;
                }

                // Initial Snapshot (type 3)
                if (frame.optInt("type") == 3 && frame.has("result")) {
                    JSONObject result = frame.getJSONObject("result");
                    java.util.Iterator<String> keys = result.keys();
                    while (keys.hasNext()) {
                        String feedName = keys.next();
                        dispatchFeedData(feedName, result.get(feedName));
                    }
                    continue;
                }

                // Live Delta Stream (type 1)
                if (frame.optInt("type") == 1 && "feed".equals(frame.optString("target"))) {
                    JSONArray args = frame.optJSONArray("arguments");
                    if (args != null && args.length() >= 2) {
                        String feedName = args.getString(0);
                        Object payload = args.get(1);
                        dispatchFeedData(feedName, payload);
                    }
                }

            } catch (Exception e) {
                Log.w(TAG, "Errore elaborazione frame SignalR: " + e.getMessage());
            }
        }
    }

    public synchronized void requestInitialFeeds() {
        if (isHandshakeComplete) {
            notifyStatus("live", "● SIGNALR LIVE");
        }
        for (FeedItem item : cachedFeeds.values()) {
            postToWebViewFeed(item.feedName, item.jsonPayload, item.isStringLiteral);
        }
    }

    public String getCachedFeedJson(String feedName) {
        FeedItem item = cachedFeeds.get(feedName);
        return item != null ? item.jsonPayload : null;
    }

    private void dispatchFeedData(String feedName, Object payload) {
        if (payload == null) return;

        // Decompressione nativa multi-thread in Java per Position.z e CarData.z
        if ("Position.z".equals(feedName) || "CarData.z".equals(feedName)) {
            if (payload instanceof String) {
                String decompressed = decompressRawDeflate((String) payload);
                if (decompressed != null && !decompressed.isEmpty() && !decompressed.equals("{}")) {
                    cachedFeeds.put(feedName, new FeedItem(feedName, decompressed, false));
                    postToWebViewFeed(feedName, decompressed, false);
                }
            }
            return;
        }

        // Tutti gli altri canali (TimingData, TrackStatus, RaceControlMessages, SessionStatus, DriverList, etc.)
        String jsonPayload = payload.toString();
        boolean isStringLiteral = (payload instanceof String) && !jsonPayload.startsWith("{") && !jsonPayload.startsWith("[");
        cachedFeeds.put(feedName, new FeedItem(feedName, jsonPayload, isStringLiteral));
        postToWebViewFeed(feedName, jsonPayload, isStringLiteral);
    }

    private void postToWebViewFeed(String feedName, String jsonPayload, boolean isStringLiteral) {
        mainHandler.post(() -> {
            if (webView == null) return;
            try {
                String jsArg;
                if (isStringLiteral) {
                    jsArg = "'" + jsonPayload.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "") + "'";
                } else {
                    jsArg = jsonPayload;
                }
                String js = "if (window.onSignalRFeed) window.onSignalRFeed('" + feedName + "', " + jsArg + ");";
                webView.evaluateJavascript(js, null);
            } catch (Exception e) {
                Log.w(TAG, "Errore evaluateJavascript feed: " + e.getMessage());
            }
        });
    }

    private void notifyStatus(String status, String detail) {
        mainHandler.post(() -> {
            if (webView == null) return;
            try {
                String safeDetail = (detail != null) ? detail.replace("'", "\\'") : "";
                String js = "if (window.onSignalRStatus) window.onSignalRStatus('" + status + "', '" + safeDetail + "');";
                webView.evaluateJavascript(js, null);
            } catch (Exception ignored) {}
        });
    }

    private void scheduleReconnect() {
        if (!isRunning) return;
        mainHandler.postDelayed(() -> {
            if (isRunning && !isHandshakeComplete && !isConnecting) {
                connectAsync();
            }
        }, 4000);
    }

    private String decompressRawDeflate(String base64Data) {
        if (base64Data == null || base64Data.isEmpty()) return "{}";
        try {
            byte[] compressed = Base64.decode(base64Data, Base64.DEFAULT);
            Inflater inflater = new Inflater(true); // nowrap = true per raw DEFLATE RFC 1951
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
            return "{}";
        }
    }
}
