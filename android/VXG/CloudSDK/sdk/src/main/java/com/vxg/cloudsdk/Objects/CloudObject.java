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


package com.vxg.cloudsdk.Objects;

import com.vxg.cloudsdk.Enums.CloudReturnCodes;
import com.vxg.cloudsdk.Helpers.ThreadHelper;
import com.vxg.cloudsdk.Interfaces.ICloudObject;
import com.vxg.cloudsdk.Interfaces.ICompletionCallback;

public abstract class CloudObject extends ThreadHelper implements ICloudObject {
    protected int    mLastErrorInt = 0;
    protected String mLastErrorStr="";

    protected int makeError(int error){
        mLastErrorInt = error;
        switch(mLastErrorInt){
            case CloudReturnCodes.OK:
                mLastErrorStr = "OK";
                break;
            case CloudReturnCodes.OK_COMPLETIONPENDING:           /* Operation Pending */
                mLastErrorStr = "Operation Pending";
                break;
            case CloudReturnCodes.ERROR_NOT_CONFIGURED:      /* Connection not attached*/
                mLastErrorStr = "Connection not attached";
                break;
            case CloudReturnCodes.ERROR_NOT_IMPLEMENTED:        /* Function not implemented*/
                mLastErrorStr = "Function not implemented";
                break;
            case CloudReturnCodes.ERROR_NO_MEMORY:      /* Out of memory */
                mLastErrorStr = "Out of memory";
                break;
            case CloudReturnCodes.ERROR_ACCESS_DENIED:      /* Permission denied */
                mLastErrorStr = "Permission denied";
                break;
            case CloudReturnCodes.ERROR_BADARGUMENT:      /* Invalid argument */
                mLastErrorStr = "Invalid argument";
                break;

            case CloudReturnCodes.ERROR_STREAM_UNREACHABLE:
                mLastErrorStr = "Stream unreachable";
                break;
            case CloudReturnCodes.ERROR_EXPECTED_FILTER:
                mLastErrorStr = "Error in filter";
                break;
            case CloudReturnCodes.ERROR_NO_CLOUD_CONNECTION:
                mLastErrorStr = "No cloud connection";
                break;
            case CloudReturnCodes.ERROR_WRONG_RESPONSE:
                mLastErrorStr = "Wrong response";
                break;
            case CloudReturnCodes.ERROR_NOT_AUTHORIZED:
                mLastErrorStr = "Authentication failed";
                break;
            case CloudReturnCodes.ERROR_FORBIDDEN:
                mLastErrorStr = "User is not permitted to perform the requested operation";
                break;
            case CloudReturnCodes.ERROR_SOURCE_NOT_CONFIGURED:
                mLastErrorStr = "Source not configured";
                break;
            case CloudReturnCodes.ERROR_INVALID_SOURCE:
                mLastErrorStr = "Invalid source";
                break;
            case CloudReturnCodes.ERROR_RECORDS_NOT_FOUND:
                mLastErrorStr = "Records not found";
                break;
            case CloudReturnCodes.ERROR_NOT_FOUND:
                mLastErrorStr = "Not found";
                break;
            case CloudReturnCodes.ERROR_TOO_MANY_REQUESTS:
                mLastErrorStr = "Too many requests";
                break;

            default:
                mLastErrorStr = "Unknown error="+error;
                break;
        }

        return error;
    }
    protected int makeError(int error, String error_str){
        mLastErrorInt = error;
        mLastErrorStr = error_str;
        return mLastErrorInt;
    }

    protected int makeHTTPError(int error){
        switch(error){
            case 0:
                return makeError(CloudReturnCodes.ERROR_NO_CLOUD_CONNECTION);
            case 200:
                return makeError(CloudReturnCodes.OK);
            case 401:
                return makeError(CloudReturnCodes.ERROR_NOT_AUTHORIZED);
            case 403:
                return makeError(CloudReturnCodes.ERROR_FORBIDDEN);
            case 404:
                return makeError(CloudReturnCodes.ERROR_NOT_FOUND);
            case 429:
                return makeError(CloudReturnCodes.ERROR_TOO_MANY_REQUESTS);
        }
        return makeError(CloudReturnCodes.ERROR_WRONG_RESPONSE);
    }

    //=>default implementation of ICloudObject

    @Override
    public boolean hasError() {
        return (mLastErrorInt != 0);
    }

    @Override
    public int getResultInt() {
        return mLastErrorInt;
    }

    @Override
    public String getResultStr() {
        return mLastErrorStr;
    }
    //<=default implementation of ICloudObject

}
