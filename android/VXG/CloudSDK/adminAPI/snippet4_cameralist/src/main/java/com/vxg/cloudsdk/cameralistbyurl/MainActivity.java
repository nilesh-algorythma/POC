/*
 *
  * Copyright (c) 2011-2017 VXG Inc.
 *
 */


package com.vxg.cloudsdk.cameralistbyurl;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.example.cameralistbyurl.R;
import com.vxg.cloudsdk.CloudSDK;
import com.vxg.cloudsdk.Enums.CloudCameraRecordingMode;
import com.vxg.cloudsdk.Enums.CloudPlayerEvent;
import com.vxg.cloudsdk.Enums.CloudPlayerState;
import com.vxg.cloudsdk.Enums.CloudReturnCodes;
import com.vxg.cloudsdk.Enums.PS_Privacy;
import com.vxg.cloudsdk.Interfaces.ICloudConnection;
import com.vxg.cloudsdk.Interfaces.ICloudObject;
import com.vxg.cloudsdk.Interfaces.ICloudPlayerCallback;
import com.vxg.cloudsdk.Interfaces.ICompletionCallback;
import com.vxg.cloudsdk.Objects.CloudCamera;
import com.vxg.cloudsdk.Objects.CloudCameraList;
import com.vxg.cloudsdk.Objects.CloudCameraListFilter;
import com.vxg.cloudsdk.Objects.CloudPlayer;
import com.vxg.cloudsdk.Objects.CloudPlayerConfig;
import com.vxg.cloudsdk.Objects.CloudTimeline;
import com.vxg.cloudsdk.Objects.CloudTrialConnection;
import com.vxg.cloudsdk.Objects.CloudUserInfo;
import com.vxg.ui.TimeLineSet;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

public class MainActivity extends Activity implements OnClickListener
{
	public final String TAG = MainActivity.class.getSimpleName();
	public  static AutoCompleteTextView edtUrl;
	private Button						btnConnect;

	private CloudCamera 				camera;

	private PlayerPanelControlVisibleTask 	playerPanelControlTask 	= null;

	private MulticastLock multicastLock = null;
	ICloudConnection mConnection;

	//SET LICENCE KEY
	String msKeyConnection = "";
	ArrayAdapter<String> adapter;
	ListView list;
	int deletePosition = -1;


	LinearLayout add_camera_layout;
	LinearLayout delete_layout;
	ProgressBar bar;

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

		setContentView(R.layout.view_camera_buy_url);

		CloudSDK.setContext(this);
		CloudSDK.setLogEnable(true);
		CloudSDK.setLogLevel(2);
		Toast.makeText(getApplicationContext(), "CloudSDK ver="+CloudSDK.getLibVersion(), Toast.LENGTH_LONG).show();

		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		edtUrl = (AutoCompleteTextView)findViewById(R.id.edit_url);
		edtUrl.setText("rtsp://184.72.239.149/vod/mp4:BigBuckBunny_115k.mov");

		bar = (ProgressBar)findViewById(R.id.progress_read_cameras);
		add_camera_layout = (LinearLayout)findViewById(R.id.add_layout);

