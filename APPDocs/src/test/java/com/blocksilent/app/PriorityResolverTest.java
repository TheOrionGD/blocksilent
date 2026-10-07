package com.blocksilent.app;

import com.blocksilent.app.context.PriorityResolver;
import com.blocksilent.app.database.entities.BlockEntity;
import com.blocksilent.app.database.entities.TimetableEntity;
import com.blocksilent.app.database.entities.WifiZoneEntity;
import com.blocksilent.app.utils.SoundModeManager;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PriorityResolverTest {

    @Test
    public void testEmergencyBreakGlassOverridesAllRules() {
        List<BlockEntity> activeBlocks = new ArrayList<>();
        activeBlocks.add(new BlockEntity("Exam Hall", 12.9716, 77.5946, 50.0f, "EXAM_HALL", SoundModeManager.MODE_SILENT, 1, true, System.currentTimeMillis()));

        TimetableEntity timetable = new TimetableEntity(1, "Exam Slot", "Monday", "09:00", "12:00", SoundModeManager.MODE_SILENT, true);
        WifiZoneEntity wifiZone = new WifiZoneEntity("AA:BB:CC:DD:EE:01", "Campus-WiFi", -60, "Exam Room 1", "1", 1, "Exam Hall", "SILENT", true, System.currentTimeMillis());

        // When emergency is active, it must resolve to NORMAL (priority 100)
        PriorityResolver.ResolutionResult result = PriorityResolver.resolve(
                true, false, true, timetable, wifiZone, activeBlocks
        );

        assertNotNull(result);
        assertEquals(SoundModeManager.MODE_NORMAL, result.soundMode);
        assertEquals("EMERGENCY", result.source);
        assertEquals(100, result.priority);
    }

    @Test
    public void testManualOverridePrecedence() {
        List<BlockEntity> activeBlocks = new ArrayList<>();
        activeBlocks.add(new BlockEntity("Classroom 101", 12.9716, 77.5946, 50.0f, "CLASSROOM", SoundModeManager.MODE_SILENT, 2, true, System.currentTimeMillis()));

        PriorityResolver.ResolutionResult result = PriorityResolver.resolve(
                false, true, false, null, null, activeBlocks
        );

        assertNotNull(result);
        assertEquals(SoundModeManager.MODE_NORMAL, result.soundMode);
        assertEquals("MANUAL_OVERRIDE", result.source);
        assertEquals(90, result.priority);
    }

    @Test
    public void testSensorFlipToSilencePrecedence() {
        List<BlockEntity> activeBlocks = new ArrayList<>();
        activeBlocks.add(new BlockEntity("General Library", 12.9716, 77.5946, 50.0f, "LIBRARY", SoundModeManager.MODE_VIBRATE, 4, true, System.currentTimeMillis()));

        // Face-down sensor triggers instant SILENT (priority 80)
        PriorityResolver.ResolutionResult result = PriorityResolver.resolve(
                false, false, true, null, null, activeBlocks
        );

        assertNotNull(result);
        assertEquals(SoundModeManager.MODE_SILENT, result.soundMode);
        assertEquals("SENSOR_FLIP", result.source);
        assertEquals(80, result.priority);
    }

    @Test
    public void testTimetablePrecedenceOverGeofence() {
        List<BlockEntity> activeBlocks = new ArrayList<>();
        activeBlocks.add(new BlockEntity("CSE Block", 12.9716, 77.5946, 50.0f, "CLASSROOM", SoundModeManager.MODE_VIBRATE, 2, true, System.currentTimeMillis()));

        TimetableEntity timetable = new TimetableEntity(1, "Operating Systems Lab", "Monday", "10:00", "12:00", SoundModeManager.MODE_SILENT, true);

        PriorityResolver.ResolutionResult result = PriorityResolver.resolve(
                false, false, false, timetable, null, activeBlocks
        );

        assertNotNull(result);
        assertEquals(SoundModeManager.MODE_SILENT, result.soundMode);
        assertEquals("TIMETABLE", result.source);
        assertEquals(70, result.priority);
    }

    @Test
    public void testWifiBssidIndoorLocalizationResolution() {
        List<BlockEntity> activeBlocks = new ArrayList<>();
        activeBlocks.add(new BlockEntity("Main Campus Block", 12.9716, 77.5946, 100.0f, "CUSTOM", SoundModeManager.MODE_NORMAL, 6, true, System.currentTimeMillis()));

        WifiZoneEntity wifiZone = new WifiZoneEntity("AA:BB:CC:DD:EE:02", "Faculty-Net", -55, "Professor Office 302", "3", 1, "Main Campus Block", "VIBRATE", true, System.currentTimeMillis());

        PriorityResolver.ResolutionResult result = PriorityResolver.resolve(
                false, false, false, null, wifiZone, activeBlocks
        );

        assertNotNull(result);
        assertEquals(SoundModeManager.MODE_VIBRATE, result.soundMode);
        assertEquals("WIFI_BSSID", result.source);
        assertEquals(60, result.priority);
    }

    @Test
    public void testGeofenceHierarchyResolution() {
        List<BlockEntity> activeBlocks = new ArrayList<>();
        // Add overlapping blocks: Library (Priority 4, Vibrate) and Exam Hall (Priority 1, Silent)
        activeBlocks.add(new BlockEntity("Library", 12.9716, 77.5946, 50.0f, "LIBRARY", SoundModeManager.MODE_VIBRATE, 4, true, System.currentTimeMillis()));
        activeBlocks.add(new BlockEntity("Exam Hall", 12.9716, 77.5946, 50.0f, "EXAM_HALL", SoundModeManager.MODE_SILENT, 1, true, System.currentTimeMillis()));

        PriorityResolver.ResolutionResult result = PriorityResolver.resolve(
                false, false, false, null, null, activeBlocks
        );

        assertNotNull(result);
        assertEquals(SoundModeManager.MODE_SILENT, result.soundMode);
        assertEquals("Exam Hall", result.location);
        assertEquals(50, result.priority);
    }

    @Test
    public void testOutsideAllZonesResolvesToAutoRestore() {
        PriorityResolver.ResolutionResult result = PriorityResolver.resolve(
                false, false, false, null, null, new ArrayList<>()
        );

        assertNotNull(result);
        assertEquals(SoundModeManager.MODE_NORMAL, result.soundMode);
        assertEquals("AUTO_RESTORE", result.source);
        assertEquals(10, result.priority);
    }
}
