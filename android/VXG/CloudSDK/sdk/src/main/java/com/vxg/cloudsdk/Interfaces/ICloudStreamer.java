package com.vxg.cloudsdk.Interfaces;

/**
 * Created by bleikher on 03.11.2017.
 */

public interface ICloudStreamer {
    int setCamera(ICloudObject camera); //  CloudCamera for stream
    ICloudObject getCamera();

    int setPreviewImage(String file);  // optional. jpeg file path

    int setConfig(String config); //restore saved CloudCamera session
    String getConfig();  //config for saving CloudCamera session

    void Start();    //start CM
    void Stop();     //stop CM
}
