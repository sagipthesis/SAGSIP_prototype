package com.example.sagip_prototype;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class Senior_Profile extends AppCompatActivity {

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_senior_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        LinearLayout goToUpdate = findViewById(R.id.change_Number);
        TextView tvFullName = findViewById(R.id.profileName);
        TextView tvnumber = findViewById(R.id.profileEmail);

        // Handle Profile Update Navigation
        goToUpdate.setOnClickListener(v -> {
            Intent intent = new Intent(Senior_Profile.this, Senior_Update_Profile.class);
            startActivity(intent);
            finish();
        });

        // Logout Button
        LinearLayout logoutButton = findViewById(R.id.logoutLayout);
        logoutButton.setOnClickListener(v -> {
            mAuth.signOut(); // Sign out the user
            Toast.makeText(Senior_Profile.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Senior_Profile.this, MainActivity.class)); // Redirect to login screen
            finish(); // Finish the current activity
        });

        // Load user data from Firestore
        String uid = mAuth.getCurrentUser().getUid();
        String userType = "seniors";

        db.collection("Sagip")
                .document("users")
                .collection(userType)
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String firstName = documentSnapshot.getString("firstName");
                        String middleName = documentSnapshot.getString("middleName");
                        String lastName = documentSnapshot.getString("lastName");
                        String mobileNumber = documentSnapshot.getString("mobileNumber");

                        String fullName = firstName + " " + middleName + " " + lastName;

                        tvFullName.setText(fullName);
                        tvnumber.setText(mobileNumber);
                    }
                });

        // Bottom Navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavBar);
        bottomNavigationView.setSelectedItemId(R.id.senior_profile);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.senior_dashboard) {
                startActivity(new Intent(getApplicationContext(), Senior_Dashboard.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.senior_profile) {
                return true;
            } else if (itemId == R.id.senior_contact) {
                startActivity(new Intent(getApplicationContext(), Senior_Emergency_Contact.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }
}