		list = (ListView)findViewById(R.id.camera_list);
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,final int position, long id) {
				deletePosition = position;
				ClearSelection();
				view.setEnabled(false);
			}
		});

		adapter=new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1,
				new ArrayList<String>());
		list.setAdapter(adapter);

		Button refresh = (Button)findViewById(R.id.button_refresh);
		refresh.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				UpdateCameras();
			}
		});

		delete_layout = (LinearLayout)findViewById(R.id.refresh_layout);

		Button delete = (Button)findViewById(R.id.button_delete);
		delete.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				DeleteItem();
				ClearSelection();
			}
		});

		edtUrl.setOnEditorActionListener(new OnEditorActionListener()
		{
			@Override
			public boolean onEditorAction(TextView v, int actionId,	KeyEvent event)
			{
				if (event != null&& (event.getKeyCode() == KeyEvent.KEYCODE_ENTER))
				{
					InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					in.hideSoftInputFromWindow(edtUrl.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
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
		UpdateCameras();
	}

	void StartDeleteItem(){
		if(deletePosition==-1)
			return;
		try{
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
							DeleteItem();
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
				DeleteItem();
			}
		} catch (Exception e){
			Log.e(TAG,e.toString());
			return;
		}
	}

	void DeleteItem(){
		if(deletePosition==-1)
			return;

		String stringId = adapter.getItem(deletePosition).split(",")[0];
		final long id = Long.parseLong(stringId);

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), "Delete item: "+id, Toast.LENGTH_LONG).show();
			}
		});

		deletePosition = -1;

		final CloudCameraList srcs = new CloudCameraList(mConnection);
		srcs.deteleCamera(id, new ICompletionCallback() {
			@Override
			public int onComplete(final Object o_result, final int result) {
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						UpdateCameras();
					}
				});
				return 0;
			}
		});
	}


	void ClearSelection(){
		for (int i=0;i<list.getCount();i++) {
			if(list.getChildAt(i)!=null)
				list.getChildAt(i).setEnabled(true);
		}
	}

	public void snippet_createCamera_by_url(final String url){
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), "snippet_createCamera_by_url", Toast.LENGTH_LONG).show();
			}
		});

		final CloudCameraList srcs = new CloudCameraList(mConnection);

		srcs.createCamera(url, new ICompletionCallback() {
			@Override
			public int onComplete(final Object o_result, final int result) {
				if(o_result != null) {
					camera = (CloudCamera) o_result;
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if(camera == null || result != 0){
								Toast.makeText(getApplicationContext(), "error add camera url="+url+" error="+result, Toast.LENGTH_LONG).show();
							}else
								UpdateCameras();
						}
					});
					return 0;
				}
				return  0;
			}
		});
	}

	void UpdateCameras(){
		delete_layout.setVisibility(View.GONE);
		add_camera_layout.setVisibility(View.GONE);
		bar.setVisibility(View.VISIBLE);

		try{
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
							ReadCameras();
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
				ReadCameras();
			}
		} catch (Exception e){
			Log.e(TAG,e.toString());
			return;
		}
	}

	void ReadCameras(){
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), "Update all cameras ", Toast.LENGTH_LONG).show();
			}
		});

		final CloudCameraList srcs = new CloudCameraList(mConnection);
		CloudCameraListFilter filter = new CloudCameraListFilter();
		filter.setPrivacy(PS_Privacy.ps_owner);
		srcs.getCameraList(filter, new ICompletionCallback() {
			@Override
			public int onComplete(final Object o_result, final int result) {
				if(o_result != null && ((List<CloudCamera>) o_result).size()>0) {
					final List<CloudCamera> allCams = (List<CloudCamera>) o_result;
					for (CloudCamera cam:allCams) {
						Log.e(TAG,cam.getName()+" "+cam.getID()+" "+cam.getURL());
					}
					runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							Toast.makeText(getApplicationContext(), "Read cameras data", Toast.LENGTH_LONG).show();
							adapter.clear();
							for (CloudCamera cam:allCams) {
								Log.e(TAG,cam.getName()+" "+cam.getID()+" "+cam.getURL());
								adapter.add(cam.getID()+",url:"+cam.getURL());
							}
							adapter.notifyDataSetChanged();
							deletePosition = -1;
							list.setAdapter(adapter);

							delete_layout.setVisibility(View.VISIBLE);
							add_camera_layout.setVisibility(View.VISIBLE);
							bar.setVisibility(View.GONE);

						}
					});
					return 0;
				}
				return  0;
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
		ClearSelection();
		deletePosition = -1;
		try{
			final String camUrl = edtUrl.getText().toString();

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
							snippet_createCamera_by_url(camUrl);
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
				snippet_createCamera_by_url(camUrl);
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
