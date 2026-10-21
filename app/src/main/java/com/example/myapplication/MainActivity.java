package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Check persistent authentication session after splash screen
        new Handler(Looper.getMainLooper()).postDelayed(this::checkAuthAndRoute, 1500);
    }

    private void checkAuthAndRoute() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        if (currentUser == null) {
            // Strict Security Policy: Always route unauthenticated users to LoginActivity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // User is authenticated with Firebase -> Read Cloud Firestore Profile Document
        String uid = currentUser.getUid();
        mFirestore.collection("users").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists() && isProfileComplete(snapshot)) {
                        String parentName = snapshot.getString("parentName");
                        String parentPhone = snapshot.getString("parentPhone");
                        String childName = snapshot.getString("childName");
                        String childRelation = snapshot.getString("parentRelation");
                        String childDevice = snapshot.getString("childDevice");

                        prefs.edit().putString("PARENT_NAME", parentName).apply();
                        if (parentPhone != null) prefs.edit().putString("PARENT_PHONE", parentPhone).apply();
                        if (childName != null) prefs.edit().putString("CHILD_NAME", childName).apply();
                        if (childRelation != null) prefs.edit().putString("CHILD_RELATION", childRelation).apply();
                        if (childDevice != null) prefs.edit().putString("CHILD_DEVICE", childDevice).apply();

                        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                        intent.putExtra("USER_NAME", parentName);
                        intent.putExtra("CHILD_NAME", childName);
                        intent.putExtra("CHILD_RELATION", childRelation);
                        intent.putExtra("CHILD_DEVICE", childDevice);
                        startActivity(intent);
                    } else {
                        // Profile missing or incomplete -> Route to Profile Completion Onboarding
                        Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
                        intent.putExtra("PREFILL_NAME", currentUser.getDisplayName());
                        intent.putExtra("PREFILL_EMAIL", currentUser.getEmail());
                        startActivity(intent);
                    }
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Network or Firestore Read Error for authenticated Firebase user:
                    // Preserve authenticated session and use cached profile if available
                    String savedParentName = prefs.getString("PARENT_NAME", "");
                    if (!savedParentName.isEmpty()) {
                        Toast.makeText(MainActivity.this, "Offline Session Loaded (Network Error)", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(MainActivity.this, "Network Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        startActivity(intent);
                    }
                    finish();
                });
    }

    private boolean isProfileComplete(DocumentSnapshot snapshot) {
        String parentName = snapshot.getString("parentName");
        String parentPhone = snapshot.getString("parentPhone");
        String childName = snapshot.getString("childName");

        return parentName != null && !parentName.trim().isEmpty()
                && parentPhone != null && !parentPhone.trim().isEmpty()
                && childName != null && !childName.trim().isEmpty();
    }
}
