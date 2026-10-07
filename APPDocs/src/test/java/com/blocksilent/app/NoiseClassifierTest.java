package com.blocksilent.app;

import com.blocksilent.app.noise.NoiseDetector;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NoiseClassifierTest {

    @Test
    public void testQuietZoneClassification() {
        assertEquals("Quiet Study Zone", NoiseDetector.classifyNoise(32.5));
        assertEquals("QUIET", NoiseDetector.getCategoryKey(32.5));

        assertEquals("Quiet Study Zone", NoiseDetector.classifyNoise(39.9));
        assertEquals("QUIET", NoiseDetector.getCategoryKey(39.9));
    }

    @Test
    public void testModerateZoneClassification() {
        assertEquals("Moderate Environment", NoiseDetector.classifyNoise(40.0));
        assertEquals("MODERATE", NoiseDetector.getCategoryKey(40.0));

        assertEquals("Moderate Environment", NoiseDetector.classifyNoise(55.2));
        assertEquals("MODERATE", NoiseDetector.getCategoryKey(55.2));

        assertEquals("Moderate Environment", NoiseDetector.classifyNoise(60.0));
        assertEquals("MODERATE", NoiseDetector.getCategoryKey(60.0));
    }

    @Test
    public void testHighNoiseZoneClassification() {
        assertEquals("High Noise / Active Zone", NoiseDetector.classifyNoise(60.1));
        assertEquals("HIGH", NoiseDetector.getCategoryKey(60.1));

        assertEquals("High Noise / Active Zone", NoiseDetector.classifyNoise(85.0));
        assertEquals("HIGH", NoiseDetector.getCategoryKey(85.0));
    }
}
