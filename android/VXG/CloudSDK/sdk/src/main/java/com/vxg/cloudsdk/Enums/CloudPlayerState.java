package com.vxg.cloudsdk.Enums;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by bleikher on 06.09.2017.
 */

public enum CloudPlayerState {
    CONNECTING(0),        /* Connection is establishing */
    CONNECTED(1),         /* Connection established */
    STARTED(2),           /* play() successfully done */
    PAUSED(3),            /* pause() successfully done */
    CLOSED(6),            /* close() successfully finished */
    EOS(12);              /* End-Of-Stream state */

    private static final Map<Integer, CloudPlayerState> typesByValue = new HashMap<Integer, CloudPlayerState>();

    static
    {
        for (CloudPlayerState type : CloudPlayerState.values())
            typesByValue.put(type.value, type);
    }

    private final int value;
    private CloudPlayerState(int value) { this.value = value; }
    public static CloudPlayerState forValue(int value) { return typesByValue.get(value); }
    public static int forType(CloudPlayerState type) { return type.value; }
}

