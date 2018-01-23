package com.vxg.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatSeekBar;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;


import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.vxg.cloudsdk.Enums.CloudPlayerEvent;
import com.vxg.cloudsdk.Interfaces.ICloudConnection;
import com.vxg.cloudsdk.Interfaces.ICloudObject;
import com.vxg.cloudsdk.Objects.CloudCamera;
import com.vxg.cloudsdk.Objects.CloudPlayer;
import com.vxg.cloudsdk.Interfaces.ICloudPlayerCallback;
import com.vxg.cloudsdk.Objects.CloudTrialConnection;
import com.vxg.cloudsdk.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import veg.mediaplayer.sdk.MediaPlayer;

/**
 * Created by alexey on 31.08.17.
 */

public class TimeLineSet extends FrameLayout {

    public TimeLine getTimeLine() {
        return t;
    }

    private TimeLine t;
    ProgressBar tPr;
    ProgressBar arr1Pr;
    ProgressBar arr2Pr;
    FrameLayout tLoadingBackground;
    private int timeLineHeightDp = 50;
    private int timeLineArrowsWidthDp = 30;
    private int scale_line_offDp = 15;
    MaterialCalendarView calendar;
    Button calButt;
    public static boolean calVisible = false;


    static int t_s;

    public class TimeLine extends AppCompatSeekBar implements TimeLineLoader.TimeLineLoaderCallback, ICloudPlayerCallback {
        long userClickedPerStart;
        long userClickedPerEnd;
        int posAnimationSpeedDp=2;
        int posAnimationDisPx;
        boolean seekCompleted = true;
        final long UPDATE_PAUSE_TIME = 1000*3;
        boolean userPressedCalDay = false;
        long cur_offset;
        boolean started;
        ProgressBar player_progress;
        final float FUTURE_OFFSET = 0.5f;
        ArrayList<Integer> recordedDays = new ArrayList<>();

        public void setPlayer(CloudPlayer player) {
            this.player = player;
            player.addCallback(this);

        }
        public void setPlayerProgress(ProgressBar pr) {
            player_progress=pr;

        }
        private CloudPlayer player = null;

        public void getPlayer(CloudPlayer p) {
            player = p;
        }

        private String background = "#000000";
        private String videoIntervalsColor = "#56CBF1";
        private String scaleColor = "#ffffff";
        private Context mContext;
        HashMap<Long, ArrayList<Pair<Long, Long>>> video_intervals = new HashMap<>();
        ArrayList<Pair<Long, Long>> visible_video_intervals = new ArrayList<>();
        List<Pair<Long, Long>> periods = new ArrayList<>();
        TimeLineLoader loader;
        public final int TYPE_MIN = 3;
        public final int TYPE_12H = 2;
        public final int TYPE_H = 1;
        int cur_type = TYPE_MIN;
        long t_visible_start = 0;
        long t_visible_end = 0;
        private boolean firstLoad = true;
        long t_start;
        long t_end;
        long thumb_time;
        int thumbWidthDp = 8;
        int recordWidthDp = 4;
        int scaleWidthDp = 2;
        int textSizeDp = 10;
        public boolean moved = false;
        float visible12hHours = 22f;
        public int delta_x;
        final public int MOVE_BY_TOUCH_LIMIT = 20;
        int animationDuration = 5;
        int animationDurationH = 5;
        int fps = 30;
        boolean moveLeftArr = false;
        boolean moveRightArr = false;

        float mPreviousX = -333333;
        float mPreviousY = -333333;
        boolean userChangedPos = false;
        int speed12h = 1;
        int acc12HPx = 1;
        int acc1HPx = 1;
        int speed1h = 1;
        int distance12hPx;
        int distance1hPx;
        boolean animation = false;
        int dis12H = 12;
        boolean ac = true;
        int MAX_SPEED;
        int scaleW12hPx;
        final long H_TIME_MS = 3600 * 1000;
        final long M_TIME_MS = 60 * 1000;
        ArrayList<Line> scaleLines12h = new ArrayList<>();
        ArrayList<Line> scaleLines = new ArrayList<>();
        Paint p;

        @Override
        public void onStatus(final CloudPlayerEvent player_event, final ICloudObject p) {


            switch (player_event) {
                case CONNECTING:

                    break;
                case CONNECTED:
//                case STARTED:
//                case PAUSED:
                    mHandler.sendEmptyMessage(2);

                    start();
                    break;
                case CLOSED:
                    mHandler.sendEmptyMessage(3);
                    stop();
                    break;
                case EOS:

                    //setUIDisconnected();
                    break;
                case SEEK_COMPLETED:
                    playerSeekCompleted();
                    break;
                case ERROR:

                    break;
                case TRIAL_VERSION:

                    break;
            }
        }


        private Thread updatePosThread = new Thread() {
            @Override
            public void run() {
                while (started) {
                    if (player != null && seekCompleted) {
                        thumb_time = player.getPosition();
//                        if(checkThumb())
//                            startPosAnimation();
                    }

                    mHandler.sendEmptyMessage(0);
                    try {
                        Thread.sleep(UPDATE_PAUSE_TIME);
                    } catch (InterruptedException e) {
                    }

                }
            }
        };

        public void showArr1Pr() {
            arr1Pr.setVisibility(VISIBLE);
        }

        public void showArr2Pr() {
            arr2Pr.setVisibility(VISIBLE);
        }

        public void hideArr1Pr() {
            arr1Pr.setVisibility(INVISIBLE);
        }

        public void hideArr2Pr() {
            arr2Pr.setVisibility(INVISIBLE);
        }

        public android.os.Handler mHandler = new android.os.Handler() {

            @Override
            public void handleMessage(Message msg) {
                if(msg.what==1) {
                    hidePr();

                }
                if(msg.what==2) {
                    TimeLineSet.this.setVisibility(VISIBLE);
                }
                if(msg.what==3) {
                    TimeLineSet.this.setVisibility(GONE);
                }

                if(msg.what==4) {
                    player_progress.setVisibility(INVISIBLE);
                }
                hideArr1Pr();
                hideArr2Pr();
                invalidate();


            }
        };

        class Point {
            int x;
            int y;

            public Point(int x, int y) {
                this.x = x;
                this.y = y;
            }
        }

        class Line {

            public int type;

            long start_time;
            long end_time;


            public String time = "";

            public Line(Point start, Point end) {
                this.start = start;
                this.end = end;

            }

            Point start;
            Point end;
        }


        public TimeLine(Context c) {
            super(c);
            setWillNotDraw(false);
            mContext = c;
            p = new Paint();
            loader = new TimeLineLoader(this);
            t_visible_end = System.currentTimeMillis();


        }

