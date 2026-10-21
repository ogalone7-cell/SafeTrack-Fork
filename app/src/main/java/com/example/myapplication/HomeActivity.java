package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private boolean isMapExpanded = false;

    private MapView osmMapView;
    private Marker deviceMarker;

    // OpenStreetMap Community Tile Source (Free, Open-Source, HTTPS, No API key required)
    public static final OnlineTileSourceBase OPENSTREETMAP_TILES = new XYTileSource(
            "OSM_France",
            0, 19, 256, ".png",
            new String[]{
                    "https://a.tile.openstreetmap.fr/osmfr/",
                    "https://b.tile.openstreetmap.fr/osmfr/",
                    "https://c.tile.openstreetmap.fr/osmfr/"
            },
            "© OpenStreetMap contributors, © OSM France"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Configure osmdroid before inflating layout (OSM Tile Usage Policy compliant)
        File cacheDir = new File(getCacheDir(), "osmdroid");
        Configuration.getInstance().setOsmdroidBasePath(cacheDir);
        Configuration.getInstance().setOsmdroidTileCache(cacheDir);
        Configuration.getInstance().setUserAgentValue("SafeTrack/1.0 (" + getPackageName() + ")");
        Configuration.getInstance().load(getApplicationContext(), getSharedPreferences("osmdroid", MODE_PRIVATE));

        setContentView(R.layout.acitivity_home);

        // Request runtime permissions (GPS/Location, Notifications)
        checkAndRequestPermissions();

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        // Get parent name passed from LoginActivity or SharedPreferences
        String userName = getIntent().getStringExtra("USER_NAME");
        if (userName == null || userName.isEmpty()) {
            userName = prefs.getString("PARENT_NAME", "Guardian");
        }

        TextView tvGreeting = findViewById(R.id.tv_greeting_name);
        if (tvGreeting != null) {
            tvGreeting.setText(getString(R.string.hello_name, userName));
        }

        // Get registered child details
        String childName = getIntent().getStringExtra("CHILD_NAME");
        if (childName == null || childName.isEmpty()) {
            childName = prefs.getString("CHILD_NAME", "");
        }

        String childRelation = getIntent().getStringExtra("CHILD_RELATION");
        if (childRelation == null || childRelation.isEmpty()) {
            childRelation = prefs.getString("CHILD_RELATION", "");
        }

        String childDevice = getIntent().getStringExtra("CHILD_DEVICE");
        if (childDevice == null || childDevice.isEmpty()) {
            childDevice = prefs.getString("CHILD_DEVICE", "");
        }

        TextView tvChildName = findViewById(R.id.tv_person_name);
        TextView tvChildDetails = findViewById(R.id.tv_person_details);
        TextView tvChildAvatarInitial = findViewById(R.id.tv_child_avatar_initial);

        if (!childName.isEmpty()) {
            tvChildName.setText(childName);
            if (tvChildAvatarInitial != null) {
                tvChildAvatarInitial.setText(String.valueOf(childName.charAt(0)).toUpperCase());
            }
        } else {
            tvChildName.setText("Child");
            if (tvChildAvatarInitial != null) {
                tvChildAvatarInitial.setText("C");
            }
        }

        StringBuilder details = new StringBuilder();
        if (!childRelation.isEmpty()) {
            details.append(childRelation);
        } else {
            details.append("Child");
        }

        if (!childDevice.isEmpty()) {
            details.append(" · SafeTrack Band #").append(childDevice);
        } else {
            details.append(" · SafeTrack Band");
        }
        tvChildDetails.setText(details.toString());

        // Initialize OpenStreetMap (osmdroid) MapView
        setupOpenStreetMap();

        // Full map toggle functionality
        TextView tvFullMap = findViewById(R.id.tv_full_map);
        MaterialCardView cardMap = findViewById(R.id.card_map);

        if (tvFullMap != null && cardMap != null) {
            tvFullMap.setOnClickListener(v -> {
                ViewGroup.LayoutParams params = cardMap.getLayoutParams();
                int density = (int) getResources().getDisplayMetrics().density;

                if (!isMapExpanded) {
                    params.height = 450 * density;
                    tvFullMap.setText("Close map");
                    isMapExpanded = true;
                } else {
                    params.height = 220 * density;
                    tvFullMap.setText(getString(R.string.full_map));
                    isMapExpanded = false;
                }
                cardMap.setLayoutParams(params);
            });
        }

        // Status badge / Map pin signal receiver simulation trigger
        MaterialCardView statusBadge = findViewById(R.id.status_badge);
        ImageView mapPin = findViewById(R.id.map_pin);

        View.OnClickListener signalTriggerListener = v -> {
            SignalReceiverManager.ThreatLevel level = SignalReceiverManager.processIncomingSignal(
                    HomeActivity.this,
                    19.0760, 72.8777,
                    "Near MG Road Metro Station, Bandra West"
            );

            if (level == SignalReceiverManager.ThreatLevel.NORMAL_UPDATE) {
                Toast.makeText(HomeActivity.this, "Signal Received (Background update / Panic tap detected)", Toast.LENGTH_SHORT).show();
            }
        };

        if (statusBadge != null) statusBadge.setOnClickListener(signalTriggerListener);
        if (mapPin != null) mapPin.setOnClickListener(signalTriggerListener);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_alerts) {
                startActivity(new Intent(HomeActivity.this, AlertHistoryActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    private void setupOpenStreetMap() {
        osmMapView = findViewById(R.id.osmMapView);
        if (osmMapView != null) {
            // Use OpenStreetMap France Community tile source (Free, Open-Source, HTTPS, No API key)
            osmMapView.setTileSource(OPENSTREETMAP_TILES);
            osmMapView.setMultiTouchControls(true);

            // Default position: Last known / tracked coordinates
            GeoPoint startPoint = new GeoPoint(19.0760, 72.8777);
            osmMapView.getController().setZoom(16.5);
            osmMapView.getController().setCenter(startPoint);

            // Create Tracked Device Marker
            deviceMarker = new Marker(osmMapView);
            deviceMarker.setPosition(startPoint);
            deviceMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            deviceMarker.setTitle("Tracked Device Location");
            deviceMarker.setSnippet("MG Road, Bandra West (GPS ±5 m)");
            osmMapView.getOverlays().add(deviceMarker);
            osmMapView.invalidate();
        }
    }

    public void updateDeviceLocationOnMap(double lat, double lng, String locationName) {
        if (osmMapView != null && deviceMarker != null) {
            GeoPoint newPoint = new GeoPoint(lat, lng);
            deviceMarker.setPosition(newPoint);
            deviceMarker.setSnippet(locationName);
            osmMapView.getController().animateTo(newPoint);
            osmMapView.invalidate();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (osmMapView != null) {
            osmMapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (osmMapView != null) {
            osmMapView.onPause();
        }
    }

    private void checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                Toast.makeText(this, "Permissions granted! SafeTrack active.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
