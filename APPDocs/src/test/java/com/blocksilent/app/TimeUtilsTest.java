package com.blocksilent.app;

import com.blocksilent.app.utils.TimeUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TimeUtilsTest {

    @Test
    public void testParseTimeToMinutes12HourFormat() {
        assertEquals(9 * 60, TimeUtils.parseTimeToMinutes("09:00 AM"));
        assertEquals(9 * 60, TimeUtils.parseTimeToMinutes("9:00 AM"));
        assertEquals(14 * 60 + 30, TimeUtils.parseTimeToMinutes("02:30 PM"));
        assertEquals(12 * 60, TimeUtils.parseTimeToMinutes("12:00 PM"));
        assertEquals(0, TimeUtils.parseTimeToMinutes("12:00 AM"));
    }

    @Test
    public void testParseTimeToMinutes24HourFormat() {
        assertEquals(9 * 60, TimeUtils.parseTimeToMinutes("09:00"));
        assertEquals(14 * 60 + 30, TimeUtils.parseTimeToMinutes("14:30"));
        assertEquals(0, TimeUtils.parseTimeToMinutes("00:00"));
        assertEquals(23 * 60 + 59, TimeUtils.parseTimeToMinutes("23:59"));
    }

    @Test
    public void testIsTimeBetween() {
        // Target: 10:15 AM, Window: 09:00 AM - 11:30 AM -> True
        assertTrue(TimeUtils.isTimeBetween("10:15 AM", "09:00 AM", "11:30 AM"));
        // Target: 14:30 (2:30 PM), Window: 09:00 AM - 05:00 PM -> True
        assertTrue(TimeUtils.isTimeBetween("14:30", "09:00 AM", "05:00 PM"));
        // Target: 08:30 AM, Window: 09:00 AM - 11:00 AM -> False
        assertFalse(TimeUtils.isTimeBetween("08:30 AM", "09:00 AM", "11:00 AM"));
    }
}
