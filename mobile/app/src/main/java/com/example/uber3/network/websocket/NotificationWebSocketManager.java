package com.example.uber3.network.websocket;

import android.annotation.SuppressLint;
import android.util.Log;

import com.example.uber3.network.model.notification.Notification;
import com.google.gson.Gson;

import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.StompHeader;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.android.schedulers.AndroidSchedulers;

import java.util.List;


public class NotificationWebSocketManager {

    private static final String TAG = "NOTIF_SOCKET";

    private StompClient stompClient;
    private final Gson gson = new Gson();

    private NotificationListener notificationListener;

    private CompositeDisposable disposables = new CompositeDisposable();

    private static NotificationWebSocketManager instance;

    public static NotificationWebSocketManager getInstance() {
        if (instance == null) {
            instance = new NotificationWebSocketManager();
        }
        return instance;
    }

    private NotificationWebSocketManager() {}

    public interface NotificationListener {
        void onNotificationReceived(Notification notification);
    }

    public void setNotificationListener(NotificationListener listener) {
        Log.d(TAG, "Setting notification listener: " + (listener != null ? "Active" : "Null"));
        this.notificationListener = listener;
    }

    @SuppressLint("CheckResult")
    public void connect(String jwtToken) {
        if (stompClient != null && stompClient.isConnected()) {
            Log.d(TAG, "Already connected — skipping");
            return;
        }

        Log.d(TAG, "Connecting to WebSocket for notifications...");

        disposables.clear();

        stompClient = Stomp.over(
                Stomp.ConnectionProvider.OKHTTP,
                "ws://10.0.2.2:8080/ws/websocket"
        );

        stompClient.withClientHeartbeat(10000).withServerHeartbeat(10000);

        Disposable lifecycle = stompClient.lifecycle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(event -> {
                    Log.d(TAG, "Lifecycle: " + event.getType());
                    switch (event.getType()) {
                        case CLOSED:
                        case ERROR:
                            Log.w(TAG, "WS closed/error — subscriptions dropped");
                            break;
                        default:
                            break;
                    }
                }, throwable -> Log.e(TAG, "Lifecycle error", throwable));

        disposables.add(lifecycle);

        subscribeToNotifications();

        stompClient.connect(
                List.of(new StompHeader("Authorization", "Bearer " + jwtToken))
        );
    }

    @SuppressLint("CheckResult")
    private void subscribeToNotifications() {
        if (stompClient == null) {
            Log.e(TAG, "subscribeToNotifications: stompClient is null");
            return;
        }

        Log.d(TAG, "Registering subscription to /user/queue/notifications");

        Disposable sub = stompClient
                .topic("/user/queue/notifications")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(frame -> {
                    String payload = frame.getPayload();
                    Log.d(TAG, "📨 Notification received: " + payload);

                    try {
                        Notification notification = new Gson().fromJson(payload, Notification.class);
                        if (notificationListener != null) {
                            notificationListener.onNotificationReceived(notification);
                        } else {
                            Log.w(TAG, "⚠️ No listener set — notification ignored");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse notification payload", e);
                    }

                }, throwable -> Log.e(TAG, "Subscription error", throwable));

        disposables.add(sub);
    }

    public void disconnect() {
        Log.d(TAG, "Disconnecting notification WebSocket...");

        notificationListener = null;
        disposables.clear();

        if (stompClient != null) {
            stompClient.disconnect();
            stompClient = null;
        }
    }

    public boolean isConnected() {
        return stompClient != null && stompClient.isConnected();
    }
}