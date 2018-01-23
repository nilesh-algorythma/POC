package com.vxg.cloudsdk.camerachangesettings;
/*
 *
  * Copyright (c) 2011-2017 VXG Inc.
 *
 */


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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

import com.example.camerachangesettings.R;
import com.vxg.cloudsdk.CloudSDK;
import com.vxg.cloudsdk.Enums.CloudPlayerEvent;
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
import com.vxg.cloudsdk.Objects.CloudTrialConnection;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener
{

    public final String TAG = MainActivity.class.getSimpleName();

    private Button btnConnect;

    private CloudCamera 				camera;

    //edit data
    public static Spinner edtId;
    public static EditText editUrl;
    public static EditText editName;
    public static EditText editLogin;
    public static CheckBox isPublic;
    public static EditText editPassword;

    public static TextView statusLabel;

    private PlayerPanelControlVisibleTask 	playerPanelControlTask 	= null;

    private WifiManager.MulticastLock multicastLock = null;
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

        setContentView(R.layout.main);

        CloudSDK.setContext(this);
        CloudSDK.setLogEnable(true);
        CloudSDK.setLogLevel(2);
        Toast.makeText(getApplicationContext(), "CloudSDK ver="+CloudSDK.getLibVersion(), Toast.LENGTH_LONG).show();

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        edtId = (Spinner)findViewById(R.id.edit_id);

        mySetttings = getSharedPreferences(MainActivity.class.getSimpleName(),Context.MODE_PRIVATE);

        btnConnect = (Button)findViewById(R.id.connected_button);
        btnConnect.setOnClickListener(this);

        Button getAllCameras = (Button)findViewById(R.id.update_all_cameras);
        getAllCameras.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                UpdateCameras();
            }
        });


        saveButon = (Button)findViewById(R.id.save_button);
        saveButon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SaveData();
            }
        });

        refreshButon = (Button)findViewById(R.id.refresh_button);
        refreshButon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                RefreshButton();
            }
        });

        //edit data
        editName = (EditText)findViewById(R.id.edit_name);
        editUrl = (EditText)findViewById(R.id.edit_url);
        editLogin = (EditText)findViewById(R.id.edit_login);
        editPassword = (EditText)findViewById(R.id.edit_password);

        statusLabel = (TextView) findViewById(R.id.camera_is_disconnected);


        isPublic = (CheckBox)findViewById(R.id.edit_public);

        editName.setEnabled(false);
        editUrl.setEnabled(false);
        editLogin.setEnabled(false);
        editPassword.setEnabled(false);

        refreshButon.setEnabled(false);
        saveButon.setEnabled(false);
        btnConnect.setEnabled(false);
        edtId.setEnabled(false);
        isPublic.setEnabled(false);
    }


    Button refreshButon;
    Button saveButon;

    void UpdateCameras(){
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
                            LoadData();
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
                LoadData();
            }
        } catch (Exception e){
            Log.e(TAG,e.toString());
            return;
        }
    }

    void LoadData(){

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "Update all cameras ", Toast.LENGTH_LONG).show();
            }
        });

        ArrayList<String> arrayList = new ArrayList<String>();
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, arrayList);

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
                            for (CloudCamera cam:allCams) {
                                Log.e(TAG,cam.getName()+" "+cam.getID()+" "+cam.getURL());
                                adapter.add(cam.getID()+","+cam.getURL());
                            }
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            edtId.setAdapter(adapter);

                            editName.setEnabled(true);
                            editUrl.setEnabled(true);
                            editLogin.setEnabled(true);
                            editPassword.setEnabled(true);

                            refreshButon.setEnabled(true);
                            saveButon.setEnabled(true);
                            btnConnect.setEnabled(true);
                            edtId.setEnabled(true);
                            isPublic.setEnabled(true);
                        }
                    });
                    return 0;
                }
                return  0;
            }
        });
    }

    SharedPreferences mySetttings;

    void SaveData(){

        if(camera!=null){
            camera.setName(editName.getText().toString());
            camera.setURL(editUrl.getText().toString());
            camera.setURLLogin(editLogin.getText().toString());
            camera.setURLPassword(editPassword.getText().toString());
            camera.setPublic(isPublic.isChecked());
            camera.save(new ICompletionCallback() {
                @Override
                public int onComplete(Object o_result, int result) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "data updated", Toast.LENGTH_LONG).show();
                        }
                    });
                    return 0;
                }
            });
        }

        SharedPreferences.Editor edit = mySetttings.edit();
        edit.putString("editId",editName.getText().toString());
        edit.putString("editUrl",editUrl.getText().toString());
        edit.putString("editLogin",editLogin.getText().toString());
        edit.putString("editPassword",editPassword.getText().toString());
        edit.putBoolean("editIsPublic",isPublic.isChecked());
        edit.apply();
    }

    void RefreshButton(){
        if(camera==null){
            statusLabel.setText(getString(R.string.camera_is_disconnected));
        } else {
            camera.refresh(new ICompletionCallback() {
                @Override
                public int onComplete(Object o_result, int result) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            editName.setText(camera.getName());
                            editUrl.setText(camera.getURL());
                            editLogin.setText(camera.getURLLogin());
                            editPassword.setText(camera.getURLPassword());
                            isPublic.setChecked(camera.isPublic());
                            statusLabel.setText(getString(R.string.camera_is_connected));
                        }
                    });
                    return 0;
                }
            });
        }
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
                            statusLabel.setText(getString(R.string.camera_is_connected));
                        } else {
                            Toast.makeText(getApplicationContext(), "get 2 camera id="+camid+" url="+camera.getURL()+" status="+camera.getResultStr(), Toast.LENGTH_LONG).show();
                            editName.setText(camera.getName());
                            editUrl.setText(camera.getURL());
                            editLogin.setText(camera.getURLLogin());
                            editPassword.setText(camera.getURLPassword());
                            isPublic.setChecked(camera.isPublic());
                            statusLabel.setText(getString(R.string.camera_is_connected));
                        }
                    }
                });
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
            final long camId = Long.parseLong(edtId.getSelectedItem().toString().split(",")[0]);

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

