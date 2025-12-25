package com.example.uber3;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        NavigationView navigationView = findViewById(R.id.navigationView);

        topAppBar.setNavigationIcon(R.drawable.ic_menu);

        topAppBar.setNavigationOnClickListener(v ->
                drawerLayout.open()
        );

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {

            } else if (id == R.id.nav_chat) {

            } else if (id == R.id.nav_ride) {

            } else if (id == R.id.nav_profile) {

            }

            drawerLayout.close();
            return true;
        });
    }
}
