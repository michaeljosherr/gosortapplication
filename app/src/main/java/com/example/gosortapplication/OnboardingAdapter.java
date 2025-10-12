package com.example.gosortapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.VH> {

    public static class Page {
        public final String title;
        public final String subtitle;
        public Page(String t, String s) { title = t; subtitle = s; }
    }

    private final List<Page> pages;

    public OnboardingAdapter(List<Page> pages) { this.pages = pages; }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_onboarding_page, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Page p = pages.get(position);
        holder.tvTitle.setText(p.title);
        holder.tvSubtitle.setText(p.subtitle);
        // image is static for now (setup.png)
        holder.iv.setAlpha(0f);
        holder.iv.animate().alpha(1f).setDuration(500).start();
    }

    @Override
    public int getItemCount() { return pages.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView iv; TextView tvTitle, tvSubtitle;
        VH(@NonNull View itemView) {
            super(itemView);
            iv = itemView.findViewById(R.id.ivPageImage);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
        }
    }
}
