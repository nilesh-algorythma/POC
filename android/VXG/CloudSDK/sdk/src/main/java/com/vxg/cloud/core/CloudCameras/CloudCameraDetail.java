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

package com.vxg.cloud.core.CloudCameras;

import com.vxg.cloud.core.CloudHelpers;
import com.vxg.cloudsdk.Helpers.MLog;

import org.json.JSONException;
import org.json.JSONObject;

public class CloudCameraDetail {
    private static final String TAG = CloudCameraDetail.class.getSimpleName();
    final public static int LOG_LEVEL = 2; //Log.VERBOSE;
    static MLog Log = new MLog(TAG, LOG_LEVEL);

    private boolean mHasError = true;
    private int mErrorStatus = 200;
    private String mErrorDetail = null;
    private long mID = 0;
    private String mUrl = "";
    private JSONObject mJSON = null;

    public CloudCameraDetail(){
        mHasError = true;
    }

    public CloudCameraDetail(JSONObject details){
        if(details == null)
            return;
        mJSON = details;

        Log.i("CloudCameraDetail = " + details.toString());
        try {
            if (details.has("errorType") && details.has("errorDetail")) {
                mHasError = true;
                mErrorDetail = details.getString("errorDetail");
                if(details.has("status")){
                    mErrorStatus = details.getInt("status");
                }
            }else{
                mHasError = false;
                if (details.has("id") && !details.isNull("id")){
                    mID = details.getInt("id");
                }

                if(details.has("url")){
                    mUrl = details.getString("url");
                }
            }
        }catch(JSONException e){

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

    public long getID(){
        return mID;
    }

    public String getUrl() { return mUrl; }

    public JSONObject toJSON() { return mJSON;};
    
    
}
