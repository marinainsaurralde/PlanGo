package com.marinainsaurralde.plango.data;

import com.marinainsaurralde.plango.model.Evento;
import com.marinainsaurralde.plango.model.FiltroEventos;
import com.marinainsaurralde.plango.model.Plan;
import com.marinainsaurralde.plango.model.TemporalKind;

import java.text.Collator;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Orden temporal local de favoritos; no modifica la colección del repositorio. */
public final class MisPlanesSorter {
    private MisPlanesSorter() { }

    public static List<Plan> ordenar(List<? extends Plan> plans) {
        return ordenar(plans, Instant.now());
    }

    public static List<Plan> ordenar(List<? extends Plan> plans, Instant referenceInstant) {
        List<Plan> result = new ArrayList<>();
        if (plans == null || plans.isEmpty()) return result;
        result.addAll(plans);
        LocalDate today = referenceInstant.atZone(FiltroEventos.MADRID_ZONE).toLocalDate();
        Collator collator = Collator.getInstance(new Locale("es", "ES"));
        collator.setStrength(Collator.PRIMARY);
        Comparator<String> titleComparator = Comparator.nullsLast(collator::compare);
        result.sort((left, right) -> compare(left, right, today, titleComparator));
        return result;
    }

    private static int compare(Plan left, Plan right, LocalDate today,
                               Comparator<String> titleComparator) {
        SortValue first = value(left, today);
        SortValue second = value(right, today);
        int group = Integer.compare(first.group, second.group);
        if (group != 0) return group;
        int date = compareDates(first, second);
        if (date != 0) return date;
        return titleComparator.compare(left == null ? null : left.getTitulo(),
                right == null ? null : right.getTitulo());
    }

    private static int compareDates(SortValue first, SortValue second) {
        if (first.date == null || second.date == null) return 0;
        int value = first.date.compareTo(second.date);
        return first.descending ? -value : value;
    }

    private static SortValue value(Plan plan, LocalDate today) {
        if (plan != null && plan.getTemporalKind() == TemporalKind.LUGAR_PERMANENTE) {
            return new SortValue(5, null, false);
        }
        if (!(plan instanceof Evento)) return new SortValue(4, null, false);
        Evento event = (Evento) plan;
        LocalDate start = date(event.getFechaInicioParsed());
        LocalDate end = date(event.getFechaFinParsed());
        if (start == null) return new SortValue(4, null, false);
        if (end != null && end.isBefore(start)) return new SortValue(4, null, false);
        if (end == null) {
            // Sin fin solo puede afirmarse que es próximo; no se inventa una duración.
            return start.isAfter(today)
                    ? new SortValue(2, start, false)
                    : new SortValue(4, null, false);
        }
        if (end.isBefore(today)) return new SortValue(3, end, true);
        if (start.isAfter(today)) return new SortValue(2, start, false);
        return new SortValue(1, end, false);
    }

    private static LocalDate date(LocalDateTime value) {
        return value == null ? null : value.toLocalDate();
    }

    private static final class SortValue {
        private final int group;
        private final LocalDate date;
        private final boolean descending;

        private SortValue(int group, LocalDate date, boolean descending) {
            this.group = group;
            this.date = date;
            this.descending = descending;
        }
    }
}
