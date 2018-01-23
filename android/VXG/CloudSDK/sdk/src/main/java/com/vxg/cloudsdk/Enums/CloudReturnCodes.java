package com.vxg.cloudsdk.Enums;

/**
 * Created by bleikher on 27.07.2017.
 */

public class CloudReturnCodes {
    public final static int OK = 0;                          /* Success */
    public final static int OK_COMPLETIONPENDING = 1;        /* Operation Pending */
    public final static int ERROR_NOT_CONFIGURED = -2;       /* Object not configured */
    public final static int ERROR_NOT_IMPLEMENTED = -1;      /* Function Not implemented*/
    public final static int ERROR_NO_MEMORY = -12;           /* Out of memory */
    public final static int ERROR_ACCESS_DENIED = -13;       /* Access denied */
    public final static int ERROR_BADARGUMENT = -22;         /* Invalid argument */
    public final static int ERROR_STREAM_UNREACHABLE = (-5049);
    public final static int ERROR_EXPECTED_FILTER = (-5050);
    public final static int ERROR_NO_CLOUD_CONNECTION = (-5051);
    public final static int ERROR_WRONG_RESPONSE = (-5052);
    public final static int ERROR_NOT_AUTHORIZED = (-5401);
    public final static int ERROR_SOURCE_NOT_CONFIGURED = (-5053);
    public final static int ERROR_INVALID_SOURCE = (-5054);
    public final static int ERROR_RECORDS_NOT_FOUND = (-5055);
    public final static int ERROR_FORBIDDEN = (-5403);
    public final static int ERROR_NOT_FOUND = (-5404);
    public final static int ERROR_TOO_MANY_REQUESTS = (-5429);
}
