package com.example.myapplication;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class NotificationPreferencesActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "NotificationPrefs";
    public static final String KEY_SOS_ALERTS = "key_sos_alerts";
    public static final String KEY_OFFLINE_ALERTS = "key_offline_alerts";
    public static final String KEY_LOW_BATTERY_ALERTS = "key_low_battery_alerts";
    public static final String KEY_LOCATION_UPDATES = "key_location_updates";
    public static final String KEY_SMS_ALERTS = "key_sms_alerts";
    public static final String KEY_SOUND_VIBRATION = "key_sound_vibration";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_preferences);

        // 1. Register Android Notification Channels (Oreo+)
        createNotificationChannels();

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Bind Back Button
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Bind Switches
        SwitchCompat switchSos = findViewById(R.id.switch_sos_alerts);
        SwitchCompat switchOffline = findViewById(R.id.switch_offline_alerts);
        SwitchCompat switchLowBattery = findViewById(R.id.switch_low_battery_alerts);
        SwitchCompat switchLocation = findViewById(R.id.switch_location_updates);
        SwitchCompat switchSms = findViewById(R.id.switch_sms_alerts);
        SwitchCompat switchSoundVibe = findViewById(R.id.switch_sound_vibration);

        // Load saved state
        if (switchSos != null) switchSos.setChecked(prefs.getBoolean(KEY_SOS_ALERTS, true));
        if (switchOffline != null) switchOffline.setChecked(prefs.getBoolean(KEY_OFFLINE_ALERTS, true));
        if (switchLowBattery != null) switchLowBattery.setChecked(prefs.getBoolean(KEY_LOW_BATTERY_ALERTS, true));
        if (switchLocation != null) switchLocation.setChecked(prefs.getBoolean(KEY_LOCATION_UPDATES, false));
        if (switchSms != null) switchSms.setChecked(prefs.getBoolean(KEY_SMS_ALERTS, false));
        if (switchSoundVibe != null) switchSoundVibe.setChecked(prefs.getBoolean(KEY_SOUND_VIBRATION, true));

        // Save switch changes
        if (switchSos != null) {
            switchSos.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean(KEY_SOS_ALERTS, isChecked).apply();
                Toast.makeText(this, "Emergency SOS Alerts " + (isChecked ? "Enabled" : "Disabled"), Toast.LENGTH_SHORT).show();
            });
        }

        if (switchOffline != null) {
            switchOffline.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean(KEY_OFFLINE_ALERTS, isChecked).apply();
                Toast.makeText(this, "Device Offline Alerts " + (isChecked ? "Enabled" : "Disabled"), Toast.LENGTH_SHORT).show();
            });
        }

        if (switchLowBattery != null) {
            switchLowBattery.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean(KEY_LOW_BATTERY_ALERTS, isChecked).apply();
                Toast.makeText(this, "Low Battery Alerts " + (isChecked ? "Enabled" : "Disabled"), Toast.LENGTH_SHORT).show();
            });
        }

        if (switchLocation != null) {
            switchLocation.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean(KEY_LOCATION_UPDATES, isChecked).apply();
                Toast.makeText(this, "Location Update Notifications " + (isChecked ? "Enabled" : "Disabled"), Toast.LENGTH_SHORT).show();
            });
        }

        if (switchSms != null) {
            switchSms.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean(KEY_SMS_ALERTS, isChecked).apply();
                Toast.makeText(this, "SMS Alerts " + (isChecked ? "Enabled" : "Disabled"), Toast.LENGTH_SHORT).show();
            });
        }

        if (switchSoundVibe != null) {
            switchSoundVibe.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean(KEY_SOUND_VIBRATION, isChecked).apply();
                Toast.makeText(this, "Sound and Vibration " + (isChecked ? "Enabled" : "Disabled"), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                NotificationChannel sosChannel = new NotificationChannel(
                        "CHANNEL_EMERGENCY_SOS",
                        "Emergency SOS Alerts",
                        NotificationManager.IMPORTANCE_HIGH
                );
                sosChannel.setDescription("High priority panic and emergency alerts");
                sosChannel.enableVibration(true);

                NotificationChannel deviceChannel = new NotificationChannel(
                        "CHANNEL_DEVICE_ALERTS",
                        "Device Telemetry Alerts",
                        NotificationManager.IMPORTANCE_DEFAULT
                );
                deviceChannel.setDescription("Device battery and status notifications");

                manager.createNotificationChannel(sosChannel);
                manager.createNotificationChannel(deviceChannel);
            }
        }
    }
}
