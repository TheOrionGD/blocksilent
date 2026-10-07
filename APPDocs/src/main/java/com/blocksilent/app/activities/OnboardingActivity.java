package com.blocksilent.app.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.blocksilent.app.R;
import com.blocksilent.app.database.AppDatabase;
import com.blocksilent.app.database.entities.SettingsEntity;
import com.blocksilent.app.geofence.GeofenceManager;
import com.blocksilent.app.utils.BatteryOptimizationHelper;
import com.blocksilent.app.utils.PermissionManager;

public class OnboardingActivity extends AppCompatActivity {

    private static final int TYPE_INTRO = 0;
    private static final int TYPE_PERMISSIONS = 1;

    private ViewPager2 viewPager;
    private Button btnSkip, btnNext;
    private OnboardingAdapter adapter;

    private static class OnboardingSlide {
        int iconRes;
        int titleRes;
        int descRes;

        OnboardingSlide(int iconRes, int titleRes, int descRes) {
            this.iconRes = iconRes;
            this.titleRes = titleRes;
            this.descRes = descRes;
        }
    }

    private final OnboardingSlide[] introSlides = new OnboardingSlide[]{
            new OnboardingSlide(R.drawable.ic_home, R.string.onboarding_title_1, R.string.onboarding_desc_1),
            new OnboardingSlide(R.drawable.ic_location, R.string.onboarding_title_2, R.string.onboarding_desc_2),
            new OnboardingSlide(R.drawable.ic_timetable, R.string.onboarding_title_4, R.string.onboarding_desc_4),
            new OnboardingSlide(R.drawable.ic_history, R.string.onboarding_title_5, R.string.onboarding_desc_5)
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.viewPagerOnboarding);
        btnSkip = findViewById(R.id.btnSkip);
        btnNext = findViewById(R.id.btnNext);

