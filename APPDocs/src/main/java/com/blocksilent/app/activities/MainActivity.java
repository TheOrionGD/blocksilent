package com.blocksilent.app.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.blocksilent.app.R;
import com.blocksilent.app.database.AppDatabase;
import com.blocksilent.app.database.entities.BlockEntity;
import com.blocksilent.app.database.entities.SettingsEntity;
import com.blocksilent.app.fragments.BlocksFragment;
import com.blocksilent.app.fragments.HistoryFragment;
import com.blocksilent.app.fragments.HomeFragment;
import com.blocksilent.app.fragments.SettingsFragment;
import com.blocksilent.app.fragments.TimetableFragment;
import com.blocksilent.app.geofence.GeofenceManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if first-time onboarding is needed
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase database = AppDatabase.getInstance(getApplicationContext());
            SettingsEntity settings = database.settingsDao().getSettingsSync();
            if (settings == null || settings.isFirstTimeLaunch()) {
                runOnUiThread(() -> {
                    Intent intent = new Intent(MainActivity.this, OnboardingActivity.class);
                    startActivity(intent);
                    finish();
                });
                return;
            }

            // Register Geofences for all active blocks
            GeofenceManager geofenceManager = new GeofenceManager(getApplicationContext());
            geofenceManager.registerAllEnabledGeofences();
        });

        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottomNavigation);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                loadFragment(new HomeFragment());
                return true;
            } else if (itemId == R.id.nav_blocks) {
                loadFragment(new BlocksFragment());
                return true;
            } else if (itemId == R.id.nav_timetable) {
                loadFragment(new TimetableFragment());
                return true;
            } else if (itemId == R.id.nav_history) {
                loadFragment(new HistoryFragment());
                return true;
            } else if (itemId == R.id.nav_settings) {
                loadFragment(new SettingsFragment());
                return true;
            }
            return false;
        });

        // Default tab: Home
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (com.blocksilent.app.utils.PermissionManager.hasLocationPermission(this)) {
            new GeofenceManager(getApplicationContext()).registerAllEnabledGeofences();
        }
    }

    public void navigateToTab(int navItemId) {
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(navItemId);
        }
    }

    public void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
