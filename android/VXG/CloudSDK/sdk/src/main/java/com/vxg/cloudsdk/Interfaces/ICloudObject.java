package com.vxg.cloudsdk.Interfaces;

/**
 * Created by bleikher on 27.07.2017.
 */

public interface ICloudObject {

    // getCamera Last Result if supported
    boolean hasError();
    int getResultInt();
    String getResultStr();

}
