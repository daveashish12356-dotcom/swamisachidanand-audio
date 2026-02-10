package com.swamisachidanand;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        View fragmentContainer = findViewById(R.id.fragment_container);
        if (fragmentContainer != null) {
            ViewCompat.setOnApplyWindowInsetsListener(fragmentContainer, (v, windowInsets) -> {
                androidx.core.graphics.Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(insets.left, insets.top, insets.right, 0);
                return windowInsets;
            });
            fragmentContainer.requestApplyInsets();
        }

        BookChapterScanner.scanAllAndSave(this);

        // Pehle fragment turant load karo — white screen na dikhe (commitNow = sync)
        if (savedInstanceState == null) {
            try {
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commitNow();
            } catch (Throwable t) {
                Log.e(TAG, "Error loading HomeFragment", t);
                // Fallback: use async commit
                try {
                    getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new HomeFragment())
                        .commit();
                } catch (Throwable t2) {
                    Log.e(TAG, "Error loading HomeFragment (async)", t2);
                }
            }
        }

        bottomNavigation = findViewById(R.id.bottom_navigation);
        if (bottomNavigation != null) {
            try {
                bottomNavigation.setOnItemSelectedListener(item -> {
                    try {
                        Fragment selectedFragment = null;
                        int itemId = item.getItemId();
                        if (itemId == R.id.nav_home) selectedFragment = new HomeFragment();
                        else if (itemId == R.id.nav_books) selectedFragment = new BooksFragment();
                        else if (itemId == R.id.nav_audio) {
                            // Pehle list khule — back stack clear karo taake Audio open karte hi sirf list dikhe
                            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                            selectedFragment = new ServerAudioFragment();
                        }
                        else if (itemId == R.id.nav_about) selectedFragment = new AboutFragment();
                        if (selectedFragment != null && !isFinishing()) {
                            getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, selectedFragment)
                                .commit();
                            return true;
                        }
                    } catch (Throwable t) {
                        Log.e(TAG, "nav click", t);
                    }
                    return false;
                });
            } catch (Throwable t) {
                Log.e(TAG, "setOnItemSelectedListener", t);
            }
        }
    }

    // Method for BooksFragment/HomeFragment to open books
    public void openBook(Book book) {
        try {
            if (book == null) return;
            Intent intent = new Intent(this, PdfViewerActivity.class);
            String pdfUrl = book.getPdfUrl();
            if (pdfUrl != null && !pdfUrl.trim().isEmpty()) {
                intent.putExtra("pdf_url", pdfUrl.trim());
                intent.putExtra("book_name", book.getName() != null ? book.getName() : "");
                intent.putExtra("book_file_name", book.getFileName() != null ? book.getFileName() : "");
            } else {
                String fileName = book.getFileName();
                if (fileName == null || (fileName = fileName.trim()).isEmpty()) return;
                intent.putExtra("book_name", fileName);
            }
            startActivity(intent);
        } catch (Throwable t) {
            Log.e(TAG, "openBook failed", t);
            android.widget.Toast.makeText(this, "બુક ખોલી શકાઈ નહીં.", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

}
