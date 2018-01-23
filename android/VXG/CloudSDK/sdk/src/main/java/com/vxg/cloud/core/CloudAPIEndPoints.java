//
//  Copyright © 2016 VXG Inc. All rights reserved.
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

package com.vxg.cloud.core;

class CloudAPIEndPoints {
    private static String mCameras = "/api/v2/cameras/";
    private static String mSvcAuth = "/svcauth/";
    private static String mServerTime = "/api/v2/server/time/";
    private static String mCameraManagers = "/api/v2/cmngrs/";
    private static String mCameraSession = "/api/v2/camsess/";
    private static String mAccount = "/api/v2/account/";

    //STORAGE (CAMERA_RECORDS)
    public static String mStorage = "/api/v2/storage/";


    public static String LIVE_URLS = "/live_urls/";


    // Account end endpoints

    static String ACCOUNT(){
        return mAccount;
    }

    static String ACCOUNT_LOGOUT(){
        return mAccount + "logout/";
    }

    static String ACCOUNT_API_TOKEN(){
        return mAccount + "token/api/";
    }

    static String ACCOUNT_CAPABILITIES(){
        return mAccount + "capabilities/";
    }

    // Some server endpoints

    static String SERVER_TIME(){
        return mServerTime;
    }

    static String SVC_AUTH() { // TODO
        return mSvcAuth;
    }

    // Cameras endpoints

    static String CAMERA_PREVIEW(long cameraID) {
        return mCameras + Long.toString(cameraID) + "/preview/";
    }

    static String CAMERA_PREVIEW_UPDATE(long cameraID) {
        return mCameras + Long.toString(cameraID) + "/preview/update/";
    }

    static String CAMERAS() {
        return CloudAPIEndPoints.mCameras;
    }

    static String CAMERA(long cameraID) {
        return mCameras + Long.toString(cameraID) + "/";
    }

    static String CAMERA_SHARING(long cameraID) {
        return mCameras + Long.toString(cameraID) + "/sharings/";
    }

    static String CAMERA_RECORDS() {
        return mStorage + "data/";
    }

    static String CAMERA_TIMELINE(long cameraID) {
        return mStorage + "timeline/" + Long.toString(cameraID) + "/";
    }

    static String CAMERA_TIMELINE_DAYS() {
        return mStorage + "activity/";
    }

    static String CAMERA_LIVE_URLS(long cameraID) {
        return mCameras + Long.toString(cameraID) + "/live_urls/";
    }

    // Camera manager endpoints

    static String CMNGRS_RESET(long cmngrsID) {
        return mCameraManagers + Long.toString(cmngrsID) + "/reset/";
    }
    static String CMNGRS(long cmngrsID) {
        return mCameraManagers + Long.toString(cmngrsID) + "/";
    }

    // Camera Session endpoints

    static String CAMSESS_RECORDS(long camsessID) {
        return mCameraSession + Long.toString(camsessID) + "/records/";
    }

    static String CAMSESS() {
        return mCameraSession;
    }

    static String CAMSESS(long camsessID) {
        return mCameraSession + Long.toString(camsessID) + "/";
    }

    static String CAMESESS_RECORDS_UPLOAD(long camsessID) {
        return mCameraSession + Long.toString(camsessID) + "/records/upload/";
    }

    static String CAMESESS_PREVIEW_UPLOAD(long camsessID) {
        return mCameraSession + Long.toString(camsessID) + "/preview/upload/";
    }

    static String CAMSESS_LIVE_STATS(long camsessID) {
        return mCameraSession + Long.toString(camsessID) + "/live_stats/";
    }

    static String CAMSESS_CHAT_SEND_MESSAGE(long camsessID) {
        return mCameraSession + Long.toString(camsessID) + "/chat/send_message/";
    }

    static String CAMSESS_PREVIEW_UPDATE(long camsessID) { // TODO
        return mCameraSession + Long.toString(camsessID) + "/preview/update/";
    }
}
