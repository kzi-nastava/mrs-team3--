package com.example.uber3.network.websocket;

import android.annotation.SuppressLint;
import android.util.Log;

import com.example.uber3.network.model.chat.ChatMessage;
import com.google.gson.Gson;

import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.StompHeader;
import ua.naiksoftware.stomp.dto.LifecycleEvent;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.android.schedulers.AndroidSchedulers;

import java.util.List;

public class ChatWebSocketManager {

    private static final String TAG = "CHAT_SOCKET";

    private StompClient stompClient;
    private final Gson gson = new Gson();

    private MessageListener messageListener;
    private CompositeDisposable disposables = new CompositeDisposable();

    private static ChatWebSocketManager instance;

    public static ChatWebSocketManager getInstance() {
        if (instance == null) {
            instance = new ChatWebSocketManager();
        }
        return instance;
    }

    private ChatWebSocketManager() {}


    public interface MessageListener {
        void onMessage(ChatMessage message);
    }

    public void setMessageListener(MessageListener listener) {
        Log.d(TAG, "Setting message listener: " + (listener != null ? "Active" : "Null"));
        this.messageListener = listener;
    }


    @SuppressLint("CheckResult")
    public void connect(String jwtToken) {
        if (stompClient != null && stompClient.isConnected()) {
            Log.d(TAG, "Already connected — skipping");
            return;
        }

        Log.d(TAG, "Connecting to WebSocket for chat...");

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
                        case OPENED:
                            Log.d(TAG, "✅ WebSocket OPENED");
                            break;
                        case CLOSED:
                            Log.w(TAG, "⚠️ WebSocket CLOSED");
                            break;
                        case ERROR:
                            Log.e(TAG, "❌ WebSocket ERROR: " + event.getException());
                            break;
                        default:
                            break;
                    }
                }, throwable -> Log.e(TAG, "Lifecycle error", throwable));

        disposables.add(lifecycle);

        subscribeToMessages();

        stompClient.connect(
                List.of(new StompHeader("Authorization", "Bearer " + jwtToken))
        );
    }


    @SuppressLint("CheckResult")
    private void subscribeToMessages() {
        if (stompClient == null) {
            Log.e(TAG, "subscribeToMessages: stompClient is null");
            return;
        }

        Log.d(TAG, "Registering subscription to /user/queue/messages");

        Disposable sub = stompClient
                .topic("/user/queue/messages")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(frame -> {
                    String payload = frame.getPayload();
                    Log.d(TAG, "📨 Chat message received: " + payload);

                    try {
                        ChatMessage message = gson.fromJson(payload, ChatMessage.class);
                        Log.d(TAG, "   From: " + message.fromUserId + " → To: " + message.toUserId);
                        Log.d(TAG, "   Content: " + message.content);

                        if (messageListener != null) {
                            messageListener.onMessage(message);
                        } else {
                            Log.w(TAG, "⚠️ No listener set — storing in ChatService buffer");
                        }


                        com.example.uber3.network.service.ChatService
                                .getInstance()
                                .onWebSocketMessage(message);

                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse chat message payload", e);
                    }

                }, throwable -> Log.e(TAG, "Subscription error", throwable));

        disposables.add(sub);
    }


    public void sendMessage(ChatMessage message) {
        if (stompClient == null || !stompClient.isConnected()) {
            Log.e(TAG, "❌ Cannot send — not connected");
            return;
        }

        String json = gson.toJson(message);
        Log.d(TAG, "📤 Sending message: " + json);

        Disposable send = stompClient
                .send("/app/chat.send", json)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> Log.d(TAG, "✅ Message sent successfully"),
                        throwable -> Log.e(TAG, "❌ Send error", throwable)
                );

        disposables.add(send);
    }


    public void disconnect() {
        Log.d(TAG, "Disconnecting chat WebSocket...");

        messageListener = null;
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