package com.blocksilent.app.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.blocksilent.app.R;
import com.blocksilent.app.utils.LocationHelper;
import com.blocksilent.app.utils.PermissionManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Locale;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String EXTRA_LATITUDE = "extra_latitude";
    public static final String EXTRA_LONGITUDE = "extra_longitude";
    public static final String EXTRA_RADIUS = "extra_radius";

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng selectedLatLng;
    private float selectedRadius = 15.0f;

    private TextView tvCoords;
    private Button btnConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        tvCoords = findViewById(R.id.tvMapCoords);
        btnConfirm = findViewById(R.id.btnConfirmLocation);

        if (getIntent().hasExtra(EXTRA_RADIUS)) {
            selectedRadius = getIntent().getFloatExtra(EXTRA_RADIUS, 15.0f);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            if (mapFragment != null) {
                mapFragment.getMapAsync(this);
            }
        } catch (Exception e) {
            Log.e("MapActivity", "Error loading map fragment", e);
            Toast.makeText(this, "Map component initializing...", Toast.LENGTH_SHORT).show();
        }

        btnConfirm.setOnClickListener(v -> {
            if (selectedLatLng != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_LATITUDE, selectedLatLng.latitude);
                resultIntent.putExtra(EXTRA_LONGITUDE, selectedLatLng.longitude);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Please tap on the map to pick a location.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (googleMap == null) return;
        mMap = googleMap;

        try {
            if (PermissionManager.hasLocationPermission(this)) {
                mMap.setMyLocationEnabled(true);
                LocationHelper.fetchAccurateCurrentLocation(this, new LocationHelper.LocationResultCallback() {
                    @Override
                    public void onLocationFetched(android.location.Location location) {
                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed() || location == null) return;
                            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            updateSelectedLocation(currentLatLng);
                            if (mMap != null) {
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17.5f));
                            }
                        });
                    }

                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> fallbackCampusLocation());
                    }
                });
            } else {
                fallbackCampusLocation();
            }
        } catch (SecurityException se) {
            Log.w("MapActivity", "SecurityException enabling my location", se);
            fallbackCampusLocation();
        } catch (Exception e) {
            Log.e("MapActivity", "Exception on map ready", e);
            fallbackCampusLocation();
        }

        mMap.setOnMapClickListener(this::updateSelectedLocation);
    }

    private void fallbackCampusLocation() {
        if (isFinishing() || isDestroyed()) return;
        LatLng defaultLatLng = new LatLng(12.9716, 77.5946);
        updateSelectedLocation(defaultLatLng);
        if (mMap != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 16f));
        }
    }

    private void updateSelectedLocation(LatLng latLng) {
        if (latLng == null || isFinishing() || isDestroyed()) return;
        selectedLatLng = latLng;
        if (mMap != null) {
            try {
                mMap.clear();

                mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title("Target Geofence Center"));

                int primaryColor = androidx.core.content.ContextCompat.getColor(this, R.color.primary);
                int fillColor = (primaryColor & 0x00FFFFFF) | 0x33000000;

                mMap.addCircle(new CircleOptions()
                        .center(latLng)
                        .radius(selectedRadius)
                        .strokeColor(primaryColor)
                        .fillColor(fillColor)
                        .strokeWidth(3f));
            } catch (Exception e) {
                Log.e("MapActivity", "Error drawing on map", e);
            }
        }

        if (tvCoords != null) {
            tvCoords.setText(String.format(Locale.US, "Lat: %.6f, Lng: %.6f (Radius: %.0fm)", latLng.latitude, latLng.longitude, selectedRadius));
        }
    }
}
