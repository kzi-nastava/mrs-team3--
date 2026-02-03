package com.example.uber3.network.manager;

import android.content.Context;
import android.content.Intent;

import com.example.uber3.MainActivity;
import com.example.uber3.network.manager.TokenManager;

public class LogoutHelper {

    public static void logout(Context context) {

        // obriši JWT
        TokenManager.logout(context);

        // vrati na login screen
        Intent intent = new Intent(context, MainActivity.class);

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        context.startActivity(intent);
    }
}