        public TimeLine(Context c, AttributeSet attr) {
            super(c, attr);
            setWillNotDraw(false);
            mContext = c;
            p = new Paint();
            loader = new TimeLineLoader(this);
            t_visible_end = System.currentTimeMillis();
            // loader.setCamera((CloudCamera)player.getSource());
            // loader.setConnection(loader.getCam()._getCloudConnection());

        }

        //load video intervals and events from server
        public void hidePr() {
            tLoadingBackground.setVisibility(INVISIBLE);
            tPr.setVisibility(INVISIBLE);
        }

        ;

        public void showPr() {
            tLoadingBackground.setVisibility(VISIBLE);
            tPr.setVisibility(VISIBLE);
        }

        ;

        @Override
        public void onLoaderEvent(int e) {

            switch (e) {
                case TimeLineLoader.EVENT_LOAD_SUCCESS:
                    video_intervals = loader.get_video_intervals();
                    t_start = loader.getStart_time();
                    t_end = loader.getEnd_time();
                    if (firstLoad) {
                        t_visible_end = t_end;
                        started = true;
                        firstLoad = false;

                            new Thread() {
                                @Override
                                public void run() {
                                    while (started) {
                                        if (player != null && seekCompleted) {
                                            thumb_time = player.getPosition();
//                        if(checkThumb())
//                            startPosAnimation();
                                        }

                                        mHandler.sendEmptyMessage(0);
                                        try {
                                            Thread.sleep(UPDATE_PAUSE_TIME);
                                        } catch (InterruptedException e) {
                                        }

                                    }
                                }
                            }.start();
                      //  init();
                    }

                    if (userPressedCalDay) {
                        userPressedCalDay = false;
                        Pair<Long,Long> p=getFirstPeriod(userClickedPerStart,userClickedPerEnd);
                        if(p!=null) {
                            setVisbleEnd(p.second - cur_offset);


                        }
                        mHandler.sendEmptyMessage(1);


                    }
                    else
                    mHandler.sendEmptyMessage(0);
                    break;
                case TimeLineLoader.EVENT_POS_UPDATE:
                    thumb_time = loader.getCur_pos();
                    mHandler.sendEmptyMessage(0);
                    break;
                case TimeLineLoader.EVENT_INTERVALS_UPDATE:
                    video_intervals = loader.get_video_intervals();
                    t_end = loader.get_cur_end();
                    mHandler.sendEmptyMessage(0);
                    break;
            }
        }

        public void setCamera(CloudCamera cam) {
            loader.setCamera(cam);
        }

        public void setConnection(ICloudConnection con) {
            loader.setConnection(con);
        }


        class ComComparator implements Comparator<Pair<Long, Long>> {
            public int compare(Pair<Long, Long> a, Pair<Long, Long> b) {
                if (a.first > b.first)
                    return 8;
                else if (a.first < b.first)
                    return -8;
                else return 0;
            }

            ComComparator() {
                ;
            }
        }

        public void select_visible_intervals() {

            long left_cash_arr_key = t_visible_start - t_visible_start % H_TIME_MS;
            long right_cash_arr_key = t_visible_end - t_visible_end % H_TIME_MS + H_TIME_MS;
            long t_key = left_cash_arr_key;
            while (t_key != right_cash_arr_key) {
                t_key += H_TIME_MS;
                ArrayList<Pair<Long, Long>> v = video_intervals.get(t_key);
                if (v != null)
                    for (Pair<Long, Long> p : v) {
                        if (!visible_video_intervals.contains(p))
                            visible_video_intervals.add(p);
                    }


            }
            ;
        }

        public void put_intervals_to_hash_map(List<Pair<Long, Long>> periods) {
            video_intervals.clear();
            int arrsQuantity = (int) ((t_end - t_start) / (3600 * 1000)) + 1;
            Collections.sort(periods, new ComComparator());
            for (Pair<Long, Long> period : periods) {

                //find left boundary of  period start

                long left_boundary_st = period.first - period.first % H_TIME_MS;
                long right_boundary_st = left_boundary_st + H_TIME_MS;
                long left_boundary_end = period.second - period.second % H_TIME_MS;

                if (left_boundary_st == left_boundary_end) {
                    ArrayList<Pair<Long, Long>> val = new ArrayList<>();
                    val.add(period);

                    video_intervals.put(left_boundary_st, val);


                } else {

                    //find boundaries between right boundaries
                    ArrayList<Long> medBound = new ArrayList<>();
                    long t_b = right_boundary_st;
                    while (t_b != left_boundary_end) {
                        t_b += H_TIME_MS;
                        if (t_b != left_boundary_end) {
                            medBound.add(t_b);
                        }

                    }

                    ArrayList<Pair<Long, Long>> left_boundary_st_val = new ArrayList<>();
                    ArrayList<Pair<Long, Long>> right_boundary_st_val = new ArrayList<>();
                    ArrayList<Pair<Long, Long>> left_boundary_end_val = new ArrayList<>();

                    left_boundary_st_val.add(period);
                    video_intervals.put(left_boundary_st, left_boundary_st_val);
                    right_boundary_st_val.add(period);
                    video_intervals.put(right_boundary_st, right_boundary_st_val);


                    if (medBound.size() > 0) {

                        left_boundary_end_val.add(period);
                        video_intervals.put(left_boundary_end, left_boundary_end_val);

                        for (Long l : medBound) {

                            ArrayList<Pair<Long, Long>> v = new ArrayList<>();
                            v.add(period);
                            video_intervals.put(l, v);
                        }


                    }


                }


            }

        }

        public void loadVideo(long start) {
            player.setPosition(start);
            player_progress.setVisibility(VISIBLE);
        }

        public int getNearest1hMin(int min) {

            return min - min % 15;

        }

        public int getNearest12hHour(int h1) {
            ArrayList<Integer> hours = new ArrayList<>();

            hours.add(1);
            hours.add(4);
            hours.add(7);
            hours.add(10);
            hours.add(13);
            hours.add(16);
            hours.add(19);
            hours.add(22);

            int dif = 50;
            int seachedH = 50;

            for (int h : hours) {

                if (h1 >= h) {
                    if (dif > h1 - h)
                        seachedH = h;
                    dif = h1 - h;
                }
            }

            return seachedH;
        }

