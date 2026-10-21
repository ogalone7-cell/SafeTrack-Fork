package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etParentName, etParentEmail, etParentPhone, etParentAge, etParentRelation, etParentPassword;
    private EditText etChildName, etChildAge, etChildDevice;
    private Spinner spParentGender, spChildGender;
    private Button btnRegister;
    private TextView txtLoginLink;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acitivity_register);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        // Bind Parent views
        etParentName = findViewById(R.id.etParentName);
        etParentEmail = findViewById(R.id.etParentEmail);
        etParentPhone = findViewById(R.id.etParentPhone);
        etParentAge = findViewById(R.id.etParentAge);
        etParentRelation = findViewById(R.id.etParentRelation);
        etParentPassword = findViewById(R.id.etParentPassword);
        spParentGender = findViewById(R.id.spParentGender);

        // Bind Child views
        etChildName = findViewById(R.id.etChildName);
        etChildAge = findViewById(R.id.etChildAge);
        etChildDevice = findViewById(R.id.etChildDevice);
        spChildGender = findViewById(R.id.spChildGender);

        btnRegister = findViewById(R.id.btnRegister);
        txtLoginLink = findViewById(R.id.txtLoginLink);

        // Check if user is already authenticated via Google (Google Profile Completion Mode)
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Password is handled by Google Auth; hide password input field for Google onboarding
            if (etParentPassword != null) {
                etParentPassword.setVisibility(View.GONE);
            }
            if (currentUser.getEmail() != null && !currentUser.getEmail().isEmpty()) {
                etParentEmail.setText(currentUser.getEmail());
                etParentEmail.setEnabled(false);
            }
            if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
                etParentName.setText(currentUser.getDisplayName());
            }
            btnRegister.setText("Complete Profile & Proceed");
        } else {
            // Prefill from Intent if available
            String prefillName = getIntent().getStringExtra("PREFILL_NAME");
            String prefillEmail = getIntent().getStringExtra("PREFILL_EMAIL");
            if (prefillName != null && !prefillName.isEmpty()) {
                etParentName.setText(prefillName);
            }
            if (prefillEmail != null && !prefillEmail.isEmpty()) {
                etParentEmail.setText(prefillEmail);
            }
        }

        // Setup gender dropdown options
        String[] genderOptions = {"Select Gender", "Male", "Female", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, genderOptions);
        spParentGender.setAdapter(adapter);
        spChildGender.setAdapter(adapter);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateForm()) {
                    String parentName = etParentName.getText().toString().trim();
                    String parentEmail = etParentEmail.getText().toString().trim();
                    String parentPhone = etParentPhone.getText().toString().trim();
                    String parentAge = etParentAge.getText().toString().trim();
                    String parentGender = spParentGender.getSelectedItem().toString();
                    String parentRelation = etParentRelation.getText().toString().trim();
                    String parentPassword = etParentPassword.getText().toString();

                    String childName = etChildName.getText().toString().trim();
                    String childAge = etChildAge.getText().toString().trim();
                    String childGender = spChildGender.getSelectedItem().toString();
                    String childDevice = etChildDevice.getText().toString().trim();

                    btnRegister.setEnabled(false);
                    Toast.makeText(RegisterActivity.this, "Processing registration...", Toast.LENGTH_SHORT).show();

                    FirebaseUser activeUser = mAuth.getCurrentUser();
                    if (activeUser != null) {
                        // User ALREADY authenticated via Google -> Complete profile without creating a 2nd account
                        saveProfileToFirestore(activeUser.getUid(), parentName, parentEmail, parentPhone, parentAge,
                                parentGender, parentRelation, null, childName, childAge, childGender, childDevice);
                    } else {
                        // Unauthenticated User -> Create new Firebase Email/Password Account
                        mAuth.createUserWithEmailAndPassword(parentEmail, parentPassword)
                                .addOnSuccessListener(authResult -> {
                                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                                    String uid = firebaseUser != null ? firebaseUser.getUid() : "";
                                    saveProfileToFirestore(uid, parentName, parentEmail, parentPhone, parentAge,
                                            parentGender, parentRelation, parentPassword, childName, childAge, childGender, childDevice);
                                })
                                .addOnFailureListener(e -> {
                                    btnRegister.setEnabled(true);
                                    if (e instanceof FirebaseAuthUserCollisionException) {
                                        etParentEmail.setError("This email is already registered");
                                        etParentEmail.requestFocus();
                                        Toast.makeText(RegisterActivity.this, "This email is already registered. Please log in or use Forgot Password.", Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(RegisterActivity.this, "Registration Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    }
                }
            }
        });

        txtLoginLink.setOnClickListener(v -> finish());
    }

    private void saveProfileToFirestore(String uid, String parentName, String parentEmail, String parentPhone,
                                       String parentAge, String parentGender, String parentRelation,
                                       String parentPassword, String childName, String childAge,
                                       String childGender, String childDevice) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("uid", uid);
        userMap.put("parentName", parentName);
        userMap.put("email", parentEmail);
        userMap.put("parentPhone", parentPhone);
        userMap.put("parentAge", parentAge);
        userMap.put("parentGender", parentGender);
        userMap.put("parentRelation", parentRelation);
        userMap.put("childName", childName);
        userMap.put("childAge", childAge);
        userMap.put("childGender", childGender);
        userMap.put("childDevice", childDevice);
        userMap.put("updatedAt", Timestamp.now());

        mFirestore.collection("users").document(uid)
                .set(userMap)
                .addOnCompleteListener(task -> {
                    btnRegister.setEnabled(true);
                    if (task.isSuccessful()) {
                        // Synchronize Room Local DB and SharedPreferences ONLY after successful Firestore persistence!
                        String salt = PasswordUtils.generateSalt();
                        String passwordHash = (parentPassword != null && !parentPassword.isEmpty())
                                ? PasswordUtils.hashPassword(parentPassword, salt)
                                : null; // Do NOT generate fake string for Google users!

                        User user = new User(parentName, parentPhone, parentRelation,
                                childName, childAge, childDevice,
                                passwordHash != null ? passwordHash : "", salt);
                        AppDatabase.getInstance(RegisterActivity.this).userDao().insertUser(user);

                        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("PARENT_NAME", parentName);
                        editor.putString("PARENT_PHONE", parentPhone);
                        editor.putString("PARENT_RELATION", parentRelation);
                        editor.putString("CHILD_NAME", childName);
                        editor.putString("CHILD_AGE", childAge);
                        editor.putString("CHILD_DEVICE", childDevice);
                        editor.apply();

                        Toast.makeText(RegisterActivity.this, "Account & Profile Created Successfully!", Toast.LENGTH_LONG).show();

                        Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
                        intent.putExtra("USER_NAME", parentName);
                        intent.putExtra("CHILD_NAME", childName);
                        intent.putExtra("CHILD_RELATION", parentRelation);
                        intent.putExtra("CHILD_DEVICE", childDevice);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(RegisterActivity.this, "Profile save error: " + (task.getException() != null ? task.getException().getMessage() : "Unknown"), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean validateForm() {
        boolean isValid = true;

        String parentName = etParentName.getText().toString().trim();
        String parentEmail = etParentEmail.getText().toString().trim();
        String parentPhone = etParentPhone.getText().toString().trim();
        String parentAgeStr = etParentAge.getText().toString().trim();
        String parentRelation = etParentRelation.getText().toString().trim();
        String parentPassword = etParentPassword.getText().toString();

        String childName = etChildName.getText().toString().trim();
        String childAgeStr = etChildAge.getText().toString().trim();

        // 1. Parent Name
        if (parentName.isEmpty()) {
            etParentName.setError("Parent name is required");
            etParentName.requestFocus();
            isValid = false;
        } else if (!containsLetters(parentName)) {
            etParentName.setError("Parent name must contain letters");
            etParentName.requestFocus();
            isValid = false;
        }

        // 2. Parent Email
        if (parentEmail.isEmpty()) {
            etParentEmail.setError("Email address is required");
            if (isValid) etParentEmail.requestFocus();
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(parentEmail).matches()) {
            etParentEmail.setError("Enter a valid email address (e.g. name@example.com)");
            if (isValid) etParentEmail.requestFocus();
            isValid = false;
        }

        // 3. Parent Phone
        if (parentPhone.isEmpty()) {
            etParentPhone.setError("Phone number is required");
            if (isValid) etParentPhone.requestFocus();
            isValid = false;
        } else if (parentPhone.length() < 10) {
            etParentPhone.setError("Enter a valid 10-digit phone number");
            if (isValid) etParentPhone.requestFocus();
            isValid = false;
        }

        // 4. Parent Age
        if (parentAgeStr.isEmpty()) {
            etParentAge.setError("Parent age is required");
            if (isValid) etParentAge.requestFocus();
            isValid = false;
        } else {
            try {
                int age = Integer.parseInt(parentAgeStr);
                if (age < 18 || age > 120) {
                    etParentAge.setError("Enter a valid parent age (18+)");
                    if (isValid) etParentAge.requestFocus();
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                etParentAge.setError("Enter a valid number for age");
                if (isValid) etParentAge.requestFocus();
                isValid = false;
            }
        }

        // 5. Parent Gender
        if (spParentGender.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select Parent Gender", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        // 6. Parent Relation
        if (parentRelation.isEmpty()) {
            etParentRelation.setError("Relation to child is required");
            if (isValid) etParentRelation.requestFocus();
            isValid = false;
        } else if (!containsLetters(parentRelation)) {
            etParentRelation.setError("Relation must contain letters (e.g. Father)");
            if (isValid) etParentRelation.requestFocus();
            isValid = false;
        }

        // 7. Parent Password (Only required for unauthenticated email/password sign-up)
        if (mAuth.getCurrentUser() == null) {
            if (parentPassword.isEmpty()) {
                etParentPassword.setError("Password is required");
                if (isValid) etParentPassword.requestFocus();
                isValid = false;
            } else if (!isValidPassword(parentPassword)) {
                etParentPassword.setError("Password must be min 5 chars, contain at least 1 symbol and 2 digits");
                if (isValid) etParentPassword.requestFocus();
                isValid = false;
            }
        }

        // 8. Child Name
        if (childName.isEmpty()) {
            etChildName.setError("Child name is required");
            if (isValid) etChildName.requestFocus();
            isValid = false;
        } else if (!containsLetters(childName)) {
            etChildName.setError("Child name must contain letters");
            if (isValid) etChildName.requestFocus();
            isValid = false;
        }

        // 9. Child Age
        if (childAgeStr.isEmpty()) {
            etChildAge.setError("Child age is required");
            if (isValid) etChildAge.requestFocus();
            isValid = false;
        } else {
            try {
                int age = Integer.parseInt(childAgeStr);
                if (age <= 0 || age >= 18) {
                    etChildAge.setError("Enter a valid child age (1 - 17)");
                    if (isValid) etChildAge.requestFocus();
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                etChildAge.setError("Enter a valid number for age");
                if (isValid) etChildAge.requestFocus();
                isValid = false;
            }
        }

        // 10. Child Gender
        if (spChildGender.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select Child Gender", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    private boolean containsLetters(String text) {
        return text.matches(".*[a-zA-Z].*");
    }

    private boolean isValidPassword(String password) {
        if (password.length() < 5) {
            return false;
        }

        int digitCount = 0;
        int symbolCount = 0;

        for (char c : password.toCharArray()) {
            if (Character.isDigit(c)) {
                digitCount++;
            } else if (!Character.isLetter(c) && !Character.isWhitespace(c)) {
                symbolCount++;
            }
        }

        return digitCount >= 2 && symbolCount >= 1;
    }
}
