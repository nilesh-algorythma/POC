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

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.firebase.tubesock.WebSocket;
import com.firebase.tubesock.WebSocketEventHandler;
import com.firebase.tubesock.WebSocketException;
import com.firebase.tubesock.WebSocketMessage;
import com.vxg.cloud.CameraManager.CameraManagerConfig;
import com.vxg.cloud.CameraManager.CreateCmdHandlers;
import com.vxg.cloud.CameraManager.Enums.CameraManagerCommandNames;
import com.vxg.cloud.CameraManager.Enums.CameraManagerDoneStatus;
import com.vxg.cloud.CameraManager.Enums.CameraManagerErrors;
import com.vxg.cloud.CameraManager.Enums.CameraManagerParameterNames;
import com.vxg.cloud.CameraManager.Interfaces.CameraManagerClientListener;
import com.vxg.cloud.CameraManager.Interfaces.CmdHandler;
import com.vxg.cloud.core.CloudAPI;
import com.vxg.cloud.core.CloudCommon.CloudRegToken;
import com.vxg.cloud.core.CloudHelpers;
import com.vxg.cloudsdk.CloudSDK;
import com.vxg.cloudsdk.Enums.CloudReturnCodes;
import com.vxg.cloudsdk.Helpers.MLog;
import com.vxg.cloudsdk.Helpers.Msg;
import com.vxg.cloudsdk.Interfaces.ICloudConnection;
import com.vxg.cloudsdk.Interfaces.ICloudObject;
import com.vxg.cloudsdk.Interfaces.ICloudStreamer;
import com.vxg.cloudsdk.Interfaces.ICloudStreamerCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import veg.mediaplayer.sdk.MediaPlayer;

public class CloudStreamer extends CloudObject implements ICloudStreamer
{

    private static final String TAG = "CloudStreamer";
    final public static int LOG_LEVEL = 2; //Log.VERBOSE;
    static MLog Log = new MLog(TAG, LOG_LEVEL);
    boolean VERBOSE = true;

    Context mContext;
    ICloudStreamerCallback mCallback;
    boolean misStarted = false;
    CameraManagerErrors m_last_error = CameraManagerErrors.REASON_SHUTDOWN;

    CloudCamera mCamera;   //either camera
    String mCameraConfig;  //or config
    CloudAPI api;
    boolean is_source_changed = false;
    CloudRegToken mRegToken;
    CameraManagerConfig mCMConfig;
    String mCamPath;

    public final static String UPLOADING_TYPE_JPG = "jpg";
    public final static String UPLOADING_CATEGORY_PREVIEW = "preview";
    VEG_WebSocket mWebSocketClient;
    private int massageId = 1;
    boolean isReconnect = false;
    String mStreamUrl = "";
    int start_stream_counter = 0;
    long mProlong_time = 0;
    String mPreviewFile;
    private static final int readTimeout = 10000;
    private static final int connectTimeout = 15000;

    //async functions
    private LinkedList<Msg> cmd_queue = new LinkedList<Msg>();
    final int CMD_Start = 1;
    final int CMD_Stop = 2;
    final int CMD_Prolongate = 3;
    final int CMD_SendPreview = 4;

    public CloudStreamer(ICloudStreamerCallback callback){
        Log.v("=>CloudStreamer");
        mContext = CloudSDK.getContext();
        mCallback = callback;
    }

    @Override
    public int setCamera(ICloudObject camera) {
        if (camera instanceof CloudCamera) {
            mCamera = (CloudCamera) camera;
            ICloudConnection conn = mCamera._getCloudConnection();
            api = conn._getAPI();
            is_source_changed = true;//(mCamid != camera.getID());
        }
        _reset();

        if(is_source_changed && mCallback != null && mCamera != null){
            mCallback.onCameraConnected();
        }

        return makeError(api == null ? CloudReturnCodes.ERROR_BADARGUMENT : CloudReturnCodes.OK);
    }

