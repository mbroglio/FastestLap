package com.the_coffe_coders.fastestlap.repository.constructor;

import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_REALTIME_DATABASE;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_TEAMS_COLLECTION;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class FirebaseConstructorRepository {
    private static final String TAG = "FirebaseConstructorRepository";
    private static final long REQUEST_TIMEOUT = 30000; // 30 seconds

    private final FirebaseDatabase database;

    // Cache for ongoing requests to avoid duplicate requests
    private final ConcurrentHashMap<String, CompletableFuture<Constructor>> pendingRequests = new ConcurrentHashMap<>();

    public FirebaseConstructorRepository() {
        this.database = FirebaseDatabase.getInstance(FIREBASE_REALTIME_DATABASE);
    }

    public void getConstructorData(String constructorId, ConstructorCallback callback) {
        String requestKey = "firebase_" + constructorId;

        // Check if there's already an ongoing request
        CompletableFuture<Constructor> existingRequest = pendingRequests.get(requestKey);
        if (existingRequest != null && !existingRequest.isDone()) {
            Log.d(TAG, "Returning existing Firebase request for constructor: " + constructorId);
            existingRequest.whenComplete((constructor, throwable) -> {
                if (throwable != null) {
                    callback.onFailure(new Exception(throwable));
                } else if (constructor != null) {
                    callback.onSuccess(constructor);
                } else {
                    callback.onFailure(new NullPointerException("No constructor data returned"));
                }
            });
            return;
        }

        // Create a new CompletableFuture for this request
        CompletableFuture<Constructor> future = new CompletableFuture<>();
        pendingRequests.put(requestKey, future);

        // Set up timeout for the request
        setupRequestTimeout(future, requestKey, REQUEST_TIMEOUT);

        DatabaseReference databaseReference = database.getReference(FIREBASE_TEAMS_COLLECTION).child(constructorId);
        Log.i(TAG, "FIREBASE => " + constructorId + " REFERENCE DB: " + databaseReference);

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Constructor constructor = snapshot.getValue(Constructor.class);
                    if (constructor != null) {
                        future.complete(constructor);
                        callback.onSuccess(constructor);
                    } else {
                        Exception error = new NullPointerException("Constructor data is null");
                        future.completeExceptionally(error);
                        callback.onFailure(error);
                    }
                } else {
                    Exception error = new NullPointerException("No constructor found");
                    future.completeExceptionally(error);
                    callback.onFailure(error);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Exception exception = error.toException();
                future.completeExceptionally(exception);
                callback.onFailure(exception);
            }
        });

        // When the future is completed (success or failure), remove it from the pending requests map
        future.whenComplete((result, throwable) -> pendingRequests.remove(requestKey));
    }

    /**
     * Sets up a timeout for a request
     */
    private void setupRequestTimeout(CompletableFuture<Constructor> future, String requestKey, long timeoutMs) {
        Thread timeoutThread = new Thread(() -> {
            try {
                Thread.sleep(timeoutMs);
                if (!future.isDone()) {
                    Log.w(TAG, "Firebase request timed out after " + timeoutMs + "ms: " + requestKey);
                    Exception timeoutException = new Exception("Request timed out after " + timeoutMs + "ms");
                    future.completeExceptionally(timeoutException);
                    pendingRequests.remove(requestKey);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        timeoutThread.setDaemon(true);
        timeoutThread.start();
    }

    public interface ConstructorCallback {
        void onSuccess(Constructor constructor);
        void onFailure(Exception exception);
    }
}