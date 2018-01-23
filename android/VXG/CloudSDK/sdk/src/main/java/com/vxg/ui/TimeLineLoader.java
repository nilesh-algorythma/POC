package com.vxg.ui;

import android.util.Pair;

import com.vxg.cloud.core.CloudCommon.CloudStorageRecord;
import com.vxg.cloudsdk.Interfaces.ICloudConnection;
import com.vxg.cloudsdk.Objects.CloudCamera;
import com.vxg.cloudsdk.Objects.CloudCameraList;
import com.vxg.cloudsdk.Objects.CloudCameraListFilter;
import com.vxg.cloudsdk.Objects.CloudTimeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Created by alexey on 14.09.17.
 */

public class TimeLineLoader {

    final long REMOVE_PAUSE_TIME=1000*30;
    final long UPDATE_TIME=1000*30;
    private CloudCamera camera;
    private  boolean loading=false;
    ArrayList<Pair<Long,Long>> all_intervals=new ArrayList<>();
    private HashMap<Long,ArrayList<Pair<Long,Long>>> video_intervals = new HashMap<>();
    private HashMap<Long,ArrayList<Event>> events=new HashMap<>();




    public CloudCamera getCam()
    {
        return camera;
    }

    static public final int EVENT_LOAD_FAILED=0;
    static public final int EVENT_LOAD_SUCCESS=1;
    static public final int EVENT_POS_UPDATE=2;

    static public final int EVENT_INTERVALS_UPDATE=3;

    final long H_TIME_MS =3600*1000;
    final long M_TIME_MS =60*1000;
    private long startTimeLine;
    private long firstLoadTime;

    public long get_cur_end() {
        Collections.sort(all_intervals,new ComComparator());

        return all_intervals.size()>0?all_intervals.get(all_intervals.size()-1).second:0;
    }

    private long cur_end;


    private List<Pair<Long,Long>> per;

    interface  TimeLineLoaderCallback
    {

       public void onLoaderEvent(int e);
    }
    TimeLineLoaderCallback callback;

    private Thread updateThread=
            new Thread()
            {
                @Override
                public void run()
                {
                    while (started) {
                        deleteDeadIntervals();
                        append();
                        callback.onLoaderEvent(EVENT_INTERVALS_UPDATE);
                        try {
                            Thread.sleep(UPDATE_TIME);
                        }
                        catch (InterruptedException e)
                        {
                            started=false;
                           break;
                        }
                    }


                }
            };
    private void deleteDeadIntervals()
    {
          if(camera!=null) {
              Pair<Integer, CloudStorageRecord> p0 = mConnection._getAPI().storageRecordFirst(camera.getID());
              if(p0.second!=null)
              {
                  long start_timeline = p0.second.getStart();
                  long st_time = start_timeline;
                  long cur_time = System.currentTimeMillis();

                  long left_boundary = start_time - start_time % H_TIME_MS - H_TIME_MS;
                  long right_boundary = cur_time - cur_time % H_TIME_MS;

                  while (left_boundary != right_boundary) {
                      ArrayList<Pair<Long, Long>> v = video_intervals.get(left_boundary);
                      Pair<Long, Long> p1 = null;
                      Long end = 0l;
                      if (v != null)
                          for (Pair<Long, Long> p : v) {

                              if (st_time > p.first && st_time < p.second) {

                                  p1 = p;
                                  end = p.second;
                                  break;
                              }

                          }

                      if (p1 != null) {
                          v.remove(p1);

                          Pair<Long, Long> p = new Pair<>(st_time, end);
                          v.add(p);
                          callback.onLoaderEvent(EVENT_INTERVALS_UPDATE);
                      }


                      left_boundary += H_TIME_MS;


                  }
              }
          }

    }


    public final long LOAD_PART_TIME =1000*3600*24;

    public long getCur_pos() {
        return cur_pos;
    }

    private long cur_pos=0;
    public long getStart_time() {
        return start_time;
    }

    private  long start_time;
    private boolean started=false;
    public long getEnd_time() {
        return end_time;
    }

