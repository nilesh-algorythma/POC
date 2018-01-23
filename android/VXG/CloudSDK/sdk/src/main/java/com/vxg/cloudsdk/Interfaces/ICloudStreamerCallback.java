package com.vxg.cloudsdk.Interfaces;

/**
 * Created by bleikher on 03.11.2017.
 */

public interface ICloudStreamerCallback {
    public void onStarted(String url_push); //Cloud gets ready for data, url_push == rtmp://...
    public void onStopped();                //Cloud closed getting the data
    public void onError(int error);
    public void onCameraConnected(); // getCamera() to get the camera
}
