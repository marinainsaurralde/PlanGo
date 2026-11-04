package com.marinainsaurralde.plango.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class Evento implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter API_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S", Locale.ROOT);
    private static final DateTimeFormatter DISPLAY_DATE_FORMAT =
            DateTimeFormatter.ofPattern("d MMM yyyy", new Locale("es", "ES"));

    private final String identificador;
    private final String titulo;
    private final String fechaInicio;
    private final String fechaFin;
    private final String distrito;
    private final String tipo;
    private final boolean gratuito;
    private final Double latitud;
    private final Double longitud;
    private final String nombreInstalacion;
    private final String diasSemana;
    private final String diasExcluidos;

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
        this.identificador = identificador;
        this.titulo = titulo;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.distrito = distrito;
        this.tipo = tipo;
        this.gratuito = gratuito;
        this.latitud = latitud;
        this.longitud = longitud;
        this.nombreInstalacion = nombreInstalacion;
        this.diasSemana = diasSemana;
        this.diasExcluidos = diasExcluidos;
    }

    public String getIdentificador() {
        return identificador;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getFechaInicio() {
        return fechaInicio;
    }

    public String getFechaFin() {
        return fechaFin;
    }

    public String getDistrito() {
        return distrito;
    }

    public String getTipo() {
        return tipo;
    }

    public boolean isGratuito() {
        return gratuito;
    }

    public Double getLatitud() {
        return latitud;
    }

    public Double getLongitud() {
        return longitud;
    }

    public String getNombreInstalacion() {
        return nombreInstalacion;
    }

    public String getDiasSemana() {
        return diasSemana;
    }

    public String getDiasExcluidos() {
        return diasExcluidos;
    }

    public LocalDateTime getFechaInicioParsed() {
        return parseDate(fechaInicio);
    }

    public LocalDateTime getFechaFinParsed() {
        return parseDate(fechaFin);
    }

    public boolean hasValidCoordinates() {
        return latitud != null
                && longitud != null
                && latitud >= -90
                && latitud <= 90
                && longitud >= -180
                && longitud <= 180;
    }

    public String getFechaInicioFormateada() {
        return formatDate(fechaInicio);
    }

    public String getFechaFinFormateada() {
        return formatDate(fechaFin);
    }

    public String getPeriodoFormateado() {
        LocalDate start = toLocalDate(getFechaInicioParsed());
        LocalDate end = toLocalDate(getFechaFinParsed());
        if (start == null && end == null) {
            return null;
        }
        if (start == null) {
            return formatLocalDate(end);
        }
        if (end == null || start.equals(end)) {
            return formatLocalDate(start);
        }
        return formatLocalDate(start) + " – " + formatLocalDate(end);
    }

    public boolean isEnCurso(ZoneId zoneId) {
        return isEnCurso(LocalDate.now(zoneId));
    }

    public boolean isEnCurso(LocalDate today) {
        LocalDate start = toLocalDate(getFechaInicioParsed());
        LocalDate end = toLocalDate(getFechaFinParsed());
        if (start == null || end == null) {
            return false;
        }
        return start.isBefore(today) && !end.isBefore(today);
    }

    private static String formatDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, API_DATE_FORMAT).format(DISPLAY_DATE_FORMAT);
        } catch (DateTimeParseException ignored) {
            return value;
        }
    }

    private static LocalDateTime parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
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
