package com.vxg.cloud.core.CloudSessions;

import android.graphics.Paint;
import android.util.Pair;

import com.google.android.gms.maps.model.LatLngBounds;
import com.vxg.cloudsdk.Helpers.MLog;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CloudCamsessFilter {
    private static String TAG = CloudCamsessFilter.class.getSimpleName();
    final public static int LOG_LEVEL = 2; //Log.VERBOSE;
    static MLog Log = new MLog(TAG, LOG_LEVEL);


    public enum CameraStatus {
        CAMERA_STATUS_ACTIVE,
        CAMERA_STATUS_INACTIVE,
        CAMERA_STATUS_ANY
    };

    public enum CameraOnline { YES, NO, ANY };
    public enum CamsessStreaming { YES, NO, ANY };
    public enum CamsessHasRecords { YES, NO, ANY };

    private int mOffset = 0;
    private boolean mMoreDetails = true;
    private int mLimit = 50;
    private long mCamsessID = 0;
    private String mStartLessThen = null;
    private String mTitle = null;
    private String mAuthorName = null;
    private String mAuthorPrefferedName = null;
    private CameraStatus mCameraStatus = CameraStatus.CAMERA_STATUS_ANY;
    private CameraOnline mCameraOnline = CameraOnline.ANY;
    private CamsessStreaming mCamsessStreaming = CamsessStreaming.ANY;
    private CamsessHasRecords mCamsessHasRecords = CamsessHasRecords.ANY;
    private boolean mLatLngUpdated = false;
    private double mLatitudeMin = 0.0f;
    private double mLatitudeMax = 0.0f;
    private double mLongitudeMin = 0.0f;
    private double mLongitudeMax = 0.0f;

    public CloudCamsessFilter(CameraStatus cameraStatus){
        mCameraStatus = cameraStatus;
    }

    public CloudCamsessFilter() {

    }
    public CloudCamsessFilter(int offset, int limit, CameraStatus cameraStatus){
        mOffset = offset;
        mLimit = limit;
        mCameraStatus = cameraStatus;
    };

    public int getOffset(){
        return mOffset;
    }

    public void setOffset(int offset){
        mOffset = offset;
    }

    public int getLimit(){
        return mLimit;
    }

    public void setLimit(int limit){
        mLimit = limit;
    }

    public void setStartLessThen(String startLessThen) {
        mStartLessThen = startLessThen;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setCamsessID(long val){
        mCamsessID = val;
    }

    public void setCameraOnline(CameraOnline cameraOnline) {
        mCameraOnline = cameraOnline;
    }

    public CameraOnline getCameraOnline() {
        return mCameraOnline;
    }

    public void setStreaming(CamsessStreaming camsessStreaming) {
        mCamsessStreaming = camsessStreaming;
    }

    public CamsessStreaming getStreaming() {
        return mCamsessStreaming;
    }

    public void setHasRecords(CamsessHasRecords camsessHasRecords) {
        mCamsessHasRecords = camsessHasRecords;
    }

    public CamsessHasRecords getHasRecords() {
        return mCamsessHasRecords;
    }

    public void setAuthorName(String authorName) {
        mAuthorName = authorName;
    }

    public String getAuthorName() {
        return mAuthorName;
    }

    public void setAuthorPreferredName(String authorName) {
        mAuthorPrefferedName = authorName;
    }

    public String getAuthorPreferredName() {
        return mAuthorPrefferedName;
    }

    public void setWithDetails(boolean moreDetails) {
        mMoreDetails = moreDetails;
    }

    public boolean getWithDetails() {
        return mMoreDetails;
    }

    public void setLatLngBounds(LatLngBounds bounds){
        mLatLngUpdated = true;

        mLatitudeMin = bounds.southwest.latitude;
        mLatitudeMax = bounds.northeast.latitude;
        mLongitudeMin = bounds.southwest.longitude;
        mLongitudeMax = bounds.northeast.longitude;
    }
    public void clearLatLngBounds(){
        mLatLngUpdated = false;
    }

    public ArrayList<Pair<String,String>> _getValues(){
        ArrayList<Pair<String,String>> params = new ArrayList<>();

        params.add(new Pair<>("offset", Integer.toString(mOffset)));
        params.add(new Pair<>("limit", Integer.toString(mLimit)));
        if(mCameraStatus == CameraStatus.CAMERA_STATUS_ACTIVE){
            params.add(new Pair<>("active", "true"));
        } else if(mCameraStatus == CameraStatus.CAMERA_STATUS_INACTIVE) {
            params.add(new Pair<>("active", "false"));
        }

        if(mCameraOnline != CameraOnline.ANY){
            params.add(new Pair<>("camera_online", mCameraOnline == CameraOnline.YES ? "true" : "false"));
        }

        if(mCamsessStreaming != CamsessStreaming.ANY){
            params.add(new Pair<>("streaming", mCamsessStreaming == CamsessStreaming.YES ? "true" : "false"));
        }

        if(mCamsessHasRecords != CamsessHasRecords.ANY){
            params.add(new Pair<>("has_records", mCamsessHasRecords == CamsessHasRecords.YES ? "true" : "false"));
        }

        if(mCamsessID > 0){
            params.add(new Pair<>("id", Long.toString(mCamsessID)));
        }

        params.add(new Pair<>("order_by", "-start")); // sorting by start

        if(mMoreDetails) {
            params.add(new Pair<>("detail", "detail")); // more details
        }

        if(mStartLessThen != null){
            params.add(new Pair<>("start__lte", mStartLessThen));
        }

        if(mTitle != null){
            // params.put("title__contains", mTitle); // case sensitive
            params.add(new Pair<>("title__icontains", mTitle)); // ignore case
        }

        if(mAuthorName != null){
            params.add(new Pair<>("author_name__icontains", mAuthorName));
        }

        if(mAuthorPrefferedName != null){
            params.add(new Pair<>("author_preferred_name__icontains", mAuthorPrefferedName));
        }

        // TODO: don't forget check situation when MAX < MIN (if it possible, of course)!!!!
        if(mLatLngUpdated && mLatitudeMin <= mLatitudeMax){
            params.add(new Pair<>("latitude__gte", Double.toString(mLatitudeMin)));
            params.add(new Pair<>("latitude__lte", Double.toString(mLatitudeMax)));
        }

        if(mLatLngUpdated && mLongitudeMin <= mLongitudeMax){
            params.add(new Pair<>("longitude__gte", Double.toString(mLongitudeMin)));
            params.add(new Pair<>("longitude__lte", Double.toString(mLongitudeMax)));
        }
        return params;
    }
}
