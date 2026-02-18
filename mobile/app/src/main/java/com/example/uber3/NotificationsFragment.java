package com.example.uber3;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uber3.adapter.NotificationAdapter;
import com.example.uber3.network.manager.TokenManager;
import com.example.uber3.network.model.notification.Notification;
import com.example.uber3.network.model.notification.NotificationType;
import com.example.uber3.network.service.NotificationService;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment
        implements NotificationService.StateListener,
        NotificationAdapter.OnNotificationActionListener {

    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private TextView tvUnreadBadge;
    private Button btnAll, btnUnread, btnRides, btnProfile, btnMarkAllRead;

    private NotificationAdapter adapter;
    private String currentFilter = "all";
    private String userRole = "PASSENGER";

    public static NotificationsFragment newInstance() {
        return new NotificationsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userRole = TokenManager.getRole(requireContext());

        bindViews(view);
        setupRecyclerView();
        setupFilterButtons();
        setupMarkAllButton();
        adjustFiltersForRole();

        NotificationService.getInstance().addListener(this);

        onNotificationsChanged(
                NotificationService.getInstance().getNotifications(),
                NotificationService.getInstance().getUnreadCount()
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        NotificationService.getInstance().removeListener(this);
    }

    private void bindViews(View view) {
        recyclerView   = view.findViewById(R.id.rvNotifications);
        emptyState     = view.findViewById(R.id.layoutEmptyState);
        tvUnreadBadge  = view.findViewById(R.id.tvUnreadBadge);
        btnAll         = view.findViewById(R.id.btnFilterAll);
        btnUnread      = view.findViewById(R.id.btnFilterUnread);
        btnRides       = view.findViewById(R.id.btnFilterRides);
        btnProfile     = view.findViewById(R.id.btnFilterProfile);
        btnMarkAllRead = view.findViewById(R.id.btnMarkAllRead);
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupFilterButtons() {
        btnAll.setOnClickListener(v -> setFilter("all"));
        btnUnread.setOnClickListener(v -> setFilter("unread"));
        btnRides.setOnClickListener(v -> setFilter("rides"));
        btnProfile.setOnClickListener(v -> setFilter("profile"));
    }

    private void adjustFiltersForRole() {
        if ("ADMIN".equals(userRole)) {
            btnRides.setVisibility(View.GONE);
            btnProfile.setVisibility(View.GONE);
        }
    }

    private void setFilter(String filter) {
        currentFilter = filter;
        updateFilterButtonStates();
        applyFilterAndRender(
                NotificationService.getInstance().getNotifications(),
                NotificationService.getInstance().getUnreadCount()
        );
    }

    private void updateFilterButtonStates() {
        setFilterButtonColor(btnAll,     currentFilter.equals("all"));
        setFilterButtonColor(btnUnread,  currentFilter.equals("unread"));
        setFilterButtonColor(btnRides,   currentFilter.equals("rides"));
        setFilterButtonColor(btnProfile, currentFilter.equals("profile"));
    }

    private void setFilterButtonColor(Button btn, boolean active) {
        if (active) {
            btn.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            requireContext().getColor(R.color.colorPrimary)));
            btn.setTextColor(0xFFFFFFFF);
        } else {
            btn.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFEEEEEE));
            btn.setTextColor(0xFF333333);
        }
    }

    private void setupMarkAllButton() {
        btnMarkAllRead.setOnClickListener(v ->
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.notif_dialog_mark_all_title)
                        .setMessage(R.string.notif_dialog_mark_all_message)
                        .setPositiveButton(R.string.notif_dialog_yes, (dialog, which) ->
                                NotificationService.getInstance().markAllAsRead(requireContext()))
                        .setNegativeButton(R.string.notif_dialog_cancel, null)
                        .show()
        );
    }

    @Override
    public void onNotificationsChanged(List<Notification> notifications, int unreadCount) {
        if (!isAdded()) return;
        applyFilterAndRender(notifications, unreadCount);
    }

    private void applyFilterAndRender(List<Notification> all, int unreadCount) {
        List<Notification> roleFiltered = NotificationService.getInstance().filterByRole(userRole);

        List<Notification> result = new ArrayList<>();
        for (Notification n : roleFiltered) {
            if (passesFilter(n)) result.add(n);
        }

        tvUnreadBadge.setText(getString(R.string.notif_unread_badge_prefix) + unreadCount);
        tvUnreadBadge.setVisibility(unreadCount > 0 ? View.VISIBLE : View.GONE);

        btnMarkAllRead.setVisibility(unreadCount > 0 ? View.VISIBLE : View.GONE);

        btnUnread.setText(getString(R.string.notif_filter_unread) + " (" + unreadCount + ")");
        btnAll.setText(getString(R.string.notif_filter_all) + " (" + roleFiltered.size() + ")");

        if (result.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }

        adapter.submitList(result);
    }

    private boolean passesFilter(Notification n) {
        switch (currentFilter) {
            case "unread":
                return !n.isRead;
            case "rides":
                return n.type.equals(NotificationType.ACCEPTED_RIDE)
                        || n.type.equals(NotificationType.DECLINED_RIDE)
                        || n.type.equals(NotificationType.RIDE_REMINDER)
                        || n.type.equals(NotificationType.FINISHED_RIDE)
                        || n.type.equals(NotificationType.RIDE_CANCELED);
            case "profile":
                return n.type.equals(NotificationType.PROFILE_CHANGE);
            default:
                return true;
        }
    }

    @Override
    public void onMarkAsRead(long notificationId) {
        NotificationService.getInstance().markAsRead(requireContext(), notificationId);
    }

    @Override
    public void onDelete(long notificationId) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.notif_dialog_delete_title)
                .setMessage(R.string.notif_dialog_delete_message)
                .setPositiveButton(R.string.notif_dialog_delete_confirm, (dialog, which) ->
                        NotificationService.getInstance()
                                .deleteNotification(requireContext(), notificationId))
                .setNegativeButton(R.string.notif_dialog_cancel, null)
                .show();
    }
}