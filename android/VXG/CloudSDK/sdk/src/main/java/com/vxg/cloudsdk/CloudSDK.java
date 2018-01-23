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

package com.vxg.cloudsdk;

import android.content.Context;

import com.vxg.cloudsdk.Helpers.MLog;

//helper class
public class CloudSDK
{
    private static final String CLOUDSDK_LIB_VERSION="2.0.8_20180112";

    private static CloudSDK sCloudSDK;
    private static Context mContext;

    public static String getLibVersion()
    {
        return CLOUDSDK_LIB_VERSION;
    }

    public static void setContext(Context context){
        mContext = context;
        MLog.app = context;
    }
    public static Context getContext(){
        return mContext;
    }

    public static void setLogEnable(boolean b){
        MLog.isSignedWithDebugKey = b;
        veg.mediacapture.sdk.MLog.isSignedWithDebugKey = b;
    }
    public static boolean getLogEnable(){
        return MLog.isSignedWithDebugKey;
    }

    public static void setLogLevel(int level){
        MLog.globalLevel = level;
    }
    public static int getLogLevel(){
        return MLog.globalLevel;
    }

}