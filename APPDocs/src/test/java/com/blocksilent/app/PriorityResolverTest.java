package com.blocksilent.app;

import com.blocksilent.app.context.PriorityResolver;
import com.blocksilent.app.database.entities.BlockEntity;
import com.blocksilent.app.database.entities.TimetableEntity;
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
        activeBlocks.add(new BlockEntity("Exam Hall", 12.9716, 77.5946, 50.0f, SoundModeManager.MODE_SILENT, true, 1));

        TimetableEntity timetable = new TimetableEntity("Exam Slot", "Monday", "09:00 AM", "12:00 PM", 1, "Exam Hall", SoundModeManager.MODE_SILENT, true);

        PriorityResolver.ResolutionResult result = PriorityResolver.resolve(
                true, false, timetable, activeBlocks
        );

        assertNotNull(result);
        assertEquals(SoundModeManager.MODE_NORMAL, result.soundMode);
        assertEquals("EMERGENCY", result.source);
        assertEquals(100, result.priority);
    }

    @Test
    public void testManualOverridePrecedence() {
        List<BlockEntity> activeBlocks = new ArrayList<>();
        activeBlocks.add(new BlockEntity("Classroom 101", 12.9716, 77.5946, 50.0f, SoundModeManager.MODE_SILENT, true, 2));

        PriorityResolver.ResolutionResult result = PriorityResolver.resolve(
                false, true, null, activeBlocks
        );

        assertNotNull(result);
        assertEquals(SoundModeManager.MODE_NORMAL, result.soundMode);
        assertEquals("MANUAL_OVERRIDE", result.source);
        assertEquals(90, result.priority);
    }

    @Test
    public void testTimetablePrecedenceOverGeofence() {
        List<BlockEntity> activeBlocks = new ArrayList<>();
        activeBlocks.add(new BlockEntity("CSE Block", 12.9716, 77.5946, 50.0f, SoundModeManager.MODE_VIBRATE, true, 2));

        TimetableEntity timetable = new TimetableEntity("Operating Systems Lab", "Monday", "10:00 AM", "12:00 PM", 1, "CSE Block", SoundModeManager.MODE_SILENT, true);

        PriorityResolver.ResolutionResult result = PriorityResolver.resolve(
                false, false, timetable, activeBlocks
        );

        assertNotNull(result);
        assertEquals(SoundModeManager.MODE_SILENT, result.soundMode);
        assertEquals("TIMETABLE", result.source);
        assertEquals(70, result.priority);
    }

    @Test
    public void testGeofenceHierarchyResolution() {
        List<BlockEntity> activeBlocks = new ArrayList<>();
        // Add overlapping blocks: Library (Priority 4, Vibrate) and Exam Hall (Priority 1, Silent)
        activeBlocks.add(new BlockEntity("Library", 12.9716, 77.5946, 50.0f, SoundModeManager.MODE_VIBRATE, true, 4));
        activeBlocks.add(new BlockEntity("Exam Hall", 12.9716, 77.5946, 50.0f, SoundModeManager.MODE_SILENT, true, 1));

        PriorityResolver.ResolutionResult result = PriorityResolver.resolve(
                false, false, null, activeBlocks
        );

        assertNotNull(result);
        assertEquals(SoundModeManager.MODE_SILENT, result.soundMode);
        assertEquals("Exam Hall", result.location);
        assertEquals(50, result.priority);
    }

    @Test
    public void testOutsideAllZonesResolvesToAutoRestore() {
        PriorityResolver.ResolutionResult result = PriorityResolver.resolve(
                false, false, null, new ArrayList<>()
        );

        assertNotNull(result);
        assertEquals(SoundModeManager.MODE_NORMAL, result.soundMode);
        assertEquals("AUTO_RESTORE", result.source);
        assertEquals(10, result.priority);
    }
}