    @Override
    public ICloudObject getCamera() {
        return mCamera;
    }

    @Override
    public int setPreviewImage(String file) {
        mPreviewFile = file;
        return 0;
    }

    @Override
    public int setConfig(String config) {
        mCameraConfig = config;

        if(mCameraConfig != null) {
            //check config
            JSONObject jo;
            try {
                jo = new JSONObject(mCameraConfig);
                String uuid = jo.getString("uuid");
                String sid = jo.getString("sid");
                String pwd = jo.getString("pwd");
                long camid = jo.getLong("camid");
                String connid = jo.getString("connid");
            } catch (JSONException e) {
                e.printStackTrace();
                mCameraConfig = null;
                return makeError(CloudReturnCodes.ERROR_NOT_CONFIGURED);
            }
        }

        return makeError(CloudReturnCodes.OK);
    }

    @Override
    public String getConfig() {
        return mCameraConfig;
    }

    void _reset()
    {
    }

    @Override
    public void Start() {
        Msg m = new Msg();
        m.func_id = CMD_Start;
        m.args = new ArrayList<Object>();
        m.func_complete = null;
        cmd_queue.add(m);

        Log.v("=Start");

        //start processing
        execute();
    }

    @Override
    public void Stop() {
        Msg m = new Msg();
        m.func_id = CMD_Stop;
        m.args = new ArrayList<Object>();
        m.func_complete = null;
        cmd_queue.add(m);

        Log.v("=Stop");

        //start processing
        execute();
    }

    private void Prolongate(long duration_ms) {
        mProlong_time = CloudHelpers.currentTimestampUTC()+duration_ms;
        Msg m = new Msg();
        m.func_id = CMD_Prolongate;
        m.args = new ArrayList<Object>();
        m.func_complete = null;
        cmd_queue.add(m);

        Log.v("=Prolongate duration_ms="+duration_ms);

        //start processing
        execute();
    }

    private void sendPreviewStreamer(String url, String file){
        Msg m = new Msg();
        m.func_id = CMD_SendPreview;
        m.args = new ArrayList<Object>();
        m.args.add(url);
        m.args.add(file);
        m.func_complete = null;
        cmd_queue.add(m);

        Log.v("=sendPreviewStreamer url="+url+" file="+file);

        //start processing
        execute();
    }

    @Override
    public void runt() {
        //processing
        int prolong_count=0;

        while(true) {
            Msg msg = cmd_queue.poll();
            if (msg == null) {

                if (prolong_count > 0) {
                    //check time
                    long cur = CloudHelpers.currentTimestampUTC();
                    if (cur > mProlong_time) {
                        mCMListener.onStreamStop("prolong");
                        prolong_count--;
                    }
                    sleep(200);
                    continue;
                }
                return;
            }

            switch (msg.func_id) {
                case CMD_Start:
                    call_Start();
                    break;
                case CMD_Stop:
                    call_Stop();
                    break;
                case CMD_SendPreview:
                    call_sendPreviewStreamer((String)msg.args.get(0), (String)msg.args.get(1));
                    break;
                case CMD_Prolongate:
                    prolong_count++;
                    continue;
            }
            return;
        }
    }

