package com.vxg.cloudsdk.Objects.Playback;


import com.vxg.cloudsdk.Helpers.MLog;

public class PlaybackSegment
{
    private static final String TAG = "PlaybackSegment";
    final public static int LOG_LEVEL = 2; //Log.VERBOSE;
    static MLog Log = new MLog(TAG, LOG_LEVEL);

    private String name = "";
    private String url = "";
    private Long id = 0L;

    private Long startTime = 0L;
    private Long stopTime = 0L;
    private Long durationTime = 0L;
    //private Long timelineStartTime = 0L;

    private Player owner = null;

    public PlaybackSegment(final String name, final String url, final Long id,
                           final Long startTime, final Long stopTime, final Long durationTime)
    {
        this.name = name;
        this.url = url;
        this.id = id;

        this.startTime = startTime;
        this.stopTime = stopTime;
        this.durationTime = durationTime;
        //this.timelineStartTime = 0L;
        this.owner = null;
    }

    synchronized public Player getOwner() { return this.owner; }
    synchronized public void setOwner(final Player owner) { this.owner = owner; }

    public String getName() { return this.name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return this.url; }
    public void setUrl(String url) { this.url = url; }

    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }

    public Long getStartTime() { return this.startTime; }
    public void setStartTime(Long startTime) { this.startTime = startTime; }

    public Long getStopTime() { return this.stopTime; }
    public void setStopTime(Long stopTime) { this.stopTime = stopTime; }

    public Long getDurationTime() { return this.durationTime; }
    public void setDurationTime(Long durationTime) { this.durationTime = durationTime; }

    //public Long getTimelineStartTime() { return this.timelineStartTime; }
    //public void setTimelineStartTime(Long timelineStartTime) { this.timelineStartTime = timelineStartTime; }

}
