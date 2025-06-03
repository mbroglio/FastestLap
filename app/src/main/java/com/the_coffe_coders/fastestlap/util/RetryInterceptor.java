package com.the_coffe_coders.fastestlap.util;

import android.util.Log;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class RetryInterceptor implements Interceptor {
    private static final String TAG = "RetryInterceptor";
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_DELAY_MS = 1000; // 1 second

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = null;
        IOException lastException = null;

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                if (response != null) {
                    response.close(); // Close previous response to avoid memory leaks
                }

                response = chain.proceed(request);

                // If we get a 429 (Too Many Requests), retry with exponential backoff
                if (response.code() == 429) {
                    Log.w(TAG, "Received 429 response, attempt " + (attempt + 1) + "/" + (MAX_RETRIES + 1));

                    if (attempt < MAX_RETRIES) {
                        response.close();

                        // Calculate delay with exponential backoff
                        long delay = INITIAL_DELAY_MS * (1L << attempt); // 1s, 2s, 4s

                        // Check if server provided Retry-After header
                        String retryAfter = response.header("Retry-After");
                        if (retryAfter != null) {
                            try {
                                delay = Long.parseLong(retryAfter) * 1000; // Convert to milliseconds
                                Log.d(TAG, "Using server-provided Retry-After: " + delay + "ms");
                            } catch (NumberFormatException e) {
                                Log.w(TAG, "Invalid Retry-After header, using exponential backoff");
                            }
                        }

                        Log.d(TAG, "Waiting " + delay + "ms before retry");

                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IOException("Request interrupted during retry delay", e);
                        }

                        continue; // Retry the request
                    } else {
                        Log.e(TAG, "Max retries exceeded for 429 response");
                        return response; // Return the 429 response after max retries
                    }
                }

                // If we get here, the response was successful or a non-retryable error
                return response;

            } catch (IOException e) {
                lastException = e;
                Log.w(TAG, "IOException on attempt " + (attempt + 1) + "/" + (MAX_RETRIES + 1), e);

                if (attempt >= MAX_RETRIES) {
                    throw e; // Re-throw the last exception if we've exhausted retries
                }

                // Wait before retrying on IOException
                try {
                    Thread.sleep(INITIAL_DELAY_MS * (1L << attempt));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Request interrupted during retry delay", ie);
                }
            }
        }

        // This should never be reached, but just in case
        if (lastException != null) {
            throw lastException;
        }

        return response;
    }
}