    int call_Start()
    {
        if((mCamera == null && mCameraConfig == null) || mCallback == null){
            mCallback.onError(CloudReturnCodes.ERROR_NOT_CONFIGURED);
            return makeError(CloudReturnCodes.ERROR_NOT_CONFIGURED);
        }

        if(mCameraConfig == null) {
            //get camera manager id
            long cmngrid = mCamera.getCameraManagerID();
            if (cmngrid == -1) {
                mCallback.onError(CloudReturnCodes.ERROR_NOT_CONFIGURED);
                return makeError(CloudReturnCodes.ERROR_NOT_CONFIGURED);
            }

            if(api == null){
                mCallback.onError(CloudReturnCodes.ERROR_NOT_CONFIGURED);
                return makeError(CloudReturnCodes.ERROR_NOT_CONFIGURED);
            }

            //get regtoken
            mRegToken = api.resetCameraManager(cmngrid);
            if (mRegToken == null) {
                mCallback.onError(CloudReturnCodes.ERROR_ACCESS_DENIED);
                return makeError(CloudReturnCodes.ERROR_ACCESS_DENIED);
            }
        }

        // if local server for streaming
        if(mRegToken.getRtmpPublish() != null){
            misStarted = true;
            mCallback.onStarted(mRegToken.getRtmpPublish());
            return makeError(CloudReturnCodes.OK);
        }

        return prepareCM();
    }

    int call_Stop()
    {
        if(mWebSocketClient != null)
            mWebSocketClient.close();
        if(misStarted){
            if(mCallback != null)
                mCallback.onStopped();
            misStarted = false;
        }
        return makeError(CloudReturnCodes.OK);
    }

    public int getPreviewWidth() {
        return 320;
    }

    public int getPreviewHeight() {
        return 240;
    }

    public void cropPreview(String origPreviewFullpath, String cropPreviewFullpath){

        // preview saved to preview_fullscreen
        File origPreview = new File(origPreviewFullpath);

        // crop image
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap srcBmp = BitmapFactory.decodeFile(origPreview.getAbsolutePath(), options);
        Bitmap dstBmp = null;
        Double bmpWidth = (double) srcBmp.getWidth();
        Double bmpHeight = (double) srcBmp.getHeight();
        Double previewWidth = (double) getPreviewWidth();
        Double previewHeight = (double) getPreviewHeight();

        // crop preview
        if (srcBmp.getWidth() <= srcBmp.getHeight()){
            Double kx = bmpWidth/previewWidth;
            int height = Double.valueOf(240*kx).intValue();
            Bitmap b = Bitmap.createBitmap(srcBmp, 0, srcBmp.getHeight()/2 - height/2, srcBmp.getWidth(), height);
            dstBmp = Bitmap.createScaledBitmap(b, getPreviewWidth(), getPreviewHeight(), false);
        }else{
            Double ky = bmpHeight/previewHeight;
            int width = Double.valueOf(320*ky).intValue();
            Bitmap b = Bitmap.createBitmap(srcBmp, srcBmp.getWidth()/2 - width/2, 0, width, srcBmp.getHeight());
            dstBmp = Bitmap.createScaledBitmap(b, getPreviewWidth(), getPreviewHeight(), false);
        }

        // save cropped preview
        try{
            FileOutputStream filePreviewOutputStream = new FileOutputStream(new File(cropPreviewFullpath));
            dstBmp.compress(Bitmap.CompressFormat.JPEG, 75, filePreviewOutputStream);
            filePreviewOutputStream.flush(); // Not really required
            filePreviewOutputStream.close(); // do not forget to close the stream
        }catch(FileNotFoundException e){
            android.util.Log.e(TAG, "File not found " + e.getMessage());
            e.printStackTrace();
        }catch(IOException e){
            android.util.Log.e(TAG, "IOException " + e.getMessage());
            e.printStackTrace();
        }
    }

