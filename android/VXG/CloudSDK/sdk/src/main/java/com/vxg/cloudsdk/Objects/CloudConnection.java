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

import android.os.SystemClock;

import com.vxg.cloud.core.CloudAPI;
import com.vxg.cloud.core.CloudHelpers;
import com.vxg.cloudsdk.Enums.CloudReturnCodes;
import com.vxg.cloudsdk.Helpers.MLog;
import com.vxg.cloudsdk.Helpers.Msg;
import com.vxg.cloudsdk.Interfaces.ICloudConnection;
import com.vxg.cloudsdk.Interfaces.ICompletionCallback;
import com.vxg.cnvrclient2.AccountProvider.AccountProviderLoginResult;
import com.vxg.cnvrclient2.AccountProvider.CNVRClient2;

import java.util.ArrayList;
import java.util.LinkedList;


public abstract class CloudConnection extends CloudObject implements ICloudConnection
{
    private static final String TAG = "CloudConnection";
    final public static int LOG_LEVEL = 2; //Log.VERBOSE;
    static MLog Log = new MLog(TAG, LOG_LEVEL);

    //available functions from ICloudConnection. add immplementation in call_xxx
    private LinkedList<Msg> cmd_queue = new LinkedList<Msg>();
    final int CMD_OPEN = 1;  //Open(String skey)
    final int CMD_GetUserInfo = 2;

    CloudAPI api = new CloudAPI(); //CloudAPI.getInstance();
    String mLogin;
    String mPassword;
    boolean mis_opened = false;
    long   m_time_last_check_token = 0;
    long   m_ServerTimeDiff = 0;
    long   m_UserID = -1;

    protected int openSync(String login, String password){
        int ret = call_open(login, password);
        if(ret == CloudReturnCodes.OK){
            //start processing
            start();
        }

        return ret;
    }
    protected int open(String login, String password, ICompletionCallback c) {
        if(c == null)
            return CloudReturnCodes.ERROR_BADARGUMENT;

        Msg m = new Msg();
        m.func_id = CMD_OPEN;
        m.args = new ArrayList<Object>();
        m.args.add(login==null?"":login);
        m.args.add(password==null?"":password);
        m.func_complete = c;
        cmd_queue.add(m);

        Log.v("=open login="+login+" password="+password+" callback="+c);

        //start processing
        start();

        synchronized (cmd_queue) {
            cmd_queue.notifyAll();
        }

        return CloudReturnCodes.OK_COMPLETIONPENDING;
    }
    protected int openSync(String token, long time_expires) {
        return CloudReturnCodes.ERROR_NOT_IMPLEMENTED;
    }
    protected int open(String token, long time_expires, ICompletionCallback c) {
        return CloudReturnCodes.ERROR_NOT_IMPLEMENTED;
    }

    public long getUserID(){
        return m_UserID;
    }

    @Override
    public int close() {
        stop(0);

        mis_opened = false;

        return 0;
    }

    public CloudAPI _getAPI(){ return api; }


    @Override
    public boolean isOpened() {
        return mis_opened;
    }


    @Override
    public CloudUserInfo getUserInfoSync() {
        return call_CMD_GetUserInfo();
    }

    @Override
    public int getUserInfo(ICompletionCallback c) {
        if(c == null)
            return CloudReturnCodes.ERROR_BADARGUMENT;

        Msg m = new Msg();
        m.func_id = CMD_GetUserInfo;
        m.func_complete = c;
        cmd_queue.add(m);

        Log.v("=getUserInfo="+" callback="+c);

        //start processing
        start();

        synchronized (cmd_queue) {
            cmd_queue.notifyAll();
        }

        return CloudReturnCodes.OK_COMPLETIONPENDING;
    }

    @Override
    public long getServerTimeDiff() { // difference time between local utc and server utc times
        return m_ServerTimeDiff;
    }


    //=>ICloudObject
    //used default implementation from CloudObject
    //<=ICloudObject

    @Override
    public void runt() {
        boolean is_close = false;
        while( is_started() && !is_close){

            synchronized (cmd_queue) {
                try {
                    cmd_queue.wait(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            check_token(false);

            Msg msg = cmd_queue.poll();
            if(msg == null){
                sleep(1);
                continue;
            }


            switch(msg.func_id){
                case CMD_OPEN:
                    msg.func_complete.onComplete(null, call_open(msg.args.get(0).toString(), msg.args.get(1).toString()));
                    break;
                case CMD_GetUserInfo:
                    {
                        CloudUserInfo userinfo = call_CMD_GetUserInfo();
                        msg.func_complete.onComplete(userinfo, getResultInt());
                        break;
                    }
            }
        }
    }

    //implementation
    protected boolean check_token(boolean force){
        if(!mis_opened)
            return false;

        long last_check = SystemClock.elapsedRealtime()-m_time_last_check_token;
        if(last_check < 10000 && !force){ //once per 10sec
            return !isInvalidToken();
        }
        m_time_last_check_token = SystemClock.elapsedRealtime();
        //Log.v("=check_token");

        if(isInvalidToken()) {
            CNVRClient2 accp2 = CNVRClient2.getInstance();
            AccountProviderLoginResult result = accp2.login(mLogin, mPassword);

            if (result.hasError()) {
                makeHTTPError(result.getErrorCode());
                Log.e("Login failed: [Error code " + result.getErrorCode() + "] " + result.getErrorDetail());
                if (result.getErrorCode() == 401) {
                    mLastErrorStr = "Invalid user or password";
                } else {
                    mLastErrorStr = "Error " + result.getErrorCode() + ": " + result.getErrorDetail();
                }
                return false;
            }else{
                makeHTTPError(200);
            }
            api.setHost(accp2.getInfo().getServiceProviderHost());
            api.setToken(accp2.getServiceProviderToken());

            //recalc m_ServerTimeDiff
            String st = api.getServerTime();
            if(st != null)
                m_ServerTimeDiff = CloudHelpers.currentTimestampUTC() - CloudHelpers.parseUTCTime(st);

            CloudUserInfo userInfo = call_CMD_GetUserInfo();
            if(userInfo == null)
                return false;
            m_UserID = userInfo.getID();
        }else{
            makeError(CloudReturnCodes.OK);
        }
        //Log.v("=check_token ret="+getResultInt());

        return true;
    }

    protected boolean isInvalidToken(){
        if(api.getToken() != null){
            if(api.getToken().calcExpireTime() < 10*60000){ // less then 10 minutes
                Log.i("isInvalidToken, token is expired");
                return true;
            }
            //Log.i("isInvalidToken, token is not expired (expire after: " + api.getToken().calcExpireTime() + ")");
            return false;
        }
        //Log.i("isInvalidToken, token is null");
        return true;
    }

    private int call_open(String login, String password){
        Log.v("=>call_open login="+login+" password="+password);

        api.resetToken();

        m_UserID = -1;
        mLogin = login;
        mPassword = password;
        mis_opened = true;

        int ret = (check_token(true)? CloudReturnCodes.OK : CloudReturnCodes.ERROR_ACCESS_DENIED);

        Log.v("<=call_open ret="+ret+" ServerTimeDiff="+m_ServerTimeDiff);
        return makeError(ret);
    }

    private CloudUserInfo call_CMD_GetUserInfo(){
        if(!mis_opened){
            makeError(CloudReturnCodes.ERROR_NO_CLOUD_CONNECTION);
            return null;
        }
        CloudUserInfo ui = new CloudUserInfo(this);
        makeError(ui.refreshSync());
        return ui;
    }

 }