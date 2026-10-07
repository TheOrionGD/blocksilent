package com.blocksilent.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.blocksilent.app.R;
import com.blocksilent.app.database.AppDatabase;
import com.blocksilent.app.database.entities.SettingsEntity;
import com.google.android.material.card.MaterialCardView;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY_MS = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        MaterialCardView cardLogo = findViewById(R.id.cardSplashLogo);
        TextView tvAppName = findViewById(R.id.tvSplashAppName);
        TextView tvTagline = findViewById(R.id.tvSplashTagline);

        if (cardLogo != null) {
            Animation scaleAnim = AnimationUtils.loadAnimation(this, R.anim.scale_up);
            cardLogo.startAnimation(scaleAnim);
        }

        if (tvAppName != null) {
            Animation fadeInAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in);
            tvAppName.startAnimation(fadeInAnim);
        }

        new Handler(Looper.getMainLooper()).postDelayed(this::checkNavigationFlow, SPLASH_DELAY_MS);
    }

    private void checkNavigationFlow() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase database = AppDatabase.getInstance(getApplicationContext());
            SettingsEntity settings = database.settingsDao().getSettingsSync();

            boolean isFirstTime = (settings == null || settings.isFirstTimeLaunch());

            runOnUiThread(() -> {
                Intent intent;
                if (isFirstTime) {
                    intent = new Intent(SplashActivity.this, OnboardingActivity.class);
                } else {
                    intent = new Intent(SplashActivity.this, MainActivity.class);
                }
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
            });
        });
    }
}
