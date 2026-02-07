package com.example.uber3.network.manager;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {

    private static final String PREF_NAME = "auth_prefs";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_ROLE = "user_role";
    private static final String KEY_EMAIL = "user_email";



    public static void saveToken(Context context, String token) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public static String getToken(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        return prefs.getString(KEY_TOKEN, null);
    }


    public static void logout(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        prefs.edit().clear().apply();
    }

    public static void saveRole(Context context, String role) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        prefs.edit().putString(KEY_ROLE, role).apply();
    }

    public static String getRole(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        return prefs.getString(KEY_ROLE, "GUEST");
    }

    public static String getUserEmail(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        return prefs.getString(KEY_EMAIL, "");
    }

    public static void saveUserEmail(Context context, String email) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        prefs.edit()
                .putString(KEY_EMAIL, email)
                .apply();
    }

    public static Long getUserId(Context context) {
        String token = getToken(context);
        if (token == null) return null;

        try {
            String[] parts = token.split("\\.");
            String payload = new String(
                    android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE)
            );

            org.json.JSONObject json = new org.json.JSONObject(payload);

            // pokušaj više mogućih claimova
            if (json.has("uid")) return json.getLong("uid");
            if (json.has("id")) return json.getLong("id");
            if (json.has("userId")) return json.getLong("userId");

            // sub je često string
            if (json.has("sub")) {
                String sub = json.getString("sub");
                return Long.parseLong(sub);
            }

            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }




}
