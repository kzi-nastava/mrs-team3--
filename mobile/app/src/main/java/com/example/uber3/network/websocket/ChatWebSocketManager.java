package com.example.uber3.network.websocket;

import android.annotation.SuppressLint;
import android.util.Log;

import com.example.uber3.network.model.chat.ChatMessage;
import com.google.gson.Gson;

import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.StompHeader;
import java.util.List;

public class ChatWebSocketManager {

    private static final String TAG = "CHAT_SOCKET";

    private StompClient stompClient;
    private final Gson gson = new Gson();

    private UiMessageListener uiListener;

    private static ChatWebSocketManager instance;

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
        this.uiListener = listener;
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
    public void subscribeToMessages() {

        stompClient.topic("/user/queue/messages")
                .subscribe(topicMessage -> {

                    String payload = topicMessage.getPayload();

                    ChatMessage cm =
                            gson.fromJson(payload, ChatMessage.class);

                    if(uiListener != null){
                        uiListener.onMessage(cm);
                    }


                }, throwable -> {
                    Log.e(TAG, "Subscribe error", throwable);
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

}
