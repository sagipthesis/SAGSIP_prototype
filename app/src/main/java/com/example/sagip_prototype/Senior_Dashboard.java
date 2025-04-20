package com.example.sagip_prototype;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Senior_Dashboard extends AppCompatActivity {

    private static final String TAG = "SeniorDashboard";
    FirebaseAuth mAuth;
    FirebaseFirestore db;

    // Only these two TextViews
    TextView tvFullName, tvCurrentLocation;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean locationUpdatesActive = false;

    private ActivityResultLauncher<String[]> locationPermissionRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_senior_dashboard);

        // Firebase init
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Check if logged in
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(Senior_Dashboard.this, MainActivity.class));
            finish();
            return;
        }

        // Find TextViews
        tvFullName = findViewById(R.id.seniorName);
        tvCurrentLocation = findViewById(R.id.tvCurrentLocation);

        // Location services
        initializeLocationServices();
        registerLocationPermissionLauncher();

        // Load user data
        loadUserData();

        // Bottom navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavBar);
        bottomNavigationView.setSelectedItemId(R.id.senior_dashboard);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.senior_dashboard) {
                return true;
            } else if (itemId == R.id.senior_profile) {
                startActivity(new Intent(getApplicationContext(), Senior_Profile.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.senior_contact) {
                startActivity(new Intent(getApplicationContext(), Senior_Emergency_Contact.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });

        // Ask location permission
        requestLocationPermissions();
    }

    private void registerLocationPermissionLauncher() {
        locationPermissionRequest = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    Boolean fineLocationGranted = result.getOrDefault(
                            Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocationGranted = result.getOrDefault(
                            Manifest.permission.ACCESS_COARSE_LOCATION, false);

                    if (fineLocationGranted != null && fineLocationGranted) {
                        startLocationUpdates();
                    } else if (coarseLocationGranted != null && coarseLocationGranted) {
                        startLocationUpdates();
                    } else {
                        Toast.makeText(this, "Location permission is needed to show your current location", Toast.LENGTH_SHORT).show();
                        tvCurrentLocation.setText("Location permission denied");
                    }
                }
        );
    }

    private void initializeLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    updateLocationUI(location);
                    stopLocationUpdates();
                }
            }
        };
    }

    private void requestLocationPermissions() {
        locationPermissionRequest.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(10000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMinUpdateIntervalMillis(5000)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());

        locationUpdatesActive = true;
        tvCurrentLocation.setText("Fetching current location...");
    }

    private void stopLocationUpdates() {
        if (locationUpdatesActive) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationUpdatesActive = false;
        }
    }

    private void updateLocationUI(Location location) {
        if (location != null) {
            getAddressFromLocation(location);
        }
    }

    private void getAddressFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        try {
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                StringBuilder addressText = new StringBuilder();

                if (address.getThoroughfare() != null) {
                    addressText.append(address.getThoroughfare());
                    if (address.getSubThoroughfare() != null) {
                        addressText.append(" ").append(address.getSubThoroughfare());
                    }
                    addressText.append(", ");
                }

                if (address.getLocality() != null) {
                    addressText.append(address.getLocality()).append(", ");
                }

                if (address.getAdminArea() != null) {
                    addressText.append(address.getAdminArea());
                }

                String locationAddress = addressText.toString();
                tvCurrentLocation.setText(locationAddress);

                Log.d(TAG, "Current location: " + locationAddress);

                // Save the current location to Firestore
                saveLocationToFirestore(location, locationAddress);
            } else {
                tvCurrentLocation.setText("Location found but address unknown");
                Log.d(TAG, "No address found for location");
            }
        } catch (IOException e) {
            tvCurrentLocation.setText("Unable to get address from location");
            Log.e(TAG, "Error getting address from location", e);
        }
    }

    private void saveLocationToFirestore(Location location, String address) {
        String uid = mAuth.getCurrentUser().getUid();
        String userType = "seniors";

        // Create a map to save location data
        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", location.getLatitude());
        locationData.put("longitude", location.getLongitude());
        locationData.put("address", address);

        // Save the location data to Firestore
        db.collection("Sagip")
                .document("users")
                .collection(userType)
                .document(uid)
                .update("currentLocation", address, "latitude", location.getLatitude(), "longitude", location.getLongitude())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Location saved successfully"))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving location", e);
                    Toast.makeText(Senior_Dashboard.this, "Failed to save location", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadUserData() {
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
                        String currentLocation = documentSnapshot.getString("currentLocation");

                        if (firstName != null && middleName != null && lastName != null) {
                            String fullName = firstName + " " + middleName + " " + lastName;
                            tvFullName.setText(fullName);
                        } else {
                            tvFullName.setText("Full Name Not Available");
                        }

                        if (currentLocation != null && !currentLocation.isEmpty()) {
                            tvCurrentLocation.setText(currentLocation);
                        } else {
                            tvCurrentLocation.setText("Waiting for location update...");
                        }

                    } else {
                        tvFullName.setText("User data not found.");
                        Log.d(TAG, "Document doesn't exist");
                    }
                })
                .addOnFailureListener(e -> {
                    tvFullName.setText("Failed to load data.");
                    Log.e(TAG, "Error fetching user data", e);
                    e.printStackTrace();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!locationUpdatesActive) {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }
}
