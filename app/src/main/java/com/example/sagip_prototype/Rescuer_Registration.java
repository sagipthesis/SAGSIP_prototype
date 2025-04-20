package com.example.sagip_prototype;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Rescuer_Registration extends AppCompatActivity {

    FirebaseAuth mAuth;
    FirebaseFirestore db;
    String userType = "rescuer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_rescuer_registration);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        EditText rescueGroupName = findViewById(R.id.rescue_group_name);
        EditText headquartersAddress = findViewById(R.id.headquarters_address);
        EditText primaryContactPerson = findViewById(R.id.primary_contact_person);
        EditText contactNumber = findViewById(R.id.contact_number);

        Button submitRescuer = findViewById(R.id.submit_rescuer);

        submitRescuer.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String groupname = rescueGroupName.getText().toString().trim();
                String headquarters = headquartersAddress.getText().toString().trim();
                String contact = primaryContactPerson.getText().toString().trim();
                String number = contactNumber.getText().toString().trim();

                if (groupname.isEmpty() || headquarters.isEmpty() || contact.isEmpty() || number.isEmpty()) {
                    Toast.makeText(Rescuer_Registration.this, "Fill all Fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!isValidPhoneNumber(number)) {
                    Toast.makeText(Rescuer_Registration.this, "Enter a valid number", Toast.LENGTH_SHORT).show();
                    return;
                }

                FirebaseUser user = mAuth.getCurrentUser();
                if (user == null) {
                    Toast.makeText(Rescuer_Registration.this, "User not authenticated", Toast.LENGTH_SHORT).show();
                    return;
                }

                String uid = user.getUid();
                String email = user.getEmail();

                db.collection("Sagip")
                        .document("users")
                        .collection(userType)
                        .whereEqualTo("email", email)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                Toast.makeText(Rescuer_Registration.this,
                                        "This email is already registered.",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                // Create a plain data object for Firestore
                                Map<String, Object> usrData = new HashMap<>();
                                usrData.put("rescuegroup", groupname);
                                usrData.put("headquarters", headquarters);
                                usrData.put("contactPerson", contact);
                                usrData.put("number", number);
                                usrData.put("email", email);
                                usrData.put("user-type", userType);

                                db.collection("Sagip")
                                        .document("users")
                                        .collection(userType)
                                        .document(uid)
                                        .set(usrData)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Intent intent = new Intent(Rescuer_Registration.this,
                                                            Rescuer_Dashboard.class);
                                                    Toast.makeText(Rescuer_Registration.this, "Verification Process", Toast.LENGTH_SHORT).show();
                                                    startActivity(intent);
                                                    finish();
                                                } else {
                                                    Toast.makeText(Rescuer_Registration.this,
                                                            "Failed to save data: " + task.getException().getMessage(),
                                                            Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(Rescuer_Registration.this,
                                    "Error checking email: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        });
            }
        });
    }

    private boolean isValidPhoneNumber(String number) {
        return !TextUtils.isEmpty(number) && number.matches("\\d{10}");
    }
}