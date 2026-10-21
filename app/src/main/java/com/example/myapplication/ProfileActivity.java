package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_profile);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        // Display parent user name
        String parentName = prefs.getString("PARENT_NAME", "Guardian");
        TextView tvUserName = findViewById(R.id.tv_user_name);
        if (tvUserName != null) {
            tvUserName.setText(parentName);
        }

        // Display device details
        String childDevice = prefs.getString("CHILD_DEVICE", "");
        TextView tvDeviceName = findViewById(R.id.tv_device_name);
        if (tvDeviceName != null && !childDevice.isEmpty()) {
            tvDeviceName.setText("SafeTrack Band #" + childDevice);
        }

        // Display emergency contact number
        String parentPhone = prefs.getString("PARENT_PHONE", "");
        TextView tvEmergencyContactNumber = findViewById(R.id.tv_emergency_contact_number);
        if (tvEmergencyContactNumber != null && !parentPhone.isEmpty()) {
            tvEmergencyContactNumber.setText(parentPhone);
        }

        // 1. Notification Preferences Click Listener
        View rowNotificationPreferences = findViewById(R.id.row_notification_preferences);
        if (rowNotificationPreferences != null) {
            rowNotificationPreferences.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, NotificationPreferencesActivity.class);
                startActivity(intent);
            });
        }

        // 2. Privacy & Data Click Listener
        View rowPrivacyData = findViewById(R.id.row_privacy_data);
        if (rowPrivacyData != null) {
            rowPrivacyData.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, PrivacyDataActivity.class);
                startActivity(intent);
            });
        }

        // 3. Log Out Button functionality
        TextView btnLogout = findViewById(R.id.btn_logout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    btnLogout.setEnabled(false);
                    Toast.makeText(ProfileActivity.this, "Signing out...", Toast.LENGTH_SHORT).show();

                    // 1. Firebase Sign Out
                    FirebaseAuth.getInstance().signOut();

                    // 2. Google Sign Out (asynchronous with completion handler)
                    try {
                        int resId = getResources().getIdentifier("default_web_client_id", "string", getPackageName());
                        String webClientId = resId != 0 ? getString(resId) : "275477497883-e7aklq13o0nbb84tet299kr1m8vkq4ka.apps.googleusercontent.com";
                        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(webClientId)
                                .requestEmail()
                                .build();

                        GoogleSignIn.getClient(ProfileActivity.this, gso)
                                .signOut()
                                .addOnCompleteListener(task -> completeLogout());
                    } catch (Exception e) {
                        completeLogout();
                    }
                }
            });
        }

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(ProfileActivity.this, HomeActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_alerts) {
                startActivity(new Intent(ProfileActivity.this, AlertHistoryActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_profile) {
                return true;
            }
            return false;
        });
    }

    private void completeLogout() {
        // Clear SharedPreferences
        getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().clear().apply();

        // Navigate to LoginActivity and clear task stack
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
