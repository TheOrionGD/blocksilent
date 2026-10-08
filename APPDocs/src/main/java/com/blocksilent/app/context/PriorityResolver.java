package com.blocksilent.app.context;

import com.blocksilent.app.database.entities.BlockEntity;
import com.blocksilent.app.database.entities.TimetableEntity;
import com.blocksilent.app.utils.SoundModeManager;

import java.util.List;

public class PriorityResolver {

    public static class ResolutionResult {
        public final String soundMode;
        public final String source;
        public final String location;
        public final String reason;
        public final String confidence;
        public final int priority;

        public ResolutionResult(String soundMode, String source, String location,
                                String reason, String confidence, int priority) {
            this.soundMode = soundMode;
            this.source = source;
            this.location = location;
            this.reason = reason;
            this.confidence = confidence;
            this.priority = priority;
        }
    }

    /**
     * Resolves the definitive sound policy across active contextual inputs:
     * 1. Emergency Break-Glass (Priority 100) -> NORMAL
     * 2. Manual Temporary Override (Priority 90) -> NORMAL
     * 3. Active Timetable Slot (Priority 70) -> Scheduled sound mode
     * 4. Real-Time GPS Geofence (Priority 50 - Exam Hall > Classroom > Lab > Library > Office > Custom)
     * 5. Default Outside All (Priority 10) -> NORMAL / Auto-Restore
     */
    public static ResolutionResult resolve(
            boolean isEmergencyActive,
            boolean isManualOverrideActive,
            TimetableEntity activeSchedule,
            List<BlockEntity> activeBlocks) {

        // 1. Emergency Break-Glass
        if (isEmergencyActive) {
            return new ResolutionResult(
                    SoundModeManager.MODE_NORMAL,
                    "EMERGENCY",
                    "Emergency Triggered",
                    "Emergency Break-Glass active: All silent automation overridden.",
                    "HIGH",
                    100
            );
        }

        // 2. Manual Temporary Override
        if (isManualOverrideActive) {
            return new ResolutionResult(
                    SoundModeManager.MODE_NORMAL,
                    "MANUAL_OVERRIDE",
                    "Manual Temporary Override",
                    "User paused automated sound rules.",
                    "HIGH",
                    90
            );
        }

        // 3. Timetable Scheduled Slot
        if (activeSchedule != null && activeSchedule.isEnabled()) {
            return new ResolutionResult(
                    activeSchedule.getSoundMode(),
                    "TIMETABLE",
                    activeSchedule.getSubjectName(),
                    "Active academic timetable: " + activeSchedule.getSubjectName() + " (" + activeSchedule.getStartTime() + " - " + activeSchedule.getEndTime() + ")",
                    "HIGH",
                    70
            );
        }

        // 4. GPS Geofencing (Find highest priority block among overlapping active blocks)
        if (activeBlocks != null && !activeBlocks.isEmpty()) {
            BlockEntity dominantBlock = activeBlocks.get(0);
            for (BlockEntity b : activeBlocks) {
                if (b.isEnabled() && b.getPriority() < dominantBlock.getPriority()) {
                    dominantBlock = b;
                }
            }

            return new ResolutionResult(
                    dominantBlock.getSoundMode(),
                    "GPS_GEOFENCE",
                    dominantBlock.getName(),
                    "Inside geofence boundary of " + dominantBlock.getName(),
                    "HIGH",
                    50
            );
        }

        // 5. Default Outside All Zones
        return new ResolutionResult(
                SoundModeManager.MODE_NORMAL,
                "AUTO_RESTORE",
                "Outside Campus Zones",
                "Outside all designated silent zones. Restoring standard profile.",
                "HIGH",
                10
        );
    }
}

