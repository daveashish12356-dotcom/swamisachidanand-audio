package com.swamisachidanand;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Audio Pravachan tab – lists pravachan entries from Firebase Firestore.
 */
public class AudioPravachanFragment extends Fragment implements PravachanAdapter.Listener {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private PravachanAdapter adapter;
    private ListenerRegistration registration;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_audio_pravachan_empty, container, false);
        recyclerView = root.findViewById(R.id.pravachan_recycler);
        progressBar = root.findViewById(R.id.pravachan_progress);
        emptyView = root.findViewById(R.id.pravachan_empty);

        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            adapter = new PravachanAdapter();
            adapter.setListener(this);
            recyclerView.setAdapter(adapter);
        }

        loadFromFirestore();

        return root;
    }

    private void loadFromFirestore() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (emptyView != null) emptyView.setVisibility(View.GONE);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (registration != null) {
            registration.remove();
            registration = null;
        }
        registration = db.collection("pravachan")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                            if (emptyView != null) {
                                emptyView.setText("પ્રવચન લોડ થઈ શક્યાં નહીં.");
                                emptyView.setVisibility(View.VISIBLE);
                            }
                            return;
                        }
                        onLoaded(value);
                    }
                });
    }

    private void onLoaded(QuerySnapshot snap) {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        List<PravachanItem> list = new ArrayList<>();
        java.util.HashSet<String> seenUrls = new java.util.HashSet<>();
        if (snap != null) {
            for (DocumentSnapshot d : snap.getDocuments()) {
                String id = d.getId();
                String title = d.getString("title");
                String url = d.getString("audioUrl");
                String speaker = d.getString("speaker");
                long createdAt = 0L;
                com.google.firebase.Timestamp ts = d.getTimestamp("createdAt");
                if (ts != null) createdAt = ts.toDate().getTime();
                if (title == null || url == null || title.trim().isEmpty() || url.trim().isEmpty()) {
                    continue;
                }
                String cleanTitle = title.trim();
                String cleanUrl = url.trim();

                // Skip test / old entries like "aud 2026 ..."
                String lowerTitle = cleanTitle.toLowerCase();
                if (lowerTitle.startsWith("aud 2026") || lowerTitle.startsWith("aud_2026")) {
                    continue;
                }

                // Avoid visible duplicates: only first item for each audioUrl
                if (seenUrls.contains(cleanUrl)) continue;
                seenUrls.add(cleanUrl);

                list.add(new PravachanItem(id, cleanTitle, cleanUrl, speaker, createdAt));
            }
        }
        if (adapter != null) adapter.setItems(list);
        if (emptyView != null) emptyView.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onPravachanClick(@NonNull PravachanItem item) {
        // Open in in-app pravachan player fragment
        if (getActivity() == null) return;
        Fragment f = PravachanPlayerFragment.newInstance(item);
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.fragment_fade_in, R.anim.fragment_fade_out,
                        R.anim.fragment_fade_in, R.anim.fragment_fade_out)
                .replace(R.id.fragment_container, f)
                .addToBackStack("pravachan_player")
                .commit();
    }

    @Override
    public void onDestroyView() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
        super.onDestroyView();
    }
}

