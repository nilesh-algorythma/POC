package com.vxg.cloudsdk.Objects.Playback;


import java.nio.ByteBuffer;

import veg.mediaplayer.sdk.MediaPlayer;
import veg.mediaplayer.sdk.MediaPlayer.PlayerNotifyCodes;
import veg.mediaplayer.sdk.MediaPlayerConfig;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.vxg.cloudsdk.Helpers.MLog;

public class Player implements MediaPlayer.MediaPlayerCallback, SurfaceHolder.Callback2
{
	private static final String TAG = "InternalPlayer";
	final public static int LOG_LEVEL = 2; //Log.VERBOSE;
	static MLog Log = new MLog(TAG, LOG_LEVEL);

	private Context		   context = null;
	private PlayerCallback callback = null;
    private MediaPlayer    player = null;
	private MediaPlayerConfig  config = null;
    private SurfaceView    surface = null;
    private int    		   id = 0;

	private PlaybackSegment segment = null;
    private Player		   mthis = null;

	private int mOldMsg = 0;
    private boolean isEOSoccuared = false;

    public enum PlayerType
    {
        None,
        Main,
        MainButNeedCloseLater,
        Standby,
    };

    public enum PlayerState
    {
        Closed,

        Opening,
        Working,
        Closing
    };

    private PlayerState state = PlayerState.Closed;
    private PlayerType type = PlayerType.None;

	public interface PlayerCallback
	{
		void ConnectStarted(final Player player);
		void ConnectFailed(final Player player);
		void PlaybackStarted(final Player player);
		void PlaybackFinished(final Player player);
		void PlaybackPaused(final Player player);
		void PlaybackSeekCompleted(final Player player);
		void FirstVideoFrameAvailable(final Player player);
		void TrialVersion(final Player player);
		void PlaybackNextSegment(final Player player);
	}

	public Player(final Context context, final MediaPlayer player, final SurfaceView surface, final MediaPlayerConfig config, final PlayerCallback callback, final int id)
	{
		this.context = context;
		this.callback = callback;
		this.player = player;
		this.config = config;
		this.surface = surface;
		
		if (this.surface != null)
			this.surface.getHolder().addCallback(this);

        if (this.config == null)
            this.config = new MediaPlayerConfig();

		//player.getSurfaceView().setZOrderOnTop(true);
//		SurfaceHolder sfhTrackHolder = player.getSurfaceView().getHolder();
//		sfhTrackHolder.setFormat(PixelFormat.TRANSPARENT);

        player.setVisibility(View.INVISIBLE);
		this.id = id;

        this.mthis = this;
	}
	
	public void setSurfaceView(final SurfaceView surface)
	{
		this.surface = surface;
		if (this.surface != null)
			this.surface.getHolder().addCallback(this);
	}
	
	public void Open(final PlaybackSegment segment, final Long offset, boolean is_paused)
	{
		if (player == null || segment == null)
			return;

		this.segment = segment;

		final String url = segment.getUrl();
		if (url == null || url.isEmpty())
			return;

		Log.d("=>Open player " + url+" this="+this);

        state = PlayerState.Opening;
        type = PlayerType.Main;

    	//MediaPlayerConfig conf = new MediaPlayerConfig();
        config.setConnectionUrl(url);

        config.setConnectionNetworkProtocol(1);
        config.setConnectionDetectionTime(5000);
        config.setConnectionBufferingTime(500);

        //config.setAspectRatioMode(1); // from external config

        config.setDecodingType(1);
        config.setRendererType(1);
        config.setSynchroEnable(1);
        config.setSynchroNeedDropVideoFrames(0);
        config.setEnableColorVideo(1);
        config.setDataReceiveTimeout(30000);
        config.setNumberOfCPUCores(0);
        config.setStartPreroll(is_paused?1:0);
        config.setStartOffest(offset);

        if (surface != null)
        	player.setSurface(surface.getHolder().getSurface());
        
        player.Open(config, this);
        segment.setOwner(this);
	}

