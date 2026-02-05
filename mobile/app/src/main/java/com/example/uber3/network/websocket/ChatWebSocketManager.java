package com.example.uber3.network.websocket;

import android.annotation.SuppressLint;
import android.util.Log;

import com.example.uber3.network.manager.TokenManager;
import com.google.gson.Gson;

import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.StompHeader;
import java.util.Arrays;
import java.util.List;

public class ChatWebSocketManager {

    private static final String TAG = "CHAT_SOCKET";

    private StompClient stompClient;
    private final Gson gson = new Gson();

    private static ChatWebSocketManager instance;

    public static ChatWebSocketManager getInstance() {
        if (instance == null) {
            instance = new ChatWebSocketManager();
        }
        return instance;
    }

    @SuppressLint("CheckResult")
    public void connect(String jwtToken) {

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

        stompClient.lifecycle().subscribe(event -> {
            Log.d(TAG, "Lifecycle event: " + event.getType());
        });
    }

    @SuppressLint("CheckResult")
    public void subscribeToMessages(MessageListener listener) {
        stompClient.topic("/user/queue/messages").subscribe(topicMessage -> {
            String payload = topicMessage.getPayload();
            listener.onMessage(payload);
        });
    }

    @SuppressLint("CheckResult")
    public void sendMessage(Object message) {
        String json = gson.toJson(message);

        stompClient.send("/app/chat.send", json).subscribe(
                () -> Log.d(TAG, "Message sent"),
                throwable -> Log.e(TAG, "Send error", throwable)
        );
    }

    public interface MessageListener {
        void onMessage(String message);
    }
}
