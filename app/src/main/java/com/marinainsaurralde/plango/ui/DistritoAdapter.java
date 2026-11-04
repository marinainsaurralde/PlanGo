package com.marinainsaurralde.plango.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CheckedTextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.marinainsaurralde.plango.R;

import java.util.ArrayList;
import java.util.List;

public final class DistritoAdapter extends RecyclerView.Adapter<DistritoAdapter.ViewHolder> {

    public interface Listener {
        void onDistrictSelected(String district);
    }

    private final List<String> districts = new ArrayList<>();
    private final Listener listener;
    private String selected;

    public DistritoAdapter(String selected, Listener listener) {
        this.selected = selected;
        this.listener = listener;
    }

    public void submitList(List<String> values) {
        districts.clear();
        districts.addAll(values);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CheckedTextView view = (CheckedTextView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_district, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String district = districts.get(position);
        holder.text.setText(district);
        holder.text.setChecked(district.equals(selected));
        holder.text.setOnClickListener(view -> {
            selected = district;
            notifyDataSetChanged();
            listener.onDistrictSelected(district);
        });
    }

    @Override
    public int getItemCount() {
        return districts.size();
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        private final CheckedTextView text;

        ViewHolder(CheckedTextView text) {
            super(text);
            this.text = text;
        }
    }
}
