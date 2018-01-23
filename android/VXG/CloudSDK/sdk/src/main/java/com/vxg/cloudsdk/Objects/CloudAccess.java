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


import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CloudAccess {
    private static final String TAG = CloudAccess.class.getSimpleName();
    private boolean mAll = false;
    private boolean mList = false;
    private boolean mPtz = false;
    private boolean mBackaudio = false;
    private boolean mLive = false;
    private boolean mPlay = false;


    public CloudAccess(JSONArray access){
        try{
            for(int i = 0; i < access.length(); i++){
                String s = access.getString(i).toLowerCase();
                if(s.equals("play")){
                    mPlay = true;
                }else if(s.equals("live")){
                    mLive = true;
                }else if(s.equals("all")){
                    mAll = true;
                }else if(s.equals("backaudio")){
                    mBackaudio = true;
                }else if(s.equals("list")){
                    mList = true;
                }else if(s.equals("ptz")){
                    mPtz = true;
                }
            }
        }catch(JSONException e){
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean allowLive(){
        return mAll || mLive;
    }

    public boolean allowPlay(){
        return mAll || mPlay;
    }

    public boolean allowAll(){
        return mAll;
    }

    public boolean allowList(){
        return mAll || mList;
    }

    public boolean allowPtz(){
        return mAll || mPtz;
    }

    public boolean allowBackAudio(){
        return mAll || mBackaudio;
    }
}
