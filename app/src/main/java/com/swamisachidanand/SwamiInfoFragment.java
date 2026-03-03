package com.swamisachidanand;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * In-app profile page for Swami Sachchidanand.
 * Shows photo, short bio, karya (books) and honours – offline static content.
 */
public class SwamiInfoFragment extends Fragment {

    public SwamiInfoFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_swami_info, container, false);
    }
}