	public void StandbyOpen(final PlaybackSegment segment)
	{
		if (player == null || segment == null)
			return;

		this.segment = segment;

		final String url = segment.getUrl();
		if (url == null || url.isEmpty())
			return;

		Log.d("=>Standby Open player " + url+" this="+this);

        state = PlayerState.Opening;
        type = PlayerType.Standby;

		//MediaPlayerConfig conf = new MediaPlayerConfig();
        config.setConnectionUrl(url);

        config.setConnectionNetworkProtocol(-1);
        config.setConnectionDetectionTime(2000);
        config.setConnectionBufferingTime(500);

		//config.setAspectRatioMode(1); // from external config

        config.setDecodingType(1);
        config.setRendererType(1);
        config.setSynchroEnable(1);
        config.setSynchroNeedDropVideoFrames(0);
        config.setEnableColorVideo(1);
        config.setDataReceiveTimeout(30000);
        config.setNumberOfCPUCores(0);
        config.setStartPreroll(1);

		if (surface != null)
			player.setSurface(surface.getHolder().getSurface());

		player.Open(config, this);
	    segment.setOwner(this);
	}

	public void Close() 
	{
		if (player == null ||  state == PlayerState.Closing)
			return;

        Log.v("Close " + this);

        state = PlayerState.Closing;

		//if (player.getState() != MediaPlayer.PlayerState.Closing)
			player.Close();

        type = PlayerType.None;
        state = PlayerState.Closed;
        isEOSoccuared = false;
	}

	public void mute(boolean bMute){
		player.toggleMute(bMute);
	}


	public void PlayStandby()
    {
        Log.v("Own PlayStandby 1" + this);
        if (player == null)
            return;

        Log.v("Own PlayStandby 2" + this);
        player.getConfig().setStartPreroll(0);
        type = PlayerType.Main;

		if (player.getState() == MediaPlayer.PlayerState.Paused)
        	player.Play();
    }

	public void PauseAndCloseLater()
	{
		if (player == null)
			return;

        Log.v("Own PauseAndCloseLater " + this);
        type = PlayerType.MainButNeedCloseLater;

		if (player.getState() == MediaPlayer.PlayerState.Started)
	        player.Pause();
	}

	public MediaPlayer getMediaPlayer()
	{
		return player;
	}
	public PlaybackSegment getPlaybackSegment()
	{
		return segment;
	}
	public MediaPlayer.PlayerState getMediaPlayerState()
	{
		return (player == null) ? MediaPlayer.PlayerState.Closed : player.getState();
	}

    public PlayerState getState() { return state; }
    public PlayerType getType()
    {
        return type;
    }

    public void updateConfig(final MediaPlayerConfig config)
    {
        if (config == null)
            return;

        this.config = config;

        player.getConfig().setAspectRatioMode(config.getAspectRatioMode());
        player.getConfig().setAspectRatioZoomModePercent(config.getAspectRatioZoomModePercent());
        player.UpdateView();
    }

	public long getStreamPosition()
	{
		if (player == null)
			return 0L;

		MediaPlayer.Position pos = player.getLiveStreamPosition();
		if(pos == null)
			return 0L;

		return pos.getCurrent();
	}

	public void setStreamPosition(long newPosition)
	{
		if (player == null)
			return;

        if(newPosition < 0L)
            newPosition = 0L;
		player.setStreamPosition(newPosition);
	}

    public void show()
    {
        if (player == null)
            return;

        Log.d("show: type " + type + ", pointer " + this);
        //if (player.getVisibility() != View.VISIBLE)
        {
            //player.getSurfaceView().setZOrderOnTop(true);
            player.setVisibility(View.VISIBLE);
            //player.invalidate();
            //player.requestLayout();
        }
    }

    public void hide()
    {
        if (player == null)
            return;

        Log.d("hide: type " + type + ", pointer " + this);
        //if (player.getVisibility() != View.INVISIBLE)
        {
            //player.getSurfaceView().setZOrderOnTop(false);
            player.setVisibility(View.INVISIBLE);
            //player.invalidate();
            //player.requestLayout();
        }
    }