        adapter = new OnboardingAdapter();
        viewPager.setAdapter(adapter);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == getTotalSlideCount() - 1) {
                    btnNext.setText("Get Started");
                    btnSkip.setVisibility(View.GONE);
                } else {
                    btnNext.setText(R.string.next);
                    btnSkip.setVisibility(View.VISIBLE);
                }
            }
        });

        btnSkip.setOnClickListener(v -> finishOnboarding());
        btnNext.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() < getTotalSlideCount() - 1) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            } else {
                finishOnboarding();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private int getTotalSlideCount() {
        return introSlides.length + 1; // 4 intro slides + 1 permissions page
    }

    private void finishOnboarding() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase database = AppDatabase.getInstance(getApplicationContext());
            SettingsEntity settings = database.settingsDao().getSettingsSync();
            if (settings == null) {
                settings = new SettingsEntity(true, true, "NORMAL", 0, false);
            } else {
                settings.setFirstTimeLaunch(false);
            }
            database.settingsDao().insertOrUpdate(settings);

            // Register geofences
            GeofenceManager geofenceManager = new GeofenceManager(getApplicationContext());
            geofenceManager.registerAllEnabledGeofences();
        });

        Intent intent = new Intent(OnboardingActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        if (requestCode == PermissionManager.REQUEST_LOCATION_PERMISSION) {
            if (PermissionManager.hasLocationPermission(this)) {
                Toast.makeText(this, "Foreground Location Granted! Now enable Background Location for screen-locked automation.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private class OnboardingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public int getItemViewType(int position) {
            return (position < introSlides.length) ? TYPE_INTRO : TYPE_PERMISSIONS;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_INTRO) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.onboarding_slide, parent, false);
                return new IntroSlideViewHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.onboarding_slide_permissions, parent, false);
                return new PermissionsSlideViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof IntroSlideViewHolder) {
                OnboardingSlide slide = introSlides[position];
                IntroSlideViewHolder vh = (IntroSlideViewHolder) holder;
                vh.ivIcon.setImageResource(slide.iconRes);
                vh.tvTitle.setText(slide.titleRes);
                vh.tvDesc.setText(slide.descRes);
            } else if (holder instanceof PermissionsSlideViewHolder) {
                PermissionsSlideViewHolder vh = (PermissionsSlideViewHolder) holder;
                bindPermissionsView(vh);
            }
        }

        private void bindPermissionsView(PermissionsSlideViewHolder vh) {
            boolean hasLoc = PermissionManager.hasLocationPermission(OnboardingActivity.this);
            boolean hasBgLoc = PermissionManager.hasBackgroundLocationPermission(OnboardingActivity.this);
            boolean hasNotif = PermissionManager.hasNotificationPermission(OnboardingActivity.this);
            boolean hasDnd = PermissionManager.hasDndPermission(OnboardingActivity.this);
            boolean hasBattery = BatteryOptimizationHelper.isBatteryOptimizationIgnored(OnboardingActivity.this);

            updateButtonState(vh.btnLocation, hasLoc, () -> PermissionManager.requestLocationPermission(OnboardingActivity.this));

            updateButtonState(vh.btnBgLocation, hasBgLoc, () -> {
                if (!PermissionManager.hasLocationPermission(OnboardingActivity.this)) {
                    new AlertDialog.Builder(OnboardingActivity.this)
                            .setTitle("Grant Location First")
                            .setMessage("Please grant Foreground Location permission before enabling Background Location.")
                            .setPositiveButton("Grant Foreground", (dialog, which) -> PermissionManager.requestLocationPermission(OnboardingActivity.this))
                            .setNegativeButton("Cancel", null)
                            .show();
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        new AlertDialog.Builder(OnboardingActivity.this)
                                .setTitle("Select 'Allow all the time'")
                                .setMessage("On the next screen, select 'Allow all the time' so BlockSilent can silence your phone even when the screen is locked or app is closed.")
                                .setPositiveButton("Continue", (dialog, which) -> PermissionManager.requestBackgroundLocationPermission(OnboardingActivity.this))
                                .setNegativeButton("Cancel", null)
                                .show();
                    } else {
                        PermissionManager.requestBackgroundLocationPermission(OnboardingActivity.this);
                    }
                }
            });

            updateButtonState(vh.btnNotif, hasNotif, () -> PermissionManager.requestNotificationPermission(OnboardingActivity.this));
            updateButtonState(vh.btnDnd, hasDnd, () -> PermissionManager.openDndSettings(OnboardingActivity.this));
            updateButtonState(vh.btnBattery, hasBattery, () -> BatteryOptimizationHelper.requestIgnoreBatteryOptimization(OnboardingActivity.this));
        }

        private void updateButtonState(Button btn, boolean isGranted, Runnable action) {
            if (btn == null) return;
            if (isGranted) {
                btn.setText("✓ Granted");
                btn.setEnabled(false);
                btn.setTextColor(Color.parseColor("#10B981"));
            } else {
                btn.setText("Grant");
                btn.setEnabled(true);
                btn.setTextColor(Color.parseColor("#3F51B5"));
                btn.setOnClickListener(v -> action.run());
            }
        }

        @Override
        public int getItemCount() {
            return getTotalSlideCount();
        }

        class IntroSlideViewHolder extends RecyclerView.ViewHolder {
            ImageView ivIcon;
            TextView tvTitle, tvDesc;

            IntroSlideViewHolder(@NonNull View itemView) {
                super(itemView);
                ivIcon = itemView.findViewById(R.id.ivOnboardingIcon);
                tvTitle = itemView.findViewById(R.id.tvOnboardingTitle);
                tvDesc = itemView.findViewById(R.id.tvOnboardingDescription);
            }
        }

        class PermissionsSlideViewHolder extends RecyclerView.ViewHolder {
            Button btnLocation, btnBgLocation, btnNotif, btnDnd, btnBattery;

            PermissionsSlideViewHolder(@NonNull View itemView) {
                super(itemView);
                btnLocation = itemView.findViewById(R.id.btnOnboardingGrantLocation);
                btnBgLocation = itemView.findViewById(R.id.btnOnboardingGrantBgLocation);
                btnNotif = itemView.findViewById(R.id.btnOnboardingGrantNotification);
                btnDnd = itemView.findViewById(R.id.btnOnboardingGrantDnd);
                btnBattery = itemView.findViewById(R.id.btnOnboardingGrantBattery);
            }
        }
    }
}
