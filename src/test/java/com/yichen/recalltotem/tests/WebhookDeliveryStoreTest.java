package com.yichen.recalltotem.tests;

import com.yichen.recalltotem.web.WebhookDeliveryStore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class WebhookDeliveryStoreTest {

    @Test
    public void recordAndRecentWorks() {
        WebhookDeliveryStore.record("2026-01-01T00:00:00Z", "test.event", "tester", 500, "server error");
        List<Map<String, Object>> recent = WebhookDeliveryStore.recent();
        assertTrue(recent.size() >= 1);
        Map<String, Object> entry = recent.get(recent.size() - 1);
        assertEquals("test.event", entry.get("event"));
    }
}
