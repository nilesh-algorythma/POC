/*
 *
  * Copyright (c) 2011-2017 VXG Inc.
 *
 */


package com.vxg.cloudsdk.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.view.inputmethod.InputMethodManager;
import android.view.WindowManager;
import android.content.SharedPreferences;
import android.view.*;
import android.widget.RelativeLayout;
import android.widget.TextView.OnEditorActionListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.TimeZone;

import android.preference.PreferenceManager;

import com.vxg.cloud.core.CloudHelpers;
import com.vxg.cloudsdk.CloudSDK;
import com.vxg.cloudsdk.Enums.CloudCameraRecordingMode;
import com.vxg.cloudsdk.Enums.CloudCameraStatus;
import com.vxg.cloudsdk.Enums.CloudPlayerEvent;
import com.vxg.cloudsdk.Enums.CloudPlayerState;
import com.vxg.cloudsdk.Enums.CloudReturnCodes;
import com.vxg.cloudsdk.Enums.PS_Privacy;
import com.vxg.cloudsdk.Interfaces.ICloudConnection;
import com.vxg.cloudsdk.Interfaces.ICloudObject;
import com.vxg.cloudsdk.Interfaces.ICloudPlayerCallback;
import com.vxg.cloudsdk.Interfaces.ICloudStreamerCallback;
import com.vxg.cloudsdk.Interfaces.ICompletionCallback;
import com.vxg.cloudsdk.Objects.CloudCamera;
import com.vxg.cloudsdk.Objects.CloudCameraList;
import com.vxg.cloudsdk.Objects.CloudCameraListFilter;
import com.vxg.cloudsdk.Objects.CloudPlayer;
import com.vxg.cloudsdk.Objects.CloudPlayerConfig;
import com.vxg.cloudsdk.Objects.CloudStreamer;
import com.vxg.cloudsdk.Objects.CloudTimeline;
import com.vxg.cloudsdk.Objects.CloudTrialConnection;
import com.vxg.cloudsdk.Objects.CloudUserInfo;
import com.vxg.ui.TimeLineSet;


public class MainActivity extends Activity implements OnClickListener
{
    private static final String TAG 	 = "VXGCloudPlayer";
    

	public  static AutoCompleteTextView	edtIpAddress;
	public  static ArrayAdapter<String> edtIpAddressAdapter;
	public  static Set<String>			edtIpAddressHistory;
	private Button						btnConnect;
	private Button						btnHistory;

	private SharedPreferences 			settings;

    private boolean 					playing = false;
	private boolean						paused = false;
    private MainActivity 				mthis = null;
	private FrameLayout					player_view;
	private CloudPlayer 				player;
	private CloudCamera 				camera;
	private CloudStreamer				cstreamer;

    private RelativeLayout 				playerStatus = null;
    private TextView 					playerStatusText = null;

	private RelativeLayout				playerLayoutControls = null;
	private Button 						btnRecord = null;
	private Button						btn_back = null;
	private Button						btn_fwd = null;
	private Button						btn_play_pause = null;
	private Button						btn_live_playback = null;
	private RelativeLayout				playerLayoutTime = null;
	private TimeLineSet timeLineSet = null;
	private TextView 					textTimeText = null;
	private PlayerPanelControlVisibleTask 	playerPanelControlTask 	= null;
	private ProgressBar					player_progress;

	private MulticastLock multicastLock = null;

    
	CloudSDK         mCloudSDK;
	ICloudConnection mConnection;

	//SET LICENCE KEY
	String msKeyConnection = "";
	

    @Override
    public void onCreate(Bundle savedInstanceState) 
	{
		String  strUrl;

		setTitle(R.string.app_name);
		super.onCreate(savedInstanceState);

		WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		multicastLock = wifi.createMulticastLock("multicastLock");
		multicastLock.setReferenceCounted(true);
		multicastLock.acquire();
		
		getWindow().requestFeature(Window.FEATURE_PROGRESS);
		getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);
		
		setContentView(R.layout.main);
		mthis = this;
		
		settings = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

		CloudSDK.setContext(this);
		CloudSDK.setLogEnable(true);
		CloudSDK.setLogLevel(2);
		Toast.makeText(getApplicationContext(), "CloudSDK ver="+CloudSDK.getLibVersion(), Toast.LENGTH_LONG).show();
		
