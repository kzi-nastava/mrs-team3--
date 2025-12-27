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
                loadFragment(HomeFragment.newInstance(currentUserRole));

            } else if (id == R.id.nav_chat) {
                topAppBar.setTitle("Chat");

            } else if (id == R.id.nav_ride) {
                topAppBar.setTitle("Ride");

            } else if (id == R.id.nav_profile) {
                topAppBar.setTitle("Profile");
                loadFragment(ProfileFragment.newInstance(currentUserRole));
            }

            drawerLayout.close();
            return true;
        });

        // DEFAULT SCREEN
        if (savedInstanceState == null) {
            topAppBar.setTitle("Home");
            loadFragment(HomeFragment.newInstance(currentUserRole));
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
