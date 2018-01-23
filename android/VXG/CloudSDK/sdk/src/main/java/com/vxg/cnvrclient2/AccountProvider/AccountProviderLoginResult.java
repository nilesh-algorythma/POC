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

package com.vxg.cnvrclient2.AccountProvider;

import com.vxg.cloudsdk.Helpers.MLog;

import org.json.JSONException;
import org.json.JSONObject;

public class AccountProviderLoginResult {
    private static final String TAG = AccountProviderLoginResult.class.getSimpleName();
    final public static int LOG_LEVEL = 2; //Log.VERBOSE;
    static MLog Log = new MLog(TAG, LOG_LEVEL);

    private boolean mHasError = true;
    private int mErrorCode = 0;
    private String mErrorDetail = "";

    public AccountProviderLoginResult(int errorCode, String errorMessage) {
        mErrorCode = errorCode;
        mErrorDetail = errorMessage;
    }

    public AccountProviderLoginResult(boolean success) {
        mHasError = !success;
    }

    public AccountProviderLoginResult(JSONObject obj) {
        try {
            Log.i(obj.toString(1));
            if (obj.has("errorDetail")) {
                mErrorCode = obj.getInt("status");
                mErrorDetail = obj.getString("errorDetail");
            }else{
                mHasError = false;
            }
        }catch(JSONException e){
            Log.e(e.getMessage());
            e.printStackTrace();
        }
    }


    public int getErrorCode(){
        return mErrorCode;
    }

    public String getErrorDetail(){
        return mErrorDetail;
    }

    public boolean hasError(){
        return mHasError;
    }
}
