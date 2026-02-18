package com.example.uber3.network.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.uber3.MainActivity;
import com.example.uber3.R;
import com.example.uber3.network.model.chat.ChatMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatService {

    private static final String TAG = "ChatService";
    private static final String CHANNEL_ID   = "chat_messages";
    private static final String CHANNEL_NAME = "Chat Messages";
    private static final int    PUSH_BASE_ID = 2000;

    private static ChatService instance;

    public static ChatService getInstance() {
        if (instance == null) {
            instance = new ChatService();
        }
        return instance;
    }

    private ChatService() {}


    private final List<ChatMessage> pendingMessages = new CopyOnWriteArrayList<>();

    private boolean chatUiVisible = false;

    private Context appContext;


    public interface IncomingMessageListener {
        void onIncomingMessage(ChatMessage message);
    }

    private IncomingMessageListener uiListener;

    public void setUiListener(IncomingMessageListener listener) {
        this.uiListener = listener;

        if (listener != null && !pendingMessages.isEmpty()) {
            Log.d(TAG, "Draining " + pendingMessages.size() + " pending messages to new listener");
            for (ChatMessage msg : new ArrayList<>(pendingMessages)) {
                listener.onIncomingMessage(msg);
            }
            pendingMessages.clear();
        }
    }

    public void removeUiListener(IncomingMessageListener listener) {
        if (this.uiListener == listener) {
            this.uiListener = null;
        }
    }


    public void initialize(Context context) {
        this.appContext = context.getApplicationContext();
        createNotificationChannel(appContext);
        Log.d(TAG, "✅ ChatService initialized");
    }

    public void setChatUiVisible(boolean visible) {
        this.chatUiVisible = visible;
    }

    public void disconnect() {
        pendingMessages.clear();
        uiListener = null;
        chatUiVisible = false;
        Log.d(TAG, "ChatService disconnected");
    }

    public void onWebSocketMessage(ChatMessage message) {
        Log.d(TAG, "onWebSocketMessage — from: " + message.fromUserId
                + "  chatUiVisible: " + chatUiVisible
                + "  listener: " + (uiListener != null ? "set" : "null"));

        if (uiListener != null) {
            uiListener.onIncomingMessage(message);
        } else {
            pendingMessages.add(message);
            Log.d(TAG, "Buffered message, total pending: " + pendingMessages.size());
        }

        if (!chatUiVisible && appContext != null) {
            showPushNotification(appContext, message);
        }
    }


    private void showPushNotification(Context context, ChatMessage message) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(MainActivity.EXTRA_OPEN_CHAT, true);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int)(message.fromUserId != null ? message.fromUserId : 0),
                intent,
                flags
        );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_email)
                        .setContentTitle("New Message")
                        .setContentText(message.content)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(message.content))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        int notifId = PUSH_BASE_ID + (int)(message.fromUserId != null ? message.fromUserId % 1000 : 0);
        manager.notify(notifId, builder.build());
        Log.d(TAG, "📲 Push notification shown for message from user " + message.fromUserId);
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Incoming chat messages");
            channel.enableVibration(true);

            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}