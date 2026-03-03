package com.swamisachidanand;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Simple web page inside app for Swamiji audio pravachan site.
 * Loads https://swamisachchidanandji.org/music in a WebView.
 */
public class MusicWebFragment extends Fragment {

    private static final String MUSIC_URL = "https://swamisachchidanandji.org/music";

    private WebView webView;
    private ProgressBar progressBar;

    public MusicWebFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_music_web, container, false);
        webView = root.findViewById(R.id.music_webview);
        progressBar = root.findViewById(R.id.music_progress);

        if (webView != null) {
            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setLoadWithOverviewMode(true);
            settings.setUseWideViewPort(true);

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                }
            });

            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onProgressChanged(WebView view, int newProgress) {
                    if (progressBar != null) {
                        if (newProgress >= 100) {
                            progressBar.setVisibility(View.GONE);
                        } else {
                            if (progressBar.getVisibility() != View.VISIBLE) {
                                progressBar.setVisibility(View.VISIBLE);
                            }
                            progressBar.setProgress(newProgress);
                        }
                    }
                }
            });

            webView.loadUrl(MUSIC_URL);
        }

        return root;
    }

    @Override
    public void onDestroyView() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        progressBar = null;
        super.onDestroyView();
    }
}

