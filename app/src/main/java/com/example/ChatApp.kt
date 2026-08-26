package com.example

import android.app.Application
import android.util.Log
import com.example.data.repository.NotificationHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase

class ChatApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:1084168273439:android:4cb242135f1cb0ca1a5f2a")
                    .setApiKey("AIzaSyAJlmsTZxqpBwM4M0d9tjv-YQCGPjNGvbM")
                    .setDatabaseUrl("https://chat-app-79120-default-rtdb.firebaseio.com")
                    .setProjectId("chat-app-79120")
                    .setStorageBucket("chat-app-79120.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(this, options)
            }
            try {
                FirebaseDatabase.getInstance("https://chat-app-79120-default-rtdb.firebaseio.com").setPersistenceEnabled(true)
            } catch (_: Exception) {}
        } catch (e: Exception) {
            Log.e("ChatApp", "Firebase initialization error: ${e.message}", e)
        }

        try {
            NotificationHelper.createNotificationChannel(this)
        } catch (e: Exception) {
            Log.e("ChatApp", "Notification channel error: ${e.message}", e)
        }
    }
}
