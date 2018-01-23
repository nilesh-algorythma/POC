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

package com.vxg.cloud.core.CloudCommon;

import com.vxg.cloudsdk.Helpers.MLog;

import org.json.JSONException;
import org.json.JSONObject;

public class CloudLiveUrls {
    private final static String TAG = CloudLiveUrls.class.getSimpleName();
    final public static int LOG_LEVEL = 2; //Log.VERBOSE;
    static MLog Log = new MLog(TAG, LOG_LEVEL);

    private String mRtmp = null;
    private boolean mHasError = true;
    private int mErrorStatus = 0;
    private String mErrorDetail = null;

    public CloudLiveUrls(){
    }

    public CloudLiveUrls(JSONObject details){
        try {
            if (details.has("errorType") && details.has("errorDetail")) {
                Log.e("Unknown json type: " + details.toString());
                mHasError = true;
                mErrorDetail = details.getString("errorDetail");
                if(details.has("status")){
                    mErrorStatus = details.getInt("status");
                }

            } else {
                mHasError = false;
                if(details.has("rtmp") && !details.isNull("rtmp")){
                    mRtmp = details.getString("rtmp");
                }
            }
        } catch(JSONException e) {
            Log.e("Constructor CloudLiveUrls error: "+ e);
            e.printStackTrace();
        }
    }

    public boolean hasError(){
        return mHasError;
    }
    public String getErrorDetail(){
        return mErrorDetail;
    }

    public int getErrorStatus(){
        return mErrorStatus;
    }

    public String rtmp(){
        return mRtmp;
    }
}
