package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class PrivacyDataActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_data);

        // Bind Back Button
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // 1. Open App Permissions Settings
        Button btnOpenSettings = findViewById(R.id.btn_open_app_settings);
        if (btnOpenSettings != null) {
            btnOpenSettings.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(PrivacyDataActivity.this, "Unable to open app settings", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // 2. Sign Out Command Button
        Button btnSignOut = findViewById(R.id.btn_sign_out_data);
        if (btnSignOut != null) {
            btnSignOut.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().clear().apply();

                Toast.makeText(PrivacyDataActivity.this, "Signed out successfully", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(PrivacyDataActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        // 3. Clear Local Cache & Data Button (With Confirmation Dialog)
        Button btnDeleteCache = findViewById(R.id.btn_delete_cache_data);
        if (btnDeleteCache != null) {
            btnDeleteCache.setOnClickListener(v -> new AlertDialog.Builder(PrivacyDataActivity.this)
                    .setTitle("Clear Local Data Cache?")
                    .setMessage("This action will clear local SharedPreferences session settings and cached database records. Your cloud profile on Firebase will remain intact.")
                    .setPositiveButton("Clear Data", (dialog, which) -> {
                        // Clear SharedPreferences
                        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                        prefs.edit().clear().apply();

                        SharedPreferences notificationPrefs = getSharedPreferences(NotificationPreferencesActivity.PREFS_NAME, MODE_PRIVATE);
                        notificationPrefs.edit().clear().apply();

                        Toast.makeText(PrivacyDataActivity.this, "Local cache cleared successfully", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show());
        }
    }
}
