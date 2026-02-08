package com.example.uber3.network.websocket;

import android.annotation.SuppressLint;
import android.util.Log;

import com.example.uber3.network.model.chat.ChatMessage;
import com.google.gson.Gson;

import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.StompHeader;
import io.reactivex.disposables.Disposable;

import java.util.List;

public class ChatWebSocketManager {

    private static final String TAG = "CHAT_SOCKET";

    private StompClient stompClient;
    private final Gson gson = new Gson();

    private UiMessageListener uiListener;
    private Disposable messageSubscription;
    private Disposable lifecycleSubscription;

    private static ChatWebSocketManager instance;
    private boolean isSubscribed = false;

    public static ChatWebSocketManager getInstance() {
        if (instance == null) {
            instance = new ChatWebSocketManager();
        }
        return instance;
    }

    public interface UiMessageListener {
        void onMessage(ChatMessage message);
    }

    public void setUiListener(UiMessageListener listener){
        Log.d(TAG, "Setting UI listener: " + (listener != null ? "Active" : "Null"));
        this.uiListener = listener;
    }

    @SuppressLint("CheckResult")
    public void connect(String jwtToken) {

        if (stompClient != null && stompClient.isConnected()) {
            Log.d(TAG, "Already connected, skipping reconnection");
            return;
        }

        Log.d(TAG, "Connecting to WebSocket...");

        stompClient = Stomp.over(
                Stomp.ConnectionProvider.OKHTTP,
                "ws://10.0.2.2:8080/ws/websocket"
        );

        stompClient.withClientHeartbeat(10000).withServerHeartbeat(10000);

        stompClient.connect(
                List.of(
                        new StompHeader("Authorization", "Bearer " + jwtToken)
                )
        );

        if (lifecycleSubscription != null && !lifecycleSubscription.isDisposed()) {
            lifecycleSubscription.dispose();
        }

        lifecycleSubscription = stompClient.lifecycle().subscribe(event -> {
            Log.d(TAG, "Lifecycle event: " + event.getType());
        });
    }

    @SuppressLint("CheckResult")
    public void subscribeToMessages() {

        if (stompClient == null) {
            Log.e(TAG, "Cannot subscribe - stompClient is null");
            return;
        }

        if (messageSubscription != null && !messageSubscription.isDisposed()) {
            Log.d(TAG, "Disposing old message subscription to prevent duplicates");
            messageSubscription.dispose();
        }

        Log.d(TAG, "Subscribing to /user/queue/messages");
        isSubscribed = true;

        messageSubscription = stompClient.topic("/user/queue/messages")
                .subscribe(topicMessage -> {

                    String payload = topicMessage.getPayload();
                    Log.d(TAG, "📨 Received WebSocket message: " + payload);

                    ChatMessage cm = gson.fromJson(payload, ChatMessage.class);

                    Log.d(TAG, "   From: " + cm.fromUserId);
                    Log.d(TAG, "   To: " + cm.toUserId);
                    Log.d(TAG, "   Content: " + cm.content);

                    if(uiListener != null){
                        Log.d(TAG, "   ✅ Delivering to UI listener");
                        uiListener.onMessage(cm);
                    } else {
                        Log.w(TAG, "   ⚠️ No UI listener set, message ignored");
                    }

                }, throwable -> {
                    Log.e(TAG, "Subscribe error", throwable);
                    isSubscribed = false;
                });
    }

    @SuppressLint("CheckResult")
    public void sendMessage(Object message) {
        if (stompClient == null || !stompClient.isConnected()) {
            Log.e(TAG, "❌ Cannot send - not connected");
            return;
        }

        String json = gson.toJson(message);

        Log.d(TAG, "📤 Sending message: " + json);

        stompClient.send("/app/chat.send", json).subscribe(
                () -> Log.d(TAG, "✅ Message sent successfully"),
                throwable -> Log.e(TAG, "❌ Send error", throwable)
        );
    }

    public boolean isSubscribed() {
        return isSubscribed && messageSubscription != null && !messageSubscription.isDisposed();
    }

    public void disconnect() {
        Log.d(TAG, "Disconnecting WebSocket...");

        isSubscribed = false;

        if (messageSubscription != null && !messageSubscription.isDisposed()) {
            messageSubscription.dispose();
            messageSubscription = null;
        }

        if (lifecycleSubscription != null && !lifecycleSubscription.isDisposed()) {
            lifecycleSubscription.dispose();
            lifecycleSubscription = null;
        }

        if (stompClient != null) {
            stompClient.disconnect();
            stompClient = null;
        }

        uiListener = null;
    }
}