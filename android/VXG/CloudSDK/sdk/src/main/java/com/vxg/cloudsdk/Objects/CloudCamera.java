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


import android.support.annotation.NonNull;
import android.util.Pair;

import com.vxg.cloud.core.CloudAPI;
import com.vxg.cloud.core.CloudCameras.CloudCameraDetail;
import com.vxg.cloud.core.CloudHelpers;
import com.vxg.cloud.core.CloudResponseWithMeta;
import com.vxg.cloudsdk.Enums.CloudCameraRecordingMode;
import com.vxg.cloudsdk.Enums.CloudCameraStatus;
import com.vxg.cloudsdk.Enums.CloudReturnCodes;
import com.vxg.cloudsdk.Helpers.MLog;
import com.vxg.cloudsdk.Helpers.Msg;
import com.vxg.cloudsdk.Interfaces.ICloudConnection;
import com.vxg.cloudsdk.Interfaces.ICloudCamera;
import com.vxg.cloudsdk.Interfaces.ICloudUpdate;
import com.vxg.cloudsdk.Interfaces.ICompletionCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class CloudCamera extends CloudObject implements ICloudCamera, ICloudUpdate
{
    private static final String TAG = "CloudCamera";
    final public static int LOG_LEVEL = 2; //Log.VERBOSE;
    static MLog Log = new MLog(TAG, LOG_LEVEL);

    final public static int TYPE_SOURCE_PULL = 1;
    final public static int TYPE_SOURCE_PUSH = 2;

    long m_srcid = -1;
    int m_srctype = -1; //unknown
    boolean m_isDirty = false; //if true, object properties were changed

    private ICloudConnection mConn;
    private CloudAPI api;
    private CloudCameraDetail cameraDetail;

    private String mUrl=null;
    private String mTimezone=null;
    private String mLogin=null;
    private String mPassword = null;
    private String mName;
    private CloudCameraStatus mCameraStatus = CloudCameraStatus.OFFLINE;
    private CloudCameraRecordingMode mRecMode = CloudCameraRecordingMode.NO_RECORDING;
    private boolean misRecording = false; //actual recording ?
    private long mDeleteAt = 0;
    private boolean misPublic = false;
    private boolean misOwner = false; //own camera
    private JSONObject mOrigJson = null;
    private Map<String,String> mChangeSet_str = new HashMap<>();
    private long mCmngrID = -1;
    private double mlat = 0.0f;
    private double mlng = 0.0f;

    //sharing
    private CloudSharingsToken mSharingsToken = null;
    private CloudSharingsToken mSharingsTokenForStream = null;

    //async functions
    private LinkedList<Msg> cmd_queue = new LinkedList<Msg>();
    private final int CMD_Refresh = 1;
    final int CMD_Save = 2;
    final int CMD_GetPreviewUrl = 3;
    final int CMD_GetTimeline = 4;
    final int CMD_GetTimelineDays = 5;
    final int CMD_EnableSharing = 6;
    final int CMD_DisableSharing = 7;
    final int CMD_EnableSharingForStream = 8;
    final int CMD_DisableSharingForStream = 9;

    public CloudCamera(ICloudConnection conn, long id){
        mConn = conn;
        api = mConn._getAPI();
        m_srcid = id;
    }

    public CloudCamera(ICloudConnection conn, JSONObject data)
    {
        mConn = conn;
        api = mConn._getAPI();

        cameraDetail = new CloudCameraDetail(data);
        m_srcid = cameraDetail.getID();
        mUrl = cameraDetail.getUrl();
        makeHTTPError(cameraDetail.getErrorStatus());
        mLastErrorStr = cameraDetail.getErrorDetail();

        _refresh_sharing(true);
        _refresh_vals(data);
    }

    // for a some ad-hoc solutions
    public JSONObject _origJson(){
        return mOrigJson;
    }

    private void _refresh_vals(JSONObject data){
        if(data == null)
            return;
        mOrigJson = data;

        mName = getJSONString(data, "name", null);
        mLogin = getJSONString(data, "login", null);
        mPassword = getJSONString(data, "password", null);
        mTimezone = getJSONString(data, "timezone", null);

        String _rec_mode = getJSONString(data, "rec_mode", "");
        if(_rec_mode.equals("on")){
            mRecMode = CloudCameraRecordingMode.CONTINUES;
        }else if(_rec_mode.equals("by_event")){
            mRecMode = CloudCameraRecordingMode.BY_EVENT;
        }else if(_rec_mode.equals("off")){
            mRecMode = CloudCameraRecordingMode.NO_RECORDING;
        }else{
            Log.e("Unknown rec_mode value: " + _rec_mode + " (set off recording)");
            mRecMode = CloudCameraRecordingMode.NO_RECORDING;
        }

        String _rec_status = getJSONString(data, "rec_status", "");
        misRecording = _rec_status.equals("on");


        String _status = getJSONString(data, "status", "");
        //'active', 'unauthorized', 'inactive', 'inactive_by_scheduler', 'offline'
        if(_status.equals("active")){
            mCameraStatus = CloudCameraStatus.ACTIVE;
        }else if(_status.equals("unauthorized")){
            mCameraStatus = CloudCameraStatus.UNAUTHORIZED;
        }else if(_status.equals("inactive")){
            mCameraStatus = CloudCameraStatus.INACTIVE;
        }else if(_status.equals("inactive_by_scheduler")){
            mCameraStatus = CloudCameraStatus.INACTIVE_BY_SCHEDULER;
        }else if(_status.equals("offline")){
            mCameraStatus = CloudCameraStatus.OFFLINE;
        }else{
            Log.e("Uknown camera status: " + _status + " (set offline)");
            mCameraStatus = CloudCameraStatus.OFFLINE;
        }

        String deleteAt = getJSONString(data, "delete_at", null);
        mDeleteAt = deleteAt != null ? CloudHelpers.parseUTCTime(deleteAt) : 0;

        misPublic = getJSONBoolean(data, "public", false);

        try {
            JSONObject jown = data.getJSONObject("owner");
            if(jown != null){
                long userID = jown.getLong("id");
                misOwner = (mConn.getUserID() == userID);
            }else{
                misOwner = true;
            }
        } catch (JSONException e) {
            //e.printStackTrace();
            misOwner = true;
        }

        mCmngrID = getJSONLong(data, "cmngrid", 0);
        mlat = getJSONDouble(data, "latitude", 0.0f);
        mlng = getJSONDouble(data, "longitude", 0.0f);
    }

    private long getJSONLong(JSONObject j, String name, long _default){
        if(!j.has(name)){
            return _default;
        }
        if(j.isNull(name)){
            return _default;
        }
        long result = _default;
        try {
            result = j.getLong(name);
        } catch (JSONException e) {
            Log.e(e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    private double getJSONDouble(JSONObject j, String name, double _default){
        if(!j.has(name)){
            return _default;
        }
        if(j.isNull(name)){
            return _default;
        }
        double result = _default;
        try {
            result = j.getDouble(name);
        } catch (JSONException e) {
            Log.e(e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    private boolean getJSONBoolean(JSONObject j, String name, boolean _default){
        if(!j.has(name)){
            return _default;
        }
        if(j.isNull(name)){
            return _default;
        }
        boolean result = _default;
        try {
            result = j.getBoolean(name);
        } catch (JSONException e) {
            Log.e(e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    private String getJSONString(JSONObject j, String name, String _default){
        if(!j.has(name)){
            return _default;
        }
        if(j.isNull(name)){
            return _default;
        }
        String result = _default;
        try {
            result = j.getString(name);
        } catch (JSONException e) {
            Log.e(e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    private void _refresh_sharing(boolean update_json){
        if(api == null)
            return;
        if(api.usedShareToken()){
            return;
        }

        if(update_json) {
            mSharingsToken = null;
            mSharingsTokenForStream = null;

            ArrayList<Pair<String, String>> params_get = new ArrayList<>();
            // params_get.add(new Pair<>("name", "COMMON_SHARING_TOKEN"));
            params_get.add(new Pair<>("detail", "detail"));
            Pair<Integer, JSONObject> p = api.getCameraSharingTokensList(getID(), params_get);
            if (p != null && p.second != null) {
                JSONObject jo = p.second;
                try {
                    JSONArray objects = jo.getJSONArray("objects");
                    for(int i = 0; i < objects.length(); i++){
                        CloudSharingsToken st = new CloudSharingsToken(objects.getJSONObject(i));
                        if(st.getName().equals(CloudSharingsToken.COMMON_SHARING_TOKEN) && mSharingsToken == null){
                            mSharingsToken = st;
                        }
                        if(st.getName().equals(CloudSharingsToken.COMMON_SHARING_TOKEN_FOR_STREAM) && mSharingsTokenForStream == null){
                            mSharingsTokenForStream = st;
                        }
                    }
                } catch (JSONException e) {
                    Log.e(e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    //public CloudAPI getCoreApi(){ return api; }
    public ICloudConnection _getCloudConnection(){return mConn;};

    @Deprecated
    public long _getCmngrID() { return mCmngrID; };

    public long getCameraManagerID() { return mCmngrID; };

    private void setDirty(boolean d) { m_isDirty = d; }
    private boolean isDirty() {return m_isDirty;}

    //=> ICloudCamera
    @Override
    public long getID() {
        return m_srcid;
    }

    @Override
    public String getPreviewURLSync() {
        return call_GetPreviewUrl();
    }

    @Override
    public int getPreviewURL(ICompletionCallback callback) {
        if(callback == null){
            return CloudReturnCodes.ERROR_BADARGUMENT;
        }

        Msg m = new Msg();
        m.func_id = CMD_GetPreviewUrl;
        m.args = new ArrayList<Object>();
        m.func_complete = callback;
        cmd_queue.add(m);

        Log.v("=getPreviewURL id="+ getID()+" callback="+callback);

        //start processing
        execute();
        return CloudReturnCodes.OK_COMPLETIONPENDING;
    }

    @Override
    public long getDeleteAt() {
        return mDeleteAt;
    }

    @Override
    public void setDeleteAt(long utc_time) {
        mDeleteAt = utc_time;
        mChangeSet_str.put("delete_at", CloudHelpers.formatTime(mDeleteAt));
        setDirty(true);
    }

    @Override
    public void setURL(String url) {

        if(url == null) {
            makeError(CloudReturnCodes.ERROR_BADARGUMENT);
            return;
        }
        mUrl = url;
        int ch0 = mUrl.indexOf("://");
        int ch1 = mUrl.indexOf("@");
        if(ch0 != -1 && ch1 != -1){
            mLogin = mUrl.substring(ch0+3, ch1);
            String[] ss = mLogin.split(":");
            if(ss.length>0){
                mLogin = ss[0];
            }
            if(ss.length>1){
                mPassword = ss[1];
            }
        }
        mChangeSet_str.put("url", mUrl);
        setDirty(true);
    }

    @Override
    public String getURL() {
        return mUrl;
    }

    @Override
    public void setURLLogin(String login) {
        mLogin = login;
        mChangeSet_str.put("login", mLogin);
        setDirty(true);
    }

    @Override
    public String getURLLogin() {
        return mLogin;
    }

    @Override
    public void setURLPassword(String password) {
        mPassword = password;
        mChangeSet_str.put("password", mPassword);
        setDirty(true);
    }

    @Override
    public String getURLPassword() {
        return mPassword;
    }

    @Override
    public void setTimezone(String timezone) {
        mTimezone = timezone;
        mChangeSet_str.put("timezone", mTimezone);
        setDirty(true);
    }

    @Override
    public String getTimezone() {
        return mTimezone;
    }

    @Override
    public void setName(String name) {
        mName = name;
        mChangeSet_str.put("name", mName);
        setDirty(true);
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public CloudCameraStatus getStatus() {
        return mCameraStatus;
    }

    @Override
    public void setPublic(boolean is_public) {
        misPublic = is_public;
        mChangeSet_str.put("public", ""+misPublic);
        setDirty(true);
    }

    @Override
    public boolean isPublic() {
        return misPublic;
    }

    @Override
    public boolean isOwner() {
        return misOwner;
    }

    @Override
    public void setRecordingMode(CloudCameraRecordingMode rec_mode) {

        mRecMode = rec_mode;
        mChangeSet_str.put("rec_mode", ""+mRecMode);
        setDirty(true);
    }

    @Override
    public CloudCameraRecordingMode getRecordingMode() {
        return mRecMode;
    }

    @Override
    public boolean isRecording() {
        return misRecording;
    }

    @Override
    public CloudTimeline getTimelineSync(long start, long end) {
        return call_GetTimeline(start, end);
    }

    @Override
    public int getTimeline(long start, long end, ICompletionCallback callback) {
        if(callback == null){
            return CloudReturnCodes.ERROR_BADARGUMENT;
        }

        Msg m = new Msg();
        m.func_id = CMD_GetTimeline;
        m.args = new ArrayList<Object>();
        m.args.add(start);
        m.args.add(end);
        m.func_complete = callback;
        cmd_queue.add(m);

        Log.v("=getTimeline srcid="+ getID()+" callback="+callback+" start="+start+" end="+end);

        //start processing
        execute();

        return CloudReturnCodes.OK_COMPLETIONPENDING;
    }

    @Override
    public ArrayList<Long> getTimelineDaysSync(boolean use_timezone) {
        return call_GetTimelineDays(use_timezone);
    }

    @Override
    public int getTimelineDays(boolean use_timezone, ICompletionCallback callback) {
        if(callback == null){
            return CloudReturnCodes.ERROR_BADARGUMENT;
        }

        Msg m = new Msg();
        m.func_id = CMD_GetTimelineDays;
        m.args = new ArrayList<Object>();
        m.args.add((Boolean)use_timezone);
        m.func_complete = callback;
        cmd_queue.add(m);

        Log.v("=getTimelineDays srcid="+ getID()+" callback="+callback+" use_timezone="+use_timezone);

        //start processing
        execute();

        return CloudReturnCodes.OK_COMPLETIONPENDING;
    }

    @Override
    public void setLatLngBounds(double latitude, double longitude) {
        mlat = latitude;
        mlng = longitude;
        mChangeSet_str.put("latitude", Double.toString(latitude));
        mChangeSet_str.put("longitude", Double.toString(longitude));
    }

    @Override
    public double getLat() {
        return mlat;
    }

    @Override
    public double getLng() {
        return mlng;
    }

    @Override
    public String enableSharingSync() {
        return call_EnableSharing();
    }

    @Override
    public int enableSharing(ICompletionCallback callback) {
        if(callback == null){
            return CloudReturnCodes.ERROR_BADARGUMENT;
        }

        Msg m = new Msg();
        m.func_id = CMD_EnableSharing;
        m.args = new ArrayList<Object>();
        m.func_complete = callback;
        cmd_queue.add(m);

        Log.v("=enableSharing srcid="+ getID()+" callback="+callback);

        //start processing
        execute();

        return CloudReturnCodes.OK_COMPLETIONPENDING;
    }

    @Override
    public int disableSharingSync() {
        return call_DisableSharing();
    }

    @Override
    public int disableSharing(ICompletionCallback callback) {
        if(callback == null){
            return CloudReturnCodes.ERROR_BADARGUMENT;
        }

        Msg m = new Msg();
        m.func_id = CMD_DisableSharing;
        m.args = new ArrayList<Object>();
        m.func_complete = callback;
        cmd_queue.add(m);

        Log.v("=disableSharing srcid="+ getID()+" callback="+callback);

        //start processing
        execute();

        return CloudReturnCodes.OK_COMPLETIONPENDING;
    }

    @Override
    public boolean isSharing() {
        return mSharingsToken != null && mSharingsToken.isEnabled();
    }

    @Override
    public String enableSharingForStreamSync() {
        return call_EnableSharingForStream();
    }

    private int executeAsyncCommand(String name, int command_id, ICompletionCallback callback){
        if(callback == null){
            return CloudReturnCodes.ERROR_BADARGUMENT;
        }
        Msg m = new Msg();
        m.func_id = command_id;
        m.args = new ArrayList<Object>();
        m.func_complete = callback;
        cmd_queue.add(m);
        Log.v("=" + name + " srcid="+ getID()+" callback="+callback);
        //start processing
        execute();
        return CloudReturnCodes.OK_COMPLETIONPENDING;
    }

    @Override
    public int enableSharingForStream(ICompletionCallback callback) {
        return executeAsyncCommand("enableSharingForStream", CMD_EnableSharingForStream, callback);
    }

    @Override
    public int disableSharingForStreamSync() {
        return call_DisableSharingForStream();
    }

    @Override
    public int disableSharingForStream(ICompletionCallback callback) {
        return executeAsyncCommand("disableSharingForStream", CMD_DisableSharingForStream, callback);
    }

    //<= ICloudCamera

    @Override
    public int refreshSync() {
        return call_Refresh();
    }

    @Override
    public int refresh(ICompletionCallback c) {
        if(c == null){
            return CloudReturnCodes.ERROR_BADARGUMENT;
        }

        Msg m = new Msg();
        m.func_id = CMD_Refresh;
        m.args = new ArrayList<Object>();
        m.func_complete = c;
        cmd_queue.add(m);

        Log.v("=refresh id="+ getID()+" callback="+c);

        //start processing
        execute();

        return CloudReturnCodes.OK_COMPLETIONPENDING;
    }

    @Override
    public int saveSync() {
        return call_Save();
    }

    @Override
    public int save(ICompletionCallback c) {
        if(c == null){
            return CloudReturnCodes.ERROR_BADARGUMENT;
        }

        Msg m = new Msg();
        m.func_id = CMD_Save;
        m.args = new ArrayList<Object>();
        m.func_complete = c;
        cmd_queue.add(m);

        Log.v("=save id="+ getID()+" callback="+c);

        //start processing
        execute();

        return CloudReturnCodes.OK_COMPLETIONPENDING;
    }

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
            case CMD_Save:
                msg.func_complete.onComplete(null, call_Save());
                break;
            case CMD_GetPreviewUrl:
                {
                    String url = call_GetPreviewUrl();
                    msg.func_complete.onComplete(url, getResultInt());
                    break;
                }
            case CMD_GetTimeline:
                {
                    CloudTimeline timeline = call_GetTimeline((long)msg.args.get(0), (long)msg.args.get(1));
                    msg.func_complete.onComplete(timeline, getResultInt());
                    break;
                }
            case CMD_GetTimelineDays:
            {
                ArrayList<Long> days = call_GetTimelineDays((Boolean)msg.args.get(0));
                msg.func_complete.onComplete(days, getResultInt());
                break;
            }
            case CMD_EnableSharing: {
                String access_token = call_EnableSharing();
                msg.func_complete.onComplete(access_token, getResultInt());
                break;
            }
            case CMD_DisableSharing: {
                msg.func_complete.onComplete(null, call_DisableSharing());
                break;
            }
            case CMD_EnableSharingForStream: {
                msg.func_complete.onComplete(null, call_DisableSharingForStream());
                break;
            }
            case CMD_DisableSharingForStream: {
                msg.func_complete.onComplete(null, call_DisableSharingForStream());
                break;
            }
        }
    }

    private int call_Refresh(){
        //refresh camera detail
        cameraDetail = api.getCamera(getID());
        makeHTTPError(cameraDetail.getErrorStatus());
        mLastErrorStr = cameraDetail.getErrorDetail();
        if(!cameraDetail.hasError()){
            //update properties
            mUrl = cameraDetail.getUrl();

            _refresh_sharing(true);
            _refresh_vals(cameraDetail.toJSON());

            setDirty(false);
        }
        Log.v("=call_Refresh() result="+cameraDetail.getErrorStatus()+" mUrl="+mUrl);
        return mLastErrorInt;
    }

    private int call_Save(){
        JSONObject data = new JSONObject();
        try {
            // put strings from changeset
            for(Map.Entry<String, String> entry : mChangeSet_str.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if(key.equals("rec_mode")){
                    String rec_mode = "off";
                    switch(mRecMode){
                        case CONTINUES:
                            rec_mode = "on";
                            break;
                        case BY_EVENT:
                            rec_mode = "by_event";
                            break;
                    }
                    data.put( "rec_mode", rec_mode);
                }else if(key.equals("timezone")) {
                    JSONObject data2 = new JSONObject();
                    data2.put("timezone", value);
                    makeHTTPError(api.updateCameraManager(mCmngrID, data2).first);
                    if (hasError())
                        return getResultInt();
                }else if(key.equals("public")){
                    data.put(key, misPublic);
                }else if(key.equals("owner")){
                    data.put(key, misOwner);
                }else {
                    data.put(key, value);
                }

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        setDirty(false);

        return makeError(api.updateCamera(getID(), data));
    }

    private String  call_GetPreviewUrl(){
        Pair<Integer, JSONObject> p1 = api.getCameraPreviewUrl(m_srcid);
        if(p1.first != 200){
            makeHTTPError(p1.first);
            return null;
        }
        makeError(CloudReturnCodes.OK);

        try {
            String stime = p1.second.getString("time");
            if(CloudHelpers.currentTimestampUTC()-CloudHelpers.parseUTCTime(stime) > 60*1000){
                //update if > 1 min
                Pair<Integer, JSONObject> p2 = api.postCameraPreviewUpdate(m_srcid);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String surl = "";
        try {
            surl = p1.second.getString("url");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return surl;
    }

    private CloudTimeline call_GetTimeline(long start, long end)
    {
        if(end < start || end - start > 2*24*3600*1000){ //2days limit
            makeError(CloudReturnCodes.ERROR_BADARGUMENT);
            return null;
        }

        CloudTimeline timeline = new CloudTimeline();
        timeline.start = start;
        timeline.end = end;
        timeline.periods = new ArrayList<Pair<Long, Long>>();

        int offset = 0;
        int limit = 100;
        long last_end = 0;
        boolean is_repeat = true;
        while(is_repeat) {

            CloudResponseWithMeta meta = api.storageTimeline(getID(), offset, limit, CloudHelpers.formatTime(start), CloudHelpers.formatTime(end));
            if (meta == null) {
                //makeError(CloudReturnCodes.ERROR_WRONG_RESPONSE);
                break;
            }

            JSONArray ja = meta.getObjects();
            if(ja == null || ja.length() == 0 )
                break;

            int i = 0;
            int processed = 0;
            try {
                for (i = 0; i < ja.length(); i++) {
                    JSONObject j = ja.getJSONObject(i);
                    JSONArray jperiod = j.getJSONArray("5");
                    for( int k=0; k< jperiod.length(); k++) {
                        JSONArray j2 = jperiod.getJSONArray(k);
                        String sstart = j2.getString(0);
                        String sdur = j2.getString(1);

                        long tstart = CloudHelpers.parseTime(sstart);
                        long tend = tstart;
                        try {
                            tend += Long.parseLong(sdur)*1000;
                        } catch (java.lang.NumberFormatException e) {
                            e.printStackTrace();
                            continue;
                        }
                        if(tstart < last_end){
                            is_repeat = false;
                            break;
                        }

                        start = tend+1000;
                        last_end = tend;

                        Pair<Long, Long> p = new Pair<>(tstart, tend);
                        timeline.periods.add(p);
                        Log.v("=call_GetTimeline " + j2.toString() + " ("+CloudHelpers.formatTime(tstart)+", "+CloudHelpers.formatTime(tend)+")");
                        processed++;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            is_repeat = (processed>0);
            //offset += limit;
        }

        makeError(CloudReturnCodes.OK);
        return timeline;
    }

    private ArrayList<Long> call_GetTimelineDays(boolean use_timezone) {

        Pair<Integer,CloudResponseWithMeta> p1 = api.storageTimelineDays(getID(), use_timezone);
        if(p1.first != 200){
            makeHTTPError(p1.first);
            return null;
        }
        makeError(CloudReturnCodes.OK);

        ArrayList<Long> list_days = new ArrayList<Long>();

        CloudResponseWithMeta meta = p1.second;
        if(meta!=null) {
            JSONArray ja = meta.getObjects();
            for (int i = 0; i < ja.length(); i++) {
                long t = 0;
                try {
                    t = CloudHelpers.parseUTCTime(ja.get(i).toString() + "T00:00:00");
                } catch (JSONException e) {
                    e.printStackTrace();
                    continue;
                }
                list_days.add((Long) t);
            }
        }

        return list_days;
    }

    private String call_EnableSharing(){
        if(api == null){
            makeError(CloudReturnCodes.ERROR_NO_CLOUD_CONNECTION);
            return null;
        }
        if(api.usedShareToken()){
            makeError(CloudReturnCodes.ERROR_ACCESS_DENIED);
            return null;
        }
        _refresh_sharing(true);
        if(mSharingsToken == null){
            //create sharing
            JSONObject params_put = new JSONObject();
            try {
                params_put.put("name", CloudSharingsToken.COMMON_SHARING_TOKEN);
                params_put.put("enabled", true);
            } catch (JSONException e) {
                Log.e(e.getMessage());
                e.printStackTrace();
            }

            Pair<Integer, JSONObject> p = api.creareCameraSharingToken(getID(), params_put);
            if(p.first != 0 && p.first != 200){
                makeHTTPError(p.first);
                return null;
            }
            mSharingsToken = new CloudSharingsToken(p.second);
        }
        if(mSharingsToken.hasError()){
            makeError(CloudReturnCodes.ERROR_FORBIDDEN);
            return null;
        }

        if(!mSharingsToken.isEnabled()){
            //update sharing enabled:true
            JSONObject params_put = new JSONObject();
            try {
                params_put.put("name", CloudSharingsToken.COMMON_SHARING_TOKEN);
                params_put.put("enabled", true);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            long shid = mSharingsToken.getID();

            Pair<Integer, JSONObject> p = api.updateCameraSharingToken(getID(), shid, params_put);
            if(p.first != 0 && p.first != 200){
                makeHTTPError(p.first);
                return null;
            }

            mSharingsToken.setEnabled(true);
        }

        if(mSharingsToken.hasError()){
            makeError(CloudReturnCodes.ERROR_FORBIDDEN);
            return null;
        }

        String token = mSharingsToken.makeAccessToken(getID());
        makeError(CloudReturnCodes.OK);
        return token;
    }

    private String call_EnableSharingForStream(){
        if(api == null){
            makeError(CloudReturnCodes.ERROR_NO_CLOUD_CONNECTION);
            return null;
        }
        if(api.usedShareToken()){
            makeError(CloudReturnCodes.ERROR_ACCESS_DENIED);
            return null;
        }
        _refresh_sharing(true);
        if(mSharingsTokenForStream == null){
            //create sharing
            JSONObject params_put = new JSONObject();
            try {
                params_put.put("name", CloudSharingsToken.COMMON_SHARING_TOKEN_FOR_STREAM);
                params_put.put("enabled", true);
            } catch (JSONException e) {
                Log.e(e.getMessage());
                e.printStackTrace();
            }

            Pair<Integer, JSONObject> p = api.creareCameraSharingToken(getID(), params_put);
            if(p.first != 0 && p.first != 200){
                makeHTTPError(p.first);
                return null;
            }
            mSharingsTokenForStream = new CloudSharingsToken(p.second);
        }
        if(mSharingsTokenForStream.hasError()){
            makeError(CloudReturnCodes.ERROR_FORBIDDEN);
            return null;
        }

        if(!mSharingsTokenForStream.isEnabled()){
            //update sharing enabled:true
            JSONObject params_put = new JSONObject();
            try {
                params_put.put("name", CloudSharingsToken.COMMON_SHARING_TOKEN_FOR_STREAM);
                params_put.put("enabled", true);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            long shid = mSharingsTokenForStream.getID();

            Pair<Integer, JSONObject> p = api.updateCameraSharingToken(getID(), shid, params_put);
            if(p.first != 0 && p.first != 200){
                makeHTTPError(p.first);
                return null;
            }
            mSharingsTokenForStream.setEnabled(true);
        }

        if(mSharingsTokenForStream.hasError()){
            makeError(CloudReturnCodes.ERROR_FORBIDDEN);
            return null;
        }

        String token = mSharingsTokenForStream.makeAccessTokenForStream(getID(), getCameraManagerID());
        makeError(CloudReturnCodes.OK);
        return token;
    }

    private int call_DisableSharing(){
        if(api == null){
            return makeError(CloudReturnCodes.ERROR_NO_CLOUD_CONNECTION);
        }
        if(api.usedShareToken()){
            return makeError(CloudReturnCodes.ERROR_ACCESS_DENIED);
        }
        _refresh_sharing(true);
        if(mSharingsToken != null && mSharingsToken.isEnabled()) {

            JSONObject params_put = new JSONObject();
            try {
                params_put.put("name", CloudSharingsToken.COMMON_SHARING_TOKEN);
                params_put.put("enabled", false);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if(mSharingsToken.hasError()){
                return makeError(CloudReturnCodes.ERROR_FORBIDDEN);
            }

            long shid = mSharingsToken.getID();

            Pair<Integer, JSONObject> p = api.updateCameraSharingToken(getID(), shid, params_put);
            if(p.first != 0 && p.first != 200){
                return makeHTTPError(p.first);
            }
            mSharingsToken.setEnabled(false);
        }
        return makeError(CloudReturnCodes.OK);
    }

    private int call_DisableSharingForStream(){
        if(api == null){
            return makeError(CloudReturnCodes.ERROR_NO_CLOUD_CONNECTION);
        }
        if(api.usedShareToken()){
            return makeError(CloudReturnCodes.ERROR_ACCESS_DENIED);
        }

        _refresh_sharing(true);
        if(mSharingsTokenForStream != null && mSharingsTokenForStream.isEnabled()) {

            JSONObject params_put = new JSONObject();
            try {
                params_put.put("name", CloudSharingsToken.COMMON_SHARING_TOKEN);
                params_put.put("enabled", false);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if(mSharingsTokenForStream.hasError()){
                return makeError(CloudReturnCodes.ERROR_FORBIDDEN);
            }

            long shid = mSharingsTokenForStream.getID();

            Pair<Integer, JSONObject> p = api.updateCameraSharingToken(getID(), shid, params_put);
            if(p.first != 0 && p.first != 200){
                return makeHTTPError(p.first);
            }
            mSharingsTokenForStream.setEnabled(false);
        }

        return makeError(CloudReturnCodes.OK);
    }

}
