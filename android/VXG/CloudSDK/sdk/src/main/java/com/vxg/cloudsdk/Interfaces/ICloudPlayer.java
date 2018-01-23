
package com.vxg.cloudsdk.Interfaces;

/**
 * Created by bleikher on 26.07.2017.
 */


import android.graphics.Bitmap;
import android.view.View;

import com.vxg.cloudsdk.Enums.CloudPlayerState;
import com.vxg.cloudsdk.Objects.CloudPlayerConfig;

import java.nio.ByteBuffer;

public interface ICloudPlayer
{
    void setSource(ICloudObject src);
    ICloudObject getSource();

    void addCallback(ICloudPlayerCallback callback);
    void removeCallback(ICloudPlayerCallback callback);

    int setConfig(CloudPlayerConfig config); //apply new CloudPlayerConfig
    CloudPlayerConfig getCloneConfig(); //get clone of CloudPlayerConfig

    void play();
    void pause();
    void close();
    CloudPlayerState getState();

    void setPosition(long nPosition);
    long getPosition();
    boolean isLive();

    Bitmap getVideoShot(int width, int height);

    void mute(boolean bMute);
    boolean isMute();
    void setVolume(int val); //0: min; 100:max
    int getVolume();

    void showTimeline(View vwTimeline);
    void hideTimeline();
}