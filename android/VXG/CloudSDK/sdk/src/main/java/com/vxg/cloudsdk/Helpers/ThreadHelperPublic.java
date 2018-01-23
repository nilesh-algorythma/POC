package com.vxg.cloudsdk.Helpers;

/**
 * Created by bleikher on 07.11.2017.
 */

public abstract class ThreadHelperPublic extends ThreadHelper{
    public ThreadHelperPublic()
    {
        super();
    }
    public void sleep(long millis){
        super.sleep(millis);
    }
    public boolean is_started(){
        return super.is_started();
    }
    public boolean is_running(){
        return super.is_running();
    }
    public void wakeup() {
        super.wakeup();
    }
    public void execute(){
        super.execute();
    }
    public void start(){
        super.start();
    }
    public void stop(long wait_millis){
        super.stop(wait_millis);
    }

}
