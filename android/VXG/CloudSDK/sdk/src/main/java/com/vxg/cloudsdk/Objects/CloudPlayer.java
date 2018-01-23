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

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.view.ViewGroup.LayoutParams;

import com.vxg.cloud.core.CloudCommon.CloudLiveUrls;
import com.vxg.cloud.core.CloudAPI;
import com.vxg.cloud.core.CloudHelpers;
import com.vxg.cloud.core.CloudResponseWithMeta;
import com.vxg.cloudsdk.CloudSDK;
import com.vxg.cloudsdk.Enums.CloudPlayerEvent;
import com.vxg.cloudsdk.Enums.CloudPlayerState;
import com.vxg.cloudsdk.Enums.CloudReturnCodes;
import com.vxg.cloudsdk.Helpers.MLog;
import com.vxg.cloudsdk.Helpers.Msg;
import com.vxg.cloudsdk.Interfaces.ICloudConnection;
import com.vxg.cloudsdk.Interfaces.ICloudObject;
import com.vxg.cloudsdk.Interfaces.ICloudPlayer;
import com.vxg.cloudsdk.Interfaces.ICloudPlayerCallback;
import com.vxg.cloudsdk.Objects.Playback.PlaybackPlayer;
import com.vxg.cloudsdk.Objects.Playback.PlaybackSegment;
import com.vxg.cloudsdk.Objects.Playback.PlaybackTimeline;
import com.vxg.cloudsdk.Objects.Playback.Player;
import com.vxg.cloudsdk.Objects.Playback.Player.PlayerCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;

import veg.mediaplayer.sdk.MediaPlayer;
import veg.mediaplayer.sdk.MediaPlayer.MediaPlayerCallback;
import veg.mediaplayer.sdk.MediaPlayer.PlayerState;
import veg.mediaplayer.sdk.MediaPlayerConfig;

import static veg.mediaplayer.sdk.MediaPlayer.PlayerState.*;


public class CloudPlayer extends CloudObject implements ICloudPlayer {
    private static final String TAG = "CloudPlayer";
    final public static int LOG_LEVEL = 2; //Log.VERBOSE;
    static MLog Log = new MLog(TAG, LOG_LEVEL);
    boolean VERBOSE = true;

    public static long POSITION_LIVE = (-1);

    private static int MODE_LIVE = 1;
    private static int MODE_PLAYBACK = 2;
    int player_mode = MODE_LIVE;

    Context mContext;
    View mView;
    CloudPlayerConfig mConfig;
    private final LinkedList<ICloudPlayerCallback> mCallbacks = new LinkedList<ICloudPlayerCallback>();
    CloudAPI api;
    long mCamid = -1;
    boolean is_source_changed = false;
    ICloudObject mSource = null;

    //VXG player wrapper
    PlaybackPlayer player;
    CloudPlayerEvent m_last_player_event = CloudPlayerEvent.CLOSED;
    boolean mis_open = false;
    boolean mis_close = false;
    boolean mis_setposition = false;
    //long mServerTime = 0;
    //long mServerStartDiff = 0;
    long mServerTimeDiff = 0;

    long m_set_position = POSITION_LIVE;
    boolean is_seek = false;

    boolean mis_disable_notify = false; //for internal player re-construction
    long m_get_position = 0; //last returned position
    long m_get_player_position = 0;
    boolean mis_paused = false;

    //audio
    private AudioManager audio_manager = null;
    boolean mis_mute = false;

    //async functions
    private LinkedList<Msg> cmd_queue = new LinkedList<Msg>();
    final int CMD_Play = 1;
    final int CMD_Pause = 2;
    final int CMD_Close = 3;
    final int CMD_SetPosision = 4;
    final int CMD_UpdateSegments = 5;