        String getMonthName(int m) {
            String name = "";
            switch (m) {
                case 0:
                    name = "Jan";
                    break;
                case 1:
                    name = "Feb";
                    break;
                case 2:
                    name = "Ma";
                    break;
                case 3:
                    name = "Ap";
                    break;
                case 4:
                    name = "May";
                    break;
                case 5:
                    name = "Jun";
                    break;
                case 6:
                    name = "Jul";
                    break;
                case 7:
                    name = "Aug";
                    break;
                case 8:
                    name = "Sep";
                    break;
                case 9:
                    name = "Oc";
                    break;
                case 10:
                    name = "Nov";
                    break;
                case 11:
                    name = "Dec";
                    break;
            }
            return name;
        }

        public void init1h() {
            distance1hPx = 900;


        }

        public void init12h() {

            float d = mContext.getResources().getDisplayMetrics().density;
            animationDuration = (int) (animationDurationH * d / 3.0);
            if (d < 3.0f)
                animationDuration += 2;

            int w = getWidth();
            scaleW12hPx = (int) (w * 1 / visible12hHours) * 3;
            //screen timeline start in ms

            if (speed12h <= 0)
                speed12h = 1;
            if (acc12HPx == 0)
                acc12HPx = 1;
            distance12hPx = scaleW12hPx / 3 * dis12H;
            int S = distance12hPx / 2;
            int v_0 = speed12h;

            int t = animationDuration / 2;
            int acc_sec = 2 * (S - v_0 * t * 1) / (t * t);
            acc12HPx = acc_sec / fps;
            MAX_SPEED = v_0 + acc_sec * t;
            scaleLines12h.clear();
            //in hours
            int scaleValue = 3;

            scaleLines12h.add(new Line(new Point((int) (w - (t_end - t_start) / (3600 * 1000) * scaleW12hPx / scaleValue), getBottom() - (int) pxFromDp(scale_line_offDp)), new Point(getWidth(), getBottom() - (int) pxFromDp(scale_line_offDp))));
            // hoursOffFromEnd+=scaleValue;


            p.setColor(Color.parseColor(scaleColor));
        }

        public void start() {
            mHandler.sendEmptyMessage(2);



            init();
            loader.setCamera((CloudCamera) player.getSource());
            loader.setConnection(loader.getCam()._getCloudConnection());
            loader.load_intervals(System.currentTimeMillis() - loader.LOAD_PART_TIME, System.currentTimeMillis());
            started = true;
        }

        public void stop() {
            firstLoad=true;
            started = false;
            mHandler.sendEmptyMessage(3);
            loader.stop();
        }

        public void init() {
            //loadData();
//            if(speed12h <=0)
//                speed12h =1;
//            if(acc12HPx ==0)
//                acc12HPx =1;
//            distance12hPx = scaleW12hPx/3 * dis12H;
//            int S= distance12hPx /2;
//            int v_0= speed12h;
//
//            int t=animationDuration/2;
//            int acc_sec=2*(S-v_0*t*1)/(t*t);
//            acc12HPx =acc_sec/fps;
//            MAX_SPEED=v_0+acc_sec*t;
//            for(int i = 0; i< MAX_SCALE_LINE_QUANTITY; i++) {
//
//                scaleLines.add(new Line(new Point((-2000),getBottom()-(int)pxFromDp(scale_line_offDp)), new Point(-2000, getBottom()-(int)pxFromDp(scale_line_offDp))));
//            }
            //recordedDays.clear();
            //  recordedDays.add()
            p = new Paint();
           // t_visible_end = System.currentTimeMillis();
            if (periods != null) {
                init12h();
                init1h();


            }

            mHandler.sendEmptyMessage(0);

        }

        protected float pxFromDp(float dp) {
            return (dp * mContext.getResources().getDisplayMetrics().density);
        }
        private boolean isThumbVisible()
        {
            return thumb_time>t_visible_start+cur_offset&&thumb_time<t_visible_end+cur_offset;
        }
        public boolean checkThumb()
        {
            if(isThumbVisible())
            {

                if((float)(thumb_time-t_visible_start-cur_offset)/(float)(t_visible_end-t_visible_start)>=0.8f)
                    return true;
                else
                    return false;
            }
            else
                return false;


        }
        public void startPosAnimation() {

            long end_pos = (long) ((t_visible_end - t_visible_start) * 0.2f);
            final long time_dis = thumb_time-t_visible_start-cur_offset - end_pos;
            final long dis = 0;
            Thread posAnim = new Thread() {
                @Override
                public void run() {
                    long dis1=0;
                    while (dis1 < time_dis) {
                        t_visible_end += (time_dis / 25);
                        dis1+= (time_dis / 25);
                        mHandler.sendEmptyMessage(0);
                        try {
                            Thread.sleep(40);
                        } catch (InterruptedException e) {

                            break;
                        }
                    }
                }
            }
                ;
            if(!animation)
                        posAnim.start();

        }
        public void moveByArrow() {
            if (animation)
                return;

            new Thread() {
                @Override
                public void run()

                {
                    animation = true;
                    while (animation) {
                        switch (cur_type) {
                            case TYPE_12H:
                                move12h();
                                break;
                            case TYPE_H:
                                move1h();
                            case TYPE_MIN:
                                moveM();
                                break;

                        }
                        t.mHandler.sendEmptyMessage(0);
                        try {
                            Thread.sleep(1000 / fps);
                        } catch (InterruptedException e) {
                        }


                    }

                    animation = false;
                    moveLeftArr = false;
                    moveRightArr = false;
                }
            }.start();


        }

        public void moveRightByArrow() {


        }

        public void moveM() {
            if (t_s < distance1hPx / 2 && ac) {
                speed1h += acc1HPx;

                t_s += speed1h;
            } else if (t_s >= 240 && ac) {
                ac = false;
                int y = t_s;
                t_s = 0;
            }
            if (t_s < distance1hPx / 2 && !ac && speed1h > 0) {
                speed1h -= acc1HPx;
                t_s += speed1h;
            } else if (t_s >= distance1hPx / 2 || speed1h <= 0) {
                animation = false;
                t_s = 0;
                speed1h = 1;
                ac = true;
            }


            if (speed1h > 0 && t_s < distance1hPx / 2) {
                long step = (long) ((float) speed1h / (float) (scaleW12hPx / 3) * (float) H_TIME_MS);
                //step=H_TIME_MS/60;
                step = H_TIME_MS / 300;
                if (moveLeftArr) {

                    //  if(t_visible_start-step>t_start) {
                    t_visible_end -= step;
                    t_visible_start -= step;
                    //  }
//                    else
//                    {
//                        long cash=t_visible_start;
//                        t_visible_start=t_start;
//                        t_visible_end-=(cash-t_start);
//                        animation =false;
//                    }

                } else {
                    //   if(t_visible_end+step<t_end) {

                    t_visible_end += step;
                    t_visible_start += step;
                    // }
//                    else
//                    {
//                        if(t_visible_end<t_end) {
//                            long cash = t_visible_end;
//                            t_visible_end = t_end;
//                            t_visible_start += (t_end - cash);
//                        }
//                        animation =false;
//                    }

                }

            }
        }

