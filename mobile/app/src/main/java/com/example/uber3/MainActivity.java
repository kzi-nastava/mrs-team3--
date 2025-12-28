package com.example.uber3;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private MaterialToolbar topAppBar;

    private String currentUserRole = "DRIVER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawerLayout);
        topAppBar = findViewById(R.id.topAppBar);
        NavigationView navigationView = findViewById(R.id.navigationView);

        topAppBar.setNavigationIcon(R.drawable.ic_menu);
        topAppBar.setNavigationOnClickListener(v -> drawerLayout.open());

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                topAppBar.setTitle("Home");

            } else if (id == R.id.nav_chat) {
                topAppBar.setTitle("Login");
                loadFragment(new LoginFragment());

            } else if (id == R.id.nav_ride) {
                topAppBar.setTitle("Ride History");
                loadFragment(DriverHistoryFragment.newInstance());

            } else if (id == R.id.nav_profile) {
                topAppBar.setTitle("Profile");
                loadFragment(ProfileFragment.newInstance(currentUserRole));

            }
            drawerLayout.close();
            return true;
        });

        if (savedInstanceState == null) {
            topAppBar.setTitle("Profile");
            loadFragment(ProfileFragment.newInstance(currentUserRole));
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

    public void loadDefaultFragment() {
        topAppBar.setTitle("Profile");
        loadFragment(ProfileFragment.newInstance(currentUserRole));

        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        topAppBar.setNavigationIcon(R.drawable.ic_menu);
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

    public String getCurrentUserRole() {
        return currentUserRole;
    }

    public void setCurrentUserRole(String role) {
        this.currentUserRole = role;
    }
}
