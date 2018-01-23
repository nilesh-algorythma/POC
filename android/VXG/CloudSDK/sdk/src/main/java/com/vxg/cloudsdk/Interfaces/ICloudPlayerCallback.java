package com.vxg.cloudsdk.Interfaces;

import com.vxg.cloudsdk.Enums.CloudPlayerEvent;

/**
 * Created by bleikher on 15.08.2017.
 */

public interface ICloudPlayerCallback {
    void onStatus(CloudPlayerEvent player_event, ICloudObject player);
}
