package com.swamisachidanand;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

public class AppTourActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "app_prefs";
    public static final String KEY_TOUR_DONE = "tour_done";

    private ViewPager2 tourPager;
    private TextView tourIndicator;
    private TextView btnSkip;
    private TextView btnNext;

    private final List<TourPage> pages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_tour);

        tourPager = findViewById(R.id.tour_pager);
        tourIndicator = findViewById(R.id.tour_indicator);
        btnSkip = findViewById(R.id.tour_skip);
        btnNext = findViewById(R.id.tour_next);

        buildPages();
        tourPager.setAdapter(new TourAdapter());
        updateUiForPosition(0);

        tourPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateUiForPosition(position);
            }
        });

        btnSkip.setOnClickListener(v -> finishTour());
        btnNext.setOnClickListener(v -> {
            int pos = tourPager.getCurrentItem();
            if (pos >= pages.size() - 1) {
                finishTour();
            } else {
                tourPager.setCurrentItem(pos + 1, true);
            }
        });
    }

    @Override
    public void onBackPressed() {
        int pos = tourPager != null ? tourPager.getCurrentItem() : 0;
        if (pos > 0 && tourPager != null) {
            tourPager.setCurrentItem(pos - 1, true);
        } else {
            super.onBackPressed();
        }
    }

    private void buildPages() {
        pages.clear();
        pages.add(new TourPage(
                R.drawable.swami_avatar,
                "🙏 સ્વાગત છે",
                "આ એપમાં તમે સ્વામીજી વિશે જાણકારી, પુસ્તકો, ઓડિયો, વિડિઓ અને દૈનિક પ્રવચન બધું એક જ જગ્યાએ જોઈ શકશો."
        ));
        pages.add(new TourPage(
                R.drawable.ic_nav_home,
                "🏠 હોમ પેજ",
                "અહીં આજનું ચિંતન, શોધ બાર અને ઝડપી શોર્ટકટ્સ મળશે. નીચેનું મેનુથી બધી મુખ્ય વસ્તુઓ ખોલાય છે."
        ));
        pages.add(new TourPage(
                R.drawable.ic_nav_books,
                "📚 પુસ્તકો પેજ",
                "અહીં સ્વામીજીના વિવિધ પુસ્તકો વાંચી શકો. પુસ્તક ખુલતા સીધું રીડિંગ પેજ આવે છે."
        ));
        pages.add(new TourPage(
                R.drawable.ic_nav_audio,
                "🎧 ઓડિયોબુક પેજ",
                "ઓડિયો પુસ્તકોના ભાગો સાંભળો. જે ભાગ સાંભળો તે યાદ રહેશે અને આગળથી ત્યાંથી ચાલુ કરી શકશો."
        ));
        pages.add(new TourPage(
                R.drawable.ic_quick_video,
                "🎬 વિડિઓ પેજ",
                "YouTube ના વિડિઓ અને શોર્ટ્સ અહીં લાઈવ આવે છે. નવી અપડેટ્સ સરળતાથી જોવા મળે છે."
        ));
        pages.add(new TourPage(
                R.drawable.ic_nav_pravachan,
                "🔔 દૈનિક પ્રવચન",
                "પ્રવચન પેજમાં રોજ નવા પ્રવચનો આવે છે. નવા પ્રવચન માટે નોટિફિકેશન પણ મળશે."
        ));
        pages.add(new TourPage(
                R.drawable.ic_quick_books,
                "🛍 પુસ્તક ઓર્ડર",
                "Book Store પેજથી તમે પુસ્તકોનો ઓર્ડર આપી શકો છો અને જરૂરી માહિતી મેળવી શકો છો."
        ));
        pages.add(new TourPage(
                R.drawable.ic_nav_sampark,
                "📞 સંપર્ક (સમ્પર્ક) પેજ",
                "અહીંથી YouTube, Facebook, Instagram, WhatsApp, Telegram લિંક ખોલી શકો અને આશ્રમ સંપર્ક માહિતી મેળવી શકો."
        ));
    }

    private void updateUiForPosition(int position) {
        int total = pages.size();
        tourIndicator.setText((position + 1) + " / " + total);
        if (position == total - 1) {
            btnNext.setText("શરૂ કરો");
        } else {
            btnNext.setText("આગળ");
        }
    }

    private void finishTour() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_TOUR_DONE, true).apply();
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private static class TourPage {
        final int iconRes;
        final String title;
        final String desc;

        TourPage(int iconRes, String title, String desc) {
            this.iconRes = iconRes;
            this.title = title;
            this.desc = desc;
        }
    }

    private class TourAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<TourAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tour_page, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            TourPage page = pages.get(position);
            holder.icon.setImageResource(page.iconRes);
            holder.title.setText(page.title);
            holder.desc.setText(page.desc);
        }

        @Override
        public int getItemCount() {
            return pages.size();
        }

        class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            ImageView icon;
            TextView title;
            TextView desc;

            VH(@NonNull View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.tour_page_icon);
                title = itemView.findViewById(R.id.tour_page_title);
                desc = itemView.findViewById(R.id.tour_page_desc);
            }
        }
    }
}