        public void move1h() {


            if (t_s < distance1hPx / 2 && ac) {
                speed1h += acc1HPx;

                t_s += speed1h;
            } else if (t_s >= 240 && ac) {
                ac = false;
                int y = t_s;
                t_s = 0;
            }
            if (t_s < distance1hPx / 2 && !ac && speed1h > 0) {
                speed1h -= acc1HPx;
                t_s += speed1h;
            } else if (t_s >= distance1hPx / 2 || speed1h <= 0) {
                animation = false;
                t_s = 0;
                speed1h = 1;
                ac = true;
            }


            if (speed1h > 0 && t_s < distance1hPx / 2) {
                long step = (long) ((float) speed1h / (float) (scaleW12hPx / 3) * (float) H_TIME_MS);
                //step=H_TIME_MS/60;
                step = H_TIME_MS / 30;
                if (moveLeftArr) {

                    //  if(t_visible_start-step>t_start) {
                    t_visible_end -= step;
                    t_visible_start -= step;
                    //   }
//                    else
//                    {
//                        if(t_visible_start>t_start) {
//                            long cash = t_visible_start;
//                            t_visible_start = t_start;
//                            t_visible_end -= (cash - t_start);
//                        }
//                        loader.load_intervals();
//                        animation =false;
//                    }

                } else {
                    //  if(t_visible_end+step<t_end) {

                    t_visible_end += step;
                    t_visible_start += step;
                    //   }
//                    else
//                    {
//                        long cash=t_visible_end;
//                        t_visible_end=t_end;
//                        t_visible_start+=(t_end-cash);
//                        animation =false;
//                    }

                }

            }


//
//            for(Line l: video_intervals12h) {
//                if(speed12h >0&&t_s< distance12hPx /2) {
//                    if(moveLeftArr) {
//                        l.start.x += speed12h;
//
//                        l.end.x += speed12h;
//                    } else
//                    {
//                        l.start.x -= speed12h;
//
//                        l.end.x -= speed12h;
//                    }
//
//                }
//
//
//            }

        }

        public void move12h() {
            if (t_s < distance12hPx / 2 && ac) {
                speed12h += acc12HPx;

                t_s += speed12h;
            } else if (t_s >= 240 && ac) {
                ac = false;
                int y = t_s;
                t_s = 0;
            }
            if (t_s < distance12hPx / 2 && !ac && speed12h > 0) {
                speed12h -= acc12HPx;
                t_s += speed12h;
            } else if (t_s >= distance12hPx / 2 || speed12h <= 0) {
                animation = false;
                t_s = 0;
                speed12h = 1;
                ac = true;
            }


            if (speed12h > 0 && t_s < distance12hPx / 2) {
                long step = (long) ((float) speed12h / (float) (scaleW12hPx / 3) * (float) H_TIME_MS);
                //step=H_TIME_MS/60;
                step = H_TIME_MS;
                if (moveLeftArr) {

                    //  if(t_visible_start-step>t_start) {
                    t_visible_end -= step;
                    t_visible_start -= step;
                    //  }
//                         else
//                         {
//                             if(t_visible_start>t_start) {
//                                 long cash = t_visible_start;
//                                 t_visible_start = t_start;
//                                 t_visible_end -= (cash - t_start);
//                             }
//                                 loader.load_intervals();
//
//                                 animation = false;
//
//                         }

                } else {
                    // if(t_visible_end+step<t_end) {

                    t_visible_end += step;
                    t_visible_start += step;
                    //  }
//                         else
//                         {
//                             if(t_visible_end<t_end) {
//                                 long cash = t_visible_end;
//                                 t_visible_end = t_end;
//                                 t_visible_start += (t_end - cash);
//                             }
//                             animation =false;
//                         }

                }

            }


//
//            for(Line l: video_intervals12h) {
//                if(speed12h >0&&t_s< distance12hPx /2) {
//                    if(moveLeftArr) {
//                        l.start.x += speed12h;
//
//                        l.end.x += speed12h;
//                    } else
//                    {
//                        l.start.x -= speed12h;
//
//                        l.end.x -= speed12h;
//                    }
//
//                }
//
//
//            }

        }

        private Pair<Long, Long> getFirstPeriod(long start, long end) {
            Pair<Long, Long> p = null;
            long left_cash_arr_key = start - start % H_TIME_MS;
            long right_cash_arr_key = end - end % H_TIME_MS;
            long t_key = left_cash_arr_key;
            while (t_key <= right_cash_arr_key) {

                ArrayList<Pair<Long, Long>> v = video_intervals.get(t_key);
                if (v != null && v.size() > 0) {
                    p = v.get(0);
                    Calendar cal=Calendar.getInstance();
                    cal.setTimeInMillis(p.second);
                    int d1=cal.get(Calendar.DAY_OF_MONTH);
                    cal.setTimeInMillis(start);
                    int d2=cal.get(Calendar.DAY_OF_MONTH);


                        if (d2 != d1) {
                            cal.set(Calendar.DAY_OF_MONTH, d2);
                            cal.set(Calendar.HOUR_OF_DAY, 23);

                            p = new Pair<Long,Long>(0l,cal.getTimeInMillis());
                        }

                    break;
                }

                t_key += H_TIME_MS;

            }
            ;

            return p;

        }

        private Line makeHorScaleLine() {
            return new Line(new Point(0, getBottom() - (int) pxFromDp(scale_line_offDp) - (int) pxFromDp(scale_line_offDp / 2)), new Point(getWidth(), getBottom() - (int) pxFromDp(scale_line_offDp) - (int) pxFromDp(scale_line_offDp / 2)));

        }

        private Line makeVerScaleLine(long time, long off) {
            int x = (int) ((double) (time - (t_visible_start + off)) / (double) ((t_visible_end + off) - (t_visible_start + off)) * (double) getWidth());
            Line l = new Line(new Point(x, (int) pxFromDp(scale_line_offDp / 2)), new Point(x, getBottom() - (int) pxFromDp(scale_line_offDp + scale_line_offDp / 2)));
            return l;
        }

