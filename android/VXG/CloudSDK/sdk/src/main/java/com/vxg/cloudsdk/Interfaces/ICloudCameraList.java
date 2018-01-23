
package com.vxg.cloudsdk.Interfaces;

/**
 * Created by bleikher on 26.07.2017.
 */


import com.vxg.cloudsdk.Objects.CloudCamera;
import com.vxg.cloudsdk.Objects.CloudCameraListFilter;

import java.util.List;

public interface ICloudCameraList
{
    // Synchroneous methods
    List<CloudCamera> getCameraListSync(CloudCameraListFilter filter);
    CloudCamera getCameraSync(long camID);
    CloudCamera findOrCreateCameraSync(String url);
    CloudCamera createCameraSync(String url);
    CloudCamera createCameraForStreamSync();  //push mode camera, for CloudStreamer use
    int deleteCameraSync(long camID);

    // Asynchronous methods
    int getCameraList(CloudCameraListFilter filter, ICompletionCallback callback);
    int getCamera(long camID, ICompletionCallback callback);
    int findOrCreateCamera(String url, ICompletionCallback callback);
    int createCamera(String url, ICompletionCallback callback);
    int createCameraForStream(ICompletionCallback callback);  //push mode camera, for CloudStreamer use
    int deteleCamera(long nCameraID, ICompletionCallback callback);

}