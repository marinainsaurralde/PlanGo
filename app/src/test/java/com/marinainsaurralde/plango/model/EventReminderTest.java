package com.marinainsaurralde.plango.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;

public final class EventReminderTest {
    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");

    @Test public void offersBothFutureOptions() {
        Evento event = event("1", "2026-11-10 00:00:00.0");
        Instant now = LocalDateTime.of(2026, 11, 8, 12, 0).atZone(MADRID).toInstant();
        assertEquals(Arrays.asList(EventReminder.Option.DAY_BEFORE_18,
                EventReminder.Option.SAME_DAY_09), EventReminder.availableOptions(event, now));
    }

    @Test public void apiMidnightDatesOnNovemberElevenAndThirteenOfferBothOptions() {
        Instant novemberSeventh = LocalDateTime.of(2026, 11, 7, 12, 0)
                .atZone(MADRID).toInstant();
        for (String start : Arrays.asList(
                "2026-11-11 00:00:00.0", "2026-11-13 00:00:00.0")) {
            assertEquals(Arrays.asList(EventReminder.Option.DAY_BEFORE_18,
                            EventReminder.Option.SAME_DAY_09),
                    EventReminder.availableOptions(event(start, start), novemberSeventh));
        }
    }

    @Test public void hidesOptionThatAlreadyPassed() {
        Evento event = event("1", "2026-11-10 00:00:00.0");
        Instant now = LocalDateTime.of(2026, 11, 9, 19, 0).atZone(MADRID).toInstant();
        assertEquals(Collections.singletonList(EventReminder.Option.SAME_DAY_09),
                EventReminder.availableOptions(event, now));
    }

    @Test public void offersNothingAfterBothTimesPassed() {
        Evento event = event("1", "2026-11-10 00:00:00.0");
        Instant now = LocalDateTime.of(2026, 11, 10, 9, 1).atZone(MADRID).toInstant();
        assertTrue(EventReminder.availableOptions(event, now).isEmpty());
    }

    @Test public void usesMadridZoneForTriggerAcrossDst() {
        Evento event = event("1", "2026-03-30 00:00:00.0");
        Instant now = Instant.parse("2026-03-28T00:00:00Z");
        EventReminder reminder = EventReminder.create("uid", event,
                EventReminder.Option.SAME_DAY_09, now);
        assertNotNull(reminder);
        assertEquals(LocalDateTime.of(2026, 3, 30, 9, 0).atZone(MADRID).toInstant(),
                Instant.ofEpochMilli(reminder.getTriggerAtMillis()));
    }

    @Test public void invalidDatesAndPermanentPlacesAreNotEligible() {
        Evento invalid = event("1", "fecha inválida");
        Plan park = new Lugar(Plan.builder(PlanIdentity.PARQUE_MADRID, "p1", "Parque")
                .planType(PlanType.NATURALEZA).temporalKind(TemporalKind.LUGAR_PERMANENTE));
        assertTrue(EventReminder.availableOptions(invalid, Instant.EPOCH).isEmpty());
        assertTrue(EventReminder.availableOptions(park, Instant.EPOCH).isEmpty());
        assertNull(EventReminder.create("uid", park, EventReminder.Option.SAME_DAY_09,
                Instant.EPOCH));
    }

    @Test public void persistenceRepresentationRoundTrips() {
        EventReminder original = new EventReminder("usuario-á", "evento:42", "Concierto",
                EventReminder.Option.DAY_BEFORE_18, 123456789L);
        EventReminder restored = EventReminder.fromJson(original.toJson());
        assertNotNull(restored);
        assertEquals(original.getUid(), restored.getUid());
        assertEquals(original.getEventId(), restored.getEventId());
        assertEquals(original.getTitle(), restored.getTitle());
        assertEquals(original.getOption(), restored.getOption());
        assertEquals(original.getTriggerAtMillis(), restored.getTriggerAtMillis());
        assertNull(EventReminder.fromJson("contenido corrupto"));
    }

    @Test public void stableIdsAreRepeatableAndIsolatedByUserAndEvent() {
        int first = EventReminder.stableId("user-a", "event-1");
        assertEquals(first, EventReminder.stableId("user-a", "event-1"));
        assertNotEquals(first, EventReminder.stableId("user-b", "event-1"));
        assertNotEquals(first, EventReminder.stableId("user-a", "event-2"));
        assertFalse(first < 0);
    }

    private Evento event(String id, String start) {
        return new Evento(id, "Evento " + id, start, start, null, null,
                false, false, null, null, null, null, null, null, null, null);
    }
}
