package com.the_coffe_coders.fastestlap;

import android.os.Bundle;
import android.util.Log;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import org.chromium.net.CronetEngine;
import org.chromium.net.UrlRequest;
import org.chromium.net.UrlRequest.Callback;
import org.chromium.net.UrlResponseInfo;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import org.json.JSONObject;
import org.chromium.net.CronetException;

public class TeamCardActivity extends AppCompatActivity {

    private static final String TAG = "TeamCardActivity";
    private static final String URL = "https://api.jolpi.ca/ergast/f1/2024/constructorstandings/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_team_card);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.team_card), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize CronetEngine
        CronetEngine cronetEngine = new CronetEngine.Builder(this).build();

        // Create a request
        UrlRequest.Builder requestBuilder = cronetEngine.newUrlRequestBuilder(
                URL,
                new MyUrlRequestCallback(),
                Executors.newSingleThreadExecutor()
        );

        UrlRequest request = requestBuilder.build();
        request.start();
    }

    private class MyUrlRequestCallback extends UrlRequest.Callback {
        private StringBuilder responseBuilder = new StringBuilder();

        @Override
        public void onRedirectReceived(UrlRequest request, UrlResponseInfo info, String newLocationUrl) {
            Log.i(TAG, "onRedirectReceived: " + newLocationUrl);
            request.followRedirect();
        }

        @Override
        public void onResponseStarted(UrlRequest request, UrlResponseInfo info) {
            Log.i(TAG, "onResponseStarted: " + info.getHttpStatusCode());
            request.read(ByteBuffer.allocateDirect(1024));
        }

        @Override
        public void onReadCompleted(UrlRequest request, UrlResponseInfo info, ByteBuffer byteBuffer) {
            byteBuffer.flip();
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            String chunk = new String(bytes);
            responseBuilder.append(chunk);
            byteBuffer.clear();
            request.read(byteBuffer);
        }

        @Override
        public void onSucceeded(UrlRequest request, UrlResponseInfo info) {
            Log.i(TAG, "onSucceeded: " + info.toString());
            String responseString = responseBuilder.toString();
            Log.i(TAG, "Full Response: " + responseString);

            try {
                JSONObject jsonResponse = new JSONObject(responseString);
                // Manipulate the response JSON as needed
                Log.i(TAG, "JSON Response: " + jsonResponse.toString(2));
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse JSON response", e);
            }
        }

        @Override
        public void onFailed(UrlRequest request, UrlResponseInfo info, CronetException error) {
            Log.e(TAG, "onFailed: ", error);
        }
    }
}
