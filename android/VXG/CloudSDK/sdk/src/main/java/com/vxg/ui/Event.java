package com.vxg.ui;

/**
 * Created by alexey on 09.10.17.
 */

public class Event {

    enum  Type{net,motion,sound};
    private Type type;
    private long time;
    private String color;

    Event(Type type,long t)
    {

        this.type=type;
        time=t;
        switch(type)
        {
            case net:
                color="#FFFFFF";
                break;
            case motion:
                color="#0080FF";
                break;
            case sound:
                color="#FF0000";
                break;

        }

    }
}
