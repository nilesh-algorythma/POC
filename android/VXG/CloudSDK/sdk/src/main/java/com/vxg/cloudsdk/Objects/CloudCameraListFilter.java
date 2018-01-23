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

import android.util.Pair;

import com.vxg.cloud.core.CloudHelpers;
import com.vxg.cloudsdk.Enums.CloudCameraStatus;
import com.vxg.cloudsdk.Enums.PS_Privacy;
import com.vxg.cloudsdk.Helpers.MLog;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CloudCameraListFilter {
    private static final String TAG = CloudCameraListFilter.class.getSimpleName();
    final public static int LOG_LEVEL = 2; //Log.VERBOSE;
    static MLog Log = new MLog(TAG, LOG_LEVEL);


    public int offset = 0;
    public int limit = 50;
    public String mName = null;
    public String url = null;

    public boolean use_geo = false;
    public double lat_min = 0.0f;
    public double lat_max = 0.0f;
    public double long_min = 0.0f;
    public double long_max = 0.0f;

    public String login = null;
    public String password = null;

    CloudCameraStatus cameraStatus = null;
    public Boolean is_recording = null;
    public Boolean is_public = null;
    public Boolean is_owner = true; //owner' cameras
    public Boolean is_forstream = null;
    private Boolean mSortByName = null;
    private Boolean mSortByCreated = null;
    private String mPartOfName = null;

    public void setOffset(int nOffset){ // default 0
        offset = nOffset;
    }
    public void setLimit(int nLimit) // max camera details in response, default 50
    {
        limit = nLimit;
    }

    // set filter by name
    public void setName(String name){
        mName = name;
    }

    public void setURL(String _url) // set filter by url
    {
        url = _url;

        if(url == null)
            return;
        int ch0 = url.indexOf("://");
        int ch1 = url.indexOf("@");
        if(ch0 != -1 && ch1 != -1){
            login = url.substring(ch0+3, ch1);
            String[] ss = login.split(":");
            if(ss.length>0){
                login = ss[0];
            }
            if(ss.length>1){
                password = ss[1];
            }
            url = url.substring(0, ch0+3)+url.substring(ch1+1);
        }
    }

    public void setLatLngBounds(double lat_min, double lat_max, double long_min, double long_max)
    {
        this.lat_min = lat_min;
        this.lat_max = lat_max;
        this.long_min = long_min;
        this.long_max = long_max;
        use_geo = true;
    }

    public void setCameraStatus(CloudCameraStatus status){
        cameraStatus = status;
    }

    public void setRecording(Boolean is_recording){
        this.is_recording = is_recording;
    }

    public void setPrivacy(PS_Privacy privacy) //set owner/public
    {
        switch (privacy){
            case ps_owner_not_public: 	//owner=true, public=false
                is_owner = true;
                is_public = false;
                break;
            case ps_owner: 			//owner=true, public=null
                //default
                is_owner = true;
                is_public = null;
                break;
            case ps_public_not_owners: 	//owner=false, public=true
                is_owner = false;
                is_public = true;
                break;
            case ps_public:			//owner=null, public=true
                is_owner = null;
                is_public = true;
                break;
            case ps_owners_public:		//owner=true, public=true
                is_owner = true;
                is_public = true;
                break;
            case ps_all:				//owner=null, public=null
                is_owner = null;
                is_public = null;
                break;
        }
    }

    public void setmPartOfName(String name){
        mPartOfName = name;
    }

    public void sortByName(Boolean asc){
        mSortByName = asc;
    }

    public void sortByCreated(Boolean asc){
        mSortByCreated = asc;
    }

    public void setForStream(Boolean is_forstream) {
        // is_forstream=true -push camera , is_forstream=false - pull camera, null - all (by default)
        this.is_forstream = is_forstream;
    }

    // returns the list of filters
    public ArrayList<Pair<String,String>> _getValues() {
        ArrayList<Pair<String,String>> params = new ArrayList<>();

        params.add(new Pair<>("offset", Integer.toString(offset)));
        params.add(new Pair<>("limit", Integer.toString(limit)));
        params.add(new Pair<>("detail", "detail"));
        if(mName != null) {
            params.add(new Pair<>("name", mName));
        }

        if(url != null) {
            params.add(new Pair<>("url", url));
        }

        if(use_geo){
            params.add(new Pair<>("latitude__gte", Double.toString(lat_min)));
            params.add(new Pair<>("latitude__lte", Double.toString(lat_max)));
            params.add(new Pair<>("longitude__gte", Double.toString(long_min)));
            params.add(new Pair<>("longitude__lte", Double.toString(long_max)));
        }
        if(cameraStatus != null){
            switch (cameraStatus){
                case ACTIVE:
                    params.add(new Pair<>("status", "active"));
                    break;
                case INACTIVE:
                    params.add(new Pair<>("status", "inactive"));
                    break;
                case UNAUTHORIZED:
                    params.add(new Pair<>("status", "unauthorized"));
                    break;
                case INACTIVE_BY_SCHEDULER:
                    params.add(new Pair<>("status", "inactive_by_scheduler"));
                    break;
                default:
                    params.add(new Pair<>("status", "offline"));
                    break;
            }
        }

        if(is_recording != null){
            params.add(new Pair<>("rec_status", is_recording.booleanValue() ? "on":"off"));
        }

        if(is_public != null){
            params.add(new Pair<>("public", is_public.booleanValue() ? "true":"false"));
        }

        if(is_owner != null){
            params.add(new Pair<>("is_owner", is_owner.booleanValue() ? "true":"false"));
        }
        if(is_forstream != null){
            params.add(new Pair<>("url__isnull", is_forstream.booleanValue() ? "true":"false"));
        }

        if(mSortByName != null) {
            params.add(new Pair<>("order_by",(mSortByName ? "name" : "-name")));
        }
        if(mSortByCreated != null) {
            params.add(new Pair<>("order_by",(mSortByCreated ? "created" : "-created")));
        }

        if(mPartOfName != null){
            params.add(new Pair<>("name__icontains", mPartOfName));
        }

        return params;
    }

    public String toUrlString_ForGetRequest(){
        String url_get_request = CloudHelpers.prepareHttpGetQuery(_getValues());
        Log.d("toUrlString_ForGetRequest: " + url_get_request);
        return url_get_request;
    };


}
