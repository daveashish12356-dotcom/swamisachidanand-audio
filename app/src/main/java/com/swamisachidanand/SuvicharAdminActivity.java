package com.swamisachidanand;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class SuvicharAdminActivity extends AppCompatActivity {

    private static final String TAG = "SuvicharAdminActivity";
    private static final String URL = "https://swami-sachidanand.web.app/suvichar-notify.html";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_suvichar_admin);
        } catch (Throwable t) {
            Log.e(TAG, "setContentView failed", t);
            finish();
            return;
        }

        try {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("આજનું ચિંતન");
            }
        } catch (Throwable ignored) {
        }

        WebView webView = findViewById(R.id.suvichar_webview);
        if (webView != null) {
            WebSettings ws = webView.getSettings();
            ws.setJavaScriptEnabled(true);
            ws.setDomStorageEnabled(true);
            webView.setWebViewClient(new WebViewClient());
            webView.loadUrl(URL);
        } else {
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