        public void drawTypeM(Canvas canvas, boolean hor) {

            canvas.drawColor(Color.parseColor(background));
            float visibleMin = 6.0f;

            if (hor) {
                visibleMin = 6.0f;
                //t_visible_end+=(FUTURE_OFFSET*M_TIME_MS * 6);
                t_visible_start = (long) (t_visible_end - M_TIME_MS * 6);
            } else {
                visibleMin = 3.0f;
                // t_visible_end+=(FUTURE_OFFSET*M_TIME_MS * visibleMin);
                t_visible_start = (long) (t_visible_end - M_TIME_MS * 3);
            }
            long f_off = (long) ((t_visible_end - t_visible_start) * FUTURE_OFFSET);
            cur_offset = f_off;
            //f_off=0;
            // f_off=0;
            visible_video_intervals.clear();


            long endMs = t_visible_end + f_off;
            //screen timeline start in ms

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(endMs);
            int w = getWidth();

            float t_m1 = 0;
            int m = cal.get(Calendar.MINUTE);


            cal.set(Calendar.MINUTE, m);
            cal.set(Calendar.SECOND, 0);

            int scaleValue = 1;
            scaleLines.clear();

            scaleLines.add(makeHorScaleLine());
            int c = 0;
            while (cal.getTimeInMillis() >= t_visible_start + f_off) {
                int h = 0;

                h = cal.get(Calendar.MINUTE);


                long t_min = cal.getTimeInMillis();
                Line l = makeVerScaleLine(t_min, f_off);
                l.time = Integer.toString(cal.get(Calendar.MINUTE));

                if (h < 10)
                    l.time = "0" + l.time;
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                String hourStr = hour > 9 ? Integer.toString(hour) + ":" : "0" + Integer.toString(hour) + ":";
                l.time = hourStr + l.time;

                l.time = l.time + " (" + cal.get(Calendar.DAY_OF_MONTH) + " " + getMonthName(cal.get(Calendar.MONTH)) + ")";

                scaleLines.add(l);

                h = cal.get(Calendar.MINUTE);
                h -= scaleValue;

                cal.set(Calendar.MINUTE, h);
            }

            drawScale(canvas);
            selectVisibleRecords(f_off);
            drawIntervals(canvas, f_off);
            DrawThumb(canvas, f_off);
        }

        private void drawIntervals(Canvas canvas, long off) {
            p.setStrokeWidth((int) pxFromDp(recordWidthDp));
            p.setColor(Color.parseColor(videoIntervalsColor));


            int v_h = (getHeight() - 2 * (int) pxFromDp(scale_line_offDp)) / 2 + (int) pxFromDp(scale_line_offDp) - (int) pxFromDp(scale_line_offDp / 2);

            for (Pair<Long, Long> per : visible_video_intervals) {
                long dif1 = per.first - (t_visible_start + off);

                float hs1 = (float) (per.first - (t_visible_start + off)) / (float) (H_TIME_MS);
                float hs2 = (float) (per.second - (t_visible_start + off)) / (float) (H_TIME_MS);

                // int x_start=(int)(hs1*((float)scaleW12hPx/(float)scaleValue));
                //  int x_end=(int)(hs2*(float)scaleW12hPx/(float)scaleValue);

                long first = System.currentTimeMillis() - 1000 * 3600 * 10;
                long second = System.currentTimeMillis();

                int x_start = (int) ((float) (per.first - (t_visible_start + off)) / (float) (t_visible_end - t_visible_start) * getWidth());
                int x_end = (int) ((float) (per.second - (t_visible_start + off)) / (float) (t_visible_end - t_visible_start) * getWidth());
                canvas.drawLine(x_start, v_h, x_end, v_h, p);


            }
        }

        private void selectVisibleRecords(long off) {
            visible_video_intervals.clear();
            //off=0;
            long left_cash_arr_key = (t_visible_start + off) - (t_visible_start + off) % H_TIME_MS;
            long right_cash_arr_key = (t_visible_end + off) - (t_visible_end + off) % H_TIME_MS + H_TIME_MS;
            long t_key = left_cash_arr_key - H_TIME_MS;

            while (t_key != right_cash_arr_key) {

                ArrayList<Pair<Long, Long>> v = video_intervals.get(t_key);
                if (v != null)
                    for (Pair<Long, Long> p : v) {
                        if (!visible_video_intervals.contains(p))
                            visible_video_intervals.add(p);
                    }

                t_key += H_TIME_MS;


            }
        }

        public void setVisbleEnd(long v_end) {
            t_visible_end = v_end;
            mHandler.sendEmptyMessage(0);
        }

        private void drawScale(Canvas canvas) {
            p.setStrokeWidth((int) pxFromDp(scaleWidthDp));
            p.setColor(Color.parseColor(scaleColor));
            for (Line l : scaleLines) {
                if (!((l.start.x < 0 && l.end.x < 0) || (l.start.x > getWidth() && l.end.x > getWidth())))
                    canvas.drawLine(l.start.x, l.start.y, l.end.x, l.end.y, p);
                p.setTextSize((int) pxFromDp(textSizeDp));
                int t_w = (int) p.measureText(l.time);


                int textOffsetDp = 20;
                canvas.drawText(l.time, l.start.x - (int) (t_w / 2), l.end.y + pxFromDp(textOffsetDp), p);
            }
        }

        private void DrawThumb(Canvas canvas, long off) {
            p.setColor(Color.parseColor(scaleColor));
            p.setStrokeWidth(pxFromDp(thumbWidthDp));
            // off=0;
            //if(visible_video_intervals.size()>0&&!userChangedPos)
            //thumb_time=visible_video_intervals.get(visible_video_intervals.size()-1).second;
            //off=0;
            int w = getWidth();
            double d = ((double) (thumb_time - (t_visible_start + off)) / (double) (t_visible_end - t_visible_start));
            int x = (int) (d * (double) w);
            Line l1 = new Line(new Point((int) ((float) (thumb_time - (t_visible_start + off)) / (float) (t_visible_end - t_visible_start) * getWidth()), (int) pxFromDp(scale_line_offDp - 5 - scale_line_offDp / 2)), new Point((int) ((float) (thumb_time - (t_visible_start + off)) / (float) (t_visible_end - t_visible_start) * getWidth()), getBottom() - (int) pxFromDp(scale_line_offDp - 5 + scale_line_offDp / 2)));
            canvas.drawLine(x, l1.start.y, x, l1.end.y, p);
        }

