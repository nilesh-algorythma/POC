package com.vxg.cloudsdk.Objects.Playback;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.vxg.cloudsdk.Helpers.MLog;

import java.nio.ByteBuffer;

import veg.mediaplayer.sdk.MediaPlayer;
import veg.mediaplayer.sdk.MediaPlayerConfig;

public class PlaybackPlayer implements Player.PlayerCallback {

    private static final String TAG = "PlaybackPlayer";
    final public static int LOG_LEVEL = 2; //Log.VERBOSE;
    static MLog Log = new MLog(TAG, LOG_LEVEL);


    private Context context = null;
    private Player.PlayerCallback callback = null;

    private PlaybackTimeline timeline = null;
    private PlayerManager playerManager = null;

    private PlaybackPlayer() {
    }

    public PlaybackPlayer(final Context context, MediaPlayer player1, MediaPlayer player2, final MediaPlayerConfig commonPlayerConfig, final Player.PlayerCallback callback) {
        this.context = context;
        this.callback = callback;
        this.timeline = new PlaybackTimeline(context);
        this.playerManager = new PlayerManager(context, player1, player2, commonPlayerConfig, this);
    }

    public void addForOpen(final PlaybackSegment segment) {
        timeline.addSegment(segment);
        Log.v("addForOpen: " + segment.getUrl());
    }

    public void open(boolean is_paused) {
        PlaybackSegment cur_segment = timeline.getNextSegment(null, false);
        if (cur_segment == null)
            return;

        if (playerManager.getFirstStandbyPlayer() != null)
            playerManager.getFirstStandbyPlayer().Close();

        if (playerManager.getFreePlayer() == null)
            return;

        Log.v("open: " + cur_segment.getUrl());
        playerManager.getFreePlayer().Open(cur_segment, 0L, is_paused);
        timeline.setCurrentSegment(cur_segment);

        PlaybackSegment nextSegment = timeline.getNextSegment(timeline.getCurrentSegment(), false);
        if (nextSegment != null) {
            Log.v("open: as standby " + nextSegment.getUrl());
            if (playerManager.getFreePlayer() != null)
                playerManager.getFreePlayer().StandbyOpen(nextSegment);
        }
    }

    public void close() {
        playerManager.closeAll();
        timeline.clearAll();
    }

    public void mute(boolean bMute) {
        playerManager.mute(bMute);
    }

    public void Play() {
        final Player player = playerManager.getMainPlayer();
        if (player == null)
            return;

        player.getMediaPlayer().Play();
    }

    public void Pause() {
        final Player player = playerManager.getMainPlayer();
        if (player == null)
            return;

        player.getMediaPlayer().Pause();
    }

    public void PauseFlush() {
        final Player player = playerManager.getMainPlayer();
        if (player == null)
            return;

        player.getMediaPlayer().PauseFlush();
    }

    public void setStreamPosition(final long newPosition, boolean is_paused) {
        final Player player = playerManager.getMainPlayer();
        if (player == null)
            return;

        PlaybackSegment newSegment = getTimeline().getSegmentByTime(newPosition);
        if (newSegment == null)
            return;

        if (newSegment == player.getPlaybackSegment()) {
            getTimeline().setCurrentTime(newPosition - newSegment.getStartTime());
            return;
        }

        change(newSegment, newPosition - newSegment.getStartTime(), is_paused);
    }

    public PlaybackTimeline getTimeline() {
        return timeline;
    }

    public MediaPlayer.PlayerState getPlayerState() {
        final Player player = playerManager.getMainPlayer();
        if (player == null)
            return MediaPlayer.PlayerState.Closed;

        return player.getMediaPlayer().getState();
    }

    public void updateConfig(final MediaPlayerConfig config) {
        playerManager.updateConfig(config);
    }

    private void change(final PlaybackSegment segment, final Long offset, boolean is_paused) {
        if (playerManager.getMainPlayerForClose() != null && playerManager.getMainPlayer() != null)
            playerManager.getMainPlayer().Close();

        if (playerManager.getMainPlayer() != null) {
            playerManager.getMainPlayer().PauseAndCloseLater();
        }

        if (playerManager.getFirstStandbyPlayer() != null)
            playerManager.getFirstStandbyPlayer().Close();

        //timeline.setCurrentSegment(segment);

        Player nextPlayer = playerManager.getFreePlayer();
        if (nextPlayer == null)
            return;

        Log.v("change: " + segment.getUrl());
        nextPlayer.Open(segment, offset, is_paused);
        timeline.setCurrentSegment(nextPlayer.getPlaybackSegment());
        //playerManager.bringToFront(nextPlayer);
    }


    @Override
    public void ConnectStarted(final Player player) {
        Log.v("ConnectStarted:  " + player + ", type " + player.getType() + ", state " + player.getState() + ", mediaplayer_state " + player.getMediaPlayerState());
        if (callback != null && player.getType() == Player.PlayerType.Main/* && playerManager.getMainPlayerForClose() == null*/)
            callback.ConnectStarted(player);
    }

