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

import android.util.Pair;

import com.vxg.cloud.core.CloudAPI;
import com.vxg.cloudsdk.Enums.CloudReturnCodes;
import com.vxg.cloudsdk.Helpers.MLog;
import com.vxg.cloudsdk.Helpers.Msg;
import com.vxg.cloudsdk.Interfaces.ICloudConnection;
import com.vxg.cloudsdk.Interfaces.ICloudUpdate;
import com.vxg.cloudsdk.Interfaces.ICompletionCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;

public class CloudUserInfo extends CloudObject implements ICloudUpdate {
    private static final String TAG = "CloudUserInfo";
    final public static int LOG_LEVEL = 2; //Log.VERBOSE;
    static MLog Log = new MLog(TAG, LOG_LEVEL);

    ICloudConnection mConn;
    CloudAPI api;

    //properties
    int m_id = -1;
    String m_email = "";
    String m_firstname = "";
    String m_lastname = "";
    String m_preferred_name = "";
    int m_cam_limit = -1;
    int m_cam_created = -1;
    //int m_stream_limit = -1;
    //int m_stream_created = -1;


    //async functions
    private LinkedList<Msg> cmd_queue = new LinkedList<Msg>();
    final int CMD_Refresh = 1;

    public CloudUserInfo(ICloudConnection conn){
        mConn = conn;
        api = mConn._getAPI();
    }

    private void _update_props(JSONObject jo1, JSONObject jo2){

        if(jo1 != null){
            try {
                m_id = jo1.getInt("id");
                m_firstname =  jo1.getString("first_name");
                m_lastname =  jo1.getString("last_name");
                m_preferred_name =  jo1.getString("preferred_name");
                m_email =  jo1.getString("email");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }else{
            m_id = -1;
            m_email = "";
            m_firstname = "";
            m_lastname = "";
            m_preferred_name = "";
        }

        if(jo2 != null){
            int lim_h = 0;
            int lim_t = 0;
            int cre_h = 0;
            int cre_t = 0;
            try {
                JSONObject jcam = jo2.getJSONObject("cameras_creation");

                JSONObject jlimits = jcam.getJSONObject("limits");
                lim_h = jlimits.getInt("hosted_cameras");
                lim_t = jlimits.getInt("total_cameras");

                JSONObject jcreated = jcam.getJSONObject("created");
                cre_h = jcreated.getInt("hosted_cameras");
                cre_t = jcreated.getInt("total_cameras");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            m_cam_limit = lim_t;
            m_cam_created = cre_t;
            //m_stream_limit = cre_t - cre_h;
            //m_stream_created = cre_t - cre_h;

        }else {
            m_cam_limit = -1;
            m_cam_created = -1;
            //m_stream_limit = -1;
            //m_stream_created = -1;
        }

    }

    public int getID() {
        return m_id;
    }
    public String getEmail() {
        return m_email;
    }
    public String getFirstName() {
        return m_firstname;
    }
    public String getLastName() {
        return m_lastname;
    }
    public String getPreferredName() {
        return m_preferred_name;
    }

    // next methods sync in any platform
    public int getCameraLimit()    // How much user can create cameras with url
    {
        return m_cam_limit;
    }

    public int getCameraCreated() // How much already created cameras with url
    {
        return m_cam_created;
    }
/*
    public int getStreamerLimit()    // How much user can create cameras for streaming
    {
        return m_stream_limit;
    }

    public int getStreamerCreated() // How much already created cameras for streaming
    {
        return m_stream_created;
    }
*/
    @Override
    public void runt() {
        //processing
        Msg msg = cmd_queue.poll();
        if(msg == null){
            return;
        }

        switch(msg.func_id){
            case CMD_Refresh:
                msg.func_complete.onComplete(null, call_Refresh());
                break;
        }
    }

    //=>ICloudUpdate
    @Override
    public int refresh(ICompletionCallback callback) {
        if(callback == null){
            return CloudReturnCodes.ERROR_BADARGUMENT;
        }

        Msg m = new Msg();
        m.func_id = CMD_Refresh;
        m.args = new ArrayList<Object>();
        m.func_complete = callback;
        cmd_queue.add(m);

        Log.v("=refresh callback="+callback);

        //start processing
        execute();

        return CloudReturnCodes.OK_COMPLETIONPENDING;
    }

    @Override
    public int save(ICompletionCallback callback) {
        return makeError(CloudReturnCodes.ERROR_WRONG_RESPONSE);
    }

    @Override
    public int refreshSync() {
        return call_Refresh();
    }

    @Override
    public int saveSync() {
        return makeError(CloudReturnCodes.ERROR_WRONG_RESPONSE);
    }
    //<=ICloudUpdate

    private int call_Refresh(){
        if(api == null) {
            return makeError(CloudReturnCodes.ERROR_NOT_CONFIGURED);
        }

        Pair<Integer, JSONObject> p1 = api.getAccount();
        Pair<Integer, JSONObject> p2 = api.getAccountCapabilities();

        if(p1.first != 200 || p2.first != 200){
            Log.e("=get userinfo error1="+p1.first+" error2="+p2.first);
            return makeHTTPError(p1.first);
        }

        _update_props(p1.second, p2.second);

        Log.v("==get userinfo OK");
        return makeError(CloudReturnCodes.OK);
    }

    @Override
    public String toString()
    {
        return new StringBuffer()
                .append("id: ").append(getID())
                .append(" Email: ").append(getEmail())
                .append(" FirstName: ").append(getFirstName())
                .append(" LastName: ").append(getLastName())
                .append(" PreferredName: ").append(getPreferredName())
                .append(" CameraLimit: ").append(getCameraLimit())
                .append(" CameraCreated: ").append(getCameraCreated())
                //.append(" StreamerLimit: ").append(getStreamerLimit())
                //.append(" StreamerCreated: ").append(getStreamerCreated())
                .toString();
    }

}
