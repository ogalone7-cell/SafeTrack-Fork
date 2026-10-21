package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acitivity_login);

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        // Configure Google Sign In Client
        String webClientId = getWebClientId();
        if (webClientId != null && !webClientId.isEmpty()) {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build();
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        }

        final EditText etName = findViewById(R.id.etName);
        final EditText etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        TextView txtRegister = findViewById(R.id.txtRegister);
        TextView txtForgot = findViewById(R.id.txtForgot);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        if (txtForgot != null) {
            txtForgot.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, ResetPasswordActivity.class);
                startActivity(intent);
            });
        }

        // Pre-fill username if passed from registration or saved
        String registeredUser = getIntent().getStringExtra("REGISTERED_USER");
        if (registeredUser == null || registeredUser.isEmpty()) {
            registeredUser = prefs.getString("PARENT_NAME", "");
        }
        if (registeredUser != null && !registeredUser.isEmpty()) {
            etName.setText(registeredUser);
        }

        // Register Google Sign-In ActivityResultLauncher
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            if (account != null && account.getIdToken() != null) {
                                firebaseAuthWithGoogle(account.getIdToken());
                            }
                        } catch (ApiException e) {
                            Toast.makeText(LoginActivity.this, "Google Sign-In Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });

        // 1. Email / Username Password Login
        btnLogin.setOnClickListener(v -> {
            String inputUsername = etName.getText().toString().trim();
            String password = etPassword.getText().toString();

            boolean isValid = true;

            if (inputUsername.isEmpty()) {
                etName.setError("Username or Email is required");
                etName.requestFocus();
                isValid = false;
            }

            if (password.isEmpty()) {
                etPassword.setError("Password is required");
                if (isValid) {
                    etPassword.requestFocus();
                }
                isValid = false;
            }

            if (!isValid) {
                return;
            }

            btnLogin.setEnabled(false);
            Toast.makeText(LoginActivity.this, "Authenticating with Firebase...", Toast.LENGTH_SHORT).show();

            // Firebase Authentication Sign In
            mAuth.signInWithEmailAndPassword(inputUsername, password)
                    .addOnSuccessListener(authResult -> {
                        btnLogin.setEnabled(true);
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        String uid = firebaseUser != null ? firebaseUser.getUid() : "";

                        checkProfileAndNavigate(uid, inputUsername);
                    })
                    .addOnFailureListener(e -> {
                        btnLogin.setEnabled(true);

                        // Fallback to local Room DB ONLY if the error is a genuine network outage (offline mode)
                        if (e instanceof FirebaseNetworkException) {
                            UserDao userDao = AppDatabase.getInstance(LoginActivity.this).userDao();
                            User user = userDao.getUserByName(inputUsername);
                            if (user == null) user = userDao.getUserByPhone(inputUsername);

                            if (user != null && PasswordUtils.verifyPassword(password, user.getPasswordHash(), user.getSalt())) {
                                prefs.edit().putString("PARENT_NAME", user.getParentName()).apply();
                                prefs.edit().putString("PARENT_PHONE", user.getParentPhone()).apply();
                                prefs.edit().putBoolean("IS_OFFLINE_SESSION", true).apply();
                                Toast.makeText(LoginActivity.this, "Network Offline: Local Session Loaded.", Toast.LENGTH_SHORT).show();
                                navigateToHome(user.getParentName(), user.getChildName(), user.getParentRelation(), user.getChildDevice());
                                return;
                            }
                        }

                        // Invalid Credentials, User Not Found, or Auth Rejection -> Show Error, DO NOT allow Room login!
                        etPassword.setError("Authentication Failed: " + e.getMessage());
                        etPassword.requestFocus();
                        Toast.makeText(LoginActivity.this, "Authentication failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });

        // 2. Google Sign-In Click Listener (Forces Account Selection Chooser)
        if (btnGoogleSignIn != null) {
            btnGoogleSignIn.setOnClickListener(v -> {
                if (mGoogleSignInClient == null) {
                    Toast.makeText(LoginActivity.this, "Google Sign-In configuration error. Check SHA-1 in Firebase Console.", Toast.LENGTH_LONG).show();
                    return;
                }

                // Force Google Account Selection Chooser by clearing cached Google session first
                mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                    Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                    googleSignInLauncher.launch(signInIntent);
                });
            });
        }

        txtRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        Toast.makeText(this, "Authenticating Google account with Firebase...", Toast.LENGTH_SHORT).show();

        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        checkProfileAndNavigate(user.getUid(), user.getDisplayName());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(LoginActivity.this, "Google Firebase Auth Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void checkProfileAndNavigate(String uid, String defaultName) {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);

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
                        prefs.edit().putBoolean("IS_OFFLINE_SESSION", false).apply();

                        Toast.makeText(LoginActivity.this, "Sign-In Successful!", Toast.LENGTH_SHORT).show();
                        navigateToHome(parentName, childName, childRelation, childDevice);
                    } else {
                        // Profile Incomplete -> Redirect to Onboarding / Profile Registration Form
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(LoginActivity.this, "Authentication successful! Please complete your Guardian & Child details.", Toast.LENGTH_LONG).show();

                        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                        intent.putExtra("PREFILL_NAME", user != null && user.getDisplayName() != null ? user.getDisplayName() : defaultName);
                        intent.putExtra("PREFILL_EMAIL", user != null && user.getEmail() != null ? user.getEmail() : "");
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(LoginActivity.this, "Error checking profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    private String getWebClientId() {
        try {
            int webClientIdResId = getResources().getIdentifier("default_web_client_id", "string", getPackageName());
            if (webClientIdResId != 0) {
                return getString(webClientIdResId);
            }
        } catch (Exception ignored) {}
        return "275477497883-e7aklq13o0nbb84tet299kr1m8vkq4ka.apps.googleusercontent.com";
    }

    private void navigateToHome(String parentName, String childName, String childRelation, String childDevice) {
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        intent.putExtra("USER_NAME", parentName != null ? parentName : "Guardian");
        intent.putExtra("CHILD_NAME", childName != null ? childName : "Child");
        intent.putExtra("CHILD_RELATION", childRelation != null ? childRelation : "Parent");
        intent.putExtra("CHILD_DEVICE", childDevice != null ? childDevice : "0417");
        startActivity(intent);
        finish();
    }
}
