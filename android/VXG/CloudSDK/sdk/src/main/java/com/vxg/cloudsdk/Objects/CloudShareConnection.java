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

import com.vxg.cloud.core.CloudAPI;
import com.vxg.cloud.core.CloudHelpers;
import com.vxg.cloudsdk.Enums.CloudReturnCodes;
import com.vxg.cloudsdk.Helpers.Msg;
import com.vxg.cloudsdk.Interfaces.ICloudConnection;
import com.vxg.cloudsdk.Interfaces.ICompletionCallback;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedList;

public class CloudShareConnection extends CloudObject implements ICloudConnection {
    private static final String TAG = CloudShareConnection.class.getSimpleName();

    private CloudAPI mCloudAPI = new CloudAPI();
    private Boolean mOpened = false;
    long   m_ServerTimeDiff = 0;

    private LinkedList<Msg> cmd_queue = new LinkedList<Msg>();
    final int CMD_GetServerDiff = 1;


    public int openSync(String share_token){
        return openSync(share_token, "https://web.skyvr.videoexpertsgroup.com");
    }

    public int openSync(String share_token, String baseurl){
        String mProtocol = "https";
        String mHost = "web.skyvr.videoexpertsgroup.com";
        String mPrefixPath = "";

        try {
            URI uri = new URI(baseurl);
            mProtocol = uri.getScheme();
            mHost = uri.getHost();
            mPrefixPath = uri.getPath();

        }catch(URISyntaxException e){
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }

        Log.i(TAG, "Protocol: " + mProtocol);
        Log.i(TAG, "Host: " + mHost);
        Log.i(TAG, "PrefixPath: " + mPrefixPath);

        mCloudAPI.setHost(mHost);
        mCloudAPI.setProtocol(mProtocol);
        mCloudAPI.setPrefixPath(mPrefixPath);
        mCloudAPI.setShareToken(share_token);
        mOpened = true;

        Msg m = new Msg();
        m.func_id = CMD_GetServerDiff;
        m.args = new ArrayList<Object>();
        cmd_queue.add(m);

        return CloudReturnCodes.OK;
    }

    public int open(String share_token, ICompletionCallback c) {
        Log.i(TAG, "");
        openSync(share_token);

        c.onComplete(null, CloudReturnCodes.OK);
        return CloudReturnCodes.OK;
    }

    @Override
    public int close() {
        return 0;
    }

    @Override
    public boolean isOpened() {
        return mOpened;
    }

    @Override
    public CloudUserInfo getUserInfoSync() {
        return null;
    }

    @Override
    public int getUserInfo(ICompletionCallback callback) {
        callback.onComplete(null, CloudReturnCodes.ERROR_NOT_IMPLEMENTED);
        return CloudReturnCodes.ERROR_NOT_IMPLEMENTED;
    }

    @Override
    public long getServerTimeDiff() {
        return m_ServerTimeDiff;
    }

    @Override
    public CloudAPI _getAPI() {
        return mOpened ? mCloudAPI : null;
    }

    @Override
    public long getUserID() {
        return 0;
    }

    @Override
    protected void runt() {

        while (is_started()) {

            Msg msg = cmd_queue.poll();
            if (msg == null) {
                return;
            }
            switch (msg.func_id) {
                case CMD_GetServerDiff:
                    String st = mCloudAPI.getServerTime();
                    if(st != null)
                        m_ServerTimeDiff = CloudHelpers.currentTimestampUTC() - CloudHelpers.parseUTCTime(mCloudAPI.getServerTime());
                    break;
            }
        }
    }
}
