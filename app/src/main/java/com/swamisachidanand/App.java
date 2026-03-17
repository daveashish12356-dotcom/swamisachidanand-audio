package com.swamisachidanand;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

public class App extends Application {
    private static final String TAG = "App";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            PDFBoxResourceLoader.init(getApplicationContext());
        } catch (Throwable t) {
            Log.e(TAG, "PDFBox init failed", t);
        }
        createNotificationChannels();
        // હંમેશા લાઇટ મોડ – સિસ્ટમ નાઇટ મોડ on હોય તો પણ એપ લાઇટ રહેશે
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // Initialize Firebase (with small delay used earlier for crash safety)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                FirebaseApp.initializeApp(this);
                subscribeToTopicSafe("suvichar");
                subscribeToTopicSafe("new_book");
                subscribeToTopicSafe("new_audio");
                subscribeToTopicSafe("new_video");
            } catch (Throwable t) {
                Log.e(TAG, "Firebase init failed", t);
            }
        }, 2000);

        // Initialize Google Mobile Ads SDK (AdMob) once at app start
        try {
            MobileAds.initialize(this, initializationStatus -> {
                Log.d(TAG, "MobileAds initialized");
            });
        } catch (Throwable t) {
            Log.e(TAG, "MobileAds init failed", t);
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        try {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm == null) return;
            NotificationChannel suvicharChannel = new NotificationChannel(
                    "suvichar_updates",
                    "આજનું ચિંતન",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            suvicharChannel.setDescription("નવો સુવિચાર આવે ત્યારે નોટિફિકેશન");
            nm.createNotificationChannel(suvicharChannel);
            NotificationChannel contentChannel = new NotificationChannel(
                    "content_updates",
                    "નવું અપડેટ",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            contentChannel.setDescription("નવા પુસ્તકો, ઓડિયો અને વિડિઓ માટે નોટિફિકેશન");
            nm.createNotificationChannel(contentChannel);
        } catch (Throwable t) {
            Log.e(TAG, "createNotificationChannels failed", t);
        }
    }

    private void subscribeToTopicSafe(String topic) {
        try {
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Subscribed to topic=" + topic);
                        } else {
                            Log.w(TAG, "Subscribe to topic failed " + topic, task.getException());
                        }
                    });
        } catch (Throwable t) {
            Log.e(TAG, "subscribeToTopicSafe " + topic, t);
        }
    }
}


