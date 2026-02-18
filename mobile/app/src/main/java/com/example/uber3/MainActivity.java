package com.example.uber3;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.uber3.network.api.ApiClient;
import com.example.uber3.network.api.ApiService;
import com.example.uber3.network.manager.LogoutHelper;
import com.example.uber3.network.manager.TokenManager;
import com.example.uber3.network.service.ChatService;
import com.example.uber3.network.service.NotificationService;
import com.example.uber3.network.websocket.ChatWebSocketManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_OPEN_NOTIFICATIONS = "open_notifications";
    public static final String EXTRA_OPEN_CHAT          = "open_chat";

    private DrawerLayout   drawerLayout;
    private MaterialToolbar topAppBar;
    private NavigationView navigationView;
    private String currentUserRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentUserRole = TokenManager.getRole(this);
        String token    = TokenManager.getToken(this);

        if (token != null) {
            ChatWebSocketManager.getInstance().connect(token);
            NotificationService.getInstance().initialize(this, token);

            ChatService.getInstance().initialize(this);
        }

        requestNotificationPermissionIfNeeded();

        drawerLayout   = findViewById(R.id.drawerLayout);
        topAppBar      = findViewById(R.id.topAppBar);
        navigationView = findViewById(R.id.navigationView);

        View header = navigationView.getHeaderView(0);
        TextView tvEmail = header.findViewById(R.id.tvEmail);
        tvEmail.setText(TokenManager.getUserEmail(this));

        topAppBar.setNavigationIcon(R.drawable.ic_menu);
        topAppBar.setNavigationOnClickListener(v -> drawerLayout.open());

        updateMenuVisibility();
        updateMenuByRole();

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                handleHomeNavigation();
            } else if (id == R.id.nav_chat) {
                handleChatNavigation();
            } else if (id == R.id.nav_ride) {
                handleRideNavigation();
            } else if (id == R.id.nav_profile) {
                topAppBar.setTitle("Profile");
                loadFragment(ProfileFragment.newInstance(currentUserRole));
            } else if (id == R.id.nav_notifications) {
                topAppBar.setTitle("Notifications");
                loadFragment(NotificationsFragment.newInstance());
            } else if (id == R.id.nav_track_ride) {
                loadRideTrackingFragment();
            } else if (id == R.id.nav_driver_dashboard) {
                topAppBar.setTitle("Driver Dashboard");
                loadFragment(DriverDashboardFragment.newInstance());
            } else if (id == R.id.nav_requests) {
                topAppBar.setTitle("Change Requests");
                loadFragment(new DriverChangeRequestFragment());
            } else if (id == R.id.nav_login) {
                topAppBar.setTitle("Login");
                loadFragment(new LoginFragment());
            } else if (id == R.id.nav_logout) {
                LogoutHelper.logout(this);
                return true;
            } else if (id == R.id.nav_register_driver) {
                topAppBar.setTitle("Register Driver");
                loadFragment(new RegisterDriverFragment());
            } else if (id == R.id.nav_report) {
                topAppBar.setTitle("Reports");
                loadFragment(new ReportFragment());
            } else if (id == R.id.nav_admin_users) {
                topAppBar.setTitle("Users Management");
                loadFragment(AdminUsersFragment.newInstance());
            }

            drawerLayout.close();
            return true;
        });

        if (savedInstanceState == null) {
            if (getIntent().getBooleanExtra(EXTRA_OPEN_NOTIFICATIONS, false)) {
                topAppBar.setTitle("Notifications");
                loadFragment(NotificationsFragment.newInstance());
            } else if (getIntent().getBooleanExtra(EXTRA_OPEN_CHAT, false)) {
                handleChatNavigation();
            } else if (TokenManager.getToken(this) != null) {
                handleHomeNavigation();
            } else {
                topAppBar.setTitle("Login");
                loadFragment(new LoginFragment());
            }
        }

        handleDeepLink(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (intent.getBooleanExtra(EXTRA_OPEN_NOTIFICATIONS, false)) {
            topAppBar.setTitle("Notifications");
            loadFragment(NotificationsFragment.newInstance());
            return;
        }

        if (intent.getBooleanExtra(EXTRA_OPEN_CHAT, false)) {
            handleChatNavigation();
            return;
        }

        handleDeepLink(intent);
    }


    private void handleHomeNavigation() {
        if ("ADMIN".equals(currentUserRole)) {
            topAppBar.setTitle("Ride History");
            loadFragment(AdminRideHistoryFragment.newInstance());
        } else if ("DRIVER".equals(currentUserRole)) {
            topAppBar.setTitle("Driver Dashboard");
            loadFragment(DriverDashboardFragment.newInstance());
        } else {
            topAppBar.setTitle("Home");
            loadFragment(HomeFragment.newInstance(currentUserRole));
        }
    }

    public void handleChatNavigation() {
        topAppBar.setTitle("Chat");
        if ("ADMIN".equals(currentUserRole)) {
            loadFragment(new AdminChatListFragment());
        } else {
            loadFragment(ChatFragment.forAdmin());
        }
    }

    private void handleRideNavigation() {
        topAppBar.setTitle("Ride History");
        if ("PASSENGER".equals(currentUserRole)) {
            loadFragment(new PassengerRideHistoryFragment());
        } else if ("DRIVER".equals(currentUserRole)) {
            loadFragment(DriverHistoryFragment.newInstance());
        } else if ("ADMIN".equals(currentUserRole)) {
            loadFragment(AdminRideHistoryFragment.newInstance());
        }
    }


    private void handleDeepLink(Intent intent) {
        if (intent == null || intent.getData() == null) return;

        Uri    data  = intent.getData();
        String path  = data.getPath();
        String token = data.getQueryParameter("token");

        if (path == null) return;

        if (path.startsWith("/reset-password")) {
            String mode = data.getQueryParameter("mode");
            if (token != null) {
                loadFragment(ResetPasswordFragment.newInstance(token, mode != null ? mode : "RESET"));
                topAppBar.setTitle("Set Password");
            }
        } else if (path.contains("/verify")) {
            handleEmailVerification(token);
        } else if (path.contains("/track")) {
            String trackingToken = data.getQueryParameter("token");
            if (trackingToken != null) {
                loadRideTrackingFragmentWithToken(trackingToken);
            }
        }
    }

    private void handleEmailVerification(String token) {
        if (token == null || token.isEmpty()) {
            showVerificationResult("invalid");
            return;
        }

        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        apiService.verifyEmail(token).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.code() == 302 || response.isSuccessful()) {
                    showVerificationResult("success");
                } else {
                    String status = "invalid";
                    if (response.code() == 410) status = "expired";
                    else if (response.code() == 409) status = "used";
                    showVerificationResult(status);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                showVerificationResult("invalid");
            }
        });
    }

    private void showVerificationResult(String status) {
        topAppBar.setTitle("Verification");
        loadFragment(VerificationResultFragment.newInstance(status));
    }

    private void updateMenuVisibility() {
        boolean loggedIn = TokenManager.getToken(this) != null;
        navigationView.getMenu().findItem(R.id.nav_login).setVisible(!loggedIn);
        navigationView.getMenu().findItem(R.id.nav_logout).setVisible(loggedIn);
    }

    private void updateMenuByRole() {
        String role = currentUserRole;

        navigationView.getMenu().findItem(R.id.nav_ride).setVisible(false);
        navigationView.getMenu().findItem(R.id.nav_requests).setVisible(false);
        navigationView.getMenu().findItem(R.id.nav_chat).setVisible(false);
        navigationView.getMenu().findItem(R.id.nav_profile).setVisible(false);
        navigationView.getMenu().findItem(R.id.nav_notifications).setVisible(false);
        navigationView.getMenu().findItem(R.id.nav_register_driver).setVisible(false);
        navigationView.getMenu().findItem(R.id.nav_admin_users).setVisible(false);
        navigationView.getMenu().findItem(R.id.nav_report).setVisible(false);
        navigationView.getMenu().findItem(R.id.nav_track_ride).setVisible(false);
        navigationView.getMenu().findItem(R.id.nav_driver_dashboard).setVisible(false);

        if ("PASSENGER".equals(role)) {
            navigationView.getMenu().findItem(R.id.nav_chat).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_ride).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_profile).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_notifications).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_report).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_track_ride).setVisible(true);
        } else if ("DRIVER".equals(role)) {
            navigationView.getMenu().findItem(R.id.nav_driver_dashboard).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_chat).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_ride).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_profile).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_notifications).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_report).setVisible(true);
        } else if ("ADMIN".equals(role)) {
            navigationView.getMenu().findItem(R.id.nav_requests).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_chat).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_profile).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_notifications).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_register_driver).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_admin_users).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_report).setVisible(true);
        }
    }

    private void loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .commit();
        }
    }

    public void loadRegisterFragment() {
        topAppBar.setTitle("Register");
        loadFragment(new RegisterFragment());
    }

    public void loadLoginFragment() {
        topAppBar.setTitle("Login");
        loadFragment(new LoginFragment());
    }

    public void loadForgotPasswordFragment() {
        topAppBar.setTitle("Forgot Password");
        loadFragment(new ForgotPasswordFragment());
    }

    public void loadRideTrackingFragment() {
        topAppBar.setTitle("Ride Tracking");
        loadFragment(RideTrackingFragment.newInstance());
    }

    public void loadRideTrackingFragmentWithToken(String trackingToken) {
        topAppBar.setTitle("Ride Tracking");
        loadFragment(RideTrackingFragment.newInstanceWithToken(trackingToken));
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }
}