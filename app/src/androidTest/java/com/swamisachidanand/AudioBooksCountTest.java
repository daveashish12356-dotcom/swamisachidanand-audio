package com.swamisachidanand;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * Check that audio page shows 5 books (including આવેગો અને લાગણીઓ).
 */
@RunWith(AndroidJUnit4.class)
public class AudioBooksCountTest {

    @Test
    public void audioPageShowsFiveBooks() throws InterruptedException {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                BottomNavigationView nav = activity.findViewById(R.id.bottom_navigation);
                if (nav != null) nav.setSelectedItemId(R.id.nav_audio);
            });
            Thread.sleep(3500);
            final int[] count = { -1 };
            scenario.onActivity(activity -> {
                View v = activity.findViewById(R.id.audio_books_card_recycler);
                if (v instanceof RecyclerView && ((RecyclerView) v).getAdapter() != null)
                    count[0] = ((RecyclerView) v).getAdapter().getItemCount();
            });
            assertEquals("Audio page should show 5 books (આવેગો અને લાગણીઓ). Got: " + count[0] + ". Uninstall app and reinstall if still 4.", 5, count[0]);
        }
    }
}
