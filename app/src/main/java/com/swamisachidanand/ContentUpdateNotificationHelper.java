package com.swamisachidanand;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

class ContentUpdateNotificationHelper {

    private static final String CHANNEL_ID = "content_updates";
    private static final int BASE_ID = 20000;

    static void showContentUpdate(Context context, String kind, String title, String body, String thumbUrl) {
        if (context == null) return;

        int targetTabId = 0;
        if ("new_book".equals(kind)) {
            targetTabId = R.id.nav_books;
        } else if ("new_audio".equals(kind)) {
            targetTabId = R.id.nav_audio;
        } else if ("new_video".equals(kind)) {
            targetTabId = R.id.nav_videos;
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (targetTabId != 0) {
            intent.putExtra(MainActivity.EXTRA_TARGET_TAB, targetTabId);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                targetTabId != 0 ? targetTabId : 0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String finalTitle = title != null && !title.isEmpty()
                ? title
                : context.getString(R.string.app_name);
        String finalBody = body != null ? body : "";

        NotificationCompat.BigPictureStyle bigPictureStyle = null;
        Bitmap large = null;
        if (thumbUrl != null && !thumbUrl.isEmpty()) {
            try {
                URL u = new URL(thumbUrl);
                HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(6000);
                conn.setDoInput(true);
                conn.connect();
                try (InputStream is = conn.getInputStream()) {
                    Bitmap bmp = BitmapFactory.decodeStream(is);
                    if (bmp != null) {
                        large = bmp;
                        bigPictureStyle = new NotificationCompat.BigPictureStyle()
                                .bigPicture(bmp)
                                .setBigContentTitle(finalTitle)
                                .setSummaryText(finalBody);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(finalTitle)
                .setContentText(finalBody)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        if (large != null) {
            builder.setLargeIcon(large);
            if (bigPictureStyle != null) {
                builder.setStyle(bigPictureStyle);
            }
        } else {
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(finalBody));
        }

        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        int id = BASE_ID + (targetTabId != 0 ? targetTabId : 1);
        nm.notify(id, builder.build());
    }
}

