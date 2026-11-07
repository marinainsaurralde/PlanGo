package com.marinainsaurralde.plango.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public final class EventReminder {
    public enum Option { DAY_BEFORE_18, SAME_DAY_09 }

    private final String uid;
    private final String eventId;
    private final String title;
    private final Option option;
    private final long triggerAtMillis;

    public EventReminder(String uid, String eventId, String title,
                         Option option, long triggerAtMillis) {
        this.uid = require(uid, "uid");
        this.eventId = require(eventId, "eventId");
        this.title = title == null || title.trim().isEmpty() ? "Plan&Go" : title.trim();
        this.option = option == null ? Option.DAY_BEFORE_18 : option;
        this.triggerAtMillis = triggerAtMillis;
    }

    public String getUid() { return uid; }
    public String getEventId() { return eventId; }
    public String getTitle() { return title; }
    public Option getOption() { return option; }
    public long getTriggerAtMillis() { return triggerAtMillis; }
    public int stableId() { return stableId(uid, eventId); }

    public static List<Option> availableOptions(Plan plan, Instant now) {
        List<Option> result = new ArrayList<>();
        for (Option option : Option.values()) {
            Long trigger = triggerMillis(plan, option);
            if (trigger != null && trigger > now.toEpochMilli()) result.add(option);
        }
        return result;
    }

    public static EventReminder create(String uid, Plan plan, Option option, Instant now) {
        Long trigger = triggerMillis(plan, option);
        String eventId = plan == null ? null : plan.getFavoriteDocumentId();
        if (trigger == null || trigger <= now.toEpochMilli() || eventId == null) return null;
        return new EventReminder(uid, eventId, plan.getTitulo(), option, trigger);
    }

    private static Long triggerMillis(Plan plan, Option option) {
        if (!(plan instanceof Evento) || plan.getTemporalKind() == TemporalKind.LUGAR_PERMANENTE) {
            return null;
        }
        LocalDate start = date(((Evento) plan).getFechaInicioParsed());
        if (start == null || option == null) return null;
        LocalDate triggerDate = option == Option.DAY_BEFORE_18 ? start.minusDays(1) : start;
        LocalTime triggerTime = option == Option.DAY_BEFORE_18
                ? LocalTime.of(18, 0) : LocalTime.of(9, 0);
        return ZonedDateTime.of(triggerDate, triggerTime, FiltroEventos.MADRID_ZONE)
                .toInstant().toEpochMilli();
    }

    private static LocalDate date(java.time.LocalDateTime value) {
        return value == null ? null : value.toLocalDate();
    }

    public String toJson() {
        try {
            return new JSONObject()
                    .put("uid", uid)
                    .put("eventId", eventId)
                    .put("title", title)
                    .put("option", option.name())
                    .put("triggerAtMillis", triggerAtMillis)
                    .toString();
        } catch (JSONException error) {
            throw new IllegalStateException("No se pudo serializar el recordatorio", error);
        }
    }

    public static EventReminder fromJson(String value) {
        if (value == null) return null;
        try {
            JSONObject json = new JSONObject(value);
            return new EventReminder(json.getString("uid"), json.getString("eventId"),
                    json.optString("title", "Plan&Go"),
                    Option.valueOf(json.getString("option")),
                    json.getLong("triggerAtMillis"));
        } catch (JSONException | IllegalArgumentException error) {
            return null;
        }
    }

    public static int stableId(String uid, String eventId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((require(uid, "uid") + "\u0000" + require(eventId, "eventId"))
                            .getBytes(StandardCharsets.UTF_8));
            int value = ((digest[0] & 0xff) << 24) | ((digest[1] & 0xff) << 16)
                    | ((digest[2] & 0xff) << 8) | (digest[3] & 0xff);
            return value & 0x7fffffff;
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String require(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name);
        return value.trim();
    }
}