    private  long end_time;
    ICloudConnection mConnection;

    public TimeLineLoader(TimeLineLoaderCallback c)

    {
        callback=c;
    }

    public CloudCamera getCamera() {
        return camera;
    }


    public void setCamera(CloudCamera cam)
    {
        camera=cam;
    }
    public void setConnection(ICloudConnection con)
    {
        mConnection=con;
    }
    public HashMap<Long, ArrayList<Pair<Long, Long>>> get_video_intervals() {
        return video_intervals;
    }
    public void loadEvents()
    {
        long t=System.currentTimeMillis();
        long ev_dis=1000*60;
        ArrayList<Event> ev_arr=new ArrayList<>();
        for(int i=0;i<10; i++)
        {
            Event.Type type=Event.Type.motion;
            Event e=new Event(type,t);
            ev_arr.add(e);
            t-=ev_dis;
        }


    }
    public void stop()
    {
        started=false;
        video_intervals.clear();
       // updateThread.interrupt();

    }
    private void append(){

        CloudTimeline cloudTimeline=null;
        if(camera!=null) {
            cloudTimeline = camera.getTimelineSync(end_time, System.currentTimeMillis());

            if (cloudTimeline != null && cloudTimeline.periods != null && cloudTimeline.periods.size() > 0) {

                put_intervals_to_hash_map(cloudTimeline.periods);
                cur_end= cloudTimeline.periods.get(cloudTimeline.periods.size()-1).second ;
                firstLoadTime = cloudTimeline.periods.get(cloudTimeline.periods.size() - 1).second;

                callback.onLoaderEvent(EVENT_LOAD_SUCCESS);


            }
        }


    }
    public void put_null_val()
    {



    }
    public boolean holeIntervalLoaded(long start,long end){


            boolean loaded = true;


        long left_boundary=start-start% H_TIME_MS;
        long right_boundary=end-end%H_TIME_MS;
        while(right_boundary!=left_boundary)
        {
            if(!video_intervals.containsKey(right_boundary)) {
                loaded = false;
                break;
            }

            right_boundary-=H_TIME_MS;


        }
        if(!loaded) {

            load_intervals(start,end);
        }
        return loaded;

    }
    public void load_intervals(final long st, final long end) {

        Thread loadThread=new Thread() {
            @Override
            public void run() {

                super.run();
                loading=true;
                boolean empty = false;

        if(video_intervals.size()==0)

            {
                start_time = System.currentTimeMillis();
                empty = true;
            }




                     CloudTimeline     cloudTimeline =  null;

                    if(camera!=null)
                    cloudTimeline =camera.getTimelineSync(st,end);









                if(cloudTimeline !=null&& cloudTimeline.periods!=null&& cloudTimeline.periods.size()>0) {

                    Collections.sort(cloudTimeline.periods, new ComComparator());
                    start_time = cloudTimeline.periods.get(0).first;

                    if(empty)
                        end_time= cloudTimeline.periods.get(cloudTimeline.periods.size()-1).second;

                    per= cloudTimeline.periods;
                    put_intervals_to_hash_map(cloudTimeline.periods);


                    callback.onLoaderEvent(EVENT_LOAD_SUCCESS);

                }
                else {


                    callback.onLoaderEvent(EVENT_LOAD_FAILED);
                }

                long left_boundary=st-st% H_TIME_MS;
                long right_boundary=end-end% H_TIME_MS;
                while(left_boundary!=right_boundary)
                {
                    if(!video_intervals.containsKey(left_boundary))
                        video_intervals.put(left_boundary,new ArrayList<Pair<Long, Long>>());
                    left_boundary+=H_TIME_MS;
                }
                if(!video_intervals.containsKey(right_boundary))
                    video_intervals.put(right_boundary,new ArrayList<Pair<Long, Long>>());
                loading=false;
        }
        };

        if(!loading)
            loadThread.start();

        if(!started)
        {
            firstLoadTime=end_time;
            cur_pos=end_time;
           //updatePosThread.start();
            //removeDeadIntervalsThread.start();
            cur_end=end_time;
            new Thread()
            {
                @Override
                public void run()
                {
                    while (started) {
                        deleteDeadIntervals();
                        append();
                        callback.onLoaderEvent(EVENT_INTERVALS_UPDATE);
                        try {
                            Thread.sleep(UPDATE_TIME);
                        }
                        catch (InterruptedException e)
                        {
                            started=false;
                            break;
                        }
                    }


                }
            }.start();

            started=true;


        }

    };
    private synchronized boolean isSuchPer(Pair<Long,Long> p, ArrayList<Pair<Long,Long>> arr){
        boolean found=false;

            for (Pair<Long, Long> p1 : arr)
                if (p1.first == p.first && p1.second == p.second) {
                    found = true;
                    break;
                }

        return  found;
    }
    private void appendAr(ArrayList<Pair<Long,Long>> appendTo,ArrayList<Pair<Long,Long>> appendIx)
    {
        for(Pair<Long,Long> p:appendIx)
            if(!isSuchPer(p,appendTo))
            appendTo.add(p);


    }
    private void put_to_intervals(long key,ArrayList<Pair<Long,Long>> val)
    {
        if(!video_intervals.containsKey(key))
            video_intervals.put(key,val);
        else
        {
            ArrayList<Pair<Long,Long>> val1= video_intervals.get(key);
            appendAr(val1,val);
        }
        appendAr(all_intervals,val);

    }
    public void put_intervals_to_hash_map(List<Pair<Long,Long>> periods)
    {

        Collections.sort(periods,new ComComparator());
        for(Pair<Long,Long> period: periods)
        {

            //find left boundary of  period start

            long left_boundary_st=period.first-period.first% H_TIME_MS;
            long right_boundary_st=left_boundary_st+ H_TIME_MS;
            long left_boundary_end=period.second-period.second% H_TIME_MS;

            if(left_boundary_st==left_boundary_end)
            {
                ArrayList<Pair<Long,Long>> val=new ArrayList<>();
                val.add(period);
                //if(!video_intervals.containsKey(left_boundary_st))
                    put_to_intervals(left_boundary_st,val);






            }

            else {

                //find boundaries between right boundaries
                ArrayList<Long> medBound = new ArrayList<>();
                long t_b =right_boundary_st;
                while (t_b != left_boundary_end) {
                    t_b +=H_TIME_MS;
                    if (t_b != left_boundary_end) {
                        medBound.add(t_b);
                    }

                }

                ArrayList<Pair<Long,Long>> left_boundary_st_val=new ArrayList<>();
                ArrayList<Pair<Long,Long>> right_boundary_st_val=new ArrayList<>();
                ArrayList<Pair<Long,Long>> left_boundary_end_val=new ArrayList<>();

                left_boundary_st_val.add(period);
                //video_intervals.put(left_boundary_st,left_boundary_st_val);
                put_to_intervals(left_boundary_st,left_boundary_st_val);
                right_boundary_st_val.add(period);
                //video_intervals.put(right_boundary_st,right_boundary_st_val);
                put_to_intervals(right_boundary_st,right_boundary_st_val);

                if(medBound.size()>0)
                {

                    left_boundary_end_val.add(period);
                   // video_intervals.put(left_boundary_end,left_boundary_end_val);
                    put_to_intervals(left_boundary_end,left_boundary_end_val);
                    for(Long l:medBound)
                    {

                        ArrayList<Pair<Long,Long>> v=new ArrayList<>();
                        v.add(period);
                        put_to_intervals(l,v);
                        //video_intervals.put(l,v);
                    }


                }





            }


        }



    }

    class ComComparator implements Comparator<Pair<Long,Long>>
    {
        public int compare(Pair<Long,Long> a,Pair<Long,Long> b)
        {
            if(a.first>b.first)
                return 8;
            else if (a.first<b.first)
                return -8;
            else return 0;
        }
        ComComparator(){;}
    }





}
