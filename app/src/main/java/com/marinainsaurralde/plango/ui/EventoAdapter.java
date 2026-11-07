package com.marinainsaurralde.plango.ui;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.marinainsaurralde.plango.R;
import com.marinainsaurralde.plango.model.Evento;
import com.marinainsaurralde.plango.model.FiltroEventos;
import com.marinainsaurralde.plango.model.Plan;
import com.marinainsaurralde.plango.model.PriceStatus;
import com.marinainsaurralde.plango.model.TemporalKind;
import com.marinainsaurralde.plango.model.PlanType;
import com.marinainsaurralde.plango.model.CategoriaTuristica;
import com.marinainsaurralde.plango.model.EventReminder;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Set;
import java.time.Instant;

public class EventoAdapter extends RecyclerView.Adapter<EventoAdapter.EventoViewHolder> {

    public interface OnEventoClickListener {
        void onEventoClick(Plan evento);
    }

    public interface OnFavoritoClickListener {
        void onFavoritoClick(Plan evento);
    }

    public interface OnReminderClickListener {
        void onReminderClick(Plan evento);
    }

    private final List<Plan> eventos = new ArrayList<>();
    private final OnEventoClickListener listener;
    private final OnFavoritoClickListener favoriteListener;
    private final OnReminderClickListener reminderListener;
    private Set<String> favoriteIds = Collections.emptySet();
    private Set<String> pendingIds = Collections.emptySet();
    private Set<String> reminderIds = Collections.emptySet();
    private boolean showFinished;

    public EventoAdapter(OnEventoClickListener listener) {
        this(listener, null);
    }

    public EventoAdapter(OnEventoClickListener listener, OnFavoritoClickListener favoriteListener) {
        this(listener, favoriteListener, null);
    }

    public EventoAdapter(OnEventoClickListener listener, OnFavoritoClickListener favoriteListener,
                         OnReminderClickListener reminderListener) {
        this.listener = listener;
        this.favoriteListener = favoriteListener;
        this.reminderListener = reminderListener;
    }

    public void setReminderState(Set<String> reminders) {
        reminderIds = reminders == null ? Collections.emptySet() : reminders;
        notifyDataSetChanged();
    }

    public void setFavoriteState(Set<String> favorites, Set<String> pending) {
        favoriteIds = favorites == null ? Collections.emptySet() : favorites;
        pendingIds = pending == null ? Collections.emptySet() : pending;
        notifyDataSetChanged();
    }

    public void setShowFinished(boolean showFinished) {
        this.showFinished = showFinished;
        notifyDataSetChanged();
    }

    public void setEventos(List<? extends Plan> nuevosEventos) {
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
        holder.bind(eventos.get(position), listener, favoriteListener,
                reminderListener, favoriteIds, pendingIds, reminderIds, showFinished);
    }

    @Override
    public int getItemCount() {
        return eventos.size();
    }

    public static class EventoViewHolder extends RecyclerView.ViewHolder {

        private final TextView title;
        private final TextView date;
        private final TextView district;
        private final TextView schedule;
        private final TextView categories;
        private final TextView priceInfo;
        private final TextView source;
        private final TextView freeBadge;
        private final TextView ongoingBadge;
        private final TextView finishedBadge;
        private final MaterialButton favoriteButton;
        private final MaterialButton reminderButton;

        EventoViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.textEventTitle);
            date = itemView.findViewById(R.id.textEventDate);
            district = itemView.findViewById(R.id.textEventDistrict);
            schedule = itemView.findViewById(R.id.textEventSchedule);
            categories = itemView.findViewById(R.id.textEventCategories);
            priceInfo = itemView.findViewById(R.id.textEventPriceInfo);
            source = itemView.findViewById(R.id.textEventSource);
            freeBadge = itemView.findViewById(R.id.textFreeBadge);
            ongoingBadge = itemView.findViewById(R.id.textOngoingBadge);
            finishedBadge = itemView.findViewById(R.id.textFinishedBadge);
            favoriteButton = itemView.findViewById(R.id.buttonFavorite);
            reminderButton = itemView.findViewById(R.id.buttonReminder);
        }

        void bind(
                Plan evento,
                OnEventoClickListener listener,
                OnFavoritoClickListener favoriteListener,
                OnReminderClickListener reminderListener,
                Set<String> favoriteIds,
                Set<String> pendingIds,
                Set<String> reminderIds,
                boolean showFinished
        ) {
            title.setText(evento.getTitulo() == null
                    ? itemView.getContext().getString(R.string.event_without_title)
                    : evento.getTitulo());

            Evento temporalEvent = evento instanceof Evento ? (Evento) evento : null;
            setOptionalText(date, evento.getTemporalKind() == TemporalKind.LUGAR_PERMANENTE
                    ? itemView.getContext().getString(R.string.permanent_place)
                    : temporalEvent == null ? null : temporalEvent.getPeriodoFormateado());
            setOptionalText(district, evento.getAddress() == null
                    ? evento.getDistrito()
                    : evento.getDistrito() == null ? evento.getAddress()
                    : evento.getAddress() + " · " + evento.getDistrito());
            setOptionalText(schedule, evento.getSchedule());
            List<String> tourismCategories=CategoriaTuristica.labelsFor(evento.getCategoryKeys());
            setOptionalText(categories,tourismCategories.isEmpty()?null:String.join(" · ",tourismCategories));
            setOptionalText(priceInfo,evento.getPriceStatus()==PriceStatus.DESCONOCIDO
                    ?null:evento.getPriceInfo());
            boolean tourism=evento.getPlanTypes().contains(PlanType.TURISMO);
            source.setVisibility(tourism?View.VISIBLE:View.GONE);
            if(tourism)source.setText(evento.getLastSourceUpdate()==null
                    ?itemView.getContext().getString(R.string.tourism_attribution)
                    :itemView.getContext().getString(R.string.tourism_source_updated,evento.getLastSourceUpdate()));
            freeBadge.setVisibility(evento.getPriceStatus() == PriceStatus.GRATIS ? View.VISIBLE : View.GONE);
            ongoingBadge.setVisibility(temporalEvent != null && temporalEvent.isEnCurso(FiltroEventos.MADRID_ZONE)
                    ? View.VISIBLE
                    : View.GONE);
            finishedBadge.setVisibility(showFinished
                    && temporalEvent != null && temporalEvent.isFinalizado(FiltroEventos.MADRID_ZONE)
                    ? View.VISIBLE
                    : View.GONE);
            String favoriteId = evento.getFavoriteDocumentId();
            boolean hasId = favoriteId != null;
            boolean favorite = hasId && favoriteIds.contains(favoriteId);
            boolean pending = hasId && pendingIds.contains(favoriteId);
            favoriteButton.setVisibility(favoriteListener == null ? View.GONE : View.VISIBLE);
            favoriteButton.setIconResource(favorite
                    ? R.drawable.ic_favorite_filled_24
                    : R.drawable.ic_favorite_border_24);
            favoriteButton.setContentDescription(itemView.getContext().getString(
                    favorite ? R.string.remove_from_plans : R.string.add_to_plans));
            favoriteButton.setEnabled(hasId && !pending);
            favoriteButton.setOnClickListener(view -> {
                if (favoriteListener != null) {
                    favoriteListener.onFavoritoClick(evento);
                }
            });
            boolean reminderActive = hasId && reminderIds.contains(favoriteId);
            boolean canCreateReminder = !EventReminder.availableOptions(evento, Instant.now()).isEmpty();
            reminderButton.setVisibility(reminderListener != null
                    && (reminderActive || canCreateReminder) ? View.VISIBLE : View.GONE);
            reminderButton.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(
                    itemView.getContext(), reminderActive
                            ? R.color.plango_coral : R.color.plango_text_secondary)));
            reminderButton.setContentDescription(itemView.getContext().getString(
                    reminderActive ? R.string.reminder_active : R.string.reminder_add));
            reminderButton.setOnClickListener(view -> {
                if (reminderListener != null) reminderListener.onReminderClick(evento);
            });
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
