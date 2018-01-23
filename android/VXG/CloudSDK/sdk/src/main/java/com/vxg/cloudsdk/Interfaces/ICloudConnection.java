

package com.vxg.cloudsdk.Interfaces;

import com.vxg.cloud.core.CloudAPI;
import com.vxg.cloudsdk.Objects.CloudUserInfo;

/**
 * Created by bleikher on 26.07.2017.
 */


public interface ICloudConnection
{
    int close();
    boolean isOpened(); // helper function
    CloudUserInfo getUserInfoSync();
    int getUserInfo(ICompletionCallback callback);
    long getServerTimeDiff(); // difference time between local utc and server utc times

    CloudAPI _getAPI();
    long getUserID();
}