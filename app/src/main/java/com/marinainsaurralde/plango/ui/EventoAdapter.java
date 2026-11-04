package com.marinainsaurralde.plango.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.marinainsaurralde.plango.R;
import com.marinainsaurralde.plango.model.Evento;

import java.util.ArrayList;
import java.util.List;

public class EventoAdapter extends RecyclerView.Adapter<EventoAdapter.EventoViewHolder> {

    public interface OnEventoClickListener {
        void onEventoClick(Evento evento);
    }

    private final List<Evento> eventos = new ArrayList<>();
    private final OnEventoClickListener listener;

    public EventoAdapter(OnEventoClickListener listener) {
        this.listener = listener;
    }

    public void setEventos(List<Evento> nuevosEventos) {
        eventos.clear();
        if (nuevosEventos != null) {
            eventos.addAll(nuevosEventos);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_evento, parent, false);
        return new EventoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventoViewHolder holder, int position) {
        holder.bind(eventos.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return eventos.size();
    }

    public static class EventoViewHolder extends RecyclerView.ViewHolder {

        private final TextView title;
        private final TextView date;
        private final TextView district;
        private final TextView freeBadge;

        EventoViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.textEventTitle);
            date = itemView.findViewById(R.id.textEventDate);
            district = itemView.findViewById(R.id.textEventDistrict);
            freeBadge = itemView.findViewById(R.id.textFreeBadge);
        }

        void bind(Evento evento, OnEventoClickListener listener) {
            title.setText(evento.getTitulo() == null
                    ? itemView.getContext().getString(R.string.event_without_title)
                    : evento.getTitulo());

            setOptionalText(date, evento.getFechaInicioFormateada());
            setOptionalText(district, evento.getDistrito());
            freeBadge.setVisibility(evento.isGratuito() ? View.VISIBLE : View.GONE);
            itemView.setOnClickListener(view -> listener.onEventoClick(evento));
        }

        private void setOptionalText(TextView view, String value) {
            boolean hasValue = value != null && !value.trim().isEmpty();
            view.setVisibility(hasValue ? View.VISIBLE : View.GONE);
            if (hasValue) {
                view.setText(value);
            }
        }
    }
}