		playerStatus 		= (RelativeLayout)findViewById(R.id.playerStatus);
		playerStatusText 	= (TextView)findViewById(R.id.playerStatusText);

		player_view = (FrameLayout) findViewById(R.id.playerView);
		player = new CloudPlayer(player_view, new CloudPlayerConfig(), new ICloudPlayerCallback(){

			@Override
			public void onStatus(final CloudPlayerEvent player_event, final ICloudObject p) {
				Log.v(TAG, "=CloudPlayer onStatus player_event="+player_event);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						switch(player_event){
							case CONNECTING:
								playerStatusText.setVisibility(View.VISIBLE);
								playerStatusText.setText("Connecting...");
								playing = true;
								break;
							case CONNECTED:
							case STARTED:
							case PAUSED:
								setHideControls();
								playerStatusText.setVisibility(View.INVISIBLE);
								playing = true;
								if(playerPanelControlTask == null) {
									playerPanelControlTask = new PlayerPanelControlVisibleTask();
									executeAsyncTask(playerPanelControlTask, "");
								}
								break;
							case CLOSED:
								setUIDisconnected();
								break;
							case EOS:
								playerStatusText.setText("EOS");
								playerStatusText.setVisibility(View.VISIBLE);
								player_progress.setVisibility(View.INVISIBLE);
								//setUIDisconnected();
								break;
							case SEEK_COMPLETED:
                                playerStatusText.setText("");
								playerStatusText.setVisibility(View.VISIBLE);
								player_progress.setVisibility(View.INVISIBLE);
								//timeLineSet.getTimeLine().playerSeekCompleted();
								break;
							case ERROR:
								playerStatusText.setText("Player error="+player.getResultStr());
								playerStatusText.setVisibility(View.VISIBLE);
								player_progress.setVisibility(View.INVISIBLE);
								//setUIDisconnected();
								break;
							case TRIAL_VERSION:
								playerStatusText.setText("DEMO VERSION LIMITATION");
								playerStatusText.setVisibility(View.VISIBLE);
								setUIDisconnected();
								break;
						}
					}
				});
			}
		});

		strUrl = settings.getString("connectionUrl", "rtsp://184.72.239.149/vod/mp4:BigBuckBunny_115k.mov");
		

		HashSet<String> tempHistory = new HashSet<String>();
		tempHistory.add("http://devimages.apple.com/iphone/samples/bipbop/gear1/prog_index.m3u8");
		tempHistory.add("rtmp://184.72.239.149/vod/BigBuckBunny_115k.mov");
		tempHistory.add("rtsp://184.72.239.149/vod/mp4:BigBuckBunny_115k.mov");
		tempHistory.add("rtsp://test:test@31.24.28.9:20556/onvif-media/media.amp");

		edtIpAddressHistory = settings.getStringSet("connectionHistory", tempHistory);

		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN); 
	
		edtIpAddress = (AutoCompleteTextView)findViewById(R.id.edit_ipaddress);
		edtIpAddress.setText(strUrl);

		edtIpAddress.setOnEditorActionListener(new OnEditorActionListener() 
		{
			@Override
			public boolean onEditorAction(TextView v, int actionId,	KeyEvent event) 
			{
				if (event != null&& (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) 
				{
					InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					in.hideSoftInputFromWindow(edtIpAddress.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
					return true;
	
				}
				return false;
			}
		});

		btnHistory = (Button)findViewById(R.id.button_history);
		// Array of choices
		btnHistory.setOnClickListener(new View.OnClickListener() 
		{
			public void onClick(View v) 
			{
				InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				in.hideSoftInputFromWindow(MainActivity.edtIpAddress.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
				
				if (edtIpAddressHistory.size() <= 0)
					return;

				MainActivity.edtIpAddressAdapter = new ArrayAdapter<String>(MainActivity.this, R.layout.history_item, new ArrayList<String>(edtIpAddressHistory));
				MainActivity.edtIpAddress.setAdapter(MainActivity.edtIpAddressAdapter);
				MainActivity.edtIpAddress.showDropDown();
			}   
		});

		btnConnect = (Button)findViewById(R.id.button_connect);
        btnConnect.setOnClickListener(this);
        
        RelativeLayout layout = (RelativeLayout) findViewById(R.id.main_view);
        layout.setOnTouchListener(new OnTouchListener() 
		{
			public boolean onTouch(View v, MotionEvent event) 
			{
				InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				if (getWindow() != null && getWindow().getCurrentFocus() != null && getWindow().getCurrentFocus().getWindowToken() != null)
					inputManager.hideSoftInputFromWindow(getWindow().getCurrentFocus().getWindowToken(), 0);
				return true;
			}
		});
        
		playerStatusText.setText("DEMO VERSION");

		playerLayoutControls = (RelativeLayout) findViewById(R.id.playerLayoutControls);
		btnRecord = (Button)findViewById(R.id.button_record);
		btn_back = (Button)findViewById(R.id.button_back);
		btn_fwd = (Button)findViewById(R.id.button_fwd);
		btn_play_pause = (Button)findViewById(R.id.button_play_pause);
		btn_live_playback = (Button)findViewById(R.id.button_live_playback);
		playerLayoutTime = (RelativeLayout)findViewById(R.id.playerLayoutTime);
		textTimeText = (TextView) findViewById(R.id.playerTimeText);
		player_progress = (ProgressBar) findViewById(R.id.player_progress);

		timeLineSet=(TimeLineSet)findViewById(R.id.timeLineSet);

		timeLineSet.getTimeLine().setPlayer(player);
		timeLineSet.setVisibility(View.GONE);
		timeLineSet.getTimeLine().setPlayerProgress(player_progress);



		btnRecord.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				if(camera != null){
					if(camera.getRecordingMode() == CloudCameraRecordingMode.CONTINUES) {
						camera.setRecordingMode(CloudCameraRecordingMode.NO_RECORDING);
					}else{
						camera.setRecordingMode(CloudCameraRecordingMode.CONTINUES);
					//	CloudTimeline timeline = camera.getTimelineSync(System.currentTimeMillis()-3600*1000*10, System.currentTimeMillis());

						//timeLineSet.t.loadData(timeline.periods);
						//timeLineSet.getTimeLine().setCamera(camera);

					}
					camera.save( new ICompletionCallback(){
						@Override
						public int onComplete(Object o_result, int result) {
							return 0;
						}
					});
				}
			}
		});

		btn_play_pause.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				if(player.getState() == CloudPlayerState.PAUSED){
					player.play();
				}else{
					player.pause();
				}
			}
		});
		btn_live_playback.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				long pos = player.getPosition();
				if(player.isLive()){
					player.pause();
					if(pos == 0L)
						pos = get_time_cur_ms();
					playerStatusText.setText("change pos to "+  get_time_string_s(pos));
					playerStatusText.setVisibility(View.VISIBLE);
					player_progress.setVisibility(View.VISIBLE);
					player.setPosition(pos);
				}else{
					player_progress.setVisibility(View.VISIBLE);
					player.setPosition(player.POSITION_LIVE);
					playerStatusText.setText("change pos to Live");
					playerStatusText.setVisibility(View.VISIBLE);
					player.play();
				}
			}
		});

		btn_back.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                long pos = player.getPosition();
				if(pos == 0L)
					pos = get_time_cur_ms();
                //back 1 min
				pos -= 1*60*1000;
				player_progress.setVisibility(View.VISIBLE);
				player.setPosition( pos );
				playerStatusText.setText("change pos to "+  get_time_string_s(pos));
				playerStatusText.setVisibility(View.VISIBLE);
                //player.play();
            }
        });
        btn_fwd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                long pos = player.getPosition();
				if(pos == 0L)
					pos = get_time_cur_ms();
                //forward 2 min
				pos += 1*60*1000;
				player_progress.setVisibility(View.VISIBLE);
				player.setPosition( pos );
				playerStatusText.setText("change pos to "+  get_time_string_s(pos));
				playerStatusText.setVisibility(View.VISIBLE);
                //player.play();
            }
        });

		setShowControls();
        
    }


	static public long get_time_cur_ms(){
		Calendar cal = new GregorianCalendar(TimeZone.getDefault());
		return cal.getTime().getTime();
	}

	static public String get_time_string(long time)
	{
		Calendar cal_cur = new GregorianCalendar(TimeZone.getDefault());
		Date ldate = new Date(time);
		Calendar cal = new GregorianCalendar(TimeZone.getDefault());
		cal.setTime(ldate);
		String str = String.format("%02d.%02d.%04d %02d:%02d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH)+1, cal.get(Calendar.YEAR), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
		return str;
	}
	static public String get_time_string_s(long time)
	{
		Calendar cal_cur = new GregorianCalendar(TimeZone.getDefault());
		Date ldate = new Date(time);
		Calendar cal = new GregorianCalendar(TimeZone.getDefault());
		cal.setTime(ldate);
		String str = String.format("%02d.%02d.%04d %02d:%02d:%02d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH)+1, cal.get(Calendar.YEAR), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
		return str;
	}


	public void snippet_show_userinfo(){
		mConnection.getUserInfo(new ICompletionCallback() {
			@Override
			public int onComplete(Object o_result, int result) {
				if(result != 0 || o_result==null)
					return 0;

				final CloudUserInfo userInfo = (CloudUserInfo)o_result;
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(getApplicationContext(), "snippet_show_userinfo="+userInfo.toString(), Toast.LENGTH_LONG).show();
					}
				});
				return 0;
			}
		});
	}

 	public void snippet_createCamera_by_url(final String url){
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), "snippet_createCamera_by_url", Toast.LENGTH_LONG).show();
			}
		});
		final CloudCameraList srcs = new CloudCameraList(mConnection);
		srcs.findOrCreateCamera(url, new ICompletionCallback() {
			@Override
			public int onComplete(Object o_result, final int result) {
				camera = (CloudCamera) o_result;
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if(camera == null){
							Toast.makeText(getApplicationContext(), "Camera failed error=" +result, Toast.LENGTH_LONG).show();
						}else {
							btnRecord.setText(camera.isRecording() ? "Stop Rec" : "Start Rec");
							Toast.makeText(getApplicationContext(), "Camera id=" + camera.getID() + " url=" + camera.getURL(), Toast.LENGTH_LONG).show();
						}

					}
				});
				if(camera != null) {
					player.setSource(camera);
					player.play();
				}

				camera.setName("Camera, "+ CloudHelpers.formatTime(CloudHelpers.currentTimestampUTC()));
				//camera.setTimezone("Asia/Novosibirsk");
				//camera.setPublic(true);
				camera.saveSync();

				/*camera.getTimelineDays(false, new ICompletionCallback(){
					@Override
					public int onComplete(Object o_result, int result) {
						ArrayList<Long> arr= (ArrayList<Long>) o_result;
						return 0;
					}
				});*/

				return 0;
			}
		});
	}

	public void snippet_createCamera_by_url_with_filter(final String url){

		runOnUiThread(new Runnable() {
						  @Override
						  public void run() {
							  Toast.makeText(getApplicationContext(), "snippet_createCamera_by_url_with_filter", Toast.LENGTH_LONG).show();
						  }
					  });

		//1. check if camera already created
		CloudCameraListFilter filter = new CloudCameraListFilter();
		filter.setURL(url);
		filter.setPrivacy(PS_Privacy.ps_owner);
		//filter.setForStream(false);
		final CloudCameraList srcs = new CloudCameraList(mConnection);
		srcs.getCameraList(filter, new ICompletionCallback() {
			@Override
			public int onComplete(final Object o_result, int result) {
				if(o_result != null && ((List<CloudCamera>) o_result).size()>0) {
					final List<CloudCamera> cams = (List<CloudCamera>) o_result;
					camera = cams.get(0);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							btnRecord.setText( camera.isRecording()? "Stop Rec" : "Start Rec");

							Toast.makeText(getApplicationContext(), "Camera already created id="+camera.getID()+" url="+camera.getURL(), Toast.LENGTH_LONG).show();

						}
					});

					//delete redundant cameras
					int cnt = cams.size();
					while(cnt>1){
						cnt--;
						srcs.deleteCameraSync(cams.get(cnt).getID());
					}

					player.setSource(camera);
					//timeLineSet.getTimeLine().start();
					player.play();

					//camera.setPublic(false);
					//int rc = camera.saveSync();
				}else{
					srcs.createCamera(url, new ICompletionCallback() {
						@Override
						public int onComplete(final Object o_result, final int result) {
							camera = (CloudCamera)o_result;
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									if(camera != null) {
										btnRecord.setText(camera.isRecording() ? "Stop Rec" : "Start Rec");
										Toast.makeText(getApplicationContext(), "Camera created " + camera.getURL() + " result=" + result, Toast.LENGTH_LONG).show();
									}
								}
							});

							if(result == CloudReturnCodes.ERROR_FORBIDDEN){
								//too much cameras, need delete
								CloudCameraListFilter filter = new CloudCameraListFilter();
								List<CloudCamera> cams = srcs.getCameraListSync(filter);
								for( CloudCamera c : cams){
									srcs.deleteCameraSync(c.getID());
								}
								return 0;
							}

							player.setSource(camera);
							player.play();

							//camera.setPublic(true);
							//int rc = camera.saveSync();
							return 0;
						}
					});
				}
				return 0;
			}
		});

	}

	public void snippet_createCamera_by_id(final long camid){
		runOnUiThread(new Runnable() {
						  @Override
						  public void run() {
							  Toast.makeText(getApplicationContext(), "snippet_createCamera_by_id", Toast.LENGTH_LONG).show();
						  }
					  });

		//1. check if camera already created
		final CloudCameraList srcs = new CloudCameraList(mConnection);
		srcs.getCamera(camid, new ICompletionCallback() {
			@Override
			public int onComplete(final Object o_result, final int result) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						CloudCamera camera = (CloudCamera)o_result;
						if(camera == null || result != 0){
							Toast.makeText(getApplicationContext(), "get camera id="+camid+" error="+result, Toast.LENGTH_LONG).show();
						}else
							Toast.makeText(getApplicationContext(), "get camera id="+camid+" url="+camera.getURL()+" status="+camera.getResultStr(), Toast.LENGTH_LONG).show();
					}
				});

				return 0;
			}
		});


	}

	public void snippet_getCameraPreviewUrl(CloudCamera camera){
		if(camera == null)
			return ;

		String url_preview = camera.getPreviewURLSync();

	}

        public boolean check_license(){
            if(msKeyConnection == null || msKeyConnection.length()<1){

                        new AlertDialog.Builder(this)
                                        .setTitle("License")
                                        .setMessage("Please set LICENCE KEY into msKeyConnection variable ")
                                        .setNeutralButton("OK", null)
                                        .show();

                        return false;
                }
            return true;
        }

	public void snipped_delete_all_cameras(){
		//delete all cameras
		CloudCameraListFilter filterd = new CloudCameraListFilter();
		CloudCameraList srcsd = new CloudCameraList(mConnection);
		List<CloudCamera> camsd = srcsd.getCameraListSync(filterd);
		for( CloudCamera c : camsd){
			srcsd.deleteCameraSync(c.getID());
		}
	}

	public void snippet_camera_get_timeline(CloudCamera camera){
		//get timeline for 1 day
		long time_start = get_time_cur_ms()-24*3600*1000;
		long time_end = get_time_cur_ms();
		CloudTimeline timeline = camera.getTimelineSync(time_start, time_end);


	}

	public void snippet_camera_start_record(CloudCamera camera){
		//start record
		//camera.setName("camera1");
		camera.setRecordingMode(CloudCameraRecordingMode.CONTINUES);
		camera.saveSync();
	}

	public void snippet_create_push_camera(){

		final CloudCameraList clist = new CloudCameraList(mConnection);
		CloudCameraListFilter filter = new CloudCameraListFilter();
		filter.setPrivacy(PS_Privacy.ps_owner);
		filter.setName("push"+msKeyConnection);
		clist.getCameraList(filter, new ICompletionCallback() {
			@Override
			public int onComplete(Object o_result, int result) {
				final List<CloudCamera> cams = (List<CloudCamera>) o_result;

				if(cams != null && cams.size() > 0) {
					//camera created before
					camera = cams.get(0);
					//delete redundant cameras
					int cnt = cams.size();
					while (cnt > 1) {
						cnt--;
						clist.deleteCameraSync(cams.get(cnt).getID());
					}
					camera.setRecordingMode(CloudCameraRecordingMode.CONTINUES);
					camera.saveSync();
					start_cloud_streamer();
				}else{
					camera = clist.createCameraForStreamSync();
					if(camera != null && camera.getResultInt() == 0 || camera.getResultInt() == 200){
						camera.setName("push"+msKeyConnection);
						camera.setRecordingMode(CloudCameraRecordingMode.CONTINUES);
						camera.saveSync();
						start_cloud_streamer();
					}else{
						camera = null;
					}
				}
				return 0;
			}
		});

	}

	private void start_cloud_streamer(){
		cstreamer = new CloudStreamer(new ICloudStreamerCallback(){
			@Override
			public void onStarted(final String url_push) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(getApplicationContext(), "CloudStreamer onStarted url_push="+url_push, Toast.LENGTH_LONG).show();
					}
				});
			}

			@Override
			public void onStopped() {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(getApplicationContext(), "CloudStreamer onStopped", Toast.LENGTH_LONG).show();
					}
				});
			}

			@Override
			public void onError(final int error) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(getApplicationContext(), "CloudStreamer onError="+error, Toast.LENGTH_LONG).show();
					}
				});
			}
			@Override
			public void onCameraConnected() {
				camera = (CloudCamera)cstreamer.getCamera();
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(getApplicationContext(), "CloudStreamer onCameraConnected="+camera.getName(), Toast.LENGTH_LONG).show();
					}
				});
			}
		});
		cstreamer.setCamera(camera);
		cstreamer.Start();
	}



	public void onClick(View v)
	{
		final String url = edtIpAddress.getText().toString();
		if (!edtIpAddressHistory.contains(url))
			edtIpAddressHistory.add(url);
		SharedPreferences.Editor ed = settings.edit();
		ed.putString("connectionUrl", url);
		ed.apply();


		if(!check_license())
			return ;

		if(cstreamer != null){
			cstreamer.Stop();
		}

		if(mConnection == null){
			playerStatusText.setText("Connecting...");

			mConnection = new CloudTrialConnection();
			((CloudTrialConnection)mConnection).open(msKeyConnection, new ICompletionCallback(){
				@Override
				public int onComplete(Object o_result, final int result) {
//					CloudCameraList l=new CloudCameraList(mConnection);
//					List<CloudCamera> cams= l.getCameraListSync(new CloudCameraListFilter());
//					CloudTimeline cloudTimeline=  null;
//					for(CloudCamera c: cams)
//					{
//						cloudTimeline=c.getTimelineSync(System.currentTimeMillis()-3600*1000*10,System.currentTimeMillis());
//						if(cloudTimeline!=null&&cloudTimeline.periods!=null)
//						{
//							//timeLineSet.t.loadData(cloudTimeline.periods);
//							timeLineSet.t.setCamera(c);
//							timeLineSet.t.init();
//
//							break;
//						}
//
//					}

					//timeLineSet.getTimeLine().setConnection(mConnection);
					//timeLineSet.getTimeLine().start();
					runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							Toast.makeText(getApplicationContext(),"Connection open result="+result, Toast.LENGTH_LONG).show();
						}
					});

					if(result == 0) {
						snippet_show_userinfo();
						snippet_createCamera_by_url(url);
						//snippet_createCamera_by_url_with_filter(url);
						//snippet_camera_get_timeline(camera);

						//snippet_create_push_camera();
					}else {
						mConnection = null;
						runOnUiThread(new Runnable()
						{
							@Override
							public void run()
							{
								setUIDisconnected();
							}
						});
					}

					return 0;
				}
			});

		}else{
			snippet_createCamera_by_url(url);
			//snippet_create_push_camera();
		}
    }
 
	protected void onPause()
	{
		Log.e(TAG, "onPause()");
		super.onPause();
	}

	@Override
  	protected void onResume() 
	{
		Log.e(TAG, "onResume()");
		super.onResume();
  	}

  	@Override
	protected void onStart() 
  	{
      	Log.e(TAG, "onStart()");
		super.onStart();
	}

  	@Override
	protected void onStop() 
  	{
  		Log.e(TAG, "onStop()");
		super.onStop();
	}

    @Override
    public void onBackPressed() 
    {
		if(!TimeLineSet.calVisible) {
			player.close();

			if (!playing) {
				super.onBackPressed();
				return;
			}

			setUIDisconnected();
		}
		else timeLineSet.onBackPressed();
    }
  	
  	@Override
  	protected void onDestroy() 
  	{
  		Log.e(TAG, "onDestroy()");

		player.close();
		
		System.gc();
		
		if (multicastLock != null) {
		    multicastLock.release();
		    multicastLock = null;
		}		
		super.onDestroy();
   	}	
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item)  
	{
		switch (item.getItemId())  
		{
			case R.id.main_opt_clearhistory:
			
				new AlertDialog.Builder(this)
				.setTitle("Clear History")
				.setMessage("Do you really want to delete the history?")
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() 
				{
					public void onClick(DialogInterface dialog, int which) 
					{
						HashSet<String> tempHistory = new HashSet<String>();
						tempHistory.add("rtsp://184.72.239.149/vod/mp4:BigBuckBunny_115k.mov");
						edtIpAddressHistory.clear();
						edtIpAddressHistory = tempHistory;  
					}
				})
				.setNegativeButton("No", new DialogInterface.OnClickListener() 
				{
					public void onClick(DialogInterface dialog, int which) 
					{
						// do nothing
					}
				}).show();
				break;
			case R.id.main_opt_exit:     
				finish();
				break;

		}
		return true;
	}

	protected void setUIDisconnected()
	{
		setTitle(R.string.app_name);
		btnConnect.setText("Connect");
		playerStatusText.setText("DEMO VERSION");
		playerStatusText.setVisibility(View.VISIBLE);
		player_progress.setVisibility(View.INVISIBLE);
		playing = false;
		if (playerPanelControlTask != null) {
			playerPanelControlTask.cancel(true);
			playerPanelControlTask = null;
		}

		setShowControls();
	}

	protected void setHideControls()
	{
		edtIpAddress.setVisibility(View.GONE);
		btnHistory.setVisibility(View.GONE);
		btnConnect.setVisibility(View.GONE);

		playerLayoutTime.setVisibility(View.VISIBLE);
		playerLayoutControls.setVisibility(View.VISIBLE);

	}

	protected void setShowControls()
	{
		setTitle(R.string.app_name);

		playerLayoutTime.setVisibility(View.GONE);
		playerLayoutControls.setVisibility(View.GONE);
		
		edtIpAddress.setVisibility(View.VISIBLE);
		btnHistory.setVisibility(View.VISIBLE);
		btnConnect.setVisibility(View.VISIBLE);
	}

	private void showStatusView() 
	{
		player_view.setVisibility(View.INVISIBLE);
		playerStatus.setVisibility(View.VISIBLE);
		
	}
	
	private void showVideoView() 
	{
        playerStatus.setVisibility(View.INVISIBLE);
		player_view.setVisibility(View.VISIBLE);

		setTitle("");
	}

	private class PlayerPanelControlVisibleTask extends AsyncTask<String, Void, Boolean>
	{
		private long time = 0;
		private int time_delay = 500;

		@Override
		protected void onPreExecute()
		{
			super.onPreExecute();
			time = System.currentTimeMillis();
		}

		@Override
		protected Boolean doInBackground(String... params)
		{
			Runnable uiRunnable = null;
			uiRunnable = new Runnable()
			{
				public void run()
				{
					long pos = player.getPosition();
					textTimeText.setText(""+ (player.isLive()?"Live: ":"Recorded: ") + (pos==0L?"--:--": get_time_string_s(pos)));
					btn_play_pause.setText(player.getState() == CloudPlayerState.PAUSED?"Play":"Pause");
					btn_live_playback.setText(player.isLive()?"to Rec":"to Live");
					if(camera != null && camera.isRecording()){
						btnRecord.setText("Stop Rec");
					}else{
						btnRecord.setText("Start Rec");
					}
					synchronized(this) { this.notify(); }
				}
			};

			boolean stop = false;
			try
			{
				do
				{
					synchronized ( uiRunnable )
					{
						runOnUiThread(uiRunnable);
						try
						{
							uiRunnable.wait();
						}
						catch ( InterruptedException e ) { stop = true; }
					}

					if (stop) break;

					Thread.sleep(time_delay);
				}
				while(!isCancelled());
			}
			catch (Exception e)
			{
			}

			synchronized ( uiRunnable )
			{
				runOnUiThread(uiRunnable);
				try
				{
					uiRunnable.wait();
				}
				catch ( InterruptedException e ) { stop = true; }
			}

			return true;
		}

		@Override
		protected void onPostExecute(Boolean result)
		{
			super.onPostExecute(result);
			playerPanelControlTask = null;
		}
		@Override
		protected void onCancelled()
		{
			super.onCancelled();
			playerPanelControlTask = null;
		}
	}


	static public <T> void executeAsyncTask(AsyncTask<T, ?, ?> task, T... params)
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		{
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
		}
		else
		{
			task.execute(params);
		}
	}



}
