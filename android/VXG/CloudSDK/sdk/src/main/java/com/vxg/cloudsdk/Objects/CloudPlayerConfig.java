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

public class CloudPlayerConfig {
    public boolean m_visibleControls = false;
    public int m_aspectRatio = 1;
    public long m_minLatency = 10000;
    public int m_timeWaitStream = 2*60; //2min

    public CloudPlayerConfig(){
        _reset(null);
    }
    public CloudPlayerConfig(CloudPlayerConfig src){
        _reset(src);
    }

    void _reset(CloudPlayerConfig src){
        if(src != null){
            m_visibleControls = src.m_visibleControls;
            m_minLatency = src.m_minLatency;
            m_aspectRatio = src.m_aspectRatio;
            m_timeWaitStream = src.m_timeWaitStream; //2min
        }else {
            m_visibleControls = false;
            m_minLatency = 10000;
            m_aspectRatio = 1;
            m_timeWaitStream = 2 * 60; //2min
        }
    }

    void visibleControls(boolean bControls){ m_visibleControls = bControls; };
    void aspectRatio(int mode){ m_aspectRatio = mode; };
    void setMinLatency(long Latency){ m_minLatency = Latency; };   // msec
    void setWaitTimeStartStream(int seconds){ m_timeWaitStream = seconds; }; // how long wait start stream before error
}
