package com.example.uber3.network.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.uber3.MainActivity;
import com.example.uber3.R;
import com.example.uber3.network.api.ApiClient;
import com.example.uber3.network.api.NotificationApi;
import com.example.uber3.network.manager.TokenManager;
import com.example.uber3.network.model.notification.Notification;
import com.example.uber3.network.model.notification.NotificationCount;
import com.example.uber3.network.model.notification.NotificationType;
import com.example.uber3.network.websocket.NotificationWebSocketManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationService {

    private static final String TAG = "NotificationService";
    private static final String CHANNEL_ID = "uber_notifications";
    private static final String CHANNEL_NAME = "Ride Notifications";
    private static final int PUSH_NOTIFICATION_BASE_ID = 1000;

    private static NotificationService instance;

    public static NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }

    private NotificationService() {}


    private final List<Notification> notifications = new CopyOnWriteArrayList<>();
    private int unreadCount = 0;

    public interface StateListener {
        void onNotificationsChanged(List<Notification> notifications, int unreadCount);
    }

    private final List<StateListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(StateListener listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    public void removeListener(StateListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        // Always deliver a fresh copy of the list so the fragment always gets new references
        List<Notification> snapshot = new ArrayList<>(notifications);
        for (StateListener l : listeners) {
            l.onNotificationsChanged(snapshot, unreadCount);
        }
    }

    public void initialize(Context context, String jwtToken) {
        createNotificationChannel(context);

        NotificationWebSocketManager wsManager = NotificationWebSocketManager.getInstance();
        wsManager.setNotificationListener(notification -> {
            android.os.Handler mainHandler =
                    new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.post(() -> handleIncomingNotification(context, notification));
        });
        wsManager.connect(jwtToken);

        loadNotifications(context);
    }

    public void loadNotifications(Context context) {
        String token = bearer(context);
        if (token == null) {
            Log.e(TAG, "loadNotifications: no token, skipping");
            return;
        }

        NotificationApi api = ApiClient.getClient(context).create(NotificationApi.class);
        api.getNotifications(token).enqueue(new Callback<List<Notification>>() {
            @Override
            public void onResponse(@NonNull Call<List<Notification>> call,
                                   @NonNull Response<List<Notification>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Loaded " + response.body().size() + " notifications");
                    notifications.clear();
                    notifications.addAll(response.body());
                    recalculateUnread();
                    notifyListeners();
                } else {
                    Log.e(TAG, "loadNotifications HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Notification>> call, @NonNull Throwable t) {
                Log.e(TAG, "loadNotifications failed", t);
            }
        });
    }

    public void markAsRead(Context context, long notificationId) {
        String token = bearer(context);
        if (token == null) return;

        NotificationApi api = ApiClient.getClient(context).create(NotificationApi.class);
        api.markAsRead(token, notificationId).enqueue(new Callback<Notification>() {
            @Override
            public void onResponse(@NonNull Call<Notification> call,
                                   @NonNull Response<Notification> response) {
                if (response.isSuccessful()) {
                    replaceWithReadCopy(notificationId);
                } else {
                    Log.e(TAG, "markAsRead HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Notification> call, @NonNull Throwable t) {
                Log.e(TAG, "markAsRead failed", t);
            }
        });
    }

    public void markAllAsRead(Context context) {
        String token = bearer(context);
        if (token == null) return;

        NotificationApi api = ApiClient.getClient(context).create(NotificationApi.class);
        api.markAllAsRead(token).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    // Replace every unread entry with a read copy
                    for (int i = 0; i < notifications.size(); i++) {
                        Notification n = notifications.get(i);
                        if (!n.isRead) {
                            notifications.set(i, copyWithRead(n, true));
                        }
                    }
                    unreadCount = 0;
                    notifyListeners();
                } else {
                    Log.e(TAG, "markAllAsRead HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "markAllAsRead failed", t);
            }
        });
    }

    public void deleteNotification(Context context, long notificationId) {
        String token = bearer(context);
        if (token == null) return;

        NotificationApi api = ApiClient.getClient(context).create(NotificationApi.class);
        api.deleteNotification(token, notificationId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    notifications.removeIf(n -> n.id == notificationId);
                    recalculateUnread();
                    notifyListeners();
                } else {
                    Log.e(TAG, "deleteNotification HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "deleteNotification failed", t);
            }
        });
    }

    private void handleIncomingNotification(Context context, Notification notification) {
        for (Notification n : notifications) {
            if (n.id == notification.id) return; // dedup
        }

        notifications.add(0, notification);
        if (!notification.isRead) unreadCount++;

        notifyListeners();
        showPushNotification(context, notification);
    }

    private void showPushNotification(Context context, Notification notification) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(MainActivity.EXTRA_OPEN_NOTIFICATIONS, true);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, (int) notification.id, intent, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(getNotificationIcon(notification.type))
                .setContentTitle(getNotificationTitle(notification.type))
                .setContentText(notification.message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notification.message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        manager.notify(PUSH_NOTIFICATION_BASE_ID + (int)(notification.id % 1000), builder.build());
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Real-time ride and profile notifications");
            channel.enableVibration(true);

            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    public List<Notification> filterByRole(String role) {
        List<Notification> result = new ArrayList<>();
        List<String> allowed = getAllowedTypes(role);
        for (Notification n : notifications) {
            if (allowed.contains(n.type)) result.add(n);
        }
        return result;
    }

    private List<String> getAllowedTypes(String role) {
        List<String> types = new ArrayList<>();
        switch (role) {
            case "ADMIN":
                types.add(NotificationType.PANIC);
                types.add(NotificationType.PROFILE_CHANGE);
                break;
            case "PASSENGER":
                types.add(NotificationType.ACCEPTED_RIDE);
                types.add(NotificationType.DECLINED_RIDE);
                types.add(NotificationType.RIDE_REMINDER);
                types.add(NotificationType.FINISHED_RIDE);
                types.add(NotificationType.RIDE_CANCELED);
                types.add(NotificationType.PROFILE_CHANGE);
                break;
            case "DRIVER":
                types.add(NotificationType.ACCEPTED_RIDE);
                types.add(NotificationType.DECLINED_RIDE);
                types.add(NotificationType.RIDE_REMINDER);
                types.add(NotificationType.RIDE_CANCELED);
                types.add(NotificationType.FINISHED_RIDE);
                types.add(NotificationType.PROFILE_CHANGE);
                break;
        }
        return types;
    }

    private void replaceWithReadCopy(long id) {
        for (int i = 0; i < notifications.size(); i++) {
            Notification n = notifications.get(i);
            if (n.id == id) {
                notifications.set(i, copyWithRead(n, true));
                break;
            }
        }
        recalculateUnread();
        notifyListeners();
    }

    private Notification copyWithRead(Notification src, boolean isRead) {
        Notification copy = new Notification();
        copy.id              = src.id;
        copy.type            = src.type;
        copy.message         = src.message;
        copy.isRead          = isRead;
        copy.createdAt       = src.createdAt;
        copy.relatedEntityId = src.relatedEntityId;
        copy.recipientRole   = src.recipientRole;
        return copy;
    }

    private void recalculateUnread() {
        int count = 0;
        for (Notification n : notifications) {
            if (!n.isRead) count++;
        }
        unreadCount = count;
    }

    /** Returns "Bearer <token>" or null if not logged in */
    private String bearer(Context context) {
        String token = TokenManager.getToken(context);
        if (token == null) return null;
        return "Bearer " + token;
    }

    public List<Notification> getNotifications() {
        return new ArrayList<>(notifications);
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void disconnect() {
        NotificationWebSocketManager.getInstance().disconnect();
        notifications.clear();
        unreadCount = 0;
        listeners.clear();
    }

    public static String getNotificationTitle(String type) {
        if (type == null) return "Notification";
        switch (type) {
            case NotificationType.ACCEPTED_RIDE:  return "Ride Accepted";
            case NotificationType.DECLINED_RIDE:  return "Ride Declined";
            case NotificationType.RIDE_REMINDER:  return "Ride Reminder";
            case NotificationType.FINISHED_RIDE:  return "Ride Finished";
            case NotificationType.RIDE_CANCELED:  return "Ride Canceled";
            case NotificationType.PROFILE_CHANGE: return "Profile Update";
            case NotificationType.PANIC:          return "🚨 Emergency Alert";
            default: return type.replace("_", " ");
        }
    }

    public static int getNotificationIcon(String type) {
        if (type == null) return android.R.drawable.ic_dialog_info;
        switch (type) {
            case NotificationType.PANIC:          return android.R.drawable.ic_dialog_alert;
            case NotificationType.ACCEPTED_RIDE:  return android.R.drawable.checkbox_on_background;
            case NotificationType.DECLINED_RIDE:  return android.R.drawable.ic_delete;
            case NotificationType.RIDE_CANCELED:  return android.R.drawable.ic_delete;
            case NotificationType.PROFILE_CHANGE: return android.R.drawable.ic_menu_myplaces;
            default:                              return android.R.drawable.ic_dialog_info;
        }
    }
}