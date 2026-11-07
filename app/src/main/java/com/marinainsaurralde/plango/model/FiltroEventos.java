package com.marinainsaurralde.plango.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public final class FiltroEventos {

    public static final ZoneId MADRID_ZONE = ZoneId.of("Europe/Madrid");
    private static final DateTimeFormatter EXCLUDED_DATE_FORMAT =
            DateTimeFormatter.ofPattern("d/M/uuuu", Locale.ROOT);

    public enum Periodo {
        TODOS,
        HOY,
        FIN_DE_SEMANA,
        PROXIMOS_7_DIAS
    }

    public static final class Criterios {
        private final Periodo periodo;
        private final boolean soloGratuitos;
        private final String distrito;
        private final PlanFilterSelection planSelection;

        public Criterios(Periodo periodo, boolean soloGratuitos, String distrito) {
            this(periodo, soloGratuitos, distrito, PlanFilterSelection.todosLosTipos());
        }

        public Criterios(Periodo periodo, boolean soloGratuitos, String distrito,
                         PlanFilterSelection planSelection) {
            this.periodo = periodo == null ? Periodo.HOY : periodo;
            this.soloGratuitos = soloGratuitos;
            this.distrito = normalize(distrito);
            this.planSelection = planSelection == null
                    ? PlanFilterSelection.culturaPorDefecto() : planSelection;
        }

        public static Criterios porDefecto() {
            return new Criterios(Periodo.HOY, false, null,
                    PlanFilterSelection.culturaPorDefecto());
        }

        public Periodo getPeriodo() {
            return periodo;
        }

        public boolean isSoloGratuitos() {
            return soloGratuitos;
        }

        public String getDistrito() {
            return distrito;
        }

        public PlanFilterSelection getPlanSelection() { return planSelection; }

    }

    private FiltroEventos() {
    }

    public static <T extends Plan> List<T> aplicar(List<T> eventos, Criterios criterios) {
        return aplicar(eventos, criterios, LocalDate.now(MADRID_ZONE));
    }

    public static <T extends Plan> List<T> aplicar(
            List<T> eventos,
            Criterios criterios,
            LocalDate hoyMadrid
    ) {
        if (eventos == null || eventos.isEmpty()) {
            return Collections.emptyList();
        }
        Criterios safeCriteria = criterios == null ? Criterios.porDefecto() : criterios;
        DateRange range = dateRange(safeCriteria.getPeriodo(), hoyMadrid);
        List<T> resultado = new ArrayList<>();
        for (T plan : eventos) {
            if (!matchesPlanSelection(plan, safeCriteria.getPlanSelection())) {
                continue;
            }
            if (safeCriteria.isSoloGratuitos() && plan.getPriceStatus() != PriceStatus.GRATIS) {
                continue;
            }
            if (safeCriteria.getDistrito() != null
                    && !safeCriteria.getDistrito().equals(normalize(plan.getDistrito()))) {
                continue;
            }
            if (range != null && plan.getTemporalKind() != TemporalKind.LUGAR_PERMANENTE
                    && (!(plan instanceof Evento)
                    || !hasOccurrenceInRange((Evento) plan, range.start, range.end))) {
                continue;
            }
            resultado.add(plan);
        }
        return resultado;
    }

    public static List<String> distritosDisponibles(List<? extends Plan> eventos) {
        Set<String> districts = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (eventos != null) {
            for (Plan evento : eventos) {
                String district = normalize(evento.getDistrito());
                if (district != null) {
                    districts.add(district);
                }
            }
        }
        return new ArrayList<>(districts);
    }

    public static List<CategoriaCultural> categoriasDisponibles(List<? extends Plan> eventos) {
        Set<CategoriaCultural> categories = EnumSet.noneOf(CategoriaCultural.class);
        if (eventos != null) for (Plan plan : eventos) {
            if (plan instanceof Evento && plan.getPlanTypes().contains(PlanType.CULTURA)) {
                categories.add(CategoriaCultural.clasificar((Evento) plan));
            }
        }
        return new ArrayList<>(categories);
    }

    public static String culturalCategoryKey(CategoriaCultural category) {
        return category == null ? null : "cultura:" + category.name();
    }

    private static boolean matchesPlanSelection(Plan plan, PlanFilterSelection selection) {
        if (plan == null || selection == null || selection.getSelectedTypes().isEmpty()) return false;
        for (PlanType type : selection.getSelectedTypes()) {
            if (!plan.getPlanTypes().contains(type)) continue;
            if (!selection.hasCategoryRestriction(type)) return true;
            Set<String> selectedKeys = selection.getSelectedCategoryKeys(type);
            if (type == PlanType.CULTURA && plan instanceof Evento) {
                if (selectedKeys.contains(culturalCategoryKey(
                        CategoriaCultural.clasificar((Evento) plan)))) return true;
            } else {
                for (String key : plan.getCategoryKeys()) {
                    if (selectedKeys.contains(key)) return true;
                }
            }
        }
        return false;
    }

    private static DateRange dateRange(Periodo periodo, LocalDate today) {
        switch (periodo) {
            case TODOS:
                return null;
            case HOY:
                return new DateRange(today, today);
            case FIN_DE_SEMANA:
                LocalDate saturday = today.getDayOfWeek() == DayOfWeek.SUNDAY
                        ? today.minusDays(1)
                        : today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
                return new DateRange(saturday, saturday.plusDays(1));
            case PROXIMOS_7_DIAS:
            default:
                return new DateRange(today, today.plusDays(6));
        }
    }

    private static boolean hasOccurrenceInRange(Evento evento, LocalDate rangeStart, LocalDate rangeEnd) {
        LocalDateTime parsedStart = evento.getFechaInicioParsed();
        if (parsedStart == null) {
            return false;
        }
        LocalDate eventStart = parsedStart.toLocalDate();
        LocalDateTime parsedEnd = evento.getFechaFinParsed();
        LocalDate eventEnd = parsedEnd == null ? eventStart : parsedEnd.toLocalDate();
        if (eventEnd.isBefore(eventStart)) {
            eventEnd = eventStart;
        }

        LocalDate overlapStart = eventStart.isAfter(rangeStart) ? eventStart : rangeStart;
        LocalDate overlapEnd = eventEnd.isBefore(rangeEnd) ? eventEnd : rangeEnd;
        if (overlapEnd.isBefore(overlapStart)) {
            return false;
        }

        Set<DayOfWeek> recurringDays = parseWeekdays(evento.getDiasSemana());
        Set<LocalDate> excludedDates = parseExcludedDates(evento.getDiasExcluidos());
        for (LocalDate date = overlapStart; !date.isAfter(overlapEnd); date = date.plusDays(1)) {
            if ((recurringDays == null || recurringDays.contains(date.getDayOfWeek()))
                    && !excludedDates.contains(date)) {
                return true;
            }
        }
        return false;
    }

    private static Set<DayOfWeek> parseWeekdays(String value) {
        if (normalize(value) == null) {
            return null;
        }
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        for (String token : value.split(",")) {
            switch (token.trim().toUpperCase(Locale.ROOT)) {
                case "L": days.add(DayOfWeek.MONDAY); break;
                case "M": days.add(DayOfWeek.TUESDAY); break;
                case "X": days.add(DayOfWeek.WEDNESDAY); break;
                case "J": days.add(DayOfWeek.THURSDAY); break;
                case "V": days.add(DayOfWeek.FRIDAY); break;
                case "S": days.add(DayOfWeek.SATURDAY); break;
                case "D": days.add(DayOfWeek.SUNDAY); break;
                default: break;
            }
        }
        return days;
    }

    private static Set<LocalDate> parseExcludedDates(String value) {
        if (normalize(value) == null) {
            return Collections.emptySet();
        }
        Set<LocalDate> dates = new HashSet<>();
        for (String token : value.split(";")) {
            try {
                dates.add(LocalDate.parse(token.trim(), EXCLUDED_DATE_FORMAT));
            } catch (DateTimeParseException ignored) {
                // La fuente contiene algunos fragmentos corruptos; no se infieren fechas.
            }
        }
        return dates;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static final class DateRange {
        private final LocalDate start;
        private final LocalDate end;

        private DateRange(LocalDate start, LocalDate end) {
            this.start = start;
            this.end = end;
        }
    }
}
