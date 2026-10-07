package com.blocksilent.app.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.blocksilent.app.R;
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

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String EXTRA_LATITUDE = "extra_latitude";
    public static final String EXTRA_LONGITUDE = "extra_longitude";
    public static final String EXTRA_RADIUS = "extra_radius";

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng selectedLatLng;
    private float selectedRadius = 50.0f;

    private TextView tvCoords;
    private Button btnConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        tvCoords = findViewById(R.id.tvMapCoords);
        btnConfirm = findViewById(R.id.btnConfirmLocation);

        if (getIntent().hasExtra(EXTRA_RADIUS)) {
            selectedRadius = getIntent().getFloatExtra(EXTRA_RADIUS, 50.0f);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
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
        mMap = googleMap;

        if (PermissionManager.hasLocationPermission(this)) {
            mMap.setMyLocationEnabled(true);
            // Automatically fetch current high-accuracy location upon opening the map
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            updateSelectedLocation(currentLatLng);
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17.5f));
                        } else {
                            fusedLocationClient.getLastLocation().addOnSuccessListener(this, lastLoc -> {
                                if (lastLoc != null) {
                                    LatLng currentLatLng = new LatLng(lastLoc.getLatitude(), lastLoc.getLongitude());
                                    updateSelectedLocation(currentLatLng);
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17.5f));
                                } else {
                                    // Campus default fallback coordinates
                                    LatLng defaultLatLng = new LatLng(12.9716, 77.5946);
                                    updateSelectedLocation(defaultLatLng);
                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 16f));
                                }
                            });
                        }
                    });
        } else {
            LatLng defaultLatLng = new LatLng(12.9716, 77.5946);
            updateSelectedLocation(defaultLatLng);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 16f));
        }

        mMap.setOnMapClickListener(this::updateSelectedLocation);
    }

    private void updateSelectedLocation(LatLng latLng) {
        selectedLatLng = latLng;
        mMap.clear();

        // Add Marker
        mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title("Target Geofence Center"));

        // Draw Geofence Radius Circle
        mMap.addCircle(new CircleOptions()
                .center(latLng)
                .radius(selectedRadius)
                .strokeColor(Color.parseColor("#3F51B5"))
                .fillColor(Color.parseColor("#333F51B5"))
                .strokeWidth(3f));

        tvCoords.setText(String.format("Lat: %.6f, Lng: %.6f (Radius: %.0fm)", latLng.latitude, latLng.longitude, selectedRadius));
    }
}
