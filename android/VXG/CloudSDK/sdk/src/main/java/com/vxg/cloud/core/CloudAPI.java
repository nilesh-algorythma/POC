//
//  Copyright © 2016 VXG Inc. All rights reserved.
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

package com.vxg.cloud.core;

import android.os.AsyncTask;
import android.util.Pair;

import com.vxg.cloud.core.CloudCameras.CloudCameraDetail;
import com.vxg.cloud.core.CloudCommon.CloudLiveUrls;
import com.vxg.cloud.core.CloudCommon.CloudRegToken;
import com.vxg.cloud.core.CloudCommon.CloudStorageRecord;
import com.vxg.cloud.core.CloudCommon.CloudToken;
import com.vxg.cloud.core.CloudSessions.CloudCamsessChatMessage;
import com.vxg.cloud.core.CloudSessions.CloudCamsessDetail;
import com.vxg.cloud.core.CloudSessions.CloudCamsessLightDetail;
import com.vxg.cloud.core.CloudSessions.CloudCamsessLiveStats;
import com.vxg.cloud.core.CloudSessions.CloudCamsessFilter;
import com.vxg.cloudsdk.Helpers.MLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CloudAPI {
    private static String TAG = CloudAPI.class.getSimpleName();
    final public static int LOG_LEVEL = 2; //Log.VERBOSE;
    static MLog Log = new MLog(TAG, LOG_LEVEL);

    private String mProtocol = "http";
    private String mHost = "";
    private String mPrefixPath = "";
    private CloudToken mToken = new CloudToken();
    private String mShareToken = null;
    private static final int readTimeout = 10000;
    private static final int connectTimeout = 2000;

    public CloudAPI(){

    }

    public void setHost(String host){
        mHost = host;
    }

    public String getHost(){
        return mHost;
    }

    public void setPrefixPath(String prefixPath){
        mPrefixPath = prefixPath;
    }

    public String getPrefixPath(){
        return mPrefixPath;
    }

    public void setProtocol(String protocol){
        mProtocol = protocol;
    }

    public String getProtocol(){
        return mProtocol;
    }

    private URL makeURL(String endPoint) {
        try{
            return new URL(mProtocol + "://" + mHost + mPrefixPath + endPoint); // TODO port
        } catch (MalformedURLException e) {
            Log.e("getURLByEndPoint() " + e.getMessage());
            return null;
        }
    }

    public void setShareToken(String share_token){
        mShareToken = share_token;
    }

    public boolean usedShareToken(){
        return mShareToken != null;
    }

    public void setToken(CloudToken token){
        mToken = token;
    }

    public CloudToken getToken(){
        return mToken;
    }

    public void resetToken(){
        mToken.reset();
    }

    public boolean tokenIsEmpty(){
        return mToken.isEmpty();
    }

    public boolean refreshToken(){
        if(mToken.isEmpty()){
            return false;
        }
        try {
            Pair<Integer,String> resp = executeGetRequest2(CloudAPIEndPoints.ACCOUNT_API_TOKEN(), null);
            JSONObject response = new JSONObject(resp.second);
            CloudToken token = new CloudToken(response);
            if(token.hasError()){
                Log.e("refreshToken, Error(" + token.getErrorStatus() + ") token refresh " + token.getErrorDetail());
                return false;
            }
            mToken = token;
            return true;
        } catch (IOException e) {
            Log.e("refreshToken, error: "+e);
            e.printStackTrace();
        } catch (JSONException e){
            Log.e("refreshToken, error (invalid json): "+ e);
            e.printStackTrace();
        }
        return false;
    }

    /*
    *   Account
    * */
    public Pair<Integer, JSONObject> getAccount(){
        JSONObject response = new JSONObject();
        Integer responseCode = 0;
        if(mToken.isEmpty()){
            return new Pair<>(responseCode, response);
        }
        try{
            Pair<Integer,String> p = executeGetRequest2(CloudAPIEndPoints.ACCOUNT(), null);
            Log.e("getAccount resp: " + p.second);
            responseCode = p.first;
            if(!p.second.equals("")) {
                response = new JSONObject(p.second);
            }
        } catch (IOException e) {
            Log.e("getAccount error: "+ e);
            e.printStackTrace();
        }catch(JSONException e){
            Log.e("getAccount has invalid json");
            e.printStackTrace();
        }
        return new Pair<>(responseCode, response);
    }

    public Pair<Integer, JSONObject> getAccountCapabilities(){
        JSONObject response = new JSONObject();
        Integer responseCode = 0;
        if(mToken.isEmpty()){
            return new Pair<>(responseCode, response);
        }
        try{
            Pair<Integer,String> p = executeGetRequest2(CloudAPIEndPoints.ACCOUNT_CAPABILITIES(), null);
            Log.i("getAccountCapabilities resp: " + p.second);
            responseCode = p.first;
            if(!p.second.equals("")) {
                response = new JSONObject(p.second);
            }
        } catch (IOException e) {
            Log.e("getAccountCapabilities error: "+ e);
            e.printStackTrace();
        }catch(JSONException e){
            Log.e("getAccountCapabilities has invalid json");
            e.printStackTrace();
        }
        return new Pair<>(responseCode, response);
    }

    //


    public CloudToken loginByGoogleToken(String token, String provider, String vendor) {
        Map<String,String> params_get = new HashMap<>();
        params_get.put("provider", provider);
        params_get.put("vendor", vendor);
        Map<String,String> params_post = new HashMap<>();
        params_post.put("token", token);

        CloudToken cloudToken = null;
        try {
            URL url = makeURL(CloudAPIEndPoints.SVC_AUTH() + "/bytoken/?" + CloudHelpers.mapToUrlQuery(params_get));
            Log.d("POST " + url);
            if(url == null){
                return null;
            }
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setReadTimeout(readTimeout);
            urlConnection.setConnectTimeout(connectTimeout);
            Log.i("readTimeout " + readTimeout);
            urlConnection.setDoInput(true);
            urlConnection.setUseCaches(false);

            urlConnection.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
            OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream());
            wr.write(CloudHelpers.mapToUrlQuery(params_post)); // TODO need check may be need like token="token"
            wr.flush();
            wr.close();

            StringBuffer buffer;
            buffer = new StringBuffer();

            Log.d("POST ResponseCode: " + urlConnection.getResponseCode() + " for URL: " + url);
            if (urlConnection.getResponseCode() == 401) {
                // throw new IOException(HTTP_EXCEPTION_INVALID_AUTH);
                // return
            }

            int codeResponse = urlConnection.getResponseCode();
            boolean isError = codeResponse >= 400;
            InputStream inputStream = isError ? urlConnection.getErrorStream() : urlConnection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
            String responseStr = buffer.toString();
            JSONObject response = new JSONObject(responseStr);
            cloudToken = new CloudToken(response);
        }catch(JSONException e){
            Log.e("LoginByGoogleToken has invalid json");
            e.printStackTrace();
        } catch (IOException e){
            Log.e(e.getMessage());
            e.printStackTrace();
        }

        return cloudToken;
    }

    public JSONObject hasActiveCamsess(long camID) {
        JSONObject response = null;

        if(mToken.isEmpty()){
            return null;
        }
        ArrayList<Pair<String, String>> params = new ArrayList<>();
        params.add(new Pair<>("detail","detail"));
        params.add(new Pair<>("camid","" + camID));
        params.add(new Pair<>("active","true"));

        try{
            Pair<Integer, String> resp = executeGetRequest2(CloudAPIEndPoints.CAMSESS(), params);
            response = new JSONObject(resp.second);
        } catch (IOException e) {
            Log.e("hasActiveCamsess error: "+ e);
            e.printStackTrace();
        }catch(JSONException e){
            Log.e("hasActiveCamsess has invalid json");
            e.printStackTrace();
        }
        return response;
    }

    public String getServerTime(){
        String serverTime = null;
        try {
            Pair<Integer,String> resp = executeGetRequest2(CloudAPIEndPoints.SERVER_TIME(), null);
            JSONObject response = new JSONObject(resp.second);
            if(response.has("utc")){
                serverTime = response.getString("utc");
            }
        } catch (IOException e) {
            Log.e("getServerTime error: "+ e);
            e.printStackTrace();
        } catch (JSONException e){
            Log.e("getServerTime error (invalid json): "+ e);
            e.printStackTrace();
        }
        return serverTime;
    }

    public CloudRegToken resetCameraManager(long cmngrsID){
        CloudRegToken regToken = null;
        if(mToken.isEmpty() && mShareToken == null){
            return regToken;
        }
        try{
            String resp = executePostRequest(CloudAPIEndPoints.CMNGRS_RESET(cmngrsID), null);
            Log.e("resetCameraManager resp: " + resp);
            JSONObject response = new JSONObject(resp);
            regToken = new CloudRegToken(response);
        } catch (IOException e) {
            Log.e("resetCameraManager error: "+ e);
            e.printStackTrace();
        }catch(JSONException e){
            Log.e("resetCameraManager has invalid json");
            e.printStackTrace();
        }
        return regToken;
    }

    public Pair<Integer, JSONObject> updateCameraManager(long cmngrsID, JSONObject data){
        Pair<Integer, JSONObject> p = new Pair<>(0, null);
        if(mToken.isEmpty()){
            return p;
        }
        try{
            Pair<Integer, String> p2 = executePutRequest2(CloudAPIEndPoints.CMNGRS(cmngrsID), data);
            Log.e("updateCameraManager resp: " + p2.second);
            p = p.create(p2.first, new JSONObject(p2.second));
        } catch (IOException e) {
            Log.e("updateCameraManager error: "+ e);
            e.printStackTrace();
        }catch(JSONException e){
            Log.e("updateCameraManager has invalid json");
            e.printStackTrace();
        }
        return p;
    }

    /*
    ****************************************************************
    ***************************************************************
    * CAMERAS
    ***************************************************************
    */

    /*
    Deprecated
    Method for create camera in cloud with custom fields.
    Please use JSONObject createCamera(JSONObject data)
     */
    @Deprecated
    public CloudCameraDetail cameraCreate(JSONObject data){
        CloudCameraDetail cameraDetail = new CloudCameraDetail();
        if(mToken.isEmpty()){
            Log.e("cameraCreate, Unauthorized request");
            return cameraDetail;
        }

        try{
            String resp = executePostRequest(CloudAPIEndPoints.CAMERAS()+"?detail=detail", data);
            Log.e("cameraCreate resp: " + resp);
            JSONObject response = new JSONObject(resp);
            cameraDetail = new CloudCameraDetail(response);
        } catch (IOException e) {
            Log.e("cameraCreate error: "+ e);
            e.printStackTrace();
        }catch(JSONException e){
            Log.e("cameraCreate has invalid json");
            e.printStackTrace();
        }
        return cameraDetail;
    }

    public Pair<Integer, JSONObject> createCamera(JSONObject data){
        JSONObject json = new JSONObject();
        Integer code = 0;
        if(mToken.isEmpty()){
            Log.e("createCamera, Unauthorized request");
            return new Pair<>(code, json);
        }
        try{
            Pair<Integer,String> p = executePostRequest2(CloudAPIEndPoints.CAMERAS() + "?detail=detail", data);
            Log.e("cameraCreate resp: " + p.second);
            code = p.first;
            if(!p.second.equals("")) {
                json = new JSONObject(p.second);
            }
        } catch (IOException e) {
            Log.e("cameraCreate error: "+ e);
            e.printStackTrace();
        }catch(JSONException e){
            Log.e("cameraCreate has invalid json");
            e.printStackTrace();
        }
        return new Pair<>(code, json);
    }

    public int deleteCamera(long camid){
        if(mToken.isEmpty()){
            return -2; //ERROR_ENOENT
        }

        if(camid == 0) return -22; //ERROR_EINVAL

        try {
            Pair<Integer,String> p = executeDeleteRequest2(CloudAPIEndPoints.CAMERA(camid));
            Log.i("deleteCamera response(code): " + p.first);
            Log.i("deleteCamera response: " + p.second);
        } catch (IOException e) {
            Log.e("deleteCamera error: "+ e);
            e.printStackTrace();
            return -13;
        }

        return 0;
    }

    /*
        get camera list
    */
    public CloudResponseWithMeta getCameraList(ArrayList<Pair<String,String>> params){
        CloudResponseWithMeta response = null;
        try{
            Pair<Integer,String> resp = executeGetRequest2(CloudAPIEndPoints.CAMERAS(), params);
            JSONObject json_response = new JSONObject(resp.second);
            response = new CloudResponseWithMeta(json_response);
        }catch(IOException e){
            Log.e("getCameraList has errors: " + e.getMessage());
            e.printStackTrace();
        }catch(JSONException e){
            Log.e("getCameraList has invalid json: " + e.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    public CloudResponseWithMeta getCameraRecords(long camid, int offset, int limit, String time_start, String time_end, boolean order_acsending){
        CloudResponseWithMeta response = null;
        try{
            ArrayList<Pair<String,String>> params = new ArrayList<>();
            params.add(new Pair<>("offset", Integer.toString(offset)));
            params.add(new Pair<>("limit", Integer.toString(limit)));
            params.add(new Pair<>("camid", Long.toString(camid)));

            if(time_start != null)
                params.add(new Pair<>("start", time_start));

            if(time_end != null)
                params.add(new Pair<>("end", time_end));

            if(order_acsending){
                params.add(new Pair<>("order_by", "time"));
            }else{
                params.add(new Pair<>("order_by", "-time"));
            }
            Pair<Integer,String> resp = executeGetRequest2(CloudAPIEndPoints.CAMERA_RECORDS(), params);
            JSONObject json_response = new JSONObject(resp.second);
            response = new CloudResponseWithMeta(json_response);
        }catch(IOException e){
            Log.e("_getCameraRecords has errors: " + e.getMessage());
            e.printStackTrace();
        }catch(JSONException e){
            Log.e("_getCameraRecords has invalid json: " + e.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    public Pair<Integer,CloudStorageRecord> storageRecordFirst(long camid){
        try{
            ArrayList<Pair<String,String>> params = new ArrayList<>();
            // HashMap<String,String> params = new HashMap<>();
            params.add(new Pair<>("limit", "1"));
            params.add(new Pair<>("camid", ""+camid));

            Pair<Integer,String> p = executeGetRequest2(CloudAPIEndPoints.CAMERA_RECORDS(), params);
            JSONObject json_response = new JSONObject(p.second);
            CloudResponseWithMeta meta = new CloudResponseWithMeta(json_response);
            if(meta.getTotalCount() == 0){
                return new Pair<>(p.first,null);
            }
            CloudStorageRecord record = new CloudStorageRecord(meta.getObjects().getJSONObject(0));
            return new Pair<>(p.first,record);
        }catch(IOException e){
            Log.e("getCameraFirstRecord has errors: " + e.getMessage());
            e.printStackTrace();
        }catch(JSONException e){
            Log.e("getCameraFirstRecord has invalid json: " + e.getMessage());
            e.printStackTrace();
        }
        return new Pair<>(-1,null);
    }

    public CloudResponseWithMeta storageTimeline(long camid, int offset, int limit, String time_start, String time_end){
        CloudResponseWithMeta response = null;
        try{
            //note, slice is 5 seconds hardcoded
            ArrayList<Pair<String,String>> params = new ArrayList<>();
            params.add(new Pair<>("slices", "5"));
            params.add(new Pair<>("offset", Integer.toString(offset)));
            params.add(new Pair<>("limit", Integer.toString(limit)));

            if(time_start != null) {
                params.add(new Pair<>("start", time_start));
            }
            if(time_end != null) {
                params.add(new Pair<>("end", time_end));
            }
            Pair<Integer,String> resp = executeGetRequest2(CloudAPIEndPoints.CAMERA_TIMELINE(camid), params);
            JSONObject json_response = new JSONObject(resp.second);
            response = new CloudResponseWithMeta(json_response);
        }catch(IOException e){
            Log.e("_getCameraTimeline has errors: " + e.getMessage());
            e.printStackTrace();
        }catch(JSONException e){
            Log.e("_getCameraTimeline has invalid json: " + e.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    public Pair<Integer,CloudResponseWithMeta> storageTimelineDays(long camid, boolean use_timezone){
        try{
            ArrayList<Pair<String,String>> params = new ArrayList<>();
            params.add(new Pair<>("camid", ""+camid));
            if(use_timezone) {
                params.add(new Pair<>("daysincamtz", ""));
            }

            Pair<Integer,String> p = executeGetRequest2(CloudAPIEndPoints.CAMERA_TIMELINE_DAYS(), params);
            JSONObject json_response = new JSONObject(p.second);
            CloudResponseWithMeta meta = new CloudResponseWithMeta(json_response);
            if(meta.getTotalCount() == 0){
                return new Pair<>(p.first,null);
            }
            return new Pair<>(p.first, meta);
        }catch(IOException e){
            Log.e("storageTimelineDays has errors: " + e.getMessage());
            e.printStackTrace();
        }catch(JSONException e){
            Log.e("storageTimelineDays has invalid json: " + e.getMessage());
            e.printStackTrace();
        }
        return new Pair<>(-1,null);
    }



    /*
    Helper method for create camera (by name) in cloud.
     */

    public CloudCameraDetail cameraCreateWithName(String name){
        JSONObject data = new JSONObject();
        try{
            data.put("name", name);
        }catch(JSONException e){
            Log.e("cameraCreateByName has invalid json");
            e.printStackTrace();
        }
        return cameraCreate(data);
    }

    public CloudCamsessDetail getCamsess(long camsessID){
        CloudCamsessDetail cameraDetail = new CloudCamsessDetail();

        if(mToken.isEmpty()){
            return cameraDetail;
        }
        try{
            Pair<Integer,String> resp = executeGetRequest2(CloudAPIEndPoints.CAMSESS(camsessID), null);
            Log.i("getCamsess resp (str): " + resp.second);
            JSONObject response = new JSONObject(resp.second);
            // Log.e("getCamsess resp(json): " + response.toString(1));
            cameraDetail = new CloudCamsessDetail(response);
        } catch (IOException e) {
            Log.i("getCamsess error: "+ e);
            e.printStackTrace();
        }catch(JSONException e){
            Log.i("getCamsess has invalid json");
            e.printStackTrace();
        }
        return cameraDetail;
    }



    public CloudCamsessDetail createCamsess(JSONObject data){
        CloudCamsessDetail cameraDetail = new CloudCamsessDetail();
        if (mToken.isEmpty()){
            return cameraDetail;
        }
        try {
            Log.e("createCamsess resp: " + data.toString(1));
            String resp = executePostRequest(CloudAPIEndPoints.CAMSESS() + "?detail=detail", data);
            Log.e("createCamsess resp: " + resp);
            JSONObject response = new JSONObject(resp);
            if(response != null) {
                cameraDetail = new CloudCamsessDetail(response);
            }
        } catch (IOException e) {
            Log.e("createCamsess error: "+ e);
            e.printStackTrace();
        } catch(JSONException e){
            Log.e("createCamsess has invalid json");
            e.printStackTrace();
        }
        return cameraDetail;
    }

    public Pair<Integer, ArrayList<CloudCameraDetail>> getCamerasListPage(ArrayList<Pair<String,String>> params){
        ArrayList<CloudCameraDetail> result = new ArrayList<>();
        Integer totalCount = 0;
        try{
            Pair<Integer, String> resp = executeGetRequest2(CloudAPIEndPoints.CAMERAS(), params);
            JSONObject json_response = new JSONObject(resp.second);
            CloudResponseWithMeta meta = new CloudResponseWithMeta(json_response);
            totalCount = meta.getTotalCount();
            Log.e("getCamerasListPage, totalCount: " + totalCount);
            JSONArray objects = meta.getObjects();
            for(int i = 0; i < objects.length(); i++){
                CloudCameraDetail cameraDetail = new CloudCameraDetail(objects.getJSONObject(i));
                result.add(cameraDetail);
            }
        }catch(IOException e){
            Log.e("getCamerasListPage has errors: " + e.getMessage());
            e.printStackTrace();
        }catch(JSONException e){
            Log.e("getCamerasListPage has invalid json: " + e.getMessage());
            e.printStackTrace();
        }
        return new Pair<>(totalCount, result);
    }

    public Pair<Integer, ArrayList<CloudCamsessDetail>> getCamsessListPage(CloudCamsessFilter filter){
        ArrayList<CloudCamsessDetail> result = new ArrayList<>();
        Integer totalCount = 0;
        try{
            Pair<Integer, String> resp = executeGetRequest2(CloudAPIEndPoints.CAMSESS(), filter._getValues());
            JSONObject json_response = new JSONObject(resp.second);
            CloudResponseWithMeta meta = new CloudResponseWithMeta(json_response);
            totalCount = meta.getTotalCount();
            JSONArray objects = meta.getObjects();
            for(int i = 0; i < objects.length(); i++){
                CloudCamsessDetail camsessDetail = new CloudCamsessDetail(objects.getJSONObject(i));
                result.add(camsessDetail);
            }
        }catch(IOException e){
            Log.e("getCamsessListPage has errors: " + e.getMessage());
            e.printStackTrace();
        }catch(JSONException e){
            Log.e("getCamsessListPage has invalid json: " + e.getMessage());
            e.printStackTrace();
        }
        return new Pair<>(totalCount, result);
    }

    public CloudResponseWithMeta _getCamsessList(CloudCamsessFilter filter){
        CloudResponseWithMeta response = null;
        try{
            Pair<Integer,String> resp = executeGetRequest2(CloudAPIEndPoints.CAMSESS(), filter._getValues());
            JSONObject json_response = new JSONObject(resp.second);
            response = new CloudResponseWithMeta(json_response);
        }catch(IOException e){
            Log.e("_getCamsessRecords has errors: " + e.getMessage());
            e.printStackTrace();
        }catch(JSONException e){
            Log.e("_getCamsessRecords has invalid json: " + e.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    public ArrayList<CloudCamsessLightDetail> getCamsessLightList(CloudCamsessFilter filter){
        filter.setWithDetails(false);
        ArrayList<CloudCamsessLightDetail> result = new ArrayList<>();
        if (mToken.isEmpty()){
            Log.e("getCamsessLightList request is unathorized");
            return result;
        }

        try{
            CloudResponseWithMeta meta = _getCamsessList(filter);
            if (meta == null)
            {
                Log.e("getCamsessLightList return null meta");
                return result;
            }

            JSONArray objects = meta.getObjects();
            for(int i = 0; i < objects.length(); i++){
                CloudCamsessLightDetail camsessDetail = new CloudCamsessLightDetail(objects.getJSONObject(i));
                result.add(camsessDetail);
            }
            Log.e("getCamsessList getTotalCount: " + meta.getTotalCount() + " loaded: " + result.size() + " json size: " + objects.toString().length());
            while(result.size() < meta.getTotalCount()){
                filter.setOffset(meta.getOffset() + meta.getLimit());
                filter.setLimit(meta.getLimit());
                meta = _getCamsessList(filter);
                objects = meta.getObjects();
                for(int i = 0; i < objects.length(); i++){
                    CloudCamsessLightDetail camsessDetail = new CloudCamsessLightDetail(objects.getJSONObject(i));
                    result.add(camsessDetail);
                }
                Log.e("getCamsessList getTotalCount: " + meta.getTotalCount() + " loaded: " + result.size());
            }
        }catch(JSONException e){
            Log.e("getCamsessList has invalid json");
            e.printStackTrace();
        } catch (Exception e) {
            Log.e("getCamsessList error: "+ e);
            e.printStackTrace();
        }
        return result;
    }

    public int updateCamera(long camera_id, JSONObject data){
        if (mToken.isEmpty()){
            Log.e("updateCamera request is unathorized");
            return (-5401); //ERROR_NOT_AUTHORIZED = (-5401);;
        }
        try {
            String resp = executePutRequest(CloudAPIEndPoints.CAMERA(camera_id), data);
            Log.e("updateCamera resp: " + resp);

            return 0;
        } catch (IOException e) {
            Log.e("updateCamera error: "+ e);
            e.printStackTrace();
            return (-5052);//ERROR_WRONG_RESPONSE = (-5052);
        }
    }

    public void updateCamsess_async(final long camsessid, final JSONObject data) {
        if(camsessid == 0) return;
        CloudHelpers.executeAsyncTask(new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                updateCamsess((int)camsessid, data);
                return null;
            }
        });
    }


    public void updateCamsess(long camsessid, JSONObject data){
        if(mToken.isEmpty()){
            return;
        }

        if(camsessid == 0) return;

        try {
            Log.i("updateCamsess request: " + data.toString(1));
            String resp = executePutRequest(CloudAPIEndPoints.CAMSESS(camsessid), data);
            Log.i("updateCamsess response: " + resp);
        } catch (JSONException e){
            Log.e("updateCamsess invalid json: "+ e);
            e.printStackTrace();
        } catch (IOException e) {
            Log.e("updateCamsess error: "+ e);
            e.printStackTrace();
        }
    }

    public void deleteCamsess_async(final long camsessid) {
        if(camsessid == 0) return;
        CloudHelpers.executeAsyncTask(new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                deleteCamsess(camsessid);
                return null;
            }
        });
    }

    public void deleteCamsess(long camsessid){
        if(mToken.isEmpty()){
            return;
        }

        if(camsessid == 0) return;

        try {
            Pair<Integer,String> p = executeDeleteRequest2(CloudAPIEndPoints.CAMSESS(camsessid));
            Log.i("updateCamsess response: " + p.second);
        } catch (IOException e) {
            Log.e("updateCamsess error: "+ e);
            e.printStackTrace();
        }
    }



    public void camsessSendChatMessage(int camsessid, String message){
        if (mToken.isEmpty()){
            Log.e("camsessSendChatMessage request is unathorized");
            return;
        }
        if(camsessid <= 0){
            Log.e("Invalid camsessid");
            return;
        }
        try {
            JSONObject data = new JSONObject();
            data.put("message", message);
            String resp_json = executePostRequest(CloudAPIEndPoints.CAMSESS_CHAT_SEND_MESSAGE(camsessid), data);
        } catch (IOException e) {
            Log.e("camsessSendChatMessage error: "+ e);
            e.printStackTrace();
        } catch(JSONException e){
            Log.e("camsessSendChatMessage has invalid json");
            e.printStackTrace();
        }
        return;
    }

    /*
        Create upload url
        input:
            duration - in ms
            size - in bytes
            time - format like "2016-06-20T04:13:25.488000"
     */
    public String createCamsessRecordsUpload(long camsessid, CloudStorageRecord recordDetails){
        if (mToken.isEmpty()){
            Log.e("createCamsessRecordsUpload request is unathorized");
            return null;
        }
        if(camsessid <= 0){
            Log.e("Invalid camsessid");
            return null;
        }
        try {
            JSONArray data = new JSONArray();
            JSONObject record_data = new JSONObject();
            record_data.put("duration", recordDetails.getDuration());
            record_data.put("size", recordDetails.getSize());
            record_data.put("time", recordDetails.getStartAsString());
            data.put(record_data);
            String resp_json = executePostRequestArray(CloudAPIEndPoints.CAMESESS_RECORDS_UPLOAD(camsessid), data);
            Log.e("createCamsessRecordsUpload resp: " + resp_json);
            JSONObject response = new JSONObject(resp_json);
            if(response.has("urls")){
                JSONArray urls = response.getJSONArray("urls");
                if(urls.length() > 0)
                    return urls.getString(0);
            }
            return null;
        } catch (IOException e) {
            Log.e("createCamsessRecordsUpload error: "+ e);
            e.printStackTrace();
        } catch(JSONException e){
            Log.e("createCamsessRecordsUpload has invalid json");
            e.printStackTrace();
        }
        return null;
    }

    /*!
     * Create upload url
     * input:
     *   duration - in ms
     *   size - in bytes
     *   time - format like "2016-06-20T04:13:25.488000"
    */

    public String createCamsessPreviewUpload(long camsessid, String time, long size, long width, long height){
        if (mToken.isEmpty()){
            Log.e("createCamsessPreviewUpload request is unathorized");
            return null;
        }

        if(camsessid <= 0){
            Log.e("createCamsessPreviewUpload, Invalid camsessid");
            return null;
        }

        try {
            JSONObject data = new JSONObject();
            data.put("width", width);
            data.put("height", height);
            data.put("size", size);
            data.put("time", time);
            String resp_json = executePostRequest(CloudAPIEndPoints.CAMESESS_PREVIEW_UPLOAD(camsessid), data);
            Log.e("createCamsessPreviewUpload resp: " + resp_json);
            JSONObject response = new JSONObject(resp_json);
            if(response.has("url") && !response.isNull("url")){
                return response.getString("url");
            }
            return null;
        } catch (IOException e) {
            Log.e("createCamsessPreviewUpload error: "+ e);
            e.printStackTrace();
        } catch(JSONException e){
            Log.e("createCamsessPreviewUpload has invalid json");
            e.printStackTrace();
        }
        return null;
    }

    private CloudResponseWithMeta _getCamsessRecords(long camsessid, int offset, int limit){
        CloudResponseWithMeta response = null;
        try{
            ArrayList<Pair<String,String>> params = new ArrayList<>();
            params.add(new Pair<>("offset", Integer.toString(offset)));
            params.add(new Pair<>("limit", Integer.toString(limit)));

            Pair<Integer,String> resp = executeGetRequest2(CloudAPIEndPoints.CAMSESS_RECORDS(camsessid), params);
            JSONObject json_response = new JSONObject(resp.second);
            response = new CloudResponseWithMeta(json_response);
        }catch(IOException e){
            Log.e("_getCamsessRecords has errors: " + e.getMessage());
            e.printStackTrace();
        }catch(JSONException e){
            Log.e("_getCamsessRecords has invalid json: " + e.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    public ArrayList<CloudStorageRecord> getCamsessRecords(long camsessid){
        ArrayList<CloudStorageRecord> records = new ArrayList<>();
        if(mToken.isEmpty()){
            return records;
        }
        try{
            CloudResponseWithMeta resp = _getCamsessRecords(camsessid, 0, 100);
            JSONArray objects = resp.getObjects();
            for(int i = 0; i < objects.length(); i++){
                CloudStorageRecord recordDetail = new CloudStorageRecord(objects.getJSONObject(i));
                records.add(recordDetail);
            }
            while(records.size() < resp.getTotalCount()){
                resp = _getCamsessRecords(camsessid, resp.getOffset() + resp.getLimit(), resp.getLimit());
                objects = resp.getObjects();
                for(int i = 0; i < objects.length(); i++){
                    CloudStorageRecord recordDetail = new CloudStorageRecord(objects.getJSONObject(i));
                    records.add(recordDetail);
                }
            }
        }catch(JSONException e){
            Log.e("getCamsessRecords has invalid json");
            e.printStackTrace();
        }
        return records;
    }

	/*!
	 * Method return camera list filtered by name
	 * 
	 * */

    public ArrayList<CloudCameraDetail> findCamerasByName(String name, int offset, int limit){
		ArrayList<CloudCameraDetail> result = new ArrayList<>();
        if(name == null){
            return result;
        }
        if (mToken.isEmpty()){
            Log.e("getCamerasByName request is unathorized");
            return result;
        }

        try{
            ArrayList<Pair<String, String>> params = new ArrayList<>();
            params.add(new Pair<>("name", name));
            params.add(new Pair<>("offset", Integer.toString(offset)));
            params.add(new Pair<>("limit", Integer.toString(limit)));

            Pair<Integer, String> resp = executeGetRequest2(CloudAPIEndPoints.CAMERAS(), params);
            JSONObject response = new JSONObject(resp.second);
            CloudResponseWithMeta meta = new CloudResponseWithMeta(response);
            JSONArray objects = meta.getObjects();
            int size = objects.length();
            for(int i = 0; i < size; i++){
				CloudCameraDetail detail = new CloudCameraDetail(meta.getObjects().getJSONObject(i));
				result.add(detail);
			}
        } catch (IOException e) {
            Log.e("getCamerasByName error: "+ e);
            e.printStackTrace();
        }catch(JSONException e){
            Log.e("getCamerasByName has invalid json");
            e.printStackTrace();
        }
        return result;
    }

    
    public CloudCameraDetail findCameraByUUID(String uuid){
        CloudCameraDetail cameraDetail = new CloudCameraDetail();
        if (mToken.isEmpty()){
            Log.e("getCameraByUUID request is unathorized");
            return cameraDetail;
        }
        if(uuid == null){
            return cameraDetail;
        }
        try{
            ArrayList<Pair<String, String>> params = new ArrayList<>();
            params.add(new Pair<>("uuid", uuid));

            Pair<Integer, String> resp = executeGetRequest2(CloudAPIEndPoints.CAMERAS(), params);
            JSONObject response = new JSONObject(resp.second);
            CloudResponseWithMeta meta = new CloudResponseWithMeta(response);
            if(meta.getTotalCount() == 1){
                cameraDetail = new CloudCameraDetail(meta.getObjects().getJSONObject(0));
            }
        } catch (IOException e) {
            Log.e("getCameraByUUID error: "+ e);
            e.printStackTrace();
        } catch(JSONException e) {
            Log.e("getCameraByUUID has invalid json");
            e.printStackTrace();
        }
        return cameraDetail;
    }

    public CloudCameraDetail getCamera(long id){
        CloudCameraDetail cameraDetail = new CloudCameraDetail();
        if (mToken.isEmpty() && mShareToken == null){
            Log.e("getCamera request is unathorized");
            return cameraDetail;
        }
        try{
            Pair<Integer, String> resp = executeGetRequest2(CloudAPIEndPoints.CAMERA(id), null);
            JSONObject response = new JSONObject(resp.second);
            cameraDetail = new CloudCameraDetail(response);
        } catch (IOException e) {
            Log.e("getCameraByUUID error: "+ e);
            e.printStackTrace();
        }catch(JSONException e){
            Log.e("getCameraByUUID has invalid json");
            e.printStackTrace();
        }
        return cameraDetail;
    }

    public CloudLiveUrls getCameraLiveUrls(long id){
        CloudLiveUrls liveUrls = new CloudLiveUrls();
        if (mToken.isEmpty() && mShareToken == null){
            Log.e("getCamera request is unathorized");
            return liveUrls;
        }
        try{
            Pair<Integer,String> resp = executeGetRequest2(CloudAPIEndPoints.CAMERA_LIVE_URLS(id), null);
            JSONObject response = new JSONObject(resp.second);
            liveUrls = new CloudLiveUrls(response);
        } catch (IOException e) {
            Log.e("getCameraLiveUrls error: "+ e);
            e.printStackTrace();
        } catch(JSONException e) {
            Log.e("getCameraLiveUrls has invalid json");
            e.printStackTrace();
        }
        return liveUrls;
    }

    public Pair<Integer, JSONObject> getCameraPreviewUrl(long id){
        JSONObject response = new JSONObject();
        Integer responseCode = 0;
        if(mToken.isEmpty()){
            return new Pair<>(responseCode, response);
        }
        try{
            Pair<Integer,String> p = executeGetRequest2(CloudAPIEndPoints.CAMERA_PREVIEW(id), null);
            Log.e("getCameraPreviewUrl resp: " + p.second);
            responseCode = p.first;
            if(!p.second.equals("")) {
                response = new JSONObject(p.second);
            }
        } catch (IOException e) {
            Log.e("getCameraPreviewUrl error: "+ e);
            e.printStackTrace();
        }catch(JSONException e){
            Log.e("getCameraPreviewUrl has invalid json");
            e.printStackTrace();
        }
        return new Pair<>(responseCode, response);
    }

    public Pair<Integer, JSONObject> postCameraPreviewUpdate(long id){
        JSONObject json = new JSONObject();
        Integer code = 0;
        if(mToken.isEmpty()){
            Log.e("postCameraPreviewUpdate, Unauthorized request");
            return new Pair<>(code, json);
        }
        try{
            Pair<Integer,String> p = executePostRequest2(CloudAPIEndPoints.CAMERA_PREVIEW_UPDATE(id), null);
            Log.e("postCameraPreviewUpdate resp: " + p.second);
            code = p.first;
            if(!p.second.equals("")) {
                json = new JSONObject(p.second);
            }
        } catch (IOException e) {
            Log.e("postCameraPreviewUpdate error: "+ e);
            e.printStackTrace();
        }catch(JSONException e){
            Log.e("postCameraPreviewUpdate has invalid json");
            e.printStackTrace();
        }
        return new Pair<>(code, json);
    }

    // camera sharing

    public Pair<Integer, JSONObject> creareCameraSharingToken(long camid, JSONObject dataPost){
        JSONObject json = new JSONObject();
        Integer code = 0;
        if(mToken.isEmpty()){
            Log.e("creareCameraSharingToken, Unauthorized request");
            return new Pair<>(code, json);
        }
        try{
            Pair<Integer,String> p = executePostRequest2(CloudAPIEndPoints.CAMERA_SHARING(camid), dataPost);
            Log.e("creareCameraSharingToken resp: " + p.second);
            code = p.first;
            if(!p.second.equals("")) {
                json = new JSONObject(p.second);
            }
        } catch (IOException e) {
            Log.e("creareCameraSharingToken error: "+ e);
            e.printStackTrace();
        }catch(JSONException e){
            Log.e("creareCameraSharingToken has invalid json");
            e.printStackTrace();
        }
        return new Pair<>(code, json);
    }

    public Pair<Integer, JSONObject> updateCameraSharingToken(long camid, long shid, JSONObject dataPut){
        JSONObject json = new JSONObject();
        Integer code = 0;
        if(mToken.isEmpty()){
            Log.e("updateCameraSharingToken, Unauthorized request");
            return new Pair<>(code, json);
        }
        try{
            Pair<Integer,String> p = executePutRequest2(CloudAPIEndPoints.CAMERA_SHARING(camid) + Long.toString(shid) + "/", dataPut);
            Log.e("updateCameraSharingToken resp: " + p.second);
            code = p.first;
            if(!p.second.equals("")) {
                json = new JSONObject(p.second);
            }
        } catch (IOException e) {
            Log.e("updateCameraSharingToken error: "+ e);
            e.printStackTrace();
        }catch(JSONException e){
            Log.e("updateCameraSharingToken has invalid json");
            e.printStackTrace();
        }
        return new Pair<>(code, json);
    }

    public Pair<Integer, JSONObject> deleteCameraSharingToken(long camid, long shid){
        JSONObject json = new JSONObject();
        Integer code = 0;
        if(mToken.isEmpty()){
            Log.e("deleteCameraSharingToken, Unauthorized request");
            return new Pair<>(code, json);
        }
        try{
            Pair<Integer,String> p = executeDeleteRequest2(CloudAPIEndPoints.CAMERA_SHARING(camid) + Long.toString(shid) + "/");
            Log.e("deleteCameraSharingToken resp: " + p.second);
            code = p.first;
            if(!p.second.equals("")) {
                json = new JSONObject(p.second);
            }
        } catch (IOException e) {
            Log.e("deleteCameraSharingToken error: "+ e);
            e.printStackTrace();
        }catch(JSONException e){
            Log.e("deleteCameraSharingToken has invalid json");
            e.printStackTrace();
        }
        return new Pair<>(code, json);
    }

    public Pair<Integer, JSONObject> getCameraSharingTokensList(long camid, ArrayList<Pair<String,String>> params){
        JSONObject json = new JSONObject();
        Integer code = 0;
        if(mToken.isEmpty()){
            Log.e("getCameraSharingTokensList, Unauthorized request");
            return new Pair<>(code, json);
        }
        try{
            Pair<Integer,String> p = executeGetRequest2(CloudAPIEndPoints.CAMERA_SHARING(camid), params);
            code = p.first;
            //Log.i("getCameraSharingTokensList resp(Code: " + code + "): " + p.second);

            if(!p.second.equals("")) {
                json = new JSONObject(p.second);
            }
        } catch (IOException e) {
            Log.e("getCameraSharingTokensList error: "+ e);
            e.printStackTrace();
        }catch(JSONException e){
            Log.e("getCameraSharingTokensList has invalid json");
            e.printStackTrace();
        }
        return new Pair<>(code, json);
    }


    public CloudCamsessLiveStats getCamsesLiveStats(long camsessid){
        CloudCamsessLiveStats camsessLiveStats = new CloudCamsessLiveStats();
        if (mToken.isEmpty()){
            Log.e("getCamsesLiveStats request is unathorized");
            return camsessLiveStats;
        }
        if(camsessid <= 0){
            return camsessLiveStats;
        }
        try{
            Pair<Integer,String> resp = executeGetRequest2(CloudAPIEndPoints.CAMSESS_LIVE_STATS(camsessid), null);
            JSONObject response = new JSONObject(resp.second);
            camsessLiveStats = new CloudCamsessLiveStats(response);
        } catch(UnknownHostException e) {
            Log.e("getCamsesLiveStats has UnknownHostException (network off)");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e("getCamsesLiveStats error: "+ e);
            e.printStackTrace();
        } catch(JSONException e) {
            Log.e("getCamsesLiveStats has invalid json");
            e.printStackTrace();
        }
        return camsessLiveStats;
    }

    public ArrayList<CloudCamsessChatMessage> getCamsessChatMessages(String chat_url){
        ArrayList<CloudCamsessChatMessage> result = new ArrayList<>();

        try {
            URL url = new URL(chat_url);
            // HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
            // Log.d(TAG, "GET " + url);

            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(readTimeout);
            urlConnection.setConnectTimeout(connectTimeout);
            urlConnection.setRequestMethod("GET");
            urlConnection.setUseCaches(true);

            StringBuffer buffer = new StringBuffer();
            int code_response = urlConnection.getResponseCode();

            if(code_response == 404){
                return result;
            }

            if(code_response == 500){
                return result;
            }

            // Log.d(TAG, "GET ResponseCode: " + code_response + " for URL: " + url);

            boolean isError = code_response >= 400;
            InputStream inputStream = isError ? urlConnection.getErrorStream() : urlConnection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }

            String resp = buffer.toString();
            // Log.d(TAG, "GET ResponseCode (" + url + ") resp: " + resp);
            JSONObject data = new JSONObject(resp);
            if(data.has("messages") && !data.isNull("messages")){
                JSONArray messages = data.getJSONArray("messages");
                for(int i = 0; i < messages.length(); i++){
                    CloudCamsessChatMessage msg = new CloudCamsessChatMessage(messages.getJSONObject(i));
                    result.add(msg);
                }
            }
        } catch(JSONException e) {
            Log.e("getCamsessChatMessages invalid json " + e.getMessage());
            e.printStackTrace();
        } catch(IOException e) {
            Log.e("getCamsessChatMessages failed " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    private Pair<Integer, String> executeGetRequest2(String endPoint, ArrayList<Pair<String,String>> params) throws IOException {
        if(params == null){
            params = new ArrayList<>();
        }

        if (mShareToken != null){
            params.add(new Pair<>("token", mShareToken));
        }

        URL url = makeURL(endPoint + "?" + CloudHelpers.prepareHttpGetQuery(params));

        // HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        Log.d("GET " + url);

        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setReadTimeout(readTimeout);
        urlConnection.setConnectTimeout(connectTimeout);
        urlConnection.setRequestMethod("GET");

        if (!mToken.isEmpty()) {
            urlConnection.setRequestProperty("Authorization", "SkyVR " + mToken.getToken());
        }

        StringBuffer buffer = new StringBuffer();
        int code_response = urlConnection.getResponseCode();
        Log.d("GET ResponseCode: " + code_response + " for URL: " + url);

        boolean isError = code_response >= 400;
        InputStream inputStream = isError ? urlConnection.getErrorStream() : urlConnection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        return new Pair<>(code_response, buffer.toString());
    }

    @Deprecated
    private String executePostRequest(String endPoint,  JSONObject data) throws IOException {

        ArrayList<Pair<String,String>> params = null;
        if (mShareToken != null){
            if(params == null)
                params = new ArrayList<Pair<String,String>>();
            params.add(new Pair<>("token", mShareToken));
        }

        URL url = makeURL(endPoint + (params != null? "?" + CloudHelpers.prepareHttpGetQuery(params):""));

        // HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        Log.d("POST " + url);

        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("POST");
        urlConnection.setReadTimeout(readTimeout);
        urlConnection.setConnectTimeout(connectTimeout);
        Log.i("readTimeout " + readTimeout);
        urlConnection.setDoInput(true);
        urlConnection.setUseCaches(false);

        if (!mToken.isEmpty()) {
            urlConnection.setRequestProperty("Authorization", "SkyVR " + mToken.getToken());
        }

        if(data != null) {
            urlConnection.setRequestProperty("Content-type", "application/json");
            OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream());
            wr.write(data.toString());
            wr.flush();
            wr.close();
        }

        StringBuffer buffer = new StringBuffer();

        Log.d("POST ResponseCode: " + urlConnection.getResponseCode() + " for URL: " + url);
        if (urlConnection.getResponseCode() == 401) {
            // throw new IOException(HTTP_EXCEPTION_INVALID_AUTH);
            // return
        }

        int codeResponse = urlConnection.getResponseCode();
        boolean isError = codeResponse >= 400;
        InputStream inputStream = isError ? urlConnection.getErrorStream() : urlConnection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        return buffer.toString();
    }

    private Pair<Integer,String> executePostRequest2(String endPoint,  JSONObject data) throws IOException {
        ArrayList<Pair<String,String>> params = null;
        if (mShareToken != null){
            if(params == null)
                params = new ArrayList<Pair<String,String>>();
            params.add(new Pair<>("token", mShareToken));
        }

        URL url = makeURL(endPoint + (params != null? "?" + CloudHelpers.prepareHttpGetQuery(params):""));
        // HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        Log.d("POST " + url);

        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("POST");
        urlConnection.setReadTimeout(readTimeout);
        urlConnection.setConnectTimeout(connectTimeout);
        Log.i("readTimeout " + readTimeout);
        urlConnection.setDoInput(true);
        urlConnection.setUseCaches(false);

        if (!mToken.isEmpty()) {
            urlConnection.setRequestProperty("Authorization", "SkyVR " + mToken.getToken());
        }

        if(data != null) {
            urlConnection.setRequestProperty("Content-type", "application/json");
            OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream());
            wr.write(data.toString());
            wr.flush();
            wr.close();
        }

        StringBuffer buffer = new StringBuffer();

        Log.d("POST ResponseCode: " + urlConnection.getResponseCode() + " for URL: " + url);
        if (urlConnection.getResponseCode() == 401) {
            // throw new IOException(HTTP_EXCEPTION_INVALID_AUTH);
            // return
        }

        int code_response = urlConnection.getResponseCode();
        boolean isError = code_response >= 400;
        InputStream inputStream = isError ? urlConnection.getErrorStream() : urlConnection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        return new Pair<>(code_response, buffer.toString());
    }

    private String executePostRequestArray(String endPoint, JSONArray data) throws IOException {
        URL url = makeURL(endPoint);
        // HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        Log.d("PUT " + url);

        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("POST");
        urlConnection.setReadTimeout(readTimeout);
        urlConnection.setConnectTimeout(connectTimeout);
        Log.i("readTimeout " + readTimeout);
        urlConnection.setDoInput(true);
        urlConnection.setUseCaches(false);

        if (!mToken.isEmpty()) {
            urlConnection.setRequestProperty("Authorization", "SkyVR " + mToken.getToken());
        }

        if(data != null) {
            urlConnection.setRequestProperty("Content-type", "application/json");
            OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream());
            Log.e("POST RequestText: " + data.toString());
            wr.write(data.toString());
            wr.flush();
            wr.close();
        }

        int codeResponse = urlConnection.getResponseCode();

        Log.d("POST ResponseCode: " + urlConnection.getResponseCode() + " for URL: " + url);
        if (urlConnection.getResponseCode() == 401) {
            // throw new IOException(HTTP_EXCEPTION_INVALID_AUTH);
            // return
        }

        boolean isError = codeResponse >= 400;
        InputStream inputStream = isError ? urlConnection.getErrorStream() : urlConnection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuffer buffer = new StringBuffer();
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        if(codeResponse != 200){
            Log.e("POST ResponseText: " + buffer.toString());
        }
        return buffer.toString();
    }


    @Deprecated
    private String executePutRequest(String endPoint, JSONObject data) throws IOException {
        URL url = makeURL(endPoint);
        // HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        Log.d("PUT " + url);

        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("PUT");
        urlConnection.setReadTimeout(readTimeout);
        urlConnection.setConnectTimeout(connectTimeout);
        Log.i("readTimeout " + readTimeout);
        urlConnection.setDoInput(true);
        urlConnection.setUseCaches(false);

        if (!mToken.isEmpty()) {
            urlConnection.setRequestProperty("Authorization", "SkyVR " + mToken.getToken());
        }

        if(data != null) {
            urlConnection.setRequestProperty("Content-type", "application/json");
            OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream());
            Log.e("PUT RequestText: " + data.toString());
            wr.write(data.toString());
            wr.flush();
            wr.close();
        }

        int codeResponse = urlConnection.getResponseCode();

        Log.d("PUT ResponseCode: " + urlConnection.getResponseCode() + " for URL: " + url);
        if (urlConnection.getResponseCode() == 401) {
            // throw new IOException(HTTP_EXCEPTION_INVALID_AUTH);
            // return
        }

        boolean isError = codeResponse >= 400;
        InputStream inputStream = isError ? urlConnection.getErrorStream() : urlConnection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuffer buffer = new StringBuffer();
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        if(codeResponse != 200){
            Log.e("PUT ResponseText: " + buffer.toString());
        }
        return buffer.toString();
    }

    private Pair<Integer,String> executePutRequest2(String endPoint,  JSONObject data) throws IOException {
        URL url = makeURL(endPoint);
        // HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        Log.d("PUT " + url);

        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("PUT");
        urlConnection.setReadTimeout(readTimeout);
        urlConnection.setConnectTimeout(connectTimeout);
        Log.i("readTimeout " + readTimeout);
        urlConnection.setDoInput(true);
        urlConnection.setUseCaches(false);

        if (!mToken.isEmpty()) {
            urlConnection.setRequestProperty("Authorization", "SkyVR " + mToken.getToken());
        }

        if(data != null) {
            urlConnection.setRequestProperty("Content-type", "application/json");
            OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream());
            wr.write(data.toString());
            wr.flush();
            wr.close();
        }

        StringBuffer buffer = new StringBuffer();

        Log.d("PUT ResponseCode: " + urlConnection.getResponseCode() + " for URL: " + url);
        if (urlConnection.getResponseCode() == 401) {
            // throw new IOException(HTTP_EXCEPTION_INVALID_AUTH);
            // return
        }

        int code_response = urlConnection.getResponseCode();
        boolean isError = code_response >= 400;
        InputStream inputStream = isError ? urlConnection.getErrorStream() : urlConnection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        return new Pair<>(code_response, buffer.toString());
    }

    private Pair<Integer,String> executeDeleteRequest2(String endPoint) throws IOException {
        URL url = makeURL(endPoint);
        // HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        Log.d("DELETE " + url);

        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("DELETE");
        urlConnection.setReadTimeout(readTimeout);
        urlConnection.setConnectTimeout(connectTimeout);
        Log.i("readTimeout " + readTimeout);
        urlConnection.setDoInput(true);
        urlConnection.setDoOutput(false);
        urlConnection.setUseCaches(false);

        if (!mToken.isEmpty()) {
            urlConnection.setRequestProperty("Authorization", "SkyVR " + mToken.getToken());
        }

        int code_response = urlConnection.getResponseCode();

        Log.d("DELETE ResponseCode: " + urlConnection.getResponseCode() + " for URL: " + url);
        if (urlConnection.getResponseCode() == 401) {
            // throw new IOException(HTTP_EXCEPTION_INVALID_AUTH);
            // return
        }

        boolean isError = code_response >= 400;
        InputStream inputStream = isError ? urlConnection.getErrorStream() : urlConnection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuffer buffer = new StringBuffer();
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        if(code_response != 200){
            Log.e("DELETE ResponseText: " + buffer.toString());
        }
        return new Pair<>(code_response, buffer.toString());
    }
}
