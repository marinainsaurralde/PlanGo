package com.marinainsaurralde.plango.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public final class ReminderRepositoryTest {
    @Test public void persistenceFilesAreSeparatedByFirebaseUid() {
        assertEquals("plango_reminders_user-a", ReminderRepository.fileName("user-a"));
        assertEquals("plango_reminders_user-b", ReminderRepository.fileName("user-b"));
        assertNotEquals(ReminderRepository.fileName("user-a"),
                ReminderRepository.fileName("user-b"));
    }
}
