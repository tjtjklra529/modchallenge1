package com.yichen.recalltotem.tests;

import com.yichen.recalltotem.config.ProtectedRegion;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ProtectedRegionScheduleTest {

    @Test
    public void regionWithoutSchedulesIsActive() {
        ProtectedRegion r = new ProtectedRegion();
        assertTrue(r.isActiveNow());
    }

    @Test
    public void regionWithActiveScheduleIsActive() {
        ProtectedRegion r = new ProtectedRegion();
        ProtectedRegion.ScheduleWindow s = new ProtectedRegion.ScheduleWindow();
        Instant now = Instant.now();
        s.startIso = now.minus(1, ChronoUnit.MINUTES).toString();
        s.endIso = now.plus(1, ChronoUnit.MINUTES).toString();
        r.schedules.add(s);
        assertTrue(r.isActiveNow());
    }

    @Test
    public void regionWithExpiredScheduleIsInactive() {
        ProtectedRegion r = new ProtectedRegion();
        ProtectedRegion.ScheduleWindow s = new ProtectedRegion.ScheduleWindow();
        Instant now = Instant.now();
        s.startIso = now.minus(10, ChronoUnit.MINUTES).toString();
        s.endIso = now.minus(5, ChronoUnit.MINUTES).toString();
        r.schedules.add(s);
        assertFalse(r.isActiveNow());
    }
}
