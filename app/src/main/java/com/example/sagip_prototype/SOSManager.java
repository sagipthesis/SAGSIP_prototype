package com.example.sagip_prototype;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import java.util.HashMap;
import java.util.Map;

public class SOSManager {
    private static final String CHANNEL_ID = "SOS_CHANNEL";
    private static final int NOTIFICATION_ID = 1;
    private static final double NEARBY_RADIUS_KM = 5.0; // 5km radius for nearby rescuers/barangay

    private final Senior_Dashboard activity;
    private final FusedLocationProviderClient fusedLocationClient;
    private final FirebaseFirestore db;
    private LocationCallback locationCallback;

    public SOSManager(Senior_Dashboard activity) {
        this.activity = activity;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);
        this.db = FirebaseFirestore.getInstance();
    }

    public void sendSOS() {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Get current location
        fusedLocationClient.getLastLocation()
            .addOnSuccessListener(location -> {
                if (location != null) {
                    // Create SOS alert in Firestore
                    createSOSAlert(location);
                    // Send notifications to nearby rescuers and barangay
                    notifyNearbyResponders(location);
                }
            });
    }

    private void createSOSAlert(Location location) {
        Map<String, Object> sosAlert = new HashMap<>();
        sosAlert.put("seniorId", activity.getCurrentUserId()); // You'll need to implement this
        sosAlert.put("seniorName", activity.getCurrentUserName()); // You'll need to implement this
        sosAlert.put("location", new GeoPoint(location.getLatitude(), location.getLongitude()));
        sosAlert.put("timestamp", System.currentTimeMillis());
        sosAlert.put("status", "active");

        db.collection("sos_alerts")
            .add(sosAlert)
            .addOnSuccessListener(documentReference -> {
                // Show notification to senior
                showNotification("SOS Alert Sent", "Help is on the way!");
            })
            .addOnFailureListener(e -> {
                // Handle error
            });
    }

    private void notifyNearbyResponders(Location location) {
        // Query for nearby rescuers
        db.collection("rescuers")
            .whereEqualTo("status", "active")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                queryDocumentSnapshots.forEach(doc -> {
                    GeoPoint rescuerLocation = doc.getGeoPoint("location");
                    if (rescuerLocation != null) {
                        float distance = calculateDistance(
                            location.getLatitude(), location.getLongitude(),
                            rescuerLocation.getLatitude(), rescuerLocation.getLongitude()
                        );
                        
                        if (distance <= NEARBY_RADIUS_KM) {
                            // Send notification to rescuer
                            sendNotificationToUser(doc.getId(), "SOS Alert", 
                                "A senior needs help nearby!");
                        }
                    }
                });
            });

        // Query for nearby barangay
        db.collection("barangay")
            .whereEqualTo("status", "active")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                queryDocumentSnapshots.forEach(doc -> {
                    GeoPoint barangayLocation = doc.getGeoPoint("location");
                    if (barangayLocation != null) {
                        float distance = calculateDistance(
                            location.getLatitude(), location.getLongitude(),
                            barangayLocation.getLatitude(), barangayLocation.getLongitude()
                        );
                        
                        if (distance <= NEARBY_RADIUS_KM) {
                            // Send notification to barangay
                            sendNotificationToUser(doc.getId(), "SOS Alert", 
                                "A senior needs help nearby!");
                        }
                    }
                });
            });
    }

    private void sendNotificationToUser(String userId, String title, String message) {
        // Get user's FCM token from Firestore
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                String fcmToken = documentSnapshot.getString("fcmToken");
                if (fcmToken != null) {
                    // Create notification data
                    Map<String, String> notificationData = new HashMap<>();
                    notificationData.put("title", title);
                    notificationData.put("body", message);
                    notificationData.put("type", "sos_alert");

                    // Create notification payload
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("to", fcmToken);
                    payload.put("data", notificationData);

                    // Send notification using Firebase Cloud Functions
                    // You'll need to implement a Cloud Function to handle this
                    // For now, we'll just log it
                    Log.d("SOSManager", "Sending notification to: " + fcmToken);
                }
            });
    }

    private void showNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(activity, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(activity);
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) 
            == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    private float calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0] / 1000; // Convert to kilometers
    }
} 