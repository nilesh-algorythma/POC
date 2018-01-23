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
import android.os.Build;
import android.text.TextUtils;
import android.util.Pair;

import com.vxg.cloud.core.CloudCommon.CloudUri;
import com.vxg.cloud.core.CloudSessions.CloudCamsessChatMessage;
import com.vxg.cloudsdk.Helpers.MLog;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class CloudHelpers {
    private static String TAG = CloudHelpers.class.getSimpleName();
    final public static int LOG_LEVEL = 2; //Log.VERBOSE;
    static MLog Log = new MLog(TAG, LOG_LEVEL);

    // Use the method parseUTCTime
    @Deprecated
    public static long parseTime(String time){
        return parseUTCTime(time);
    }

    public CloudUri parseUri(String str){
        CloudUri uri = new CloudUri(str);
        return uri;
    }

    public static long parseUTCTime(String time) {
        Calendar cal = Calendar.getInstance();
        String format1 = "yyyy-MM-dd'T'HH:mm:ss";
        String format2 = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS";
        String format3 = "yyyy-MM-dd'T'HH:mm:ss.SSS";
        String format = null;
        if(time.length() == format1.length()-2){
            format = format1;
        }else if(time.length() == format2.length()-2){
            //bug. SimpleDateFormat doesn't support nanos
            //format = format2;
            format = format3;
            time = time.substring(0, format3.length()-2);
        }else if(time.length() == format3.length()-2){
            format = format3;
        }
        if(format == null)
            return 0;

        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            cal.setTime(sdf.parse(time));
        } catch (ParseException e) {
            Log.e("parseTime"+ e);
        }
        return cal.getTime().getTime();
    }

    @Deprecated
    public static String mapToUrlQuery(Map<String,String> params){
        String sQuery = "";
        for (String key : params.keySet()) {
            String sKey = "";
            String sValue = "";
            try {
                sKey = URLEncoder.encode(key, "utf-8");
                sValue = URLEncoder.encode(params.get(key), "utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if(!key.isEmpty()) {
                sQuery += sQuery.length() != 0 ? "&" : "";
                sQuery += sKey + "=" + sValue;
            }
        }
        return sQuery;
    }

    public static String prepareHttpGetQuery(ArrayList<Pair<String,String>> params){
        ArrayList<String> params2 = new ArrayList<>();
        for (Pair<String,String> item : params) {
            String sKey = "";
            String sValue = "";
            try {
                sKey = URLEncoder.encode(item.first, "utf-8");
                sValue = URLEncoder.encode(item.second, "utf-8");
                if(!sKey.isEmpty()) {
                    params2.add(sKey + "=" + sValue);
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        String sQuery = TextUtils.join("&", params2);
        return sQuery;
    }

    public static String formatTime(long time){
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(cal.getTime());
    }

    public static String formatTime_withSSSSSS(long time){
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        //bug. SimpleDateFormat doesn't support nanos
        //SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return (sdf.format(cal.getTime())+"000");
    }
    public static String formatTime_forMediaFileUploading(long time){
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss.SSS", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(cal.getTime());
    }

    public static long currentTimestampUTC(){
        Calendar cal = Calendar.getInstance();
        return cal.getTime().getTime();
    }

    public static String formatCurrentTimestampUTC(){
        return formatTime(currentTimestampUTC());
    }

    public static String formatCurrentTimestampUTC_SSSSSS(){
        return formatTime_withSSSSSS(currentTimestampUTC());
    }

    public static String formatCurrentTimestampUTC_MediaFileUploading(){
        return formatTime_forMediaFileUploading(currentTimestampUTC());
    }

    public static ArrayList<CloudCamsessChatMessage> getMessagesAfter(ArrayList<CloudCamsessChatMessage> messages, CloudCamsessChatMessage afterMessage){
        ArrayList<CloudCamsessChatMessage> result = new ArrayList<>();
        int foundEqualMessage = -1;
        for(int i = 0; i < messages.size(); i++){
            if(messages.get(i).equals(afterMessage)){
                foundEqualMessage = i;
            }
        }
        if(foundEqualMessage >= 0){
            for(int i = foundEqualMessage + 1; i < messages.size(); i++){
                result.add(messages.get(i));
            }
        }else{
            result.addAll(messages);
        }
        return result;
    }


    public static <T> void executeAsyncTask(AsyncTask<T, ?, ?> task, T... params) {
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
