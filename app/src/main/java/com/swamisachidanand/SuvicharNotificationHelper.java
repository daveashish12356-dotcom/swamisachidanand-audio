package com.swamisachidanand;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

class SuvicharNotificationHelper {

    private static final String CHANNEL_ID = "suvichar_updates";
    private static final int NOTIFICATION_ID = 10001;

    static void showNewSuvicharNotification(Context context, String text, String author) {
        if (context == null || text == null || text.trim().isEmpty()) return;

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = context.getString(R.string.app_name);
        String contentTitle = context.getString(R.string.suvichar_notification_title);

        StringBuilder body = new StringBuilder(text.trim());
        if (author != null && !author.trim().isEmpty()) {
            body.append("\n— ").append(author.trim());
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(contentTitle != null ? contentTitle : title)
                .setContentText(text.trim())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body.toString()))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        manager.notify(NOTIFICATION_ID, builder.build());
    }
}

