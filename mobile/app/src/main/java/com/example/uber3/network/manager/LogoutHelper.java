package com.example.uber3.network.manager;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.uber3.MainActivity;
import com.example.uber3.network.websocket.ChatWebSocketManager;

public class LogoutHelper {

    private static final String TAG = "LogoutHelper";

    public static void logout(Context context) {

        Log.d(TAG, "Logging out - cleaning up WebSocket and tokens");

        ChatWebSocketManager.getInstance().disconnect();

        TokenManager.logout(context);

        Intent intent = new Intent(context, MainActivity.class);

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        context.startActivity(intent);
    }
}