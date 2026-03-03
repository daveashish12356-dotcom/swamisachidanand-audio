package com.swamisachidanand;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Opens YouTube mobile website inside a WebView, so video looks like YouTube app but stays inside this app.
 */
public class YouTubePlayerActivity extends AppCompatActivity {

    public static final String EXTRA_VIDEO_ID = "video_id";
    public static final String EXTRA_VIDEO_TITLE = "video_title";

    private WebView webView;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_youtube_player);

        String videoId = getIntent().getStringExtra(EXTRA_VIDEO_ID);
        String title = getIntent().getStringExtra(EXTRA_VIDEO_TITLE);
        if (videoId != null && !videoId.isEmpty()) {
            RecentVideoHelper.saveRecentVideoId(this, videoId);
        }
        if (videoId == null || videoId.isEmpty()) {
            Toast.makeText(this, "Video not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView titleView = findViewById(R.id.player_title);
        if (title != null && !title.isEmpty()) {
            titleView.setText(title);
        } else {
            titleView.setText("Video");
        }

        ImageButton backBtn = findViewById(R.id.player_back);
        backBtn.setOnClickListener(v -> onBackPressed());

        webView = findViewById(R.id.youtube_webview);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());

        // Load normal YouTube mobile watch page – no embed, no 152-4, behaves like YouTube inside WebView.
        String url = "https://m.youtube.com/watch?v=" + videoId + "&autoplay=1";
        webView.loadUrl(url);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.destroy();
        }
        super.onDestroy();
    }
}
