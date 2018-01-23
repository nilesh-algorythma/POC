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

import com.vxg.cloudsdk.Interfaces.ICompletionCallback;

public class CloudTokenConnection extends CloudConnection{

    public int openSync(String token, long time_expires){
        return super.openSync(token, time_expires);
    }
    public int open(String token, long time_expires, ICompletionCallback c)
    {
        return super.open(token, time_expires, c);
    }

}
