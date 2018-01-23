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

import android.util.Base64;

import com.vxg.cloudsdk.Enums.CloudReturnCodes;
import com.vxg.cloudsdk.Interfaces.ICloudStreamerCallback;
import com.vxg.cloudsdk.Interfaces.ICompletionCallback;
import com.vxg.cloudsdk.Objects.CloudCamera;
import com.vxg.cloudsdk.Objects.CloudCameraList;
import com.vxg.cloudsdk.Objects.CloudShareConnection;
import com.vxg.cloudsdk.Objects.CloudStreamer;

import org.json.JSONException;
import org.json.JSONObject;

public class CloudStreamerSDK extends CloudStreamer {

    CloudShareConnection mConnection;
    CloudStreamer streamer;


    public CloudStreamerSDK(ICloudStreamerCallback callback) {
        super(callback);
        streamer = this;
    }

    public int setSource(String channel){

        // Decode data on other side, by processing encoded data
        String str_dec = new String(Base64.decode(channel, Base64.DEFAULT));

        String token;
        long camid;
        long cmngrid;
        String svcpUrl = "";
        try {
            JSONObject json = new JSONObject(str_dec);
            token = json.getString("token");
            camid = json.getLong("camid");
            cmngrid = json.getLong("cmngrid");
            if(json.has("svcp") && !json.isNull("svcp")){
                svcpUrl = json.getString("svcp");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return CloudReturnCodes.ERROR_BADARGUMENT;
        }

        mConnection = new CloudShareConnection();
        if(svcpUrl.isEmpty()) {
            mConnection.openSync(token);
        }else{
            mConnection.openSync(token, svcpUrl);
        }

        CloudCameraList clist = new CloudCameraList(mConnection);
        clist.getCamera(camid, new ICompletionCallback() {
            @Override
            public int onComplete(Object o_result, int result) {
                makeError(result);

                if(o_result == null){
                    return 0;
                }
                CloudCamera camera = (CloudCamera) o_result;

                streamer.setCamera(camera);
                return 0;
            }
        });
        return 0;
    }

}
