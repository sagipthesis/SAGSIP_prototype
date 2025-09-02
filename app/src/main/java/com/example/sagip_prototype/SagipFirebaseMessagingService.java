package com.example.sagip_prototype;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.firestore.FirebaseFirestore;

public class SagipFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "SagipFCMService";
    private FirebaseFirestore db;

    @Override
    public void onCreate() {
        super.onCreate();
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public void onNewToken(String token) {
        // Save the new FCM token to Firestore
        saveTokenToFirestore(token);
    }

    private void saveTokenToFirestore(String token) {
        // Get the current user's ID
        String userId = getCurrentUserId();
        if (userId != null) {
            db.collection("users").document(userId)
                .update("fcmToken", token)
                .addOnFailureListener(e -> {
                    // Handle error
                });
        }
    }

    private String getCurrentUserId() {
        // Implement this method to get the current user's ID
        // You can use FirebaseAuth.getInstance().getCurrentUser().getUid()
        return null; // Replace with actual implementation
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            sendNotification(title, body);
        }

        // Check if message contains a data payload
        if (remoteMessage.getData().size() > 0) {
            // Handle data message
            String title = remoteMessage.getData().get("title");
            String body = remoteMessage.getData().get("body");
            String type = remoteMessage.getData().get("type");
            
            if (title != null && body != null) {
                sendNotification(title, body);
            }
        }
    }

    private void sendNotification(String title, String messageBody) {
        Intent intent = new Intent(this, Senior_Dashboard.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        String channelId = "SOS_CHANNEL";
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create the notification channel for Android Oreo and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "SOS Alerts",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0, notificationBuilder.build());
    }
} 