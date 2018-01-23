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

import com.vxg.cloud.core.CloudCameras.CloudCameraDetail;
import com.vxg.cloud.core.CloudAPI;
import com.vxg.cloud.core.CloudResponseWithMeta;
import com.vxg.cloudsdk.Enums.CloudReturnCodes;
import com.vxg.cloudsdk.Enums.PS_Privacy;
import com.vxg.cloudsdk.Helpers.MLog;
import com.vxg.cloudsdk.Helpers.Msg;
import com.vxg.cloudsdk.Interfaces.ICloudConnection;
import com.vxg.cloudsdk.Interfaces.ICloudCameraList;
import com.vxg.cloudsdk.Interfaces.ICompletionCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CloudCameraList extends CloudObject implements ICloudCameraList
{
    private static final String TAG = CloudCameraList.class.getSimpleName();
    final public static int LOG_LEVEL = 2; //Log.VERBOSE;
    static MLog Log = new MLog(TAG, LOG_LEVEL);

    ICloudConnection mConn;
    CloudAPI api;

    //available functions from ICloudCameraList. add immplementation in call_xxx
    private LinkedList<Msg> cmd_queue = new LinkedList<Msg>();
    final int CMD_CreateCamera = 1;  //CreateCamera(String surl)
    final int CMD_GetCamera = 2;
    final int CMD_DeleteCamera = 3;
    final int CMD_ListCameras = 4;
    final int CMD_FindOrCreateCamera = 5;
    final int CMD_CreateCameraForStream = 6;

    public CloudCameraList(ICloudConnection conn){
        if(conn == null){
            mConn = null;
            api = null;
        }else{
            mConn = conn;
            api = mConn._getAPI();
        }
    }


    //=>ICloudCameraList
    @Override
    public CloudCamera createCameraSync(String surl) {
        return call_CreateCamera(surl);
    }

    @Override
    public CloudCamera createCameraForStreamSync() {
        return call_CreateCameraForStream();
    }

    @Override
    public int createCamera(String surl, ICompletionCallback c) {

        if(c == null){
            return CloudReturnCodes.ERROR_BADARGUMENT;
        }

        Msg m = new Msg();
        m.func_id = CMD_CreateCamera;
        m.args = new ArrayList<Object>();
        m.args.add(surl==null?"":surl);
        m.func_complete = c;
        cmd_queue.add(m);

        Log.v("=CreateCamera surl="+surl+" callback="+c);

        //start processing
        execute();

        return CloudReturnCodes.OK_COMPLETIONPENDING;
    }

    @Override
    public int createCameraForStream(ICompletionCallback c) {
        if(c == null){
            return CloudReturnCodes.ERROR_BADARGUMENT;
        }

        Msg m = new Msg();
        m.func_id = CMD_CreateCameraForStream;
        m.args = new ArrayList<Object>();
        m.func_complete = c;
        cmd_queue.add(m);

        Log.v("=createCameraForStream callback="+c);

        //start processing
        execute();

        return CloudReturnCodes.OK_COMPLETIONPENDING;
    }


    @Override
    public CloudCamera getCameraSync(long srcid) {
        return call_GetCamera(srcid);
    }

    @Override
    public CloudCamera findOrCreateCameraSync(String url) {
        return call_FindOrCreateCamera(url);
    }

    @Override
    public int getCamera(long srcid, ICompletionCallback c) {
        if(c == null){
            return CloudReturnCodes.ERROR_NOT_IMPLEMENTED;
        }

        Msg m = new Msg();
        m.func_id = CMD_GetCamera;
        m.args = new ArrayList<Object>();
        m.args.add(srcid);
        m.func_complete = c;
        cmd_queue.add(m);

        Log.v("=GetCamera srcid="+srcid+" callback="+c);

        //start processing
        execute();

        return CloudReturnCodes.OK_COMPLETIONPENDING;
    }

    @Override
    public int findOrCreateCamera(String url, ICompletionCallback callback) {
        if(callback == null){
            return CloudReturnCodes.ERROR_BADARGUMENT;
        }

        Msg m = new Msg();
        m.func_id = CMD_FindOrCreateCamera;
        m.args = new ArrayList<Object>();
        m.args.add(url);
        m.func_complete = callback;
        cmd_queue.add(m);

        Log.v("=findOrCreateCamera url="+url+" callback="+callback);

        //start processing
        execute();

        return CloudReturnCodes.OK_COMPLETIONPENDING;
    }

    @Override
    public int deleteCameraSync(long srcid) {
        return call_DeleteCamera(srcid);
    }

    @Override
    public int deteleCamera(long srcid, ICompletionCallback c) {
        if(c == null){
            return CloudReturnCodes.ERROR_BADARGUMENT;
        }

        Msg m = new Msg();
        m.func_id = CMD_DeleteCamera;
        m.args = new ArrayList<Object>();
        m.args.add(srcid);
        m.func_complete = c;
        cmd_queue.add(m);

        Log.v("=DeleteCamera srcid="+srcid+" callback="+c);

        //start processing
        execute();

        return CloudReturnCodes.OK_COMPLETIONPENDING;
    }

    @Override
    public List<CloudCamera> getCameraListSync(CloudCameraListFilter filter) {
        return call_ListCameras(filter);
    }

    @Override
    public int getCameraList(CloudCameraListFilter filter, ICompletionCallback c) {
        if(c == null){
            return CloudReturnCodes.ERROR_BADARGUMENT;
        }

        Msg m = new Msg();
        m.func_id = CMD_ListCameras;
        m.args = new ArrayList<Object>();
        m.args.add(filter);
        m.func_complete = c;
        cmd_queue.add(m);

        Log.v("=ListCameras filter="+filter+" callback="+c);

        //start processing
        execute();

        return CloudReturnCodes.OK_COMPLETIONPENDING;
    }
    //<=ICloudCameraList

    //=>ICloudObject
    //used default implementation from CloudObject
    //<=ICloudObject

    @Override
    public void runt() {

        //processing
        Msg msg = cmd_queue.poll();
        if(msg == null){
            return;
        }

        switch(msg.func_id){
            case CMD_FindOrCreateCamera:
            {
                CloudCamera camera = call_FindOrCreateCamera(msg.args.get(0).toString());
                msg.func_complete.onComplete(camera, getResultInt());
                break;
            }
            case CMD_CreateCamera:
                {
                    CloudCamera camera = call_CreateCamera(msg.args.get(0).toString());
                    msg.func_complete.onComplete(camera, getResultInt());
                    break;
                }
            case CMD_CreateCameraForStream:
            {
                CloudCamera camera = call_CreateCameraForStream();
                msg.func_complete.onComplete(camera, getResultInt());
                break;
            }
            case CMD_GetCamera:
                {
                    CloudCamera camera = call_GetCamera((Long) msg.args.get(0));
                    msg.func_complete.onComplete(camera, getResultInt());
                    break;
                }
            case CMD_DeleteCamera:
                {
                    msg.func_complete.onComplete(null, call_DeleteCamera((Long) msg.args.get(0)));
                    break;
                }
            case CMD_ListCameras:
                {
                    List<CloudCamera> cameras = call_ListCameras((CloudCameraListFilter)msg.args.get(0));
                    msg.func_complete.onComplete(cameras, getResultInt());
                    break;
                }
        }
    }

    //implementation
    private CloudCamera call_FindOrCreateCamera(String url){
        if(url == null) {
            makeError(CloudReturnCodes.ERROR_BADARGUMENT);
            return null;//CloudReturnCodes.ERROR_BADARGUMENT;
        }
        if(api == null) {
            makeError(CloudReturnCodes.ERROR_NOT_CONFIGURED);
            return null;//CloudReturnCodes.ERROR_NOT_CONFIGURED;
        }

        CloudCameraListFilter filter = new CloudCameraListFilter();
        filter.setPrivacy(PS_Privacy.ps_owner);
        filter.setURL(url);
        List<CloudCamera> list = call_ListCameras(filter);
        if(list == null || list.size()<1){
            //create new camera
            return call_CreateCamera(url);
        }
        return list.get(0);
    }
    private CloudCamera call_CreateCamera(String surl){

        if(surl == null) {
            makeError(CloudReturnCodes.ERROR_BADARGUMENT);
            return null;//CloudReturnCodes.ERROR_BADARGUMENT;
        }
        if(api == null) {
            makeError(CloudReturnCodes.ERROR_NOT_CONFIGURED);
            return null;//CloudReturnCodes.ERROR_NOT_CONFIGURED;
        }


        JSONObject jsonCam = new JSONObject();
        try {
            jsonCam.put("url", surl);
            jsonCam.put("detail", "detail");
            //jsonCam.put("name", "abcd");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        CloudCameraDetail cameraDetail = api.cameraCreate(jsonCam);
        if(cameraDetail.toJSON() == null){
            makeError(CloudReturnCodes.ERROR_NO_CLOUD_CONNECTION);
            return null;
        }
        makeHTTPError(cameraDetail.getErrorStatus());

        CloudCamera camera = new CloudCamera(mConn, cameraDetail.toJSON());
        return camera;
    }

    private CloudCamera call_CreateCameraForStream()
    {
        if(api == null) {
            makeError(CloudReturnCodes.ERROR_NOT_CONFIGURED);
            return null;//CloudReturnCodes.ERROR_NOT_CONFIGURED;
        }

        JSONObject jsonCam = new JSONObject();
        try {
            jsonCam.put("detail", "detail");
            //jsonCam.put("name", "abcd");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        CloudCameraDetail cameraDetail = api.cameraCreate(jsonCam);
        if(cameraDetail.toJSON() == null){
            makeError(CloudReturnCodes.ERROR_NO_CLOUD_CONNECTION);
            return null;
        }
        makeHTTPError(cameraDetail.getErrorStatus());

        CloudCamera camera = new CloudCamera(mConn, cameraDetail.toJSON());
        return camera;
    }

    private CloudCamera call_GetCamera(long srcid){
        if(api == null) {
            makeError(CloudReturnCodes.ERROR_NOT_CONFIGURED);
            return null;//CloudReturnCodes.ERROR_NOT_CONFIGURED;
        }
        CloudCamera camera = new CloudCamera(mConn, srcid);
        makeError(camera.refreshSync());
        return camera;
    }

    private int call_DeleteCamera(long srcid){
        return makeError(api.deleteCamera(srcid));
    }

    private List<CloudCamera> call_ListCameras(CloudCameraListFilter filter){
        ArrayList<CloudCamera> list = new ArrayList<CloudCamera>();

        if(api == null) {
            makeError(CloudReturnCodes.ERROR_NOT_CONFIGURED);
            return list;//CloudReturnCodes.ERROR_NOT_CONFIGURED;
        }

        String login = filter.login;
        String password = filter.password;
        CloudResponseWithMeta meta = api.getCameraList(filter._getValues());
        if(meta == null){
            makeError(CloudReturnCodes.ERROR_NOT_FOUND);
            return list;
        }

        JSONArray camarray = meta.getObjects();
        if(camarray == null){
            makeError(CloudReturnCodes.ERROR_NOT_FOUND);
            return list;
        }
        int cnt = camarray.length();
        for(int i=0; i<cnt; i++){
            JSONObject jcam = null;
            try {
                jcam = camarray.getJSONObject(i);
            } catch (JSONException e) {
                e.printStackTrace();
                continue;
            }
            if(jcam == null)
                continue;

            if(login != null) {
                String sl = null;
                try {
                    sl = jcam.getString("login");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if(sl == null || !login.equals(sl)){
                    continue;
                }
            }
            if(password != null) {
                String sl = null;
                try {
                    sl = jcam.getString("password");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if(sl == null || !password.equals(sl)){
                    continue;
                }
            }

            CloudCamera cam = new CloudCamera(mConn, jcam);
            list.add(cam);
        }
        makeError(CloudReturnCodes.OK);

        return list;
    }
}