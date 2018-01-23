package com.vxg.cloudsdk.Enums;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by bleikher on 22.08.2017.
 */

public enum CloudCameraRecordingMode {
    CONTINUES(0), /* Continues continuous recording (rec_mode == on) */
    BY_EVENT(1),  /* Recording by event (rec_mode == by_event) */
    NO_RECORDING(2); /* Recording is stopped (rec_mode == off) */

    private static final Map<Integer, CloudCameraRecordingMode> typesByValue = new HashMap<Integer, CloudCameraRecordingMode>();

    static
    {
        for (CloudCameraRecordingMode type : CloudCameraRecordingMode.values())
            typesByValue.put(type.value, type);
    }

    private final int value;
    private CloudCameraRecordingMode(int value) { this.value = value; }
    public static CloudCameraRecordingMode forValue(int value) { return typesByValue.get(value); }
    public static int forType(CloudCameraRecordingMode type) { return type.value; }

}
