//
//  Copyright © 2017 VXG Inc. All rights reserved.
//  Contact: https://www.videoexpertsgroup.com/contact-vxg/
//  This file is part of the demonstration of the VXG Cloud Platform.
//
//  Commercial License Usage
//  Licensees holding valid commercial VXG licenses may use this file in
//  accordance with the commercial license agreement provided with the
//  Software or, alternatively, in accordance with the terms contained in
//  a written agreement between you and VXG Inc. For further information
//  use the contact form at https://www.videoexpertsgroup.com/contact-vxg/
//

package com.vxg.cloudsdk.Interfaces;

import com.vxg.cloudsdk.Enums.CloudCameraRecordingMode;
import com.vxg.cloudsdk.Enums.CloudCameraStatus;
import com.vxg.cloudsdk.Objects.CloudTimeline;

import java.util.ArrayList;

public interface ICloudCamera
{
    long getID();     //get srcid

    //=> get preview image
    String getPreviewURLSync();
    int getPreviewURL(ICompletionCallback callback);
    //<= get preview image

    long getDeleteAt();
    void setDeleteAt(long utc_time);

    // => pull camera
    void setURL(String url); // except push cameras
    String getURL();
    void setURLLogin(String login);
    String getURLLogin();
    void setURLPassword(String password);
    String getURLPassword();
    // <= pull camera

    void setTimezone(String timezone);
    String getTimezone();

    void setName(String name);
    String getName();

    CloudCameraStatus getStatus();

    //=>set camera to Public mode
    void setPublic(boolean is_public);
    boolean isPublic();
    //<=

    boolean isOwner();

    //=>record
    void setRecordingMode(CloudCameraRecordingMode rec_mode); // don't forget call ICloudObject.save() to apply
    CloudCameraRecordingMode getRecordingMode();          //don't forget call ICloudObject.refresh() to update
    boolean isRecording();

    CloudTimeline getTimelineSync(long start, long end);
    int getTimeline(long start, long end, ICompletionCallback callback);

    ArrayList<Long> getTimelineDaysSync(boolean use_timezone);
    int           getTimelineDays(boolean use_timezone, ICompletionCallback callback);
    //<=record

    //location coordinates
    void setLatLngBounds(double latitude, double longitude);
    double getLat();
    double getLng();

    // sharing cameras
    String enableSharingSync(); //returns a sharing token
    int enableSharing(ICompletionCallback callback);
    int disableSharingSync(); //disables a sharing token
    int disableSharing(ICompletionCallback callback);
    boolean isSharing();

    String enableSharingForStreamSync(); //returns a sharing token
    int enableSharingForStream(ICompletionCallback callback);
    int disableSharingForStreamSync(); //disables a sharing token
    int disableSharingForStream(ICompletionCallback callback);

}