package com.vxg.cloudsdk.Objects.Playback;


import android.content.Context;

import com.vxg.cloud.core.CloudHelpers;
import com.vxg.cloudsdk.Helpers.MLog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PlaybackTimeline
{
    private static final String TAG = "PlaybackTimeline";
    final public static int LOG_LEVEL = 2; //Log.VERBOSE;
    static MLog Log = new MLog(TAG, LOG_LEVEL);

    // members
    private Context context = null;
    private ArrayList<PlaybackSegment> segments = new ArrayList<>();
    private PlaybackSegment currentSegment = null;
    private Long duration = 0L;

    private PlaybackTimeline() {}
    public PlaybackTimeline(final Context context){ this.context = context; duration = 0L; }

    // Segments methods
    public void addSegment(final PlaybackSegment segment)
    {
        //segment.setTimelineStartTime(duration);

        int index = 0;
        for(PlaybackSegment seg : segments){

            if(seg.getStartTime().longValue() == segment.getStartTime().longValue() && seg.getStopTime().longValue() == seg.getStopTime().longValue()){
                Log.v("<=addSegment segment exists");
                return;
            }

            if( segment.getStartTime().longValue() >= seg.getStopTime().longValue() ){
                index = 1+ segments.indexOf(seg);
            }
        }

        segments.add(index, segment);
        duration += segment.getDurationTime();
    }
    public void removeSegment(final PlaybackSegment segment)
    {
        segments.remove(segment);
        duration -= segment.getDurationTime();
    }

    public PlaybackSegment getNextSegment(final PlaybackSegment segment, boolean makeCurrent)
    {
        if (segments.size() <= 0)
            return (makeCurrent ? (currentSegment = null) : null);


        if (segment == null || segments.indexOf(segment) == -1)
        {
            return (makeCurrent ? (currentSegment = segments.get(0)) : segments.get(0));
        }

        if (segments.indexOf(segment) == (segments.size() - 1))
            return (makeCurrent ? (currentSegment = null/*segments.get(0)*/) : null/*segments.get(0)*/);

        return  (makeCurrent ? (currentSegment = segments.get(segments.indexOf(segment) + 1)) :
                                segments.get(segments.indexOf(segment) + 1));

    }

    public PlaybackSegment getCurrentSegment(){ return currentSegment; }
    public void setCurrentSegment(final PlaybackSegment segment){
        currentSegment = segment;
        String ss = "setCurrentSegment owner="+segment.getOwner()+" ["+ CloudHelpers.formatTime(segment.getStartTime())+", "+CloudHelpers.formatTime(segment.getStopTime())+"]; ";
        Log.v(ss);
    }


    public void clearAll()
    {
        segments.clear();
        duration = 0L;
        currentSegment = null;
    }

    // Timeline methods
    public Long getStartTime()
    {
        if (segments.size() <= 0)
            return 0L;

        return segments.get(0).getStartTime();
    }

    public String getStartTimeAsString()
    {
        if (segments.size() <= 0)
            return "00:00:00";

        return new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date(getStartTime()));
    }

    public Long getStopTime()
    {
        if (segments.size() <= 0)
            return 0L;

        return segments.get(segments.size() - 1).getStartTime();
    }

    public String getStopTimeAsString()
    {
        if (segments.size() <= 0)
            return "00:00:00";

        return new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date(getStopTime()));
    }

    public Long getDuration()
    {
        return duration;
    }

    public Long getCurrentTime()
    {
        //Log.v(TAG, "getCurrentTime: segment " + getCurrentSegment() + ", owner: " + getCurrentSegment().getOwner());
        if (getCurrentSegment() == null ||
                getCurrentSegment().getOwner() == null)
            return -1L;

        long pos =  getCurrentSegment().getOwner().getStreamPosition();

        //Log.v(TAG, "getCurrentTime: relative position " + pos + ", segment: " + getCurrentSegment().getDurationTime());
        if (pos <= 0L)
            return -1L;

        //Log.v(TAG, "getCurrentTime: absolute position " + (getCurrentSegment().getTimelineStartTime() + pos) + ", absolute duration: " + getDuration());
        return getCurrentSegment().getStartTime().longValue() + pos;
    }

    public void setCurrentTime(final Long newTime)
    {
        if (getCurrentSegment() == null ||
                getCurrentSegment().getOwner() == null)
            return;

        getCurrentSegment().getOwner().setStreamPosition(newTime);
    }

    public List<Map.Entry<Long, Long>> getSegmentsTimes() { return new ArrayList<>(); }

    public ArrayList<PlaybackSegment> getSegments() { return segments; }

    public PlaybackSegment getSegmentByTime(final Long time)
    {
        if (getSegments().size() <= 0)
            return null;

        int last = getSegments().size() - 1;
        if (time.longValue() < getSegments().get(0).getDurationTime().longValue() || last <= 0)
        {
            Log.v("getSegmentByTime: time " + time + ", segment: " + 0);
            return getSegments().get(0);
        }

        if (time.longValue() > getSegments().get(last).getStartTime().longValue())
        {
            Log.v("getSegmentByTime: time " + time + ", segment: " + last);
            return getSegments().get(last);
        }

        for (int i = 0; i < getSegments().size(); i++)
        {
            if (time.longValue() > getSegments().get(i).getStartTime().longValue() &&
                    time.longValue() < (getSegments().get(i).getStartTime().longValue() + getSegments().get(i).getDurationTime().longValue()))
            {
                Log.v("getSegmentByTime: time " + time + ", segment: " + i);
                return getSegments().get(i);
            }
        }

        Log.v("getSegmentByTime: time " + time + ", segment: " + null);
        return null;
    }

    public Long setCurrentTimeBySegment(final PlaybackSegment segment) { return 0L; }

}
