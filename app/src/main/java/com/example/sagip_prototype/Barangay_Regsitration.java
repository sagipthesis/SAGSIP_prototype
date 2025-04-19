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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Barangay_Regsitration extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private EditText barangayNameEditText, municipalityCityEditText, provinceEditText,
            barangayCaptainEditText, barangaySecretaryEditText, barangayHallAddressEditText,
            contactNumberEditText, populationEditText, landAreaEditText;

    private String userType = "barangay";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_barangay_regsitration);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();

        Button submitToBrgy = findViewById(R.id.registrer_brgy);
        submitToBrgy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitBarangayRegistration();
            }
        });
    }

    private void initializeViews() {
        barangayNameEditText = findViewById(R.id.barangay_name);
        municipalityCityEditText = findViewById(R.id.municipality_city);
        provinceEditText = findViewById(R.id.province);
        barangayCaptainEditText = findViewById(R.id.barangay_captain);
        barangaySecretaryEditText = findViewById(R.id.barangay_secretary);
        barangayHallAddressEditText = findViewById(R.id.barangay_hall_address);
        contactNumberEditText = findViewById(R.id.contact_number);
        populationEditText = findViewById(R.id.population);
        landAreaEditText = findViewById(R.id.land_area);
    }

    private void submitBarangayRegistration() {
        String barangayName = barangayNameEditText.getText().toString().trim();
        String municipalityCity = municipalityCityEditText.getText().toString().trim();
        String province = provinceEditText.getText().toString().trim();
        String barangayCaptain = barangayCaptainEditText.getText().toString().trim();
        String barangaySecretary = barangaySecretaryEditText.getText().toString().trim();
        String barangayHallAddress = barangayHallAddressEditText.getText().toString().trim();
        String contactNumber = contactNumberEditText.getText().toString().trim();
        String population = populationEditText.getText().toString().trim();
        String landArea = landAreaEditText.getText().toString().trim();

        if (barangayName.isEmpty() || municipalityCity.isEmpty() || province.isEmpty() ||
                barangayCaptain.isEmpty() || barangaySecretary.isEmpty() ||
                barangayHallAddress.isEmpty() || contactNumber.isEmpty() ||
                population.isEmpty() || landArea.isEmpty()) {
            Toast.makeText(this, "Fill all Fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isInvalidPhoneNumber(contactNumber)) {
            Toast.makeText(this, "Enter a valid number", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        String email = user.getEmail();

        checkIfEmailExistsAndRegister(uid, email, barangayName, municipalityCity, province,
                barangayCaptain, barangaySecretary, barangayHallAddress, contactNumber,
                population, landArea);
    }

    private void checkIfEmailExistsAndRegister(String uid, String email, String barangayName,
                                               String municipalityCity, String province,
                                               String barangayCaptain, String barangaySecretary,
                                               String barangayHallAddress, String contactNumber,
                                               String population, String landArea) {

        db.collection("Sagip")
                .document("users")
                .collection(userType)
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(Barangay_Regsitration.this,
                                "This email is already registered.",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Map<String, Object> usrData = new HashMap<>();
                        usrData.put("barangayName", barangayName);
                        usrData.put("municipalityCity", municipalityCity);
                        usrData.put("province", province);
                        usrData.put("barangayCaptain", barangayCaptain);
                        usrData.put("barangaySecretary", barangaySecretary);
                        usrData.put("barangayHallAddress", barangayHallAddress);
                        usrData.put("contactNumber", contactNumber);
                        usrData.put("population", population);
                        usrData.put("landArea", landArea);
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
                                            Toast.makeText(Barangay_Regsitration.this, "Verification Process", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(Barangay_Regsitration.this, Verification_Page.class));
                                            finish();
                                        } else {
                                            Toast.makeText(Barangay_Regsitration.this,
                                                    "Failed to save data: " + task.getException().getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Barangay_Regsitration.this,
                            "Error checking email: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private boolean isInvalidPhoneNumber(String number) {
        return TextUtils.isEmpty(number) || !number.matches("\\d{10}");
    }
}
