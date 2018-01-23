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

package com.vxg.cloud.core.CloudSessions;

import com.vxg.cloudsdk.Helpers.MLog;

import org.json.JSONException;
import org.json.JSONObject;

public class CloudCamsessLightDetail {
    private static final String TAG = CloudCamsessLightDetail.class.getSimpleName();
    final public static int LOG_LEVEL = 2; //Log.VERBOSE;
    static MLog Log = new MLog(TAG, LOG_LEVEL);


    private String mDetails;
    private boolean mHasError = true;
    private String mErrorDetail = "";
    private int mErrorStatus = 0;
    private boolean mIsActive = false;
    private int mID = 0;
    private int mCamID = 0;
    private double mLongitude = 0.0;
    private double mLatitude = 0.0;

    public CloudCamsessLightDetail(){
        mHasError = true;
    }

    public CloudCamsessLightDetail(JSONObject details){
        try {
            mDetails = details.toString(1);

            if (details.has("errorType") && details.has("errorDetail")) {
                mHasError = true;
                mErrorDetail = details.getString("errorDetail");
                if(details.has("status")){
                    mErrorStatus = details.getInt("status");
                }
            } else {
                mHasError = false;
                if(details.has("active") && !details.isNull("active")){
                    mIsActive = details.getBoolean("active");
                }

                if(details.has("id") && !details.isNull("id")){
                    mID = details.getInt("id");
                }

                if(details.has("camid") && !details.isNull("camid")){
                    mCamID = details.getInt("camid");
                }

                if(details.has("longitude") && !details.isNull("longitude")){
                    mLongitude = details.getDouble("longitude");
                }

                if(details.has("latitude") && !details.isNull("latitude")){
                    mLatitude = details.getDouble("latitude");
                }
            }
        } catch(JSONException e) {
            Log.e("Constructor CamsessDetail error: "+ e);
            mHasError = true;
            e.printStackTrace();
        }
    }

    public boolean isActive(){
        return mIsActive;
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

    public int getID(){
        return mID;
    }

    public int getCamID(){
        return mCamID;
    }

    public double getLongitude(){
        return mLongitude;
    }

    public double getLatitude(){
        return mLatitude;
    }

    public String toString(){
        return mDetails;
    }

}
