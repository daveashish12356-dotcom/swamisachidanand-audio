package com.swamisachidanand;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Full-screen slideshow – જે ફોટો પર ટૅપ કર્યો એથી શરૂઆત, બધા ફોટા અલગ અલગ એનિમેશન સાથે.
 */
public class FullScreenSlideshowActivity extends AppCompatActivity {

    public static final String EXTRA_START_INDEX = "start_index";

    private ImageView imageView;
    private TextView counterView;
    private List<String> urls = new ArrayList<>();
    private int currentIndex;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final int SLIDE_DURATION_MS = 4000;
    private Runnable nextSlideRunnable;
    private boolean paused = false;

    private static final int[] ANIM_IDS = {
        R.anim.fragment_fade_in,
        R.anim.slide_in_right,
        R.anim.slide_in_left,
        R.anim.slide_in_bottom,
        R.anim.zoom_in
    };
    private final Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slideshow_fullscreen);

        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_FULLSCREEN);

        imageView = findViewById(R.id.slideshow_image);
        counterView = findViewById(R.id.slideshow_counter);
        ImageButton back = findViewById(R.id.slideshow_back);
        if (back != null) back.setOnClickListener(v -> finish());

        List<String> list = PhotoGalleryActivity.getGalleryUrls();
        if (list != null) urls = new ArrayList<>(list);
        int start = getIntent().getIntExtra(EXTRA_START_INDEX, 0);
        if (start < 0 || start >= urls.size()) start = 0;
        currentIndex = start;

        imageView.setOnClickListener(v -> {
            paused = !paused;
            if (paused) {
                handler.removeCallbacks(nextSlideRunnable);
            } else {
                scheduleNext();
            }
        });

        if (urls.isEmpty()) {
            finish();
            return;
        }
        showCurrent();
        scheduleNext();
    }

    private void showCurrent() {
        String url = urls.get(currentIndex);
        int animId = ANIM_IDS[random.nextInt(ANIM_IDS.length)];
        Animation anim = AnimationUtils.loadAnimation(this, animId);
        imageView.clearAnimation();
        Glide.with(this)
            .load(url)
            .fitCenter()
            .transition(DrawableTransitionOptions.withCrossFade(150))
            .into(imageView);
        imageView.startAnimation(anim);
        if (counterView != null) {
            counterView.setText((currentIndex + 1) + " / " + urls.size());
        }
    }

    private void scheduleNext() {
        handler.removeCallbacks(nextSlideRunnable);
        nextSlideRunnable = () -> {
            if (paused || urls.isEmpty()) return;
            currentIndex = (currentIndex + 1) % urls.size();
            showCurrent();
            scheduleNext();
        };
        handler.postDelayed(nextSlideRunnable, SLIDE_DURATION_MS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(nextSlideRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!paused && !urls.isEmpty()) scheduleNext();
    }
}
