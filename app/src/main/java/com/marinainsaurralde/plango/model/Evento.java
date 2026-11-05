package com.marinainsaurralde.plango.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class Evento extends Plan {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter API_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S", Locale.ROOT);
    private static final DateTimeFormatter DISPLAY_DATE_FORMAT =
            DateTimeFormatter.ofPattern("d MMM yyyy", new Locale("es", "ES"));

    private final String fechaInicio;
    private final String fechaFin;
    private final String tipo;
    private final boolean gratuito;
    private final boolean gratuidadInformada;
    private final String diasSemana;
    private final String diasExcluidos;
    private final String hora;
    private final String precio;

    public Evento(
            String identificador,
            String titulo,
            String fechaInicio,
            String fechaFin,
            String distrito,
            String tipo,
            boolean gratuito,
            Double latitud,
            Double longitud,
            String nombreInstalacion,
            String diasSemana,
            String diasExcluidos
    ) {
        this(identificador, titulo, fechaInicio, fechaFin, distrito, tipo, gratuito, true,
                latitud, longitud, nombreInstalacion, diasSemana, diasExcluidos,
                null, null, null);
    }

    public Evento(
            String identificador,
            String titulo,
            String fechaInicio,
            String fechaFin,
            String distrito,
            String tipo,
            boolean gratuito,
            Double latitud,
            Double longitud,
            String nombreInstalacion,
            String diasSemana,
            String diasExcluidos,
            String hora,
            String precio,
            String urlOficial
    ) {
        this(identificador, titulo, fechaInicio, fechaFin, distrito, tipo, gratuito, true,
                latitud, longitud, nombreInstalacion, diasSemana, diasExcluidos,
                hora, precio, urlOficial);
    }

    public Evento(
            String identificador,
            String titulo,
            String fechaInicio,
            String fechaFin,
            String distrito,
            String tipo,
            boolean gratuito,
            boolean gratuidadInformada,
            Double latitud,
            Double longitud,
            String nombreInstalacion,
            String diasSemana,
            String diasExcluidos,
            String hora,
            String precio,
            String urlOficial
    ) {
        this(defaultPlan(identificador, titulo, distrito, tipo, gratuito, gratuidadInformada,
                        latitud, longitud, nombreInstalacion, urlOficial),
                fechaInicio, fechaFin, tipo, gratuito, gratuidadInformada,
                diasSemana, diasExcluidos, hora, precio);
    }

    public Evento(
            Plan.Builder plan,
            String fechaInicio,
            String fechaFin,
            String tipo,
            boolean gratuito,
            boolean gratuidadInformada,
            String diasSemana,
            String diasExcluidos,
            String hora,
            String precio
    ) {
        super(plan);
        this.fechaInicio = normalize(fechaInicio);
        this.fechaFin = normalize(fechaFin);
        this.tipo = normalize(tipo);
        this.gratuito = gratuito;
        this.gratuidadInformada = gratuidadInformada;
        this.diasSemana = normalize(diasSemana);
        this.diasExcluidos = normalize(diasExcluidos);
        this.hora = normalize(hora);
        this.precio = normalize(precio);
    }

    private static Plan.Builder defaultPlan(
            String identificador,
            String titulo,
            String distrito,
            String tipo,
            boolean gratuito,
            boolean gratuidadInformada,
            Double latitud,
            Double longitud,
            String nombreInstalacion,
            String urlOficial
    ) {
        return Plan.builder(PlanIdentity.EVENTO_MADRID, identificador, titulo)
                .planType(PlanType.CULTURA)
                .temporalKind(TemporalKind.EVENTO)
                .categoryKey(tipo)
                .priceStatus(priceStatus(gratuito, gratuidadInformada))
                .distrito(distrito)
                .coordinates(latitud, longitud)
                .nombreInstalacion(nombreInstalacion)
                .urlOficial(urlOficial);
    }

    private static PriceStatus priceStatus(boolean gratuito, boolean informed) {
        if (!informed) return PriceStatus.DESCONOCIDO;
        return gratuito ? PriceStatus.GRATIS : PriceStatus.DE_PAGO;
    }

    public String getFechaInicio() { return fechaInicio; }
    public String getFechaFin() { return fechaFin; }
    public String getTipo() { return tipo; }
    public boolean isGratuito() { return gratuito; }
    public boolean isGratuidadInformada() { return gratuidadInformada; }
    public String getDiasSemana() { return diasSemana; }
    public String getDiasExcluidos() { return diasExcluidos; }
    public String getHora() { return hora; }
    public String getPrecio() { return precio; }

    public LocalDateTime getFechaInicioParsed() { return parseDate(fechaInicio); }
    public LocalDateTime getFechaFinParsed() { return parseDate(fechaFin); }
    public String getFechaInicioFormateada() { return formatDate(fechaInicio); }
    public String getFechaFinFormateada() { return formatDate(fechaFin); }

    public String getPeriodoFormateado() {
        LocalDate start = toLocalDate(getFechaInicioParsed());
        LocalDate end = toLocalDate(getFechaFinParsed());
        if (start == null && end == null) return null;
        if (start == null) return formatLocalDate(end);
        if (end == null || start.equals(end)) return formatLocalDate(start);
        return formatLocalDate(start) + " – " + formatLocalDate(end);
    }

    public boolean isEnCurso(ZoneId zoneId) { return isEnCurso(LocalDate.now(zoneId)); }

    public boolean isEnCurso(LocalDate today) {
        LocalDate start = toLocalDate(getFechaInicioParsed());
        LocalDate end = toLocalDate(getFechaFinParsed());
        return start != null && end != null && start.isBefore(today) && !end.isBefore(today);
    }

    public boolean isFinalizado(ZoneId zoneId) {
        LocalDate end = toLocalDate(getFechaFinParsed());
        return end != null && end.isBefore(LocalDate.now(zoneId));
    }

    private static String formatDate(String value) {
        if (value == null) return null;
        try {
            return LocalDateTime.parse(value, API_DATE_FORMAT).format(DISPLAY_DATE_FORMAT);
        } catch (DateTimeParseException ignored) {
            return value;
        }
    }

    private static LocalDateTime parseDate(String value) {
        if (value == null) return null;
        try {
            return LocalDateTime.parse(value, API_DATE_FORMAT);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static LocalDate toLocalDate(LocalDateTime value) {
        return value == null ? null : value.toLocalDate();
    }

    private static String formatLocalDate(LocalDate value) {
        return value == null ? null : value.format(DISPLAY_DATE_FORMAT);
    }
}
