package com.swamisachidanand;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseService";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "FCM new token=" + token);
        // If needed, send token to your backend.
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        try {
            String text = null;
            String author = null;
            String kind = null;

            if (remoteMessage.getData() != null && !remoteMessage.getData().isEmpty()) {
                kind = remoteMessage.getData().get("kind");
                text = remoteMessage.getData().get("text");
                author = remoteMessage.getData().get("author");
            }

            RemoteMessage.Notification notif = remoteMessage.getNotification();
            if ((text == null || text.trim().isEmpty()) && notif != null) {
                text = notif.getBody();
            }

            if (kind != null && !kind.isEmpty() && !"suvichar".equals(kind)) {
                // Generic content update: new_book / new_audio / new_video / new_pravachan
                String dataTitle = remoteMessage.getData() != null ? remoteMessage.getData().get("title") : null;
                String title = dataTitle != null && !dataTitle.trim().isEmpty()
                    ? dataTitle
                    : (notif != null && notif.getTitle() != null ? notif.getTitle() : "નવું અપડેટ");
                String body = notif != null && notif.getBody() != null ? notif.getBody() : (text != null ? text : "");
                String thumbUrl = null;
                if (remoteMessage.getData() != null) {
                    thumbUrl = remoteMessage.getData().get("thumbUrl");
                }
                ContentUpdateNotificationHelper.showContentUpdate(
                        getApplicationContext(),
                        kind,
                        title,
                        body,
                        thumbUrl
                );
                return;
            }

            if (text == null || text.trim().isEmpty()) {
                Log.w(TAG, "Received FCM message but no text found");
                return;
            }

            SuvicharNotificationHelper.showNewSuvicharNotification(
                    getApplicationContext(),
                    text,
                    author
            );
        } catch (Throwable t) {
            Log.e(TAG, "Error in onMessageReceived", t);
        }
    }
}

