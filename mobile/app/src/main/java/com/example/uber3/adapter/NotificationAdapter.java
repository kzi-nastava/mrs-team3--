package com.example.uber3.adapter;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uber3.R;
import com.example.uber3.network.model.notification.Notification;
import com.example.uber3.network.model.notification.NotificationType;
import com.example.uber3.network.service.NotificationService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    public interface OnNotificationActionListener {
        void onMarkAsRead(long notificationId);
        void onDelete(long notificationId);
    }

    private List<Notification> items = new ArrayList<>();
    private OnNotificationActionListener actionListener;

    public NotificationAdapter(OnNotificationActionListener listener) {
        this.actionListener = listener;
    }

    public void submitList(List<Notification> newList) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return items.size(); }
            @Override public int getNewListSize() { return newList.size(); }

            @Override
            public boolean areItemsTheSame(int o, int n) {
                return items.get(o).id == newList.get(n).id;
            }

            @Override
            public boolean areContentsTheSame(int o, int n) {
                Notification a = items.get(o), b = newList.get(n);
                return a.isRead == b.isRead && safeEquals(a.message, b.message);
            }
        });

        items = new ArrayList<>(newList);
        result.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() { return items.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final View unreadIndicator;
        private final TextView tvIcon;
        private final TextView tvTitle;
        private final TextView tvMessage;
        private final TextView tvTime;
        private final ImageButton btnMarkRead;
        private final ImageButton btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            unreadIndicator = itemView.findViewById(R.id.viewUnreadIndicator);
            tvIcon         = itemView.findViewById(R.id.tvNotificationIcon);
            tvTitle        = itemView.findViewById(R.id.tvNotificationTitle);
            tvMessage      = itemView.findViewById(R.id.tvNotificationMessage);
            tvTime         = itemView.findViewById(R.id.tvNotificationTime);
            btnMarkRead    = itemView.findViewById(R.id.btnMarkRead);
            btnDelete      = itemView.findViewById(R.id.btnDelete);
        }

        void bind(Notification notification) {
            tvIcon.setText(getIcon(notification.type));
            tvTitle.setText(NotificationService.getNotificationTitle(notification.type));
            tvTitle.setTextColor(getTitleColor(notification.type));
            tvMessage.setText(notification.message);
            tvTime.setText(formatTime(notification.createdAt));

            // Unread styling
            unreadIndicator.setVisibility(notification.isRead ? View.INVISIBLE : View.VISIBLE);
            tvTitle.setTypeface(null,
                    notification.isRead ? Typeface.NORMAL : Typeface.BOLD);
            tvMessage.setTypeface(null,
                    notification.isRead ? Typeface.NORMAL : Typeface.BOLD);

            if (notification.isRead) {
                itemView.setBackgroundColor(
                        ContextCompat.getColor(itemView.getContext(), android.R.color.white));
                btnMarkRead.setVisibility(View.GONE);
            } else {
                itemView.setBackgroundColor(
                        ContextCompat.getColor(itemView.getContext(), R.color.notification_unread_bg));
                btnMarkRead.setVisibility(View.VISIBLE);
            }

            btnMarkRead.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onMarkAsRead(notification.id);
            });

            btnDelete.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onDelete(notification.id);
            });
        }

        private String getIcon(String type) {
            if (type == null) return "🔔";
            switch (type) {
                case NotificationType.PANIC:          return "🚨";
                case NotificationType.PROFILE_CHANGE: return "👤";
                case NotificationType.ACCEPTED_RIDE:  return "✅";
                case NotificationType.DECLINED_RIDE:  return "❌";
                case NotificationType.RIDE_REMINDER:  return "⏰";
                case NotificationType.FINISHED_RIDE:  return "🏁";
                case NotificationType.RIDE_CANCELED:  return "🚫";
                default:                              return "🔔";
            }
        }

        private int getTitleColor(String type) {
            int colorRes;
            if (type == null) {
                colorRes = R.color.notif_default;
            } else switch (type) {
                case NotificationType.PANIC:
                case NotificationType.DECLINED_RIDE:
                case NotificationType.RIDE_CANCELED:
                    colorRes = R.color.notif_red;    break;
                case NotificationType.ACCEPTED_RIDE:
                case NotificationType.FINISHED_RIDE:
                    colorRes = R.color.notif_green;  break;
                case NotificationType.RIDE_REMINDER:
                    colorRes = R.color.notif_orange; break;
                case NotificationType.PROFILE_CHANGE:
                    colorRes = R.color.notif_blue;   break;
                default:
                    colorRes = R.color.notif_default; break;
            }
            return ContextCompat.getColor(itemView.getContext(), colorRes);
        }

        private String formatTime(String dateString) {
            if (dateString == null) return "";
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                Date date = sdf.parse(dateString);
                if (date == null) return dateString;

                long diffMs = System.currentTimeMillis() - date.getTime();
                long mins   = TimeUnit.MILLISECONDS.toMinutes(diffMs);
                long hours  = TimeUnit.MILLISECONDS.toHours(diffMs);
                long days   = TimeUnit.MILLISECONDS.toDays(diffMs);

                if (mins < 1)   return "Just now";
                if (mins < 60)  return mins + "m ago";
                if (hours < 24) return hours + "h ago";
                if (days < 7)   return days + "d ago";
                return new SimpleDateFormat("MMM d", Locale.getDefault()).format(date);

            } catch (ParseException e) {
                return dateString;
            }
        }
    }

    private static boolean safeEquals(String a, String b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }
}