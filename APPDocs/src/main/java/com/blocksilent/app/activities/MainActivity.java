package com.blocksilent.app.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.blocksilent.app.R;
import com.blocksilent.app.database.AppDatabase;
import com.blocksilent.app.database.entities.SettingsEntity;
import com.blocksilent.app.fragments.BlocksFragment;
import com.blocksilent.app.fragments.HistoryFragment;
import com.blocksilent.app.fragments.HomeFragment;
import com.blocksilent.app.fragments.SettingsFragment;
import com.blocksilent.app.fragments.TimetableFragment;
import com.blocksilent.app.geofence.GeofenceManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

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
                loadFragment(new HomeFragment(), false);
                return true;
            } else if (itemId == R.id.nav_blocks) {
                loadFragment(new BlocksFragment(), false);
                return true;
            } else if (itemId == R.id.nav_timetable) {
                loadFragment(new TimetableFragment(), false);
                return true;
            } else if (itemId == R.id.nav_history) {
                loadFragment(new HistoryFragment(), false);
                return true;
            } else if (itemId == R.id.nav_settings) {
                loadFragment(new SettingsFragment(), false);
                return true;
            }
            return false;
        });

        // Handle back press to pop child fragments
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        // Default tab: Home
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (com.blocksilent.app.utils.PermissionManager.hasLocationPermission(this)) {
            new GeofenceManager(getApplicationContext()).registerAllEnabledGeofences();
            try {
                Intent serviceIntent = new Intent(this, com.blocksilent.app.services.LocationMonitoringService.class);
                androidx.core.content.ContextCompat.startForegroundService(this, serviceIntent);
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Failed to start LocationMonitoringService", e);
            }
        }
    }

    public void navigateToTab(int navItemId) {
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(navItemId);
        }
    }

    public void loadFragment(Fragment fragment) {
        loadFragment(fragment, true);
    }

    public void loadFragment(Fragment fragment, boolean addToBackStack) {
        if (isFinishing() || isDestroyed()) return;
        var tx = getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.fragmentContainer, fragment);
        if (addToBackStack) {
            tx.addToBackStack(null);
        } else {
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        tx.commitAllowingStateLoss();
    }
}
