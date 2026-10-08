package com.blocksilent.app.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.blocksilent.app.R;
import com.blocksilent.app.database.AppDatabase;
import com.blocksilent.app.database.entities.BlockEntity;
import com.blocksilent.app.geofence.GeofenceManager;
import com.blocksilent.app.utils.LocationHelper;
import com.blocksilent.app.utils.PermissionManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;
import java.util.Locale;

public class AddEditBlockActivity extends AppCompatActivity {

    public static final String EXTRA_BLOCK_ID = "extra_block_id";
    private static final int REQUEST_MAP_LOCATION = 201;

    private TextInputEditText etName, etLat, etLng, etCustomRadius;
    private ChipGroup chipGroupRadius;
    private RadioGroup radioGroupSoundMode;
    private RadioButton rbSilent, rbVibrate, rbNormal;
    private SwitchMaterial switchEnable;
    private Button btnUseCurrentLocation, btnSelectOnMap, btnSaveBlock;
    private TextView tvTitle;

    private long existingBlockId = -1;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_block);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        tvTitle = findViewById(R.id.tvAddEditTitle);
        etName = findViewById(R.id.etBlockName);
        etLat = findViewById(R.id.etLatitude);
        etLng = findViewById(R.id.etLongitude);
        etCustomRadius = findViewById(R.id.etCustomRadius);
        chipGroupRadius = findViewById(R.id.chipGroupRadius);
        radioGroupSoundMode = findViewById(R.id.radioGroupSoundMode);
        rbSilent = findViewById(R.id.rbSilent);
        rbVibrate = findViewById(R.id.rbVibrate);
        rbNormal = findViewById(R.id.rbNormal);
        switchEnable = findViewById(R.id.switchEnableAutomation);
        btnUseCurrentLocation = findViewById(R.id.btnUseCurrentLocation);
        btnSelectOnMap = findViewById(R.id.btnSelectOnMap);
        btnSaveBlock = findViewById(R.id.btnSaveBlock);

        if (getIntent().hasExtra(EXTRA_BLOCK_ID)) {
            existingBlockId = getIntent().getLongExtra(EXTRA_BLOCK_ID, -1);
            tvTitle.setText("Edit Block Geofence");
            loadExistingBlockData(existingBlockId);
        } else {
            autoFetchCurrentLocation();
        }

        btnUseCurrentLocation.setOnClickListener(v -> autoFetchCurrentLocation());
        btnSelectOnMap.setOnClickListener(v -> {
            Intent intent = new Intent(AddEditBlockActivity.this, MapActivity.class);
            intent.putExtra(MapActivity.EXTRA_RADIUS, getSelectedRadius());
            startActivityForResult(intent, REQUEST_MAP_LOCATION);
        });

        btnSaveBlock.setOnClickListener(v -> saveBlock());
    }

    private void autoFetchCurrentLocation() {
        if (isFinishing() || isDestroyed()) return;

        if (!PermissionManager.hasLocationPermission(this)) {
            PermissionManager.requestLocationPermission(this);
            return;
        }

        if (!LocationHelper.isGpsEnabled(this)) {
            Toast.makeText(this, "Please enable GPS / Location Services for accurate geofencing.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            return;
        }

        btnUseCurrentLocation.setEnabled(false);
        btnUseCurrentLocation.setText("Acquiring GPS...");

        LocationHelper.fetchAccurateCurrentLocation(this, new LocationHelper.LocationResultCallback() {
            @Override
            public void onLocationFetched(android.location.Location location) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    btnUseCurrentLocation.setEnabled(true);
                    btnUseCurrentLocation.setText("Current Location");
                    if (location != null) {
                        etLat.setText(String.format(Locale.US, "%.6f", location.getLatitude()));
                        etLng.setText(String.format(Locale.US, "%.6f", location.getLongitude()));
                        Toast.makeText(AddEditBlockActivity.this, "Current GPS location acquired!", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    btnUseCurrentLocation.setEnabled(true);
                    btnUseCurrentLocation.setText("Current Location");
                    Toast.makeText(AddEditBlockActivity.this, message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (PermissionManager.hasLocationPermission(this) && existingBlockId <= 0) {
            autoFetchCurrentLocation();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MAP_LOCATION && resultCode == RESULT_OK && data != null) {
            double lat = data.getDoubleExtra(MapActivity.EXTRA_LATITUDE, 0.0);
            double lng = data.getDoubleExtra(MapActivity.EXTRA_LONGITUDE, 0.0);
            etLat.setText(String.format(Locale.US, "%.6f", lat));
            etLng.setText(String.format(Locale.US, "%.6f", lng));
        }
    }

    private float getSelectedRadius() {
        String customStr = etCustomRadius.getText() != null ? etCustomRadius.getText().toString().trim() : "";
        if (!TextUtils.isEmpty(customStr)) {
            try {
                return Float.parseFloat(customStr);
            } catch (NumberFormatException ignored) {}
        }

        int checkedId = chipGroupRadius.getCheckedChipId();
        if (checkedId == R.id.chip25m) return 25.0f;
        if (checkedId == R.id.chip75m) return 75.0f;
        if (checkedId == R.id.chip100m) return 100.0f;
        return 50.0f; // Default
    }

    private void loadExistingBlockData(long id) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase database = AppDatabase.getInstance(getApplicationContext());
            BlockEntity block = database.blockDao().getBlockById(id);
            if (block != null) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    etName.setText(block.getName());
                    etLat.setText(String.format(Locale.US, "%.6f", block.getLatitude()));
                    etLng.setText(String.format(Locale.US, "%.6f", block.getLongitude()));
                    switchEnable.setChecked(block.isEnabled());

                    if ("SILENT".equalsIgnoreCase(block.getSoundMode())) {
                        rbSilent.setChecked(true);
                    } else if ("VIBRATE".equalsIgnoreCase(block.getSoundMode())) {
                        rbVibrate.setChecked(true);
                    } else {
                        rbNormal.setChecked(true);
                    }

                    float r = block.getRadius();
                    if (r == 25.0f) chipGroupRadius.check(R.id.chip25m);
                    else if (r == 50.0f) chipGroupRadius.check(R.id.chip50m);
                    else if (r == 75.0f) chipGroupRadius.check(R.id.chip75m);
                    else if (r == 100.0f) chipGroupRadius.check(R.id.chip100m);
                    else etCustomRadius.setText(String.valueOf((int) r));
                });
            }
        });
    }

    private void saveBlock() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String latStr = etLat.getText() != null ? etLat.getText().toString().trim() : "";
        String lngStr = etLng.getText() != null ? etLng.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) {
            etName.setError("Name cannot be empty.");
            return;
        }

        double latitude, longitude;
        try {
            latitude = Double.parseDouble(latStr);
            if (latitude < -90 || latitude > 90) {
                etLat.setError("Latitude must be valid (-90 to 90).");
                return;
            }
        } catch (Exception e) {
            etLat.setError("Latitude must be valid.");
            return;
        }

        try {
            longitude = Double.parseDouble(lngStr);
            if (longitude < -180 || longitude > 180) {
                etLng.setError("Longitude must be valid (-180 to 180).");
                return;
            }
        } catch (Exception e) {
            etLng.setError("Longitude must be valid.");
            return;
        }

        float radius = getSelectedRadius();
        if (radius <= 0) {
            Toast.makeText(this, "Radius must be greater than zero.", Toast.LENGTH_SHORT).show();
            return;
        }

        String soundMode = "SILENT";
        int checkedSoundId = radioGroupSoundMode.getCheckedRadioButtonId();
        if (checkedSoundId == R.id.rbVibrate) {
            soundMode = "VIBRATE";
        } else if (checkedSoundId == R.id.rbNormal) {
            soundMode = "NORMAL";
        }

        boolean enabled = switchEnable.isChecked();
        final String mode = soundMode;

        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase database = AppDatabase.getInstance(getApplicationContext());
            if (existingBlockId > 0) {
                BlockEntity existing = database.blockDao().getBlockById(existingBlockId);
                if (existing != null) {
                    existing.setName(name);
                    existing.setLatitude(latitude);
                    existing.setLongitude(longitude);
                    existing.setRadius(radius);
                    existing.setSoundMode(mode);
                    existing.setEnabled(enabled);
                    database.blockDao().update(existing);
                }
            } else {
                BlockEntity newBlock = new BlockEntity(name, latitude, longitude, radius, mode, enabled, 5);
                database.blockDao().insert(newBlock);
            }

            // Re-register geofences immediately
            List<BlockEntity> enabledBlocks = database.blockDao().getEnabledBlocksSync();
            GeofenceManager geofenceManager = new GeofenceManager(getApplicationContext());
            geofenceManager.registerGeofences(enabledBlocks);

            com.blocksilent.app.context.ContextEngine.getInstance(getApplicationContext()).evaluateAndApplyContext("BLOCK_SAVED");

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(this, "Block saved successfully!", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }
}
