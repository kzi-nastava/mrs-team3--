package com.example.uber3;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.uber3.network.manager.LogoutHelper;
import com.example.uber3.network.manager.TokenManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private MaterialToolbar topAppBar;
    private NavigationView navigationView;

    private String currentUserRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        currentUserRole = TokenManager.getRole(this);


        drawerLayout = findViewById(R.id.drawerLayout);
        topAppBar = findViewById(R.id.topAppBar);
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
                topAppBar.setTitle("Home");
                loadFragment(HomeFragment.newInstance(currentUserRole));

            } else if (id == R.id.nav_chat) {
                topAppBar.setTitle("Chat");

            } else if (id == R.id.nav_ride) {
                topAppBar.setTitle("Ride History");
                loadFragment(DriverHistoryFragment.newInstance());

            } else if (id == R.id.nav_profile) {
                topAppBar.setTitle("Profile");
                loadFragment(ProfileFragment.newInstance(currentUserRole));

            } else if (id == R.id.nav_requests) {
                topAppBar.setTitle("Change Requests");
                loadFragment(new DriverChangeRequestFragment());
            } else if (id == R.id.nav_login) {
                topAppBar.setTitle("Login");
                loadFragment(new LoginFragment());

            } else if (id == R.id.nav_logout) {
                LogoutHelper.logout(this);
                return true;
            }

            drawerLayout.close();
            return true;
        });

        if (savedInstanceState == null) {

            if (TokenManager.getToken(this) != null) {
                topAppBar.setTitle("Home");
                loadFragment(HomeFragment.newInstance(currentUserRole));
            } else {
                topAppBar.setTitle("Login");
                loadFragment(new LoginFragment());
            }
        }
    }

    private void updateMenuVisibility() {
        boolean loggedIn =
                TokenManager.getToken(this) != null;

        navigationView.getMenu()
                .findItem(R.id.nav_login)
                .setVisible(!loggedIn);

        navigationView.getMenu()
                .findItem(R.id.nav_logout)
                .setVisible(loggedIn);
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

    private void updateMenuByRole() {

        String role = currentUserRole;



        navigationView.getMenu().findItem(R.id.nav_ride).setVisible(false);
        navigationView.getMenu().findItem(R.id.nav_requests).setVisible(false);
        navigationView.getMenu().findItem(R.id.nav_chat).setVisible(false);
        navigationView.getMenu().findItem(R.id.nav_profile).setVisible(false);

        if (role.equals("PASSENGER")) {
            navigationView.getMenu().findItem(R.id.nav_chat).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_ride).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_profile).setVisible(true);

        }

        if (role.equals("DRIVER")) {
            navigationView.getMenu().findItem(R.id.nav_chat).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_ride).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_profile).setVisible(true);

        }

        if (role.equals("ADMIN")) {
            navigationView.getMenu().findItem(R.id.nav_requests).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_chat).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_profile).setVisible(true);

        }
    }

}
