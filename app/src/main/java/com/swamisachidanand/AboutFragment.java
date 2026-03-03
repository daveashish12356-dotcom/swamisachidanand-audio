package com.swamisachidanand;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class AboutFragment extends Fragment {

    private static final String TAG = "AboutFragment";

    public AboutFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = null;
        try {
            view = inflater.inflate(R.layout.fragment_about, container, false);
            if (view == null) return container != null ? new View(container.getContext()) : null;
        } catch (Throwable t) {
            Log.e(TAG, "onCreateView inflate", t);
            return container != null ? new View(container.getContext()) : null;
        }

        // Sampark: YouTube, Facebook, WhatsApp, Telegram – website (swamisachchidanandji.org) links
        bindSocialLink(view, R.id.sampark_youtube, getString(R.string.url_youtube));
        bindSocialLink(view, R.id.sampark_facebook, getString(R.string.url_facebook));
        bindSocialLink(view, R.id.sampark_whatsapp, getString(R.string.url_whatsapp));
        bindSocialLink(view, R.id.sampark_telegram, getString(R.string.url_telegram));

        // Ashram cards - bind each directly
        bindAshram(view, R.id.ashram1, "શ્રી ભક્તિ નિકેતન આશ્રમ, દંતાલી",
                "પ.પૂ.મહર્ષિ સ્વામી શ્રીસચ્ચિદાનંદજી પરમહંસ (પદ્મભૂષણશ્રી)",
                "પેટલાદ જી. આણંદ, ગુજરાત – ૩૮૮૪૫૦",
                "9428013551, 9824112625", "swamisachchidanandji.org",
                "https://maps.app.goo.gl/NEmwiYD8SiUvcMs49", R.drawable.bhakti_niketan_ashram);

        bindAshram(view, R.id.ashram2, "સાધનાશ્રમ, કોબા",
                "સચ્ચિદાનંદ સેવા સમાજ ટ્રસ્ટ, કોબા",
                "કોબા હાઇવે, કોબા, જી. ગાંધીનગર, ગુજરાત – ૩૮૨૪૨૬",
                "8238142042", "swamisachchidanandji.org",
                "https://maps.app.goo.gl/hF8P8aTpqA1mhK9C9", R.drawable.sadhana_ashram);

        bindAshram(view, R.id.ashram3, "વૃધ્ધાશ્રમ, ઊંઝા",
                "શ્રી સચ્ચિદાનંદ સેવા સમાજ ટ્રસ્ટ ઊંઝા",
                "ઊંઝા, જી. મહેસાણા, ગુજરાત – ૩૮૪૧૭૦",
                "9879104099", "swamisachchidanandji.org",
                "https://maps.app.goo.gl/6UFWKWH9DfVVBFhA8", R.drawable.vridhashram);

        bindAshram(view, R.id.ashram4, "સુઈગામ",
                "સ્વામી શ્રી સચ્ચિદાનંદજી સેવા સમાજ ટ્રસ્ટ, સુઈગામ",
                "સુઈગામ, જી. બનાસકાંઠા, ગુજરાત – ૩૮૫૫૭૦",
                "", "swamisachchidanandji.org",
                "https://maps.app.goo.gl/mLbBT1F5i2vjSMPCA", 0);

        View scrollView = view.findViewById(R.id.about_scroll_view);
        if (scrollView != null && getActivity() instanceof MainActivity) {
            scrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).onScrolled(scrollY - oldScrollY);
            });
        }

        return view;
    }

    private void bindSocialLink(View root, int viewId, String url) {
        View v = root.findViewById(viewId);
        if (v != null && url != null && !url.isEmpty()) {
            v.setOnClickListener(v1 -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (Exception e) {
                    Log.e(TAG, "Open link", e);
                }
            });
        }
    }

    private void bindAshram(View root, int cardId, String title, String desc, String address,
                            String phone, String website, String mapUrl, int thumbResId) {
        View card = root.findViewById(cardId);
        if (card == null) return;
        TextView t = card.findViewById(R.id.ashram_title);
        TextView d = card.findViewById(R.id.ashram_desc);
        TextView a = card.findViewById(R.id.ashram_address);
        TextView p = card.findViewById(R.id.ashram_phone);
        TextView w = card.findViewById(R.id.ashram_website);
        TextView m = card.findViewById(R.id.ashram_map);
        ImageView img = card.findViewById(R.id.ashram_thumbnail);
        View phoneRow = card.findViewById(R.id.ashram_phone_row);
        View websiteRow = card.findViewById(R.id.ashram_website_row);
        View mapRow = card.findViewById(R.id.ashram_map_row);

        if (t != null) t.setText(title);
        if (d != null) { d.setText(desc); d.setVisibility(desc != null && !desc.isEmpty() ? View.VISIBLE : View.GONE); }
        if (a != null) a.setText(address);
        if (p != null) p.setText(phone);
        if (w != null) w.setText(website);
        if (m != null) m.setText(mapUrl);
        if (phoneRow != null) phoneRow.setVisibility(phone != null && !phone.isEmpty() ? View.VISIBLE : View.GONE);
        if (websiteRow != null) websiteRow.setVisibility(website != null && !website.isEmpty() ? View.VISIBLE : View.GONE);
        if (mapRow != null) mapRow.setVisibility(mapUrl != null && !mapUrl.isEmpty() ? View.VISIBLE : View.GONE);
        if (img != null) {
            if (thumbResId != 0) {
                img.setImageResource(thumbResId);
                img.setVisibility(View.VISIBLE);
            } else {
                img.setVisibility(View.GONE);
            }
        }

        if (p != null && phone != null && !phone.isEmpty()) {
            p.setOnClickListener(v -> {
                String tel = phone.replaceAll("[^0-9+]", "").split(",")[0].trim();
                if (!tel.startsWith("+")) tel = "+91" + tel;
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + tel)));
            });
        }
        if (w != null && website != null && !website.isEmpty()) {
            w.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(website.startsWith("http") ? website : "https://" + website))));
        }
        if (m != null && mapUrl != null && !mapUrl.isEmpty()) {
            m.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mapUrl))));
        }
    }
}