    public CloudPlayer(View view, CloudPlayerConfig config, ICloudPlayerCallback callback) {
        mView = view;
        mConfig = config;
        mContext = CloudSDK.getContext();
        addCallback(callback);

        MediaPlayer _player1 = new MediaPlayer(mContext);
        MediaPlayer _player2 = new MediaPlayer(mContext);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER);
        _player1.setLayoutParams(params);
        _player2.setLayoutParams(params);
        ((FrameLayout) mView).addView(_player1);
        ((FrameLayout) mView).addView(_player2);

        MediaPlayerConfig commonPlayerConfig = new MediaPlayerConfig();
        if (mConfig != null){
            commonPlayerConfig.setAspectRatioMode(mConfig.m_aspectRatio);
        }

        player = new PlaybackPlayer(mContext, _player1, _player2, commonPlayerConfig, player_callback);

        if (mContext == null || mView == null) {
            makeError(CloudReturnCodes.ERROR_NOT_CONFIGURED);
        }

        audio_manager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
    }

    private void _reset(){
        player_mode = MODE_LIVE;

        //VXG player wrapper
        //mServerTime = 0;
        m_set_position = POSITION_LIVE;
        mis_disable_notify = false; //for internal player re-construction
        m_get_position = 0;
        mis_paused = false;
        is_seek = false;
    }

    PlayerCallback player_callback = new PlayerCallback() {

        @Override
        public void ConnectStarted(Player p) {
            //mServerTime = CloudHelpers.parseUTCTime(api.getServerTime());
            //mServerStartDiff = SystemClock.elapsedRealtime();
            Notify(CloudPlayerEvent.CONNECTED);
        }

        @Override
        public void ConnectFailed(Player p) {
            makeError(CloudReturnCodes.ERROR_NO_CLOUD_CONNECTION);
            Notify(CloudPlayerEvent.ERROR);
        }

        @Override
        public void PlaybackStarted(Player p) {
            //if(mServerStartDiff != 0) {
            //    mServerTime += (SystemClock.elapsedRealtime() - mServerStartDiff);
            //    mServerStartDiff = 0;
            //}
            Notify(CloudPlayerEvent.STARTED);
        }

        @Override
        public void PlaybackFinished(Player p) {
            Notify(CloudPlayerEvent.EOS);
        }

        @Override
        public void PlaybackPaused(Player p) {
            Notify(CloudPlayerEvent.PAUSED);
        }

        @Override
        public void PlaybackSeekCompleted(Player p) {
            is_seek = false;
            Notify(CloudPlayerEvent.SEEK_COMPLETED);
        }

        @Override
        public void FirstVideoFrameAvailable(Player p) {

            if(is_seek) {
                is_seek = false;
                Notify(CloudPlayerEvent.SEEK_COMPLETED);
            }

            if(mis_paused){
                if(isLive()){
                    player.PauseFlush();
                }else{
                    player.Pause();
                }
            }else {
                player.Play();
            }
        }

        @Override
        public void TrialVersion(Player player) {
            Notify(CloudPlayerEvent.TRIAL_VERSION);
            close();
        }

        @Override
        public void PlaybackNextSegment(Player player) {
            Msg m = new Msg();
            m.func_id = CMD_UpdateSegments;
            m.args = new ArrayList<Object>();
            m.func_complete = null;
            cmd_queue.add(m);

            Log.v("=PlaybackNextSegment");

            //start processing
            execute();
        }
    };

    void Notify(CloudPlayerEvent player_event) {
        if (m_last_player_event == player_event && player_event != CloudPlayerEvent.SEEK_COMPLETED)
            return;
        m_last_player_event = player_event;
        if (!mis_disable_notify && !mCallbacks.isEmpty()) {
            synchronized (mCallbacks) {
                for( ICloudPlayerCallback cb: mCallbacks){
                    cb.onStatus(player_event, this);
                }
            }
        }
    }

    @Override
    public void setSource(ICloudObject src) {
        if (src instanceof CloudCamera) {
            mSource = src;
            CloudCamera camera = (CloudCamera) src;
            ICloudConnection conn = camera._getCloudConnection();
            mServerTimeDiff = conn.getServerTimeDiff();
            api = conn._getAPI();
            is_source_changed = true;//(mCamid != camera.getID());
            mCamid = camera.getID();
        }
        _reset();

        if(MediaPlayer.PlayerState.Closed != player.getPlayerState()){
            close();
        }
        Notify(CloudPlayerEvent.SOURCE_CHANGED);
        makeError(api == null ? CloudReturnCodes.ERROR_BADARGUMENT : CloudReturnCodes.OK);
    }

    @Override
    public ICloudObject getSource() {
        return mSource;
    }

    @Override
    public void addCallback(ICloudPlayerCallback callback) {
        if(callback == null)
            return;
        synchronized (mCallbacks) {
            mCallbacks.add(callback);
            Log.v("addCallback callback="+callback+" cnt="+mCallbacks.size());
        }
    }

    @Override
    public void removeCallback(ICloudPlayerCallback callback) {
        if(callback == null)
            return;
        synchronized (mCallbacks) {
            mCallbacks.remove(callback);
            Log.v("removeCallback callback="+callback+" cnt="+mCallbacks.size());
        }
    }

    private int _applyConfig(){
        if (mConfig == null)
            return 0;

        Log.v("applyConfig");

        MediaPlayerConfig config = new MediaPlayerConfig();
        config.setAspectRatioMode(mConfig.m_aspectRatio);
        player.updateConfig(config);
        return 0;
    }

    @Override
    public int setConfig(CloudPlayerConfig config) {
        mConfig = config;
        return _applyConfig();
    }

    @Override
    public CloudPlayerConfig getCloneConfig() {
        CloudPlayerConfig config = new CloudPlayerConfig(mConfig);
        return config;
    }

    @Override
    public void play() {

        mis_paused = false;

        Msg m = new Msg();
        m.func_id = CMD_Play;
        m.args = new ArrayList<Object>();
        m.func_complete = null;
        cmd_queue.add(m);

        Log.v("=play");

        //start processing
        execute();
    }

    @Override
    public void pause() {
        mis_paused = true;

        Msg m = new Msg();
        m.func_id = CMD_Pause;
        m.args = new ArrayList<Object>();
        m.func_complete = null;
        cmd_queue.add(m);

        Log.v("=pause");

        //start processing
        execute();
    }

    @Override
    public void close() {

        //reset all pending commands
        cmd_queue.clear();

        is_source_changed = true;

        Msg m = new Msg();
        m.func_id = CMD_Close;
        m.args = new ArrayList<Object>();
        m.func_complete = null;
        cmd_queue.add(m);

        Log.v("=close");

        //start processing
        execute();
    }

    @Override
    public CloudPlayerState getState() {
        if(m_last_player_event == CloudPlayerEvent.EOS){
            return CloudPlayerState.EOS;
        }
        MediaPlayer.PlayerState state = player.getPlayerState();
        switch(state){
            case Opening:
                return CloudPlayerState.CONNECTING;
            case Opened:
                return CloudPlayerState.CONNECTED;
            case Started:
                return mis_paused?CloudPlayerState.PAUSED:CloudPlayerState.STARTED;
            case Paused:
                return CloudPlayerState.PAUSED;
        }
        return CloudPlayerState.CLOSED;
    }

    @Override
    public void setPosition(long nPosition) {
        Msg m = new Msg();
        m.func_id = CMD_SetPosision;
        m.args = new ArrayList<Object>();
        m.args.add(nPosition);
        m.func_complete = null;
        cmd_queue.add(m);

        Log.v("=setPosition");

        //start processing
        execute();
    }

    @Override
    public long getPosition() {
        long pos = 0L;
        PlaybackTimeline timeline = player.getTimeline();
        if (timeline != null && timeline.getCurrentSegment() != null) {
            pos += timeline.getCurrentTime();
            if (pos == -1L)
                pos = 0L;
        }
        if (isLive()) {
            //pos += mServerTime;

            //show current_pts-serverDiff
            if(getState()==CloudPlayerState.PAUSED
                    //|| m_get_player_position == pos
                    ){
                pos = 0; //pos not changed
            }else{
                m_get_player_position = pos;
                pos = CloudHelpers.currentTimestampUTC() - mServerTimeDiff;
            }
        }
        if (pos == 0) {
            pos = m_get_position;
        }
        m_get_position = pos;

        /*if(VERBOSE){
            print_segments();
            Log.v("<=getPosition="+pos+" "+CloudHelpers.formatTime(pos));
        }*/

        if(pos == 0 && m_set_position != POSITION_LIVE){
            pos = m_set_position;
        }

        return pos;
    }

    @Override
    public boolean isLive() {
        return (player_mode == MODE_LIVE);
    }

    @Override
    public Bitmap getVideoShot(int width, int height) {
        if(player == null)
            return null;
        MediaPlayer.VideoShot videoShot =  player.getVideoShot(-1, -1);
        if(videoShot == null || videoShot.getData() == null || videoShot.getWidth() < 1 || videoShot.getHeight() < 1)
            return null;

        Bitmap bmp_src = Bitmap.createBitmap(videoShot.getWidth(), videoShot.getHeight(), Bitmap.Config.ARGB_8888);
        bmp_src.copyPixelsFromBuffer(videoShot.getData());

        Bitmap bmp_dst = Bitmap.createScaledBitmap(bmp_src, width, height, true);
        return bmp_dst;
    }

    @Override
    public void mute(boolean bMute) {
        mis_mute = bMute;
        player.mute(bMute);
    }

    @Override
    public boolean isMute() {
        return mis_mute;
    }

    @Override
    public void setVolume(int val) {
        int currentVolume = audio_manager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVolume = audio_manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if(currentVolume != val && val <= maxVolume) {
            audio_manager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);
        }
    }

    @Override
    public int getVolume() {
        int currentVolume = audio_manager.getStreamVolume(AudioManager.STREAM_MUSIC);
        return currentVolume;
    }

    @Override
    public void showTimeline(View vwTimeline) {
        //TODO
    }

    @Override
    public void hideTimeline() {
        //TODO
    }

    @Override
    public void runt() {

        //processing
        Msg msg = cmd_queue.poll();
        if (msg == null) {
            return;
        }

        switch (msg.func_id) {
            case CMD_Play:
                call_Play();
                break;
            case CMD_Pause:
                call_Pause();
                break;
            case CMD_SetPosision:
                call_SetPosition((long) msg.args.get(0));
                break;
            case CMD_Close:
                call_Close();
                break;
            case CMD_UpdateSegments:
                update_segments(getPosition());
                break;
        }
    }

    private int call_Open() {
        if (mContext == null) {
            return makeError(CloudReturnCodes.ERROR_NOT_CONFIGURED);
        }

        PlayerState ps = player.getPlayerState();
        boolean is_need_reopen = (m_last_player_event == CloudPlayerEvent.TRIAL_VERSION || m_last_player_event == CloudPlayerEvent.ERROR || m_last_player_event == CloudPlayerEvent.EOS || ps == PlayerState.Closed || is_source_changed);

        if (!is_need_reopen) {
            return 1;
        }

        if (is_source_changed || isLive()) {
            mis_disable_notify = true;
            call_Close();
            mis_disable_notify = false;
        }
        is_source_changed = false;

        if (api == null) {
            makeError(CloudReturnCodes.ERROR_NOT_CONFIGURED);
            Notify(CloudPlayerEvent.ERROR);
            return CloudReturnCodes.ERROR_NOT_CONFIGURED;
        }

        if (isLive()) {
            //live mode
            CloudLiveUrls liveurl = null;
            long tick_cur = CloudHelpers.currentTimestampUTC();
            do {
                if (liveurl != null) {
                    sleep(100);
                }
                Notify(CloudPlayerEvent.CONNECTING);
                liveurl = api.getCameraLiveUrls(mCamid);
                if (liveurl.hasError() && (liveurl.getErrorStatus() == 404 || liveurl.getErrorStatus()==0)) {
                    return makeHTTPError(liveurl.getErrorStatus());
                }
                if(CloudHelpers.currentTimestampUTC()-tick_cur > 60*1000){
                    return makeHTTPError(404);
                }
            } while (liveurl.hasError() && !is_source_changed);

            if (is_source_changed) {
                return 0;
            }

            PlaybackSegment segment = new PlaybackSegment("", liveurl.rtmp(), mCamid, 0L, 0L, 0L);
            player.addForOpen(segment);
        } else {
            //playback mode
            PlaybackSegment segment = check_segment(m_set_position);
            if (segment == null) {
                player.Pause();
                Notify(CloudPlayerEvent.EOS);
                return CloudReturnCodes.ERROR_RECORDS_NOT_FOUND;
            }

        }


        mis_open = false;
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Notify(CloudPlayerEvent.CONNECTING);

                player.open(mis_paused);

                mis_open = true;
                wakeup();
            }
        });
        while (!mis_open) {
            sleep(50);
        }

        return 0;
    }

    private int call_Close() {
        if (mContext == null) {
            return makeError(CloudReturnCodes.ERROR_NOT_CONFIGURED);
        }

        PlayerState ps = player.getPlayerState();
        mis_close = false;
        if (ps != PlayerState.Closed) {
            ((Activity) mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    player.close();
                    Notify(CloudPlayerEvent.CLOSED);
                    mis_close = true;
                    wakeup();
                }
            });
            while (!mis_close) {
                sleep(50);
            }
        }

        return 0;
    }

    private int call_Play() {
        call_Open();
        player.Play();
        return 0;
    }

    private int call_Pause() {
        call_Open();

        if (isLive()) {
            player.PauseFlush();
        } else {
            player.Pause();
        }
        return 0;
    }

    private int call_SetPosition(long position) {

        is_seek = true;

        if (position == POSITION_LIVE) {
            //switch to LIVE mode
            if (isLive())
                return 0;

            player_mode = MODE_LIVE;
            m_set_position = position;

            //re-open
            mis_disable_notify = true;
            call_Close();
            mis_disable_notify = false;

            call_Open();

        } else {

            //switch to MODE_PLAYBACK
            if (isLive()) {
                //mis_disable_notify = true;
                //call_Close();
                //mis_disable_notify = false;
                player.getTimeline().clearAll();
            }

            player_mode = MODE_PLAYBACK;
            m_set_position = position;
            int ret = call_Open();
            if(CloudReturnCodes.ERROR_RECORDS_NOT_FOUND == ret){
                //segments not found
                return CloudReturnCodes.ERROR_RECORDS_NOT_FOUND;
            }
            else if(ret == 1){
                //already opened. need to check segments
                PlaybackSegment segment = check_segment(m_set_position);
                if (segment == null) {
                    player.Pause();
                    Notify(CloudPlayerEvent.EOS);
                    return CloudReturnCodes.ERROR_RECORDS_NOT_FOUND;
                }
            }

            mis_setposition = false;
            ((Activity) mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    player.Pause();
                    player.setStreamPosition(m_set_position, mis_paused);
                    if(!mis_paused)
                        player.Play();
                    mis_setposition = true;
                    wakeup();
                }
            });
            int ticks = 100;
            while (!mis_setposition && 0<ticks--) {
                sleep(50);
            }

        }
        return 0;
    }

    private void print_segments(){
        ArrayList<PlaybackSegment> segments = player.getTimeline().getSegments();
        PlaybackSegment segment = player.getTimeline().getCurrentSegment();

        String ss = "=print_segments cur=";
        if(segment != null){
            ss += "owner="+segment.getOwner()+" ["+CloudHelpers.formatTime(segment.getStartTime())+", "+CloudHelpers.formatTime(segment.getStopTime())+"]; ";
        }else
            ss += "null;";

        if(segments != null){
            ss += "range= ";
            for(PlaybackSegment seg : segments){
                ss += " ["+CloudHelpers.formatTime(seg.getStartTime())+", "+CloudHelpers.formatTime(seg.getStopTime())+"]; ";
            }
        }else{
            ss += " range=null";
        }
        Log.v(ss);
    }

    private PlaybackSegment check_segment(long pos) {
        if (isLive() || pos == POSITION_LIVE) {
            return null;
        }

        PlaybackTimeline timeline = player.getTimeline();
        PlaybackSegment segment = timeline.getSegmentByTime(pos);
        if (segment == null || segment.getStartTime() > pos || segment.getStopTime() < pos) {

            //load segments [pos-1min, pos + 3min]
            CloudResponseWithMeta meta = api.getCameraRecords(mCamid, 0, 50, CloudHelpers.formatTime(pos - 1 * 60 * 1000), CloudHelpers.formatTime(pos + 3 * 60 * 1000), true);
            JSONArray ja = (meta != null) ? meta.getObjects() : null;

            if (ja != null) {
                Log.v("=check_segment playback records=" + ja.toString());
                for (int i = 0; i < ja.length(); i++) {
                    try {
                        JSONObject jr = ja.getJSONObject(i);
                        String url = jr.getString("url");
                        long start = CloudHelpers.parseUTCTime(jr.getString("start"));
                        long end = CloudHelpers.parseUTCTime(jr.getString("end"));

                        segment = new PlaybackSegment("", url, mCamid, start, end, end - start);
                        timeline.addSegment(segment);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
        segment = timeline.getSegmentByTime(pos);
        if(VERBOSE){
            print_segments();
            Log.v("<=check_segments ");
        }
        return (segment);
    }

    private void update_segments(long pos) {
        if (isLive() || pos == POSITION_LIVE) {
            return;
        }

        if(VERBOSE){
            print_segments();
        }


        PlaybackTimeline timeline = player.getTimeline();
        ArrayList<PlaybackSegment> segments = timeline.getSegments();

        //remove obsolete
        if(segments != null) {
            int cnt = segments.size();
            int i = 0;
            while( i < cnt ){
                PlaybackSegment segment = segments.get(i);
                if (segment.getStopTime() < (pos - 2 * 60 * 1000) && (segment.getOwner() == null) ) {
                    timeline.removeSegment(segment);
                    cnt = segments.size();
                    continue;
                }
                i++;
            }
        }
        segments = timeline.getSegments();
        long pos_end = (segments != null && segments.size() > 0) ? segments.get(segments.size() - 1).getStopTime() : 0L;

        if (pos_end == 0L || ((pos + 3 * 60 * 1000) - pos_end) < (2 * 60 * 1000)) {

            //load segments pos + 5min
            CloudResponseWithMeta meta = api.getCameraRecords(mCamid, 0, 50, CloudHelpers.formatTime(pos_end + 1), CloudHelpers.formatTime((pos + 3 * 60 * 1000)), true);
            JSONArray ja = (meta != null) ? meta.getObjects() : null;

            if (ja != null) {
                Log.v("=update_segments playback records=" + ja.toString());
                for (int i = 0; i < ja.length(); i++) {
                    try {
                        JSONObject jr = ja.getJSONObject(i);
                        String url = jr.getString("url");
                        long start = CloudHelpers.parseUTCTime(jr.getString("start"));
                        long end = CloudHelpers.parseUTCTime(jr.getString("end"));

                        PlaybackSegment seg = new PlaybackSegment("", url, mCamid, start, end, end - start);
                        timeline.addSegment(seg);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if(VERBOSE){
            print_segments();
            Log.v("<=update_segments ");
        }

    }
}