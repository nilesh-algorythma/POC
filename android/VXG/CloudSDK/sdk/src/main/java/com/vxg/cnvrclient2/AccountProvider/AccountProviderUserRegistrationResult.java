package com.vxg.cnvrclient2.AccountProvider;

import com.vxg.cloudsdk.Helpers.MLog;

import org.json.JSONException;
import org.json.JSONObject;

public class AccountProviderUserRegistrationResult {
    private static final String TAG = AccountProviderUserRegistrationResult.class.getSimpleName();
    final public static int LOG_LEVEL = 2; //Log.VERBOSE;
    static MLog Log = new MLog(TAG, LOG_LEVEL);

    private boolean mHasError = true;
    private int mErrorCode = 0;
    private String mErrorDetail = "";

    public AccountProviderUserRegistrationResult(int errorCode, String errorMessage) {
        mErrorCode = errorCode;
        mErrorDetail = errorMessage;
    }

    public AccountProviderUserRegistrationResult(boolean success) {
        mHasError = !success;
    }

    public AccountProviderUserRegistrationResult(JSONObject obj) {
        try {
            Log.i( obj.toString(1));
            if (obj.has("errorDetail")) {
                mErrorCode = obj.getInt("status");
                mErrorDetail = obj.getString("errorDetail");
            }else{
                mHasError = false;
            }
        }catch(JSONException e){
            Log.e( e.getMessage());
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
