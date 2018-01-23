/*
 *
  * Copyright (c) 2011-2017 VXG Inc.
 *
 */


package com.vxg.cloudsdk.snippetviewcamerabyid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.example.snippetviewbyurl.R;
import com.vxg.cloudsdk.CloudSDK;
import com.vxg.cloudsdk.Enums.CloudPlayerEvent;
import com.vxg.cloudsdk.Interfaces.ICloudConnection;
import com.vxg.cloudsdk.Interfaces.ICloudObject;
import com.vxg.cloudsdk.Interfaces.ICloudPlayerCallback;
import com.vxg.cloudsdk.Interfaces.ICompletionCallback;
import com.vxg.cloudsdk.Objects.CloudCamera;
import com.vxg.cloudsdk.Objects.CloudCameraList;
import com.vxg.cloudsdk.Objects.CloudPlayer;
import com.vxg.cloudsdk.Objects.CloudPlayerConfig;
import com.vxg.cloudsdk.Objects.CloudTrialConnection;

public class MainActivity extends Activity implements OnClickListener
{

	public final String TAG = MainActivity.class.getSimpleName();

	public  static AutoCompleteTextView	edtId;
	private Button						btnConnect;

	private FrameLayout					player_view;
	private CloudPlayer 				player;
	private CloudCamera 				camera;

	private PlayerPanelControlVisibleTask 	playerPanelControlTask 	= null;

	private MulticastLock multicastLock = null;
	ICloudConnection mConnection;

	//SET LICENCE KEY
	String msKeyConnection = "";


    @Override
    public void onCreate(Bundle savedInstanceState)
	{
		setTitle(R.string.app_name);
		super.onCreate(savedInstanceState);

		WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		multicastLock = wifi.createMulticastLock("multicastLock");
		multicastLock.setReferenceCounted(true);
		multicastLock.acquire();

		getWindow().requestFeature(Window.FEATURE_PROGRESS);
		getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);

		setContentView(R.layout.view_camera_buy_id);

		CloudSDK.setContext(this);
		CloudSDK.setLogEnable(true);
		CloudSDK.setLogLevel(2);
		Toast.makeText(getApplicationContext(), "CloudSDK ver="+CloudSDK.getLibVersion(), Toast.LENGTH_LONG).show();

		player_view = (FrameLayout) findViewById(R.id.playerView);
		player = new CloudPlayer(player_view, new CloudPlayerConfig(), new ICloudPlayerCallback(){

			@Override
			public void onStatus(final CloudPlayerEvent player_event, final ICloudObject p) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						switch(player_event){
							case CONNECTING:
								break;
							case CONNECTED:
							case STARTED:
							case PAUSED:
								if(playerPanelControlTask == null) {
									playerPanelControlTask = new PlayerPanelControlVisibleTask();
									executeAsyncTask(playerPanelControlTask, "");
								}
								break;
							case CLOSED:
								setUIDisconnected();
								break;
							case EOS:
								break;
							case SEEK_COMPLETED:
								break;
							case ERROR:
								break;
							case TRIAL_VERSION:
								setUIDisconnected();
								break;
						}
					}
				});
			}
		});

		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		edtId = (AutoCompleteTextView)findViewById(R.id.edit_id);
		edtId.setText("128362");

		edtId.setOnEditorActionListener(new OnEditorActionListener()
		{
			@Override
			public boolean onEditorAction(TextView v, int actionId,	KeyEvent event)
			{
				if (event != null&& (event.getKeyCode() == KeyEvent.KEYCODE_ENTER))
				{
					InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					in.hideSoftInputFromWindow(edtId.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
					return true;

				}
				return false;
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
    }

	public void snippet_createCamera_by_id(final long camid){
		runOnUiThread(new Runnable() {
						  @Override
						  public void run() {
							  Toast.makeText(getApplicationContext(), "snippet_createCamera_by_id", Toast.LENGTH_LONG).show();
						  }
					  });

		final CloudCameraList srcs = new CloudCameraList(mConnection);
		srcs.getCamera(camid, new ICompletionCallback() {
			@Override
			public int onComplete(final Object o_result, final int result) {

                camera = (CloudCamera)o_result;

                runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if(camera == null || result != 0){
							Toast.makeText(getApplicationContext(), "get 1 camera id="+camid+" error="+result, Toast.LENGTH_LONG).show();
						}else
							Toast.makeText(getApplicationContext(), "get 2 camera id="+camid+" url="+camera.getURL()+" status="+camera.getResultStr(), Toast.LENGTH_LONG).show();
					}
				});
                player.setSource(camera);
                player.play();
				return 0;
			}
		});
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

	public void onClick(View v)
	{
		try{
			final long camId = Long.parseLong(edtId.getText().toString());

			if(!check_license())
				return ;

			if(mConnection == null){
				mConnection = new CloudTrialConnection();
				((CloudTrialConnection)mConnection).open(msKeyConnection, new ICompletionCallback(){
					@Override
					public int onComplete(Object o_result, final int result) {
						runOnUiThread(new Runnable()
						{
							@Override
							public void run()
							{
								Toast.makeText(getApplicationContext(),"Connection open result="+result, Toast.LENGTH_LONG).show();
							}
						});

						if(result == 0) {
							snippet_createCamera_by_id(camId);
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
				snippet_createCamera_by_id(camId);
			}
		} catch (Exception e){
			Log.e(TAG,e.toString());
			return;
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

	protected void setUIDisconnected()
	{
		setTitle(R.string.app_name);
		btnConnect.setText("Connect");
		if (playerPanelControlTask != null) {
			playerPanelControlTask.cancel(true);
			playerPanelControlTask = null;
		}
	}



	private class PlayerPanelControlVisibleTask extends AsyncTask<String, Void, Boolean>
	{
		private int time_delay = 500;

		@Override
		protected void onPreExecute()
		{
			super.onPreExecute();
		}

		@Override
		protected Boolean doInBackground(String... params)
		{
			Runnable uiRunnable = null;
			uiRunnable = new Runnable()
			{
				public void run()
				{
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
