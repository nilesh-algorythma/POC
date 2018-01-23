package com.vxg.cloudsdk.Enums;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by bleikher on 15.08.2017.
 */


public enum CloudPlayerEvent {
    CONNECTING(0),        /* Connection is establishing */
    CONNECTED(1),         /* Connection established */
    STARTED(2),           /* play() successfully done */
    PAUSED(3),            /* pause() successfully done */
    CLOSED(6),            /* close() successfully finished */
    EOS(12),              /* End-Of-Stream event */
    SEEK_COMPLETED(17),    /* setPosition() finished */
    ERROR(105),            /* Stream was disconnected event */
    TRIAL_VERSION(-999), /* Trial version limitation */
    SOURCE_CHANGED(3000);

    private static final Map<Integer, CloudPlayerEvent> typesByValue = new HashMap<Integer, CloudPlayerEvent>();

    static
    {
        for (CloudPlayerEvent type : CloudPlayerEvent.values())
            typesByValue.put(type.value, type);
    }

    private final int value;
    private CloudPlayerEvent(int value) { this.value = value; }
    public static CloudPlayerEvent forValue(int value) { return typesByValue.get(value); }
    public static int forType(CloudPlayerEvent type) { return type.value; }
}

