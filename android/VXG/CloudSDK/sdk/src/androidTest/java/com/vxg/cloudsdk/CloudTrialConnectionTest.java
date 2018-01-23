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

package com.vxg.cloudsdk;
import android.support.test.runner.AndroidJUnit4;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;

import android.test.suitebuilder.annotation.SmallTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import com.vxg.cloudsdk.Enums.CloudReturnCodes;
import com.vxg.cloudsdk.Objects.CloudTrialConnection;
import com.vxg.cloudsdk.Objects.CloudUserInfo;
// import android.support.test.runner.AndroidJUnitRunner;

@RunWith(JUnit4.class)
@LargeTest
public class CloudTrialConnectionTest {
    private static String TAG = CloudTrialConnectionTest.class.getSimpleName();

    @Before
    public void setUp() {
        // TODO
    }

    @Test
    public void test1() {
        Log.i(TAG, "test1");
    }

    @Test
    public void test2() {
        Log.i(TAG, "test1");
    }

    @Test
    public void testOpenDemoConnection() {
        Log.i(TAG, "testOpenDemoConnection");
        // throw new RuntimeException();

        // CloudSDK.setContext(mMockContext);
        // CloudSDK.setLogEnable(true);
        // CloudSDK.setLogLevel(2);

        CloudTrialConnection conn = new CloudTrialConnection();
        int r_open = conn.openSync("demo");
        assertThat(r_open, is(CloudReturnCodes.OK));

        // throw new RuntimeException();

        Log.i(TAG, Long.toString(conn.getServerTimeDiff()));
        CloudUserInfo info = conn.getUserInfoSync();
        Log.i(TAG, "Email: " + info.getEmail());
        Log.i(TAG, "Preferred name: " + info.getPreferredName());
    }
}