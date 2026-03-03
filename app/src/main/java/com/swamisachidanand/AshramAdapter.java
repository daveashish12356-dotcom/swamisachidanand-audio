package com.swamisachidanand;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AshramAdapter extends RecyclerView.Adapter<AshramAdapter.AshramViewHolder> {

    private final List<AshramItem> items = new ArrayList<>();

    void setItems(List<AshramItem> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AshramViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ashram, parent, false);
        return new AshramViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AshramViewHolder h, int position) {
        AshramItem item = items.get(position);
        h.title.setText(item.title);
        h.desc.setText(item.desc);
        h.desc.setVisibility(item.desc != null && !item.desc.isEmpty() ? View.VISIBLE : View.GONE);
        h.address.setText(item.address);
        h.phone.setText(item.phone);
        h.website.setText(item.website);
        h.map.setText(item.mapUrl != null ? item.mapUrl : "");

        h.phoneRow.setVisibility(item.phone != null && !item.phone.isEmpty() ? View.VISIBLE : View.GONE);
        h.websiteRow.setVisibility(item.website != null && !item.website.isEmpty() ? View.VISIBLE : View.GONE);
        h.mapRow.setVisibility(item.mapUrl != null && !item.mapUrl.isEmpty() ? View.VISIBLE : View.GONE);

        if (item.thumbnailResId != 0) {
            h.thumbnail.setImageResource(item.thumbnailResId);
        } else {
            h.thumbnail.setImageResource(R.drawable.book_placeholder);
        }
        h.thumbnail.setVisibility(View.VISIBLE);

        h.phone.setOnClickListener(v -> {
            if (item.phone != null && !item.phone.isEmpty()) {
                String tel = item.phone.replaceAll("[^0-9+]", "");
                if (!tel.startsWith("+")) tel = "+91" + tel;
                v.getContext().startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + tel)));
            }
        });

        h.website.setOnClickListener(v -> {
            if (item.website != null && !item.website.isEmpty()) {
                String url = item.website.startsWith("http") ? item.website : "https://" + item.website;
                v.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            }
        });

        h.map.setOnClickListener(v -> {
            if (item.mapUrl != null && !item.mapUrl.isEmpty()) {
                v.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(item.mapUrl)));
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class AshramViewHolder extends RecyclerView.ViewHolder {
        final TextView title, desc, address, phone, website, map;
        final ImageView thumbnail;
        final LinearLayout phoneRow, websiteRow, mapRow;

        AshramViewHolder(View v) {
            super(v);
            title = v.findViewById(R.id.ashram_title);
            desc = v.findViewById(R.id.ashram_desc);
            address = v.findViewById(R.id.ashram_address);
            phone = v.findViewById(R.id.ashram_phone);
            website = v.findViewById(R.id.ashram_website);
            map = v.findViewById(R.id.ashram_map);
            thumbnail = v.findViewById(R.id.ashram_thumbnail);
            phoneRow = v.findViewById(R.id.ashram_phone_row);
            websiteRow = v.findViewById(R.id.ashram_website_row);
            mapRow = v.findViewById(R.id.ashram_map_row);
        }
    }
}
