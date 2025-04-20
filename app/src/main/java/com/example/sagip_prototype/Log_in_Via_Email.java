package com.example.sagip_prototype;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Arrays;
import java.util.List;

public class Log_in_Via_Email extends AppCompatActivity {

    private static final String TAG = "Log_in_Via_Email";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText email, password;
    private TextView forgotPassword;
    private Button loginBtn;

    private final List<String> userTypes = Arrays.asList("seniors", "hospital", "rescuer", "barangay");
    private int currentUserTypeIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_log_in_via_email);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        email = findViewById(R.id.user_email);
        password = findViewById(R.id.user_password);
        forgotPassword = findViewById(R.id.forgot_password);
        loginBtn = findViewById(R.id.login_btn);

        // Setup forgot password functionality
        forgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String emailAddress = email.getText().toString().trim();
                if (TextUtils.isEmpty(emailAddress)) {
                    Toast.makeText(Log_in_Via_Email.this, "Please enter your email above", Toast.LENGTH_SHORT).show();
                    return;
                }

                sendPasswordResetEmail(emailAddress);
            }
        });

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String getEmail = email.getText().toString().trim();
                String getPassword = password.getText().toString().trim();

                if (TextUtils.isEmpty(getEmail)) {
                    email.setError("Email is required");
                    return;
                }

                if (TextUtils.isEmpty(getPassword)) {
                    password.setError("Password is required");
                    return;
                }

                // Show progress
                loginBtn.setEnabled(false);
                loginBtn.setText("Signing in...");

                signInWithEmail(getEmail, getPassword);
            }
        });
    }

    private void sendPasswordResetEmail(String emailAddress) {
        mAuth.sendPasswordResetEmail(emailAddress)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(Log_in_Via_Email.this, "Password reset email sent", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(Log_in_Via_Email.this, "Failed to send reset email: " +
                                            (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void signInWithEmail(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<com.google.firebase.auth.AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<com.google.firebase.auth.AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                String uid = user.getUid();
                                // Find the user type based on UID
                                findUserTypeByUID(uid);
                            }
                        } else {
                            // If sign in fails, display the specific error message
                            String errorMessage = "Authentication failed";
                            if (task.getException() != null) {
                                errorMessage = task.getException().getMessage();
                            }
                            Toast.makeText(Log_in_Via_Email.this, errorMessage, Toast.LENGTH_LONG).show();

                            // Reset button state
                            loginBtn.setEnabled(true);
                            loginBtn.setText("Login");
                        }
                    }
                });
    }

    private void findUserTypeByUID(final String uid) {
        // Try to find which type of user this is based on the UID
        checkNextUserType(uid, 0);
    }

    private void checkNextUserType(final String uid, final int index) {
        if (index >= userTypes.size()) {
            // We've checked all user types and didn't find the user
            Toast.makeText(this, "User account found but not registered. Please complete registration.", Toast.LENGTH_SHORT).show();
            // Redirect to registration page
            Intent intent = new Intent(Log_in_Via_Email.this, Rescuer_Registration.class);
            intent.putExtra("UID", uid);
            startActivity(intent);
            finish();
            return;
        }

        String userType = userTypes.get(index);

        // Query to find user with this UID in this user type collection
        db.collection("Sagip")
                .document("users")
                .collection(userType)
                .whereEqualTo("uid", uid)  // Try to find document with this UID
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                            // Found the user in this collection
                            goToHomeScreen(userType);
                        } else {
                            // User not found in this collection, try next one
                            checkNextUserType(uid, index + 1);
                        }
                    }
                });
    }

    private void goToHomeScreen(String userType) {
        Intent intent;
        switch (userType) {
            case "seniors":
                intent = new Intent(Log_in_Via_Email.this, Senior_Dashboard.class);
                break;
            case "hospital":
                intent = new Intent(Log_in_Via_Email.this, Hospital_Dashboard.class);
                break;
            case "rescuer":
                intent = new Intent(Log_in_Via_Email.this, Rescuer_Dashboard.class);
                break;
            case "barangay":
                intent = new Intent(Log_in_Via_Email.this, Barangay_Dashboard.class);
                break;
            default:
                Toast.makeText(this, "Unknown user type", Toast.LENGTH_SHORT).show();
                loginBtn.setEnabled(true);
                loginBtn.setText("Login");
                return;
        }

        // Clear activity stack and go to dashboard
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}