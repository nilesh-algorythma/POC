package com.vxg.cloudsdk.Enums;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by bleikher on 22.08.2017.
 */

public enum CloudCameraStatus {
    ACTIVE(0),
    UNAUTHORIZED(1),
    INACTIVE(2),
    INACTIVE_BY_SCHEDULER(3),
    OFFLINE(4);

    private static final Map<Integer, CloudCameraStatus> typesByValue = new HashMap<Integer, CloudCameraStatus>();

    static
    {
        for (CloudCameraStatus type : CloudCameraStatus.values())
            typesByValue.put(type.value, type);
    }

    private final int value;
    private CloudCameraStatus(int value) { this.value = value; }
    public static CloudCameraStatus forValue(int value) { return typesByValue.get(value); }
    public static int forType(CloudCameraStatus type) { return type.value; }

}
