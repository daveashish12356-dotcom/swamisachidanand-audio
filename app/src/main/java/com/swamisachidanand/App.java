package com.swamisachidanand;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.firebase.FirebaseApp;
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
        // Auto-detect and follow system night mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        // Firebase init temporarily disabled — crash debug ke liye
        // new Handler(Looper.getMainLooper()).postDelayed(() -> {
        //     try {
        //         FirebaseApp.initializeApp(this);
        //     } catch (Throwable t) {
        //         Log.e(TAG, "Firebase init failed", t);
        //     }
        // }, 2000);
    }
}