	private Handler handler = new Handler()
    {
		@Override
	    public void handleMessage(Message msg) 
	    {
	    	PlayerNotifyCodes status = (PlayerNotifyCodes) msg.obj;
	        switch (status) 
	        {
//	        	case CP_CONNECT_STARTING:
//	    			break;

//                case PLP_PLAY_SUCCESSFUL:
//                    state = PlayerState.Working;
//                    break;

	        	case PLP_CLOSE_STARTING:
	    			break;
	                
	        	case PLP_CLOSE_SUCCESSFUL:
	    			System.gc();
	                break;
	                
	        	case PLP_CLOSE_FAILED:
	        		break;
	               
//	        	case CP_CONNECT_FAILED:
//                    if (callback != null)
//                        callback.PlaybackFinished(mthis, true);
//	    			break;
	                
	            case PLP_BUILD_FAILED:
//                    if (callback != null)
//                        callback.PlaybackFinished(mthis, true);
	    			break;
	                
	            case PLP_PLAY_FAILED:
//                    if (callback != null)
//                        callback.PlaybackFinished(mthis, true);
	    			break;
	                
	            case PLP_ERROR:
	            {
//                    if (callback != null)
//                        callback.PlaybackFinished(mthis, true);
	            	break;
	            }

	            case CP_INTERRUPTED:
	    			break;
	                
	            //case CONTENT_PROVIDER_ERROR_DISCONNECTED:
	            case CP_STOPPED:
	            case VDP_STOPPED:
	            case VRP_STOPPED:
	            case ADP_STOPPED:
	            case ARP_STOPPED:
	                break;
	
	            case CP_ERROR_DISCONNECTED:
	                break;
	                
//	            case VDP_CRASH:
//	            {
//                    if (callback != null)
//                        callback.PlaybackFinished(mthis, true);
//
//	            	break;
//	            }
	                
	            default:
	        }
	    }
	};

	// callback from Native Player 
	@Override
	public int OnReceiveData(ByteBuffer buffer, int size, long pts) 
	{
		Log.d("Form Native Player OnReceiveData: size: " + size + ", pts: " + pts);
		return 0;
	}

	@Override
	public int Status(int arg)
	{
		
		PlayerNotifyCodes status = PlayerNotifyCodes.forValue(arg);
		if (handler == null)
			return 0;
		
		Log.d("Form Native Player: " + this + " status: " + status + ", type " + getType() + ", state " + getState() + ", mediaplayer_state " + getMediaPlayerState());
	    switch (status)
	    {
            case CP_CONNECT_STARTING:
                if (callback != null)
                    callback.ConnectStarted(mthis);
                break;

            case CP_CONNECT_FAILED:
                if (callback != null && getState() != Player.PlayerState.Closing)
                    callback.ConnectFailed(mthis);
                break;

            case PLP_PLAY_SUCCESSFUL:
                state = PlayerState.Working;
                if (callback != null)
                    callback.PlaybackStarted(mthis);
                break;

			case PLP_PLAY_PAUSE:
				if (callback != null)
					callback.PlaybackPaused(mthis);
				break;
			case PLP_SEEK_COMPLETED:
				if (callback != null)
					callback.PlaybackSeekCompleted(mthis);
				break;

			case VRP_FIRSTFRAME:
                state = Player.PlayerState.Working;
	    		if (callback != null && (type == PlayerType.Main || type == PlayerType.MainButNeedCloseLater) && getState() != Player.PlayerState.Closing)
	    			callback.FirstVideoFrameAvailable(this);
				break;

            case VRP_LASTFRAME:
                if (callback != null && getState() != Player.PlayerState.Closing)
                {
                    callback.PlaybackFinished(mthis);
                }
                break;
			case PLP_TRIAL_VERSION:
				if (callback != null && getState() != Player.PlayerState.Closing)
				{
					callback.TrialVersion(mthis);
				}
				break;

			// for asynchronous process
	        default:
				Message msg = new Message();
				msg.obj = status;
				handler.removeMessages(mOldMsg);
				mOldMsg = msg.what;
				handler.sendMessage(msg);
	    }
	    
		return 0;
	}
    
	@Override
	public void surfaceCreated(SurfaceHolder holder) 
	{
		if (player == null)
			return;

		player.setSurface(holder.getSurface());
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) 
	{
		if (player == null)
			return;

		player.setSurface(holder.getSurface(), width, height);
		player.UpdateView();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) 
	{
		if (player == null)
			return;

		player.setSurface(null);
	}

	@Override
	public void surfaceRedrawNeeded(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		
	}
    
}

