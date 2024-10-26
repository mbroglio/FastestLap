package com.the_coffe_coders.fastestlap;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.chromium.net.CronetEngine;
import org.chromium.net.CronetException;
import org.chromium.net.UrlRequest;
import org.chromium.net.UrlRequest.Callback;
import org.chromium.net.UrlResponseInfo;

import java.nio.ByteBuffer;
import java.util.concurrent.Executors;

public class TeamCardActivity extends AppCompatActivity {

    private static final String TAG = "TeamCardActivity";
    private static final String URL = "https://api.jolpi.ca/ergast/f1/2024/constructorstandings/";

    // Use StringBuilder to accumulate the response
    private StringBuilder responseBuilder = new StringBuilder();

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

        fetchConstructorStandings();  // Call the method to fetch data
    }

    private void fetchConstructorStandings() {
        // Initialize Cronet Engine
        CronetEngine cronetEngine = new CronetEngine.Builder(this).build();

        // Set up the request
        UrlRequest.Builder requestBuilder = cronetEngine.newUrlRequestBuilder(
                URL,
                new Callback() {
                    @Override
                    public void onSucceeded(UrlRequest request, UrlResponseInfo info) {
                        Log.d(TAG, "Request succeeded with response code: " + info.getHttpStatusCode());
                        // Optionally check Content-Length or other headers
                        String contentLength = info.getHttpStatusText();
                        Log.d(TAG, "Response Content-Length: " + contentLength);
                    }

                    @Override
                    public void onFailed(UrlRequest request, UrlResponseInfo info, CronetException error) {
                        Log.e(TAG, "Request failed", error);
                    }

                    @Override
                    public void onRedirectReceived(UrlRequest request, UrlResponseInfo info, String newLocationUrl) throws Exception {
                        // Handle redirects if needed
                    }

                    @Override
                    public void onResponseStarted(UrlRequest request, UrlResponseInfo info) {
                        Log.d(TAG, "Response started with code: " + info.getHttpStatusCode());
                        // Start reading the response
                        request.read(ByteBuffer.allocateDirect(1024));
                    }

                    // FIX: Response is empty
                    @Override
                    public void onReadCompleted(UrlRequest request, UrlResponseInfo info, ByteBuffer byteBuffer) {
                        if (byteBuffer.remaining() == 0) {
                            // No more data to read
                            Log.d(TAG, "No more data to read");
                            Log.d(TAG, "Response: " + responseBuilder.toString());
                            updateUI(responseBuilder.toString()); // Use the accumulated response
                            return;
                        }

                        byteBuffer.flip();
                        byte[] responseBytes = new byte[byteBuffer.remaining()];
                        byteBuffer.get(responseBytes);

                        // Append the read data to the StringBuilder
                        responseBuilder.append(new String(responseBytes));

                        Log.d(TAG, "Read " + responseBytes.length + " bytes");

                        byteBuffer.clear();
                        request.read(byteBuffer);  // Continue reading
                    }
                },
                Executors.newSingleThreadExecutor()
        );

        // Start the request
        UrlRequest request = requestBuilder.build();
        request.start();
    }

    private void updateUI(String response) {
        // Update your UI components here
        Log.d(TAG, "Updating UI with response: " + response);
        // For example, you could parse the JSON and update a TextView
    }
}