    int call_sendPreviewStreamer(String url, String file){

        String file_new = file;
        int idx = file_new.lastIndexOf('.');
        if(idx > 0){
            file_new = file_new.substring(0, idx-1)+"_cropped"+".jpg";
        }
        cropPreview(file, file_new);

        URL mUploadPreviewURL = null;
        try {
            mUploadPreviewURL = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return -1;
        }

        File mPreviewFilename = new File(file_new);

        Log.i("=>call_sendPreviewStreamer url=" + mUploadPreviewURL +" file="+mPreviewFilename);

        try {
            HttpURLConnection urlConnection = (HttpURLConnection) mUploadPreviewURL.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setReadTimeout(readTimeout);
            urlConnection.setConnectTimeout(connectTimeout);
            Log.i("readTimeout " + readTimeout);
            urlConnection.setDoInput(true);
            urlConnection.setUseCaches(false);

            int size = (int) mPreviewFilename.length();
            byte[] body = new byte[size];
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(mPreviewFilename));
            buf.read(body, 0, body.length);
            buf.close();
            // urlConnection.setRequestProperty("Content-Length",  Integer.toString(body1.length));
            DataOutputStream request = new DataOutputStream(urlConnection.getOutputStream());
            request.write(body);
            request.flush();
            request.close();

            int codeResponse = urlConnection.getResponseCode();
            Log.i("codeResponse " + codeResponse);

            /*boolean isError = codeResponse >= 400;
            InputStream inputStream = isError ? urlConnection.getErrorStream() : urlConnection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }*/

        }catch(FileNotFoundException e){
            Log.e("File not found ");
            e.printStackTrace();
            return -1;
        }catch(ProtocolException e){
            Log.e("Wrong protocol ");
            e.printStackTrace();
            return -1;
        }catch(IOException e){
            Log.e("Somthing wrong");
            e.printStackTrace();
            return -1;
        }
        return 0;
    }

    private int prepareCM(){

        mCMConfig = new CameraManagerConfig();
        if(mCameraConfig == null) {
            mCMConfig.setUUID("");
            mCMConfig.setSID("");
            mCMConfig.setPwd("");
            //mCMConfig.setConnID("");
            mCMConfig.setUploadURL("");
            mCMConfig.setMediaServer("");
            mCMConfig.setCamID(mCamera.getID());

            mCMConfig.setRegToken(mRegToken);
            mCMConfig.setConnID(null);

            Log.v("=prepareCM with reset");
        }else{
            Log.v("=prepareCM with config");

            JSONObject jo;
            try {
                jo = new JSONObject(mCameraConfig);
                mCMConfig.setUUID(jo.getString("uuid"));
                mCMConfig.setSID(jo.getString("sid"));
                mCMConfig.setPwd(jo.getString("pwd"));
                mCMConfig.setUploadURL("");
                mCMConfig.setMediaServer("");
                mCMConfig.setCamID(jo.getLong("camid"));
                mCMConfig.setRegToken(null);
                mCMConfig.setConnID(jo.getString("connid"));
            } catch (JSONException e) {
                e.printStackTrace();
                mCameraConfig = null;
                return makeError(CloudReturnCodes.ERROR_NOT_CONFIGURED);
            }
        }

        try {
            mCMConfig.setCMVersion( "Android CM " + mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        //start websocket
        URI uri = mCMConfig.getAddress();
        Log.i("=prepareCM CameraConfiguration " + uri);
        mWebSocketClient = new VEG_WebSocket(mWebSocketListener, mCMListener, uri);

        return makeError(CloudReturnCodes.OK);
    }

    private void cmd_register() {
        if (mWebSocketClient.isOpen()) {
            JSONObject json = getBaseJSON(CameraManagerCommandNames.REGISTER);
            try{
                json.put(CameraManagerParameterNames.VER, mCMConfig.getCMVersion());
                json.put(CameraManagerParameterNames.TZ, mCMConfig.getCameraTimezone());
                json.put(CameraManagerParameterNames.VENDOR, mCMConfig.getCameraVendor());
                if(mCMConfig.getPwd() != null){
                    json.put(CameraManagerParameterNames.PWD, mCMConfig.getPwd());
                }
                if(mCMConfig.getSID() != null){
                    json.put(CameraManagerParameterNames.PREV_SID, mCMConfig.getSID());
                }

                if(mCMConfig.getRegToken() != null){
                    json.put(CameraManagerParameterNames.REG_TOKEN, mCMConfig.getRegToken().getToken());
                }

            }catch(JSONException e){
                Log.e("=cmd_register. Invalid json " + e.getMessage());
                e.printStackTrace();
            }

            mWebSocketClient.sendJSON(json.toString());
        } else {
            Log.e("<=cmd_register: webSocketClient is't Open");
        }
    }
    public void cmd_cam_register() {
        JSONObject json = getBaseJSON(CameraManagerCommandNames.CAM_REGISTER);
        try {
            json.put(CameraManagerParameterNames.IP, mCMConfig.getCameraIPAddress());
            json.put(CameraManagerParameterNames.UUID, mCMConfig.getUUID());
            json.put(CameraManagerParameterNames.BRAND, mCMConfig.getCameraBrand());
            json.put(CameraManagerParameterNames.MODEL, mCMConfig.getCameraModel());
            json.put(CameraManagerParameterNames.SN, mCMConfig.getCameraSerialNumber());
            json.put(CameraManagerParameterNames.VERSION, mCMConfig.getCameraVersion());
        }catch(JSONException e){
            Log.e("<=cmd_cam_register. err Could not camera register "+e.getMessage());
            e.printStackTrace();
            return;
        }

        if (mWebSocketClient != null)
            mWebSocketClient.sendJSON(json.toString());

        //controllerHandler.sendEmptyMessage(StreamController.ACTION_OPEN_STREAM_ACTIVITY);
    }


    private JSONObject getBaseJSON(String cmd) {
        JSONObject obj = new JSONObject();
        try {
            obj.put(CameraManagerParameterNames.CMD, cmd);
            obj.put(CameraManagerParameterNames.MSGID, massageId++);
        }catch(JSONException e){
            Log.e(e.getMessage());
            e.printStackTrace();
        }
        return obj;
    }


    //=> WebSocketApiListener
    WebSocketApiListener mWebSocketApiListener = new WebSocketApiListener(){
        @Override
        public void onPreparedCM() {
            Log.i("=>onPreparedCM");
            String mediaServerURL = mCMConfig.getMediaServer();
            String cam_path = mCamPath;//mVXGCloudCamera.getCamPath();
            String stream_id = "Main";//mVXGCloudCamera.getStreamID();
            String sid = mCMConfig.getSID();
            //mCamID = mCMConfig.getCamID();

            String url = "rtmp://" + mediaServerURL + "/" + cam_path + stream_id + "?sid=" + sid ;

            mStreamUrl = url;

            Log.i("<=onPreparedCM mStreamUrl="+mStreamUrl);
        }

        @Override
        public void onServerConnClose(CameraManagerErrors reason) {
            Log.i("=onServerConnClose reason="+reason);


            if(reason != CameraManagerErrors.LOST_SERVER_CONNECTION && m_last_error != reason)
                mCallback.onError(CloudReturnCodes.ERROR_STREAM_UNREACHABLE);
            if(misStarted){
                mCallback.onStopped();
                misStarted = false;
            }
            m_last_error = reason;
            start_stream_counter=0;
        }

        @Override
        public void onUpdatedCameraManagerConfig(CameraManagerConfig config) {
            JSONObject jo = new JSONObject();
            try {
                if(config.getUUID() != null)
                    jo.put("uuid", config.getUUID());
                if(config.getSID() != null)
                    jo.put("sid", config.getSID());
                if(config.getPwd() != null)
                    jo.put("pwd", config.getPwd());
                if(config.getConnID() != null)
                    jo.put("connid", config.getConnID());
                jo.put("camid", config.getCamID());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mCameraConfig = jo.toString();
        }
    };
    interface WebSocketApiListener {
        void onPreparedCM();
        void onServerConnClose(CameraManagerErrors reason);
        void onUpdatedCameraManagerConfig(CameraManagerConfig config);
    }
    //<= WebSocketApiListener

    //=> CameraManager listener
    CameraManagerClientListener mCMListener = new CameraManagerClientListener(){

        @Override
        public CameraManagerConfig getConfig() {
            return mCMConfig;
        }

        @Override
        public void setConfig(CameraManagerConfig config) {
            mCMConfig = config;
        }

        @Override
        public void sendCmdDone(int cmd_id, String cmd, CameraManagerDoneStatus status) {
            try {
                JSONObject data = new JSONObject();
                data.put(CameraManagerParameterNames.CMD, CameraManagerCommandNames.DONE);
                data.put(CameraManagerParameterNames.REFID, cmd_id);
                data.put(CameraManagerParameterNames.ORIG_CMD, cmd);
                data.put(CameraManagerParameterNames.STATUS, status.toString());

                if (mWebSocketClient != null) {
                    mWebSocketClient.sendJSON(data.toString());
                    // TODO remove it
                    switch (cmd) {
                        case CameraManagerCommandNames.HELLO :
                            cmd_cam_register();
                            break;
                    }
                }
            } catch(JSONException e) {
                Log.e("<=sendCmdDone, invalid json");
            }
        }

        @Override
        public void send(JSONObject response) {
            mWebSocketClient.sendJSON(response.toString());
        }

        @Override
        public void sendPreview(long cam_id) {

            if(mPreviewFile == null || mPreviewFile.length() < 1){
                return;
            }
            Log.i("=>sendPreview: " + cam_id);
            if (cam_id == mCMConfig.getCamID()) {
                final String url =
                        "http://" +
                                mCMConfig.getUploadURL() + "/" +
                                mCamPath +
                                "?sid=" + mCMConfig.getSID() +
                                "&cat=" + UPLOADING_CATEGORY_PREVIEW +
                                "&type=" + UPLOADING_TYPE_JPG +
                                "&start=" + CloudHelpers.formatCurrentTimestampUTC_MediaFileUploading();
                Log.i("=sendPreview " + url);
                sendPreviewStreamer(url, mPreviewFile);
            } else {
                Log.e("=sendPreview. Unknown camera !!!" + cam_id + " (expected " + mCMConfig.getCamID());
            }
        }

        @Override
        public void onUpdatedConfig() {
            mWebSocketApiListener.onUpdatedCameraManagerConfig(mCMConfig);
        }

        @Override
        public void onByeReconnect() {
            isReconnect = true;
            if (mWebSocketClient != null)
                mWebSocketClient.close();
            URI uri = mCMConfig.getAddress();
            if(uri != null) {
                mWebSocketClient = new VEG_WebSocket(mWebSocketListener, mCMListener, uri);
            }else{
                Log.e("<=onByeReconnect. uri is null");
            }
            isReconnect = false;
        }

        @Override
        public void onByeError() {
            mWebSocketApiListener.onServerConnClose(CameraManagerErrors.REASON_ERROR);
        }

        @Override
        public void onByeSystemError() {
            mWebSocketApiListener.onServerConnClose(CameraManagerErrors.REASON_SYSTEM_ERROR);
        }

        @Override
        public void onByeInvalidUser() {
            mWebSocketApiListener.onServerConnClose(CameraManagerErrors.REASON_INVALID_USER);
        }

        @Override
        public void onByeAuthFailure() {
            mWebSocketApiListener.onServerConnClose(CameraManagerErrors.REASON_AUTH_FAILURE);
        }

        @Override
        public void onByeConnConflict() {
            mWebSocketApiListener.onServerConnClose(CameraManagerErrors.REASON_CONN_CONFLICT);
        }

        @Override
        public void onByeShutdown() {
            mWebSocketApiListener.onServerConnClose(CameraManagerErrors.REASON_SHUTDOWN);
        }

        @Override
        public void onByeDelete() {
            mWebSocketApiListener.onServerConnClose(CameraManagerErrors.REASON_DELETED);
        }

        @Override
        public void onStreamStart(String reason) {
            if(start_stream_counter == 0) {
                misStarted = true;
                mCallback.onStarted(mStreamUrl);
            }
            start_stream_counter = start_stream_counter + 1;
        }

        @Override
        public void onStreamStop(String reason) {
            if(reason != null && reason.equals("prolong")){
            }else{
                //stop in 3 sec
                start_stream_counter = start_stream_counter + 1;
                Prolongate(3000);
            }

            if(start_stream_counter > 0) {
                start_stream_counter = start_stream_counter - 1;
            }
            if(start_stream_counter == 0 && misStarted) {
                misStarted = false;
                mCallback.onStopped();
            }
        }
    };
    //<= CameraManager listener


    //=> WEB SOCKET PART
    ServerWebSocketListener mWebSocketListener = new ServerWebSocketListener() {
        @Override
        public void onCloseWebSocket(URI uri, boolean wasOpened) {
            mWebSocketApiListener.onServerConnClose(CameraManagerErrors.LOST_SERVER_CONNECTION);
        }

        @Override
        public void onErrorWebSocket(CameraManagerErrors error) {
            mWebSocketApiListener.onServerConnClose(error);
        }

        @Override
        public void onOpenWebSocket() {
            cmd_register();
        }

        @Override
        public void onCamHelloReceived(int refid, String orig_cmd, int cam_id, String media_url, boolean activity) {
            Log.i("=>onCamHelloReceived refid="+refid+" orig_cmd="+orig_cmd+" cam_id="+cam_id+" media_url="+media_url+" activity="+activity);
            mCMConfig.setCamID(cam_id);
            mWebSocketApiListener.onUpdatedCameraManagerConfig(mCMConfig);
            mCamPath = media_url;
            mCMConfig.setCameraActivity(activity);
            mWebSocketApiListener.onPreparedCM();
            mCMListener.sendCmdDone(refid, orig_cmd, CameraManagerDoneStatus.OK);
            Log.i("<=onCamHelloReceived refid="+refid+" orig_cmd="+orig_cmd+" cam_id="+cam_id+" media_url="+media_url+" activity="+activity);
        }
    };

    interface ServerWebSocketListener {
        void onCloseWebSocket(URI uri, boolean wasOpened);
        void onErrorWebSocket(CameraManagerErrors error);
        void onOpenWebSocket();
        void onCamHelloReceived(int refid, String orig_cmd, int cam_id, String media_url, boolean activity);
    }

    class VEG_WebSocket extends WebSocket {
        private final String TAG = "VEG_WebSocket";

        private ServerWebSocketListener serverWebSocketListener;
        private URI curURI;
        private boolean isOpen = false;
        private boolean wasOpened = false;
        private HashMap<String, CmdHandler> mHandlers = CreateCmdHandlers.create();
        private CameraManagerClientListener mClient = null;

        public VEG_WebSocket(ServerWebSocketListener listener, CameraManagerClientListener client, final URI uri) {
            super(uri);
            this.curURI = uri;
            android.util.Log.i(TAG, "mHandlers.size(): " + mHandlers.size());
            mClient = client;
            serverWebSocketListener = listener;
            this.setEventHandler(new WebSocketEventHandler() {
                @Override
                public void onOpen() {
                    android.util.Log.i(TAG, "WebSocket Opened." + curURI);
                    isOpen = true;
                    wasOpened = true;
                    serverWebSocketListener.onOpenWebSocket();
                }

                @Override
                public void onMessage(WebSocketMessage message) {
                    if(!message.isText()) // could be binary
                        return;
                    android.util.Log.i(TAG, "Receive message: " + message.getText());

                    JSONObject messageJson = null;
                    String cmd = "";
                    try {
                        messageJson = new JSONObject(message.getText());
                        if(messageJson.has("cmd") && !messageJson.isNull("cmd")){
                            cmd = messageJson.getString("cmd");
                        }
                    } catch (JSONException e) {
                        android.util.Log.e(TAG, "onMessage, invalid json: " + message.getText());
                        e.printStackTrace();
                    }

                    if(mHandlers.containsKey(cmd)) {
                        mHandlers.get(cmd).handle(messageJson, mClient);
                    } else {
                        //JsonHelper json = new JsonHelper(message.getText());
                        JSONObject json = null;
                        try {
                            json = new JSONObject(message.getText());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        int msgid = -1;
                        try {
                            msgid = json.getInt(CameraManagerParameterNames.MSGID);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        int camid = -1;
                        try {
                            camid = json.getInt(CameraManagerParameterNames.CAM_ID);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        String mediaurl = "";
                        try {
                            mediaurl = json.getString(CameraManagerParameterNames.MEDIA_URL);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        boolean is_activity = false;
                        try {
                            is_activity = json.getBoolean(CameraManagerParameterNames.ACTIVITY);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        // OLD HANDLE COMMAND
                        switch (cmd) {
                            case CameraManagerCommandNames.CAM_HELLO:
                                // TODO redesign to CmdHandler
                                    serverWebSocketListener.onCamHelloReceived(
                                            msgid,
                                            CameraManagerCommandNames.CAM_HELLO,
                                            camid,
                                            mediaurl,
                                            is_activity
                                    );
                                break;
                            default:
                                android.util.Log.e(TAG, "Unhandled comamnd '" + cmd + "'");
                        }
                    }
                }

                @Override
                public void onClose() {
                    android.util.Log.i(
                            TAG,
                            "VXGCloudCamera Closed "
                                    + " wasOpened=" + wasOpened
                    );
                    isOpen = false;
                    serverWebSocketListener.onCloseWebSocket(curURI, wasOpened);
                }

                @Override
                public void onError(WebSocketException e) {
                    android.util.Log.e(TAG, "onError: " + e.getMessage() );
                }

                @Override
                public void onLogMessage(String msg) {
                    android.util.Log.d(TAG, "onLogMessage: " + msg);
                    if (msg != null) {
                        if(msg.contains("connect failed: ETIMEDOUT (Connection timed out)")) {// || msg.contains("failed to connect to"))
                            android.util.Log.e(TAG, "Block 8888 port");
                            serverWebSocketListener.onErrorWebSocket(CameraManagerErrors.BLOCK_PORT);
                        }else if (msg.contains("connect failed: ENETUNREACH (Network is unreachable)")) {// || msg.contains("failed to connect to"))
                            android.util.Log.e(TAG, "connect failed: ENETUNREACH (Network is unreachable)");
                            serverWebSocketListener.onErrorWebSocket(CameraManagerErrors.NETWORK_IS_UNREACHABLE);
                        } else if (msg.contains("recvfrom failed: ETIMEDOUT (Connection timed out)")) {
                            android.util.Log.e(TAG, "recvfrom failed: ETIMEDOUT (Connection timed out)");
                            serverWebSocketListener.onErrorWebSocket(CameraManagerErrors.CONNECTION_TIMEOUT);
                        } else if (msg.contains("failed to connect to " + CameraManagerConfig.ADDRESS) && msg.contains("(port " + CameraManagerConfig.PORT + ") after ")) {
                            android.util.Log.e(TAG, "WebSocketListener: " + msg);
                            serverWebSocketListener.onErrorWebSocket(CameraManagerErrors.CONNECTION_TIMEOUT);
                        }else {
                            android.util.Log.e(TAG, "Unhandled message: " + msg);
                        }
                    }
                }
                //IO Error проверку
            });
            this.connect();
        }

        public void sendJSON(String text) throws NotYetConnectedException {
            if (isOpen) {
                android.util.Log.i(TAG, "Send: " + text);
                super.send(text);
            }
            else {
                android.util.Log.e(TAG, "Can't send: " + text);
            }
        }

        public boolean isOpen() {
            return super.isConnected();
        }
    }
    //<= WEB SOCKET PART

}