    @Override
    public void ConnectFailed(final Player player) {
        Log.v("ConnectFailed:  " + player + ", type " + player.getType() + ", state " + player.getState() + ", mediaplayer_state " + player.getMediaPlayerState());
        if (callback != null && player.getType() == Player.PlayerType.Main && playerManager.getMainPlayerForClose() == null) {
//            ((Activity) context).runOnUiThread(new Runnable()
//            {
//                @Override
//                public void run()
//                {
//                    boolean ret = startNextSegmentAsMain(player);
//                    if (!ret && callback != null)
//                        callback.ConnectFailed();
//                }
//            });
            final Player nextPlayer = playerManager.getFirstStandbyPlayer();
            Log.v("ConnectFailed:  next standby " + nextPlayer);
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (nextPlayer == null || nextPlayer.getState() == Player.PlayerState.Closed) {
                        Log.v("ConnectFailed: failed next standby " + nextPlayer);
                        if (callback != null) {
                            Log.v("ConnectFailed: exit nextPlayer == null || nextPlayer.getState() == Player.PlayerState.Closed " + nextPlayer);
                            callback.ConnectFailed(player);
                        }

                        return;
                    }

                    //if (nextPlayer.getState() != Player.PlayerState.Working)
                    //{
                    //    return;
                    //}

                    Log.v("ConnectFailed: next standby go main state and play " + nextPlayer);
                    nextPlayer.PlayStandby();

                    Log.v("ConnectFailed:  show new main " + nextPlayer);
                    //nextPlayer.show();
                    //player.hide();
                    playerManager.bringToFront(nextPlayer);

                    timeline.setCurrentSegment(nextPlayer.getPlaybackSegment());

                    //Log.v(TAG, "PlaybackFinished: try open next segment as standby " + nextPlayer);
                    startNextSegmentAsStandby(player, nextPlayer.getPlaybackSegment());
                }
            });
            return;
        }

        if (player.getType() == Player.PlayerType.Main && playerManager.getMainPlayerForClose() != null) {
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    boolean ret = startNextSegmentAsMain(player);
                    if (!ret && callback != null) {
                        Log.v("ConnectFailed: exit player.getType() == Player.PlayerType.Main && playerManager.getMainPlayerForClose() != null " + player);
                        callback.ConnectFailed(player);
                    }
                }
            });

            return;
        }

        if (player.getType() == Player.PlayerType.Standby) {
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    boolean ret = startNextSegmentAsStandby(player);
                    if (!ret && callback != null) {
                        Log.v("ConnectFailed: no exit player.getType() == Player.PlayerType.Standby " + player);
//                        callback.ConnectFailed(player);
                    }
                }
            });

            return;
        }
    }

    @Override
    public void PlaybackStarted(final Player player) {
        Log.v("PlaybackStarted:  " + player + ", type " + player.getType() + ", state " + player.getState() + ", mediaplayer_state " + player.getMediaPlayerState());
        if (callback != null)
            callback.PlaybackStarted(player);
    }

    @Override
    public void PlaybackFinished(final Player player) {
        Log.v("PlaybackFinished:  " + player + ", type " + player.getType() + ", state " + player.getState() + ", mediaplayer_state " + player.getMediaPlayerState());
        //if(true)
        //    return;

        // if no segments available - close and exit
        PlaybackSegment nextSegment = timeline.getNextSegment(timeline.getCurrentSegment(), false);
        if (nextSegment == null) {
            Log.v("PlaybackFinished:  " + "Last segment detected. Exit.");
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //player.Close();
                    if (callback != null)
                        callback.PlaybackFinished(player);

                    if (playerManager.getMainPlayer() != null) {
                        playerManager.getMainPlayer().PauseAndCloseLater();
                    }

                }
            });

            return;
        } else {
            if (callback != null)
                callback.PlaybackNextSegment(player);
        }

        if (player.getType() == Player.PlayerType.Main) {
            final Player nextPlayer = playerManager.getFirstStandbyPlayer();
            Log.v("PlaybackFinished:  next standby " + nextPlayer);
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (nextPlayer == null || nextPlayer.getState() == Player.PlayerState.Closed) {
                        Log.v("PlaybackFinished: exit next standby " + nextPlayer);
                        if (callback != null)
                            callback.PlaybackFinished(player);

                        if (playerManager.getMainPlayer() != null) {
                            playerManager.getMainPlayer().PauseAndCloseLater();
                        }

                        return;
                    }

                    //if (nextPlayer.getState() != Player.PlayerState.Working)
                    //{
                    //    return;
                    //}

                    Log.v("PlaybackFinished: next standby go main state and play " + nextPlayer);
                    nextPlayer.PlayStandby();

                    Log.v("PlaybackFinished:  show new main " + nextPlayer);
                    //nextPlayer.show();
                    //player.hide();
                    playerManager.bringToFront(nextPlayer);

                    timeline.setCurrentSegment(nextPlayer.getPlaybackSegment());

                    //Log.v(TAG, "PlaybackFinished: try open next segment as standby " + nextPlayer);
                    startNextSegmentAsStandby(player, nextPlayer.getPlaybackSegment());
                }
            });
        }
    }

    @Override
    public void PlaybackPaused(Player player) {
        if (callback != null && player.getType() == Player.PlayerType.Main)
            callback.PlaybackPaused(player);
    }

    @Override
    public void PlaybackSeekCompleted(Player player) {
        if (callback != null && player.getType() == Player.PlayerType.Main)
            callback.PlaybackSeekCompleted(player);
    }

    @Override
    public void FirstVideoFrameAvailable(final Player player) {
        Log.v("FirstVideoFrameAvailable:  " + player + ", type " + player.getType() + ", state " + player.getState() + ", mediaplayer_state " + player.getMediaPlayerState());

        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //if (player.getMediaPlayerState() == MediaPlayer.PlayerState.Paused)
                //    player.getMediaPlayer().Play();
                if (callback != null)
                    callback.FirstVideoFrameAvailable(player);


                playerManager.bringToFront(player);

                if (playerManager.getMainPlayerForClose() != null) {
                    playerManager.getMainPlayerForClose().Close();
                    startNextSegmentAsStandby(player.getPlaybackSegment());
                }
            }
        });

    }

    @Override
    public void TrialVersion(Player player) {
        if (callback != null)
            callback.TrialVersion(player);
    }

    @Override
    public void PlaybackNextSegment(Player player) {
        if (callback != null)
            callback.PlaybackNextSegment(player);
    }

    private boolean startNextSegmentAsMain(final Player player) {
        if (player == null) {
            Log.v("startNextSegmentAsMain:: failed player " + player);
            return false;
        }

        player.Close();

        if (player.getPlaybackSegment() == null) {
            Log.v("startNextSegmentAsMain:: failed player segment " + player.getPlaybackSegment());
            return false;
        }

        PlaybackSegment nextSegment = timeline.getNextSegment(player.getPlaybackSegment(), false);
        if (nextSegment == null) {
            Log.v("startNextSegmentAsMain:: failed next segment " + nextSegment);
            return false;
        }

        Log.v("startOpennextSegmentAsMain:: new as main " + nextSegment.getUrl());
        playerManager.getFreePlayer().Open(nextSegment, 0L, false);
        return true;
    }

    private boolean startNextSegmentAsStandby(final Player player) {
        if (player == null) {
            Log.v("startNextSegmentAsStandby:: failed player " + player);
            return false;
        }

        player.Close();

        if (player.getPlaybackSegment() == null) {
            Log.v("startNextSegmentAsStandby:: failed player segment " + player.getPlaybackSegment());
            return false;
        }

        PlaybackSegment nextSegment = timeline.getNextSegment(player.getPlaybackSegment(), false);
        if (nextSegment == null) {
            Log.v("startNextSegmentAsStandby:: failed next segment " + nextSegment);
            return false;
        }

        Log.v("startNextSegmentAsStandby:: new as standby " + nextSegment.getUrl());
        playerManager.getFreePlayer().StandbyOpen(nextSegment);
        return true;
    }

    private boolean startNextSegmentAsStandby(final Player player, final PlaybackSegment current) {
        if (player == null)
            return false;

        Log.v("startNextSegmentAsStandby: player Close " + player);
        player.Close();

        if (current == null)
            return false;

        PlaybackSegment nextSegment = timeline.getNextSegment(current, false);
        if (nextSegment == null) {
            return false;
        }

        Log.v("startNextSegmentAsStandby: new as standby " + playerManager.getFreePlayer());
        playerManager.getFreePlayer().StandbyOpen(nextSegment);
        Log.v("startNextSegmentAsStandby: new as standby url " + nextSegment.getUrl());
        return true;
    }

    private boolean startNextSegmentAsStandby(final PlaybackSegment current) {
        if (current == null)
            return false;

        PlaybackSegment nextSegment = timeline.getNextSegment(current, false);
        if (nextSegment == null || playerManager.getFreePlayer() == null) {
            return false;
        }

        Log.v("startNextSegmentAsStandby: new as standby " + playerManager.getFreePlayer());
        playerManager.getFreePlayer().StandbyOpen(nextSegment);
        Log.v("startNextSegmentAsStandby: new as standby url " + nextSegment.getUrl());
        return true;
    }

    public MediaPlayer.VideoShot getVideoShot(int width, int height) {
        final Player player = playerManager.getMainPlayer();
        if (player == null)
            return null;

        return player.getMediaPlayer().getVideoShot(width, height);
    }
}
