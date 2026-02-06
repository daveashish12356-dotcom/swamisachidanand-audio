package com.swamisachidanand;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import java.io.IOException;
import java.io.InputStream;

/** 3 second splash - image dikhane ke baad MainActivity open. */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 3000; // 3 seconds - yehi image shuru me aata tha

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        try {
            setContentView(R.layout.activity_splash);
        } catch (Throwable t) {
            finish();
            startActivity(new Intent(this, MainActivity.class));
            return;
        }

        ImageView splashImage = findViewById(R.id.splash_image);
        if (splashImage != null) {
            // Pehle splash.jpg / aapki photo (Desktop se copy karke assets me rakhe)
            String[] tryImages = {"splash.jpg", "FOYHY6rVIAYd03r.jpg", "home_photo2.jpg", "splash.png"};
            boolean loaded = false;
            for (String name : tryImages) {
                try (InputStream is = getAssets().open(name)) {
                    Bitmap bitmap = BitmapFactory.decodeStream(is);
                    if (bitmap != null) {
                        splashImage.setImageBitmap(bitmap);
                        loaded = true;
                        break;
                    }
                } catch (IOException ignored) { }
            }
            if (!loaded) {
                try {
                    splashImage.setImageResource(android.R.drawable.ic_menu_gallery);
                } catch (Throwable ignored) {}
            }
            try {
                splashImage.setAlpha(0f);
                splashImage.animate().alpha(1f).setDuration(500).start();
            } catch (Throwable ignored) {}
        }

        // 3 sec ke baad MainActivity - hamesha chalna chahiye
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                if (!isFinishing()) {
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    finish();
                }
            } catch (Throwable t) {
                try {
                    startActivity(new Intent(this, MainActivity.class));
                } catch (Throwable t2) {
                    finish();
                }
            }
        }, SPLASH_DURATION);
    }
}

