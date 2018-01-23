package com.vxg.cloudsdk.Objects;

import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class CloudSharingsToken {
    private static final String TAG = CloudSharingsToken.class.getSimpleName();
    private JSONObject mOrigJson = null;
    private String mErrorDetail = "";
    private int mErrorStatus = 0;
    private boolean mHasError = true;
    private boolean mIncludePublic = false;
    private boolean mEnabled = true; // default true;
    private String mCreated = "";
    private String mExpire = "";
    private String mName = "";
    private String mToken = "";
    private long mID = 0;
    private CloudAccess mAccess = null;

    public static final String COMMON_SHARING_TOKEN = "COMMON_SHARING_TOKEN";
    public static final String COMMON_SHARING_TOKEN_FOR_STREAM = "COMMON_SHARING_TOKEN_FOR_STREAM";

    public CloudSharingsToken(JSONObject obj){
        if(obj == null)
            return;
        mOrigJson = obj;

        Log.i(TAG, obj.toString());
        try {
            if (obj.has("errorType") && obj.has("errorDetail")) {
                mHasError = true;
                mErrorDetail = obj.getString("errorDetail");
                if(obj.has("status")){
                    mErrorStatus = obj.getInt("status");
                }
            }else{
                mHasError = false;
                if (obj.has("id") && !obj.isNull("id")){
                    mID = obj.getInt("id");
                }

                if(obj.has("name") && !obj.isNull("name")){
                    mName = obj.getString("name");
                }

                if(obj.has("token") && !obj.isNull("token")){
                    mToken = obj.getString("token");
                }

                if(obj.has("include_public") && !obj.isNull("include_public")){
                    mIncludePublic = obj.getBoolean("include_public");
                }

                if(obj.has("enabled") && !obj.isNull("enabled")){
                    mEnabled = obj.getBoolean("enabled");
                }else{
                    mEnabled = true;
                }

                if(obj.has("created") && !obj.isNull("created")){
                    mCreated = obj.getString("created");
                }

                if(obj.has("expire") && !obj.isNull("expire")){
                    mExpire = obj.getString("expire");
                }

                if(obj.has("access") && !obj.isNull("access")){
                    mAccess = new CloudAccess(obj.getJSONArray("access"));
                }
            }
        }catch(JSONException e){
            mHasError = true;
            mErrorStatus = -1;
            mErrorDetail = e.getMessage();
            Log.e(TAG, e.getMessage());
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

    public long getID(){
        return mID;
    }

    public String getName(){
        return mName;
    }

    public String getToken(){
        return mToken;
    }

    public String getCreated(){
        return mCreated;
    }

    public boolean isIncludePublic(){
        return mIncludePublic;
    }

    public boolean isEnabled(){
        return mEnabled;
    }

    public CloudAccess getAccess(){
        return mAccess;
    }

    public JSONObject _origJson(){
        return mOrigJson;
    }

    public void setEnabled(boolean val){
        try {
            mOrigJson.put("enabled", val);
            mEnabled = val;
        }catch(JSONException e){
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
    }

    public String makeAccessToken(long camid){
        String sResult = "";
        JSONObject jsonAccessToken = new JSONObject();
        try {
            jsonAccessToken.put("token", mToken);
            jsonAccessToken.put("camid", camid);
            jsonAccessToken.put("access", "watch");
            sResult = jsonAccessToken.toString();
            byte[] data = sResult.getBytes("UTF-8");
            sResult = Base64.encodeToString(data,Base64.NO_WRAP);
        }catch(JSONException e){
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }catch(UnsupportedEncodingException e){
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
        return sResult;

    }

    public String makeAccessTokenForStream(long camid, long cmngrid){
        String sResult = "";
        JSONObject jsonAccessToken = new JSONObject();
        try {
            jsonAccessToken.put("token", mToken);
            jsonAccessToken.put("camid", camid);
            jsonAccessToken.put("cmngrid", cmngrid);
            jsonAccessToken.put("access", "all");
            sResult = jsonAccessToken.toString();
            byte[] data = sResult.getBytes("UTF-8");
            sResult = Base64.encodeToString(data,Base64.NO_WRAP);
        }catch(JSONException e){
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }catch(UnsupportedEncodingException e){
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
        return sResult;
    }
}
