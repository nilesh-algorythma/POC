package com.vxg.cloudsdk.Helpers;

import com.vxg.cloudsdk.Interfaces.ICompletionCallback;

import java.util.List;

/**
 * Created by bleikher on 27.07.2017.
 */

public class Msg {
    public int                  func_id = -1;           //function id, CMD_XXX
    public List<Object>         args = null;            //<in> arguments
    public ICompletionCallback  func_complete = null;   //internal completion callback
}