        public void drawTypeH(Canvas canvas, boolean hor) {
            if (hor)
                t_visible_start = (long) (t_visible_end - H_TIME_MS * 1.6f);
            else
                t_visible_start = (long) (t_visible_end - H_TIME_MS * 0.8f);
            //  t_visible_start=(long)(t_visible_end-H_TIME_MS*1.7f);
            long f_off = (long) ((t_visible_end - t_visible_start) * FUTURE_OFFSET);
            cur_offset = f_off;
            // f_off=0;
            visible_video_intervals.clear();

            visible_video_intervals.clear();
            canvas.drawColor(Color.parseColor(background));
            p.setStrokeWidth(1);
            p.setColor(Color.parseColor(scaleColor));
            long endMs = t_visible_end;
            //screen timeline start in ms

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(endMs + f_off);
            int w = getWidth();
            float t_m = cal.get(Calendar.MINUTE);

            float t_m1 = 0;
            float minutesFromEnd = 0;
            int m = cal.get(Calendar.MINUTE);

            t_m1 = getNearest1hMin((int) t_m);

            cal.set(Calendar.MINUTE, (int) t_m1);
            cal.set(Calendar.SECOND, 0);

            int scaleValue = 15;
            scaleLines.clear();
            scaleLines.add(makeHorScaleLine());
            int c = 0;
            while (c++ < 8) {
                int h = 0;

                h = cal.get(Calendar.MINUTE);


                long t_min = cal.getTimeInMillis();
                Line l = makeVerScaleLine(t_min, f_off);
                l.time = Integer.toString(cal.get(Calendar.MINUTE));

                if (h < 10)
                    l.time = "0" + l.time;
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                String hourStr = hour > 9 ? Integer.toString(hour) + ":" : "0" + Integer.toString(hour) + ":";
                l.time = hourStr + l.time;

                l.time = l.time + " (" + cal.get(Calendar.DAY_OF_MONTH) + " " + getMonthName(cal.get(Calendar.MONTH)) + ")";
                scaleLines.add(l);

                h = cal.get(Calendar.MINUTE);
                h -= scaleValue;

                cal.set(Calendar.MINUTE, h);
            }

            drawScale(canvas);
            selectVisibleRecords(f_off);
            drawIntervals(canvas, f_off);
            DrawThumb(canvas, f_off);
        }

        public void playerSeekCompleted() {

            seekCompleted = true;
            mHandler.sendEmptyMessage(4);
        }

        public void drawType12H(Canvas canvas, boolean hor) {
            if (hor)
                t_visible_start = (long) (t_visible_end - visible12hHours * H_TIME_MS);
            else
                t_visible_start = (long) (t_visible_end - visible12hHours * H_TIME_MS / 2);
            long f_off = (long) ((t_visible_end - t_visible_start) * FUTURE_OFFSET);
            cur_offset = f_off;
            visible_video_intervals.clear();
            canvas.drawColor(Color.parseColor(background));
            p.setStrokeWidth(1);
            p.setColor(Color.parseColor(scaleColor));
            long endMs = t_visible_end + f_off;
            //screen timeline start in ms

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(endMs);
            int w = getWidth();
            float t_h = cal.get(Calendar.HOUR_OF_DAY);

            float t_h1 = 0;
            float hoursOffFromEnd = 0;
            int m = cal.get(Calendar.MINUTE);
            // boolean pm=cal.get(Calendar.AM_PM)==Calendar.PM;
            //  if(pm)
            //   t_h+=12;
            if (t_h != 0) {
                t_h1 = getNearest12hHour((int) t_h);
            } else {
                t_h1 = 22;
                cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH) - 1);
                hoursOffFromEnd = 2 + (float) m / 60.0f;
            }
            //  float c_t=t_h1;
            //   if(pm)
            //   t_h1-=12;

            cal.set(Calendar.HOUR_OF_DAY, (int) t_h1);
            cal.set(Calendar.MINUTE, 0);

            //if(c_t<12)
            //   cal.set(Calendar.AM_PM,Calendar.AM);
            // else
            // cal.set(Calendar.AM_PM,Calendar.PM);
            //pm=cal.get(Calendar.AM_PM)==Calendar.PM;
            long cur_scale_time = cal.getTimeInMillis();
            int scaleValue = 3;
            scaleLines.clear();
            scaleLines.add(makeHorScaleLine());

            //add thumb

            // hoursOffFromEnd+=scaleValue;
            int c = 0;
            while (c++ < 8) {
                int h = 0;

                h = cal.get(Calendar.HOUR_OF_DAY);
                // h=cal.get(Calendar.AM_PM)==Calendar.AM?h:h+12;


                long t_m = cal.getTimeInMillis();
                Line l = makeVerScaleLine(t_m, f_off);
                l.time = new Integer(h).toString();

                if (h < 10)
                    l.time = "0" + l.time;
                l.time = l.time + ":00";
                if (h == 1)
                    l.time = cal.get(Calendar.DATE) + " " + getMonthName(cal.get(Calendar.MONTH));
                if (h == 4 || h == 19)
                    l.time = l.time + " (" + cal.get(Calendar.DATE) + " " + getMonthName(cal.get(Calendar.MONTH)) + ")";
                scaleLines.add(l);

                h = cal.get(Calendar.HOUR_OF_DAY);
                h -= scaleValue;

                cal.set(Calendar.HOUR_OF_DAY, h);
            }

