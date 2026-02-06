package com.swamisachidanand;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;

import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.io.InputStream;

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
        ImageView swamijiPhoto = view.findViewById(R.id.swamiji_photo);
        TextView swamijiName = view.findViewById(R.id.swamiji_name);
        TextView aboutText = view.findViewById(R.id.about_text);
        TextView ashram1Text = view.findViewById(R.id.ashram1_text);
        TextView ashram2Text = view.findViewById(R.id.ashram2_text);
        TextView ashram3Text = view.findViewById(R.id.ashram3_text);
        if (swamijiName != null) swamijiName.setText("પદ્મભૂષણ શ્રી સ્વામી સચ્ચિદાનંદ");
        
        // Load photo from assets if available
        try {
            android.app.Activity act = getActivity();
            if (act == null) return view;
            String[] imageFiles = {"swamiji.jpg", "Swami_Sachchidanand.jpg", "swamiji.png", "swamiji_photo.jpg", "swamiji_photo.png"};
            boolean photoLoaded = false;

            for (String imageFile : imageFiles) {
                try {
                    InputStream is = act.getAssets().open(imageFile);
                    Bitmap bitmap = BitmapFactory.decodeStream(is);
                    if (bitmap != null && swamijiPhoto != null) {
                        swamijiPhoto.setImageBitmap(bitmap);
                        photoLoaded = true;
                        is.close();
                        Log.d(TAG, "Photo loaded successfully: " + imageFile);
                        break;
                    }
                    is.close();
                } catch (IOException e) {
                    Log.d(TAG, "Trying next photo file, current: " + imageFile);
                }
            }
            
            if (!photoLoaded && swamijiPhoto != null) {
                swamijiPhoto.setImageResource(android.R.drawable.sym_def_app_icon);
                Log.d(TAG, "Swamiji photo not found in assets, using default icon");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading photo", e);
            if (swamijiPhoto != null) swamijiPhoto.setImageResource(android.R.drawable.sym_def_app_icon);
        }
        
        // Set Swamiji information from Wikipedia
        String aboutContent = "પદ્મભૂષણ શ્રી સ્વામી સચ્ચિદાનંદ (જન્મ: નાનાલાલ મોતીલાલ ત્રિવેદી, ૧૯૩૨) એક પ્રખ્યાત ગુજરાતી સંત, લેખક અને આધ્યાત્મિક ગુરુ છે.\n\n" +
                "તેમના સર્જનમાં ભક્તિ સાહિત્ય, ઉપદેશ, યાત્રા વર્ણન અને આધ્યાત્મિક ચિંતનનો સમાવેશ થાય છે. તેઓ ભક્તિ, ઉપદેશ, ધાર્મિક યાત્રાઓ અને આધ્યાત્મિક સાહિત્ય પર અનેક પુસ્તકો લખ્યા છે.\n\n" +
                "તેમના ઉપદેશો અને લેખનથી લાખો લોકોને આધ્યાત્મિક માર્ગદર્શન અને ભક્તિનો પથ મળ્યો છે. સ્વામી સચ્ચિદાનંદે ભારત સરકાર દ્વારા પદ્મભૂષણ પુરસ્કાર પ્રાપ્ત કર્યો છે અને ગુજરાત સરકાર દ્વારા નર્મદ સુવર્ણચંદ્રક પણ પ્રાપ્ત કર્યો છે.";
        
        if (aboutText != null) aboutText.setText(aboutContent);
        String ashram1 = "ભક્તિનિકેતન આશ્રમ\n" +
                         "પો.ઓ. નં. ૧૯, દંતાલી ૩૮૮૪૫૦\n" +
                         "તા. પેટલાદ, જિલ્લો: આણંદ\n" +
                         "ફોન: ૦૨૬૯૭-૨૫૨૪૮૦";
        
        ashram1Text.setText(ashram1);
        
        // Set Ashram 2 - સાધનાશ્રમ
        String ashram2 = "સાધનાશ્રમ\n" +
                         "કોબા-૩૮૨૦૦૯\n" +
                         "(જિ. ગાંધીનગર)\n" +
                         "ફોન: ૦૭૯-૨૩૨૭૬૨૨૬";
        
        if (ashram2Text != null) ashram2Text.setText(ashram2);
        String ashram3 = "વૃદ્ધાશ્રમ\n" +
                         "પાટણ રોડ,\n" +
                         "ઊંઝા ૩૮૪૧૭૦\n" +
                         "(જિ. મહેસાણા)\n" +
                         "ફોન: ૦૨૭૬૭-૨૫૪૪૮૩";
        
        if (ashram3Text != null) ashram3Text.setText(ashram3);
        return view;
    }
}

