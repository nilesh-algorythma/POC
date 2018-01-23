package com.vxg.cloudsdk.Interfaces;

/**
 * Created by bleikher on 21.08.2017.
 */

public interface ICloudUpdate {

    // Asynchronous methods
    int refresh(ICompletionCallback callback);
    int save(ICompletionCallback callback);

    // Synchronous methods
    int refreshSync();
    int saveSync();

}