            drawScale(canvas);
            selectVisibleRecords(f_off);
            drawIntervals(canvas, f_off);
            DrawThumb(canvas, f_off);

        }

        @Override
        protected void onDraw(Canvas canvas) {
            boolean hor = mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

            switch (cur_type) {
                case TYPE_12H:
                    drawType12H(canvas, hor);
                    break;
                case TYPE_H:
                    drawTypeH(canvas, hor);
                    break;
                case TYPE_MIN:
                    drawTypeM(canvas, hor);
                    break;
            }


        }
    }


    public TimeLineSet(Context c) {
        super(c);

    }


    public void onBackPressed() {
        calVisible=false;
        calendar.setVisibility(GONE);
        calButt.setVisibility(VISIBLE);

    }

    public TimeLineSet(final Context c, AttributeSet attr) {
        super(c, attr);
        t = new TimeLine(c);

        t.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {

                //t.init();


            }
        });
        t.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {

                float x = e.getX();
                float y = e.getY();

                if (t.mPreviousX == -333333)
                    t.mPreviousX = x;

                float dx = x - t.mPreviousX;
                float dy = y - t.mPreviousY;

                switch (e.getAction()) {
                    case MotionEvent.ACTION_UP:
                        t.mPreviousX = -333333;
                        t.delta_x = 0;
                        if (!t.moved) {
                            t.seekCompleted = false;
                            int w = t.getWidth();
                            double d = (double) x / (double) w;
                            long f_off = (long) ((t.t_visible_end - t.t_visible_start) * t.FUTURE_OFFSET);
                            long time = (long) (d * (double) (t.t_visible_end - t.t_visible_start) + (double) (t.t_visible_start + f_off));

                            t.thumb_time = time;
                            double d1 = Math.ceil((double) ((float) (t.thumb_time - t.t_visible_start) / (float) (t.t_visible_end - t.t_visible_start) * 10)) / 10;
                            t.loadVideo(time);
                            t.userChangedPos = true;
                            t.invalidate();
                        }
                        t.moved = false;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int d = t.delta_x;
                        if (d < 0)
                            d *= -1;
                        if (d > (int) t.pxFromDp(t.MOVE_BY_TOUCH_LIMIT)) {
                            t.moved = true;
                            long delta_x = 0;

                            if (t.cur_type == t.TYPE_12H)
                                delta_x = (long) ((float) dx / (float) ((float) t.scaleW12hPx / 3) * (float) t.H_TIME_MS);

                            if (t.cur_type == t.TYPE_H)
                                delta_x = (long) ((float) dx / (float) ((float) t.scaleW12hPx / 3) * (float) t.H_TIME_MS / 30);
                            if (t.cur_type == t.TYPE_MIN)
                                delta_x = (long) ((float) dx / (float) ((float) t.scaleW12hPx / 3) * (float) t.H_TIME_MS / 300);


//                            if(t.t_visible_start-delta_x>t.t_start&&
//                                    t.t_visible_end-delta_x<t.t_end) {


                            boolean moveLeft = false;
                            if (dx > 0)
                                moveLeft = true;


                            if (!moveLeft) {
                                long c_time = t.loader.get_cur_end();
                                long start = t.t_visible_end;
                                int h = 20;
                                long end = t.t_visible_end + t.H_TIME_MS * h;
                                if (!(c_time > t.t_visible_start + t.cur_offset && c_time < (t.t_visible_end - t.t_visible_start) / 2 + t.t_visible_start + t.cur_offset) && t.loader.holeIntervalLoaded(start, end)) {

                                    t.t_visible_start -= delta_x;
                                    t.t_visible_end -= delta_x;
                                } else if (!(c_time > t.t_visible_start + t.cur_offset && c_time < (t.t_visible_end - t.t_visible_start) / 2 + t.t_visible_start + t.cur_offset))
                                    t.showArr2Pr();

                            } else {

                                int h = 20;
                                long start = t.t_visible_start - t.H_TIME_MS * h;
                                long end = t.t_visible_start;
                                if (t.loader.holeIntervalLoaded(start, end)) {

                                    t.t_visible_start -= delta_x;
                                    t.t_visible_end -= delta_x;
                                } else t.showArr1Pr();
                            }


//                            }
//                            else
//                            {
//
//                                if(t.t_visible_start-delta_x<t.t_start)
//                                {
//                                    long cash=t.t_visible_start;
//                                    t.t_visible_start=t.t_start;
//                                    t.t_visible_end-=(cash-t.t_start);
//                                }
//
//                                if(t.t_visible_end-delta_x>t.t_end)
//                                {
//                                    long cash=t.t_visible_end;
//                                    t.t_visible_end=t.t_end;
//                                    t.t_visible_start+=(t.t_end-cash);
//                                }
//
//                            }
                            t.mPreviousX = x;
                            t.mPreviousY = y;
                            t.invalidate();
                            break;
                        }
                }
                t.delta_x -= dx;

                return true;
            }
        });

        LayoutParams p = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) t.pxFromDp(timeLineHeightDp));
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams((int) t.pxFromDp(timeLineArrowsWidthDp), ViewGroup.LayoutParams.MATCH_PARENT);
        FrameLayout.LayoutParams p3 = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        FrameLayout tCon = new FrameLayout(c);
        tCon.setLayoutParams(p1);

        p3.gravity = Gravity.CENTER;
        p.gravity = Gravity.BOTTOM;
        LinearLayout lin = new LinearLayout(c);
        lin.setOrientation(LinearLayout.HORIZONTAL);
        lin.setLayoutParams(p);
        FrameLayout arr1Con = new FrameLayout(c);
        arr1Con.setLayoutParams(p2);
        FrameLayout arr2Con = new FrameLayout(c);
        arr2Con.setLayoutParams(p2);
        ImageView arr1 = new ImageView(c);
        ImageView arr2 = new ImageView(c);
        arr1Pr = new ProgressBar(c);
        arr2Pr = new ProgressBar(c);


        arr1.setImageResource(R.drawable.left);
        arr2.setImageResource(R.drawable.right);
        arr1.setLayoutParams(p3);
        arr2.setLayoutParams(p3);
        arr1.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                long start = 0;
                long end = 0;
//                public  final int TYPE_MIN=3;
//                public final int TYPE_12H=2;
//                public final int TYPE_H=1;
                switch (t.cur_type) {
                    case 2:
                        start = t.t_visible_start - t.H_TIME_MS * 5;
                        end = t.t_visible_start;
                        break;
                    case 1:
                        start = t.t_visible_start - t.H_TIME_MS;
                        end = t.t_visible_start;
                        break;
                    case 3:
                        start = t.t_visible_start - t.M_TIME_MS * 5;
                        end = t.t_visible_start;
                        break;

                }
                int h = 20;
                start = t.t_visible_start - t.H_TIME_MS * h;
                end = t.t_visible_start;
                if (t.loader.holeIntervalLoaded(start, end)) {
                    t.moveLeftArr = true;
                    t.moveByArrow();
                } else t.showArr1Pr();
            }
        });

        arr2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                long start = 0;
                long end = 0;

                switch (t.cur_type) {
                    case 2:
                        start = t.t_visible_end;
                        end = t.t_visible_end + t.H_TIME_MS * 5;
                        break;
                    case 1:
                        start = t.t_visible_end;
                        end = t.t_visible_end + t.H_TIME_MS;
                        ;
                        break;
                    case 3:
                        start = t.t_visible_end;
                        end = t.t_visible_end + t.M_TIME_MS * 5;
                        break;

                }
                start = t.t_visible_end;
                int h = 20;
                end = t.t_visible_end + t.H_TIME_MS * h;
                boolean willCrossFuture = false;
                boolean crossesFuture = false;
                long e = t.loader.get_cur_end();
                if (t.loader.get_cur_end() >= t.t_visible_start && t.loader.get_cur_end() <= t.t_visible_end)
                    crossesFuture = true;
                if (!crossesFuture && t.loader.get_cur_end() < t.t_visible_end + t.H_TIME_MS * h)
                    willCrossFuture = true;
                long c_time = t.loader.get_cur_end();

                if (!(c_time > t.t_visible_start + t.cur_offset && c_time < t.t_visible_end + t.cur_offset) && t.loader.holeIntervalLoaded(start, end)) {
                    t.moveRightArr = true;
                    t.moveByArrow();
                } else if (!(c_time > t.t_visible_start + t.cur_offset && c_time < t.t_visible_end + t.cur_offset))
                    t.showArr2Pr();

            }
        });
        tPr = new ProgressBar(c);
        tLoadingBackground = new FrameLayout(c);
        tLoadingBackground.setBackgroundColor(Color.parseColor("#000000"));
        tLoadingBackground.setAlpha(0.8f);
        tLoadingBackground.setVisibility(INVISIBLE);
        tPr.setVisibility(INVISIBLE);
        arr1Pr.setVisibility(INVISIBLE);
        arr2Pr.setVisibility(INVISIBLE);
        tLoadingBackground.setBackgroundColor(Color.parseColor("#000000"));
        tLoadingBackground.setAlpha(0.8f);
        //t.setLayoutParams(p1);
        arr1Con.addView(arr1);
        arr2Con.addView(arr2);
        arr1Con.addView(arr1Pr);
        arr2Con.addView(arr2Pr);
        lin.addView(arr1Con);

        tCon.addView(t);
        tCon.addView(tLoadingBackground);
        tCon.addView(tPr);
        lin.addView(tCon);
        lin.addView(arr2Con);


        LinearLayout.LayoutParams p6 = new LinearLayout.LayoutParams((int) t.pxFromDp(timeLineArrowsWidthDp), ViewGroup.LayoutParams.MATCH_PARENT);

        final TextView switchScaleBut = new TextView(c);
        switchScaleBut.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                t.cur_type++;
                if (t.cur_type > 3)
                    t.cur_type = 1;
                switch (t.cur_type) {
                    case 1:
                        switchScaleBut.setText("H");
                        break;
                    case 2:
                        switchScaleBut.setText("12H");
                        break;
                    case 3:
                        switchScaleBut.setText("Min");
                        break;


                }
                t.invalidate();
            }
        });
        //    switchScaleBut.setBackground(c.getResources().getDrawable(R.drawable.phone));
        switchScaleBut.setText("Min");
        switchScaleBut.setLayoutParams(p6);
        lin.addView(switchScaleBut);

        Calendar cal = Calendar.getInstance();

        calendar = new MaterialCalendarView(c);
        //calendar.setSelectedDate(cal);
        int t_d = cal.get(Calendar.DAY_OF_MONTH);
        t_d--;
        cal.set(Calendar.DAY_OF_MONTH, t_d);
        //calendar.setSelectedDate(cal);
        calendar.setPagingEnabled(false);
        calendar.addDecorator(new DayViewDecorator() {
            @Override
            public boolean shouldDecorate(CalendarDay day) {
//                return day.getCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
//                        day.getCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY ;
                return true;
            }

            @Override
            public void decorate(DayViewFacade view) {
                view.addSpan(new ForegroundColorSpan(Color.WHITE));
            }
        });

        calendar.addDecorator(new DayViewDecorator() {
            @Override
            public boolean shouldDecorate(CalendarDay day) {
                CalendarDay date = CalendarDay.today();
                Calendar cal = day.getCalendar();
                int d1 = cal.get(Calendar.DAY_OF_MONTH);
                cal.setTimeInMillis(System.currentTimeMillis());
                int d2 = cal.get(Calendar.DAY_OF_MONTH);
                return d2 - d1 < 4 && d2 - d1 >= 0;
            }

            @Override
            public void decorate(DayViewFacade view) {
                // view.addSpan(new ForegroundColorSpan(Color.BLUE));
                ImageView d = new ImageView(c);
                d.setImageResource(R.drawable.blue);

                Drawable d1 = d.getDrawable();
                view.setBackgroundDrawable(d1);
            }

        });
        calendar.setSelectionColor(Color.parseColor("#000000"));
        calendar.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                Calendar cal = date.getCalendar();

                int d1 = date.getDay();
                cal.setTimeInMillis(System.currentTimeMillis());
                int d2 = cal.get(Calendar.DAY_OF_MONTH);
                cal.set(Calendar.DAY_OF_MONTH, d1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                if (d2 - d1 < 4 && d2 - d1 >= 0) {
                   // t.setVisbleEnd(date.getCalendar().getTimeInMillis());


                    t.userPressedCalDay = true;
                    t.userClickedPerStart=cal.getTimeInMillis() ;
                    t.userClickedPerEnd=cal.getTimeInMillis()+ 24 * t.H_TIME_MS;
                    t.loader.load_intervals(t.userClickedPerStart, t.userClickedPerEnd);
                    t.showPr();

                    onBackPressed();
                }
            }
        });
        // Calendar cal=Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.set(Calendar.DAY_OF_MONTH, 1);
        //calendar.s(cal.getTimeInMillis());
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        //calendar.setMaxDate(cal.getTimeInMillis());
        // addView(calendar);
        LinearLayout lin1 = new LinearLayout(c);
        lin1.setOrientation(LinearLayout.VERTICAL);
        FrameLayout calCon = new FrameLayout(c);
        calCon.addView(calendar);
        lin1.addView(calCon);
        lin1.addView(lin);
        addView(lin1);
        calButt = new Button(c);
        FrameLayout.LayoutParams p5 = new FrameLayout.LayoutParams((int) t.pxFromDp(50), (int) t.pxFromDp(50));
        //calendar.setLayoutParams(p5);
        calButt.setLayoutParams(p5);
        calButt.setText("Cal.");
        calendar.setTileHeightDp(40);
        calendar.setVisibility(GONE);
        calButt.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                calVisible = true;
                calendar.setVisibility(VISIBLE);
                calButt.setVisibility(INVISIBLE);
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(System.currentTimeMillis());
                int day = cal.get(Calendar.DAY_OF_MONTH);
                //calendar.setDate(cal.getTimeInMillis());

                day--;
                cal.set(Calendar.DAY_OF_MONTH, day);
                // calendar.setDate(cal.getTimeInMillis());
                day--;
                cal.set(Calendar.DAY_OF_MONTH, day);
                // calendar.setDate(cal.getTimeInMillis());


            }
        });
        calButt.setTextColor(Color.parseColor("#ffffff"));
        calCon.addView(calButt);


    }

}
