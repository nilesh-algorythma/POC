package com.vxg.cloudsdk.Objects.Playback;


import android.app.Activity;
import android.content.Context;
import android.view.View;

import java.util.ArrayList;

import veg.mediaplayer.sdk.MediaPlayer;
import veg.mediaplayer.sdk.MediaPlayerConfig;

import android.view.ViewGroup;

import com.vxg.cloudsdk.Helpers.MLog;

public class PlayerManager  implements Player.PlayerCallback
{
    private static final String TAG = "PlayerManager";
    final public static int LOG_LEVEL = 2; //Log.VERBOSE;
    static MLog Log = new MLog(TAG, LOG_LEVEL);

    private Context context = null;
    private Player.PlayerCallback callback = null;

    private ArrayList<Player> players = new ArrayList<>();

    private PlayerManager() {}
    public PlayerManager(final Context context, MediaPlayer player1, MediaPlayer player2, final MediaPlayerConfig commonPlayerConfig, final Player.PlayerCallback callback)
    {
        this.context = context;
        this.callback = callback;

        this.players.add(new Player(context, player1, null, commonPlayerConfig, this, 0));
        this.players.add(new Player(context, player2, null, commonPlayerConfig, this, 1));
    }

    public Player getFreePlayer()
    {
        for (int i = 0; i < players.size(); i++)
        {
            if (players.get(i).getType() == Player.PlayerType.None)
                return players.get(i);
        }

        return null;
    }

    public Player getMainPlayer()
    {
        for (int i = 0; i < players.size(); i++)
        {
            if (players.get(i).getType() == Player.PlayerType.Main)
                return players.get(i);
        }

        return null;
    }

    public Player getMainPlayerForClose()
    {
        for (int i = 0; i < players.size(); i++)
        {
            if (players.get(i).getType() == Player.PlayerType.MainButNeedCloseLater)
                return players.get(i);
        }

        return null;
    }

    public Player getFirstStandbyPlayer()
    {
        for (int i = 0; i < players.size(); i++)
        {
            if (players.get(i).getType() == Player.PlayerType.Standby)
                return players.get(i);
        }

        return null;
    }

    public void updateConfig(final MediaPlayerConfig config) {
        for (int i = 0; i < players.size(); i++)
            players.get(i).updateConfig(config);
    }

    public void bringToFront(final Player player)
    {
        if (player == null)
            return;

        // firstly show
        for (int i = 0; i < players.size(); i++)
        {
            if (player == players.get(i))
            {
                players.get(i).show();
                break;
            }
        }

        // hide others
        for (int i = 0; i < players.size(); i++)
        {
            if (player != players.get(i))
            {
                players.get(i).hide();
            }
        }
    }

    private void moveToBack(View myCurrentView)
    {
        ViewGroup myViewGroup = ((ViewGroup) myCurrentView.getParent());
        int index = myViewGroup.indexOfChild(myCurrentView);
        for(int i = 0; i<index; i++)
        {
            myViewGroup.bringChildToFront(myViewGroup.getChildAt(i));
        }
    }
    public void bringToFront2(final Player player)
    {
        if (player == null)
            return;

        // firstly show
        // hide others
        for (int i = 0; i < players.size(); i++)
        {
            if (player != players.get(i))
            {
                moveToBack(players.get(i).getMediaPlayer());
                //players.get(i).hide();
            }
        }
    }


    public void closeAll()
    {
        for (int i = 0; i < players.size(); i++)
            players.get(i).Close();
    }

    public void mute(boolean bMute){
        for (int i = 0; i < players.size(); i++)
        {
            Player p = players.get(i);
            if(p == null)
                continue;
            p.mute(bMute);
        }

    }

    @Override
     public void ConnectStarted(final Player player)
    {
        Log.v("ConnectStarted:  " + player);
        if (callback != null)
            callback.ConnectStarted(player);
    }

    @Override
     public void ConnectFailed(final Player player)
    {
        Log.v("ConnectFailed:  " + player);
        if (callback != null)
            callback.ConnectFailed(player);

    }

    @Override
     public void PlaybackStarted(final Player player)
    {
        Log.v("PlaybackStarted:  " + player);
        if (callback != null)
            callback.PlaybackStarted(player);

    }

    @Override
     public void PlaybackFinished(final Player player)
    {
        Log.v("PlaybackFinished:  " + player);
        if (callback != null)
            callback.PlaybackFinished(player);

    }
    @Override
    public void PlaybackPaused(final Player player)
    {
        Log.v("PlaybackPaused:  " + player);
        if (callback != null)
            callback.PlaybackPaused(player);

    }

    @Override
    public void PlaybackSeekCompleted(final Player player)
    {
        Log.v("PlaybackSeekCompleted:  " + player);
        if (callback != null)
            callback.PlaybackSeekCompleted(player);

    }

    @Override
     public void FirstVideoFrameAvailable(final Player player)
    {
        Log.v("FirstVideoFrameAvailable:  " + player);
        if (callback != null)
            callback.FirstVideoFrameAvailable(player);

//        if (player.getPlayerType() != Player.PlayerType.Main)
//            return;

//        ((Activity) context).runOnUiThread(new Runnable()
//        {
//            @Override
//            public void run()
//            {
//
//                Log.v(TAG, "FirstVideoFrameAvailable: setZOrderOnTop: will " + (player.getPlayerType() == Player.PlayerType.Main) + " for " + player.getMediaPlayer());
//                show(player);
//
//                if (player.getPlayerType() == Player.PlayerType.Main)
//                    Log.v(TAG, "now Visible url: " + player.getMediaPlayer().getConfig().getConnectionUrl());
//
//                Player playerNeedForClose = getMainPlayerForClose();
//                Log.v(TAG, "FirstVideoFrameAvailable: 1 close old after change pos between segments " + playerNeedForClose);
//
//            }
//        });

    }

    @Override
    public void TrialVersion(Player player) {
        Log.v("TrialVersion:  " + player);
        if (callback != null)
            callback.TrialVersion(player);
    }

    @Override
    public void PlaybackNextSegment(Player player) {

    }

}
