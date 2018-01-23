package com.vxg.cloudsdk.Helpers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/*
 * bleikher. Thread class for handy thread usage
 */
public abstract class ThreadHelper implements Runnable{
	
	boolean 		isstarted = false; //start/stop status
	boolean 		isrunning = false; //true when run() is running
	Thread 			thread=null;
	
	Object 			objWait = new Object();
	CountDownLatch  objLatch = null;

	int				cnt_exec = 0;
	
	protected ThreadHelper(){
	}
	
	protected void sleep(long millis){
		synchronized (objWait){
			try {
				objWait.wait(millis);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	protected boolean is_started(){
		return (thread != null && isstarted); 
	}
	protected boolean is_running(){
		return (thread != null && isrunning);
	}
	
	@Override
	public void run(){
		int cnt;
		isrunning = true;
		synchronized (objWait){
			objLatch = new CountDownLatch(1);
			cnt = cnt_exec;
		}

		while(cnt > 0) {
			runt();
			synchronized (objWait){
				cnt_exec--;
				cnt = cnt_exec;
			}
		}
		
		if(objLatch != null)
			objLatch.countDown();

		isrunning = false;
	}
	//=> abstract members
	protected abstract void runt();
	//<= abstract members

	protected void wakeup(){
		synchronized (objWait){
			objWait.notifyAll();
		}
	}
	protected void execute() {
		synchronized (objWait) {
			if (is_started() && cnt_exec == 1) {
				//prolong the thread
				cnt_exec++;
			}
			if (cnt_exec > 1) {
				//thread is running
				return;
			}

			//restart the thread
			cnt_exec = 1;
			isstarted = true;
			objLatch = null;
			thread = new Thread(this);
			thread.start();
		}
	}
	protected void start(){
		synchronized (objWait) {
			if(is_started())
				return;

			cnt_exec = 1;
			isstarted = true;
			objLatch = null;
			thread = new Thread(this);
			thread.start();
		}
	}

	protected void stop(long wait_millis){
		synchronized (objWait){
			if(!is_started())
				return;

			cnt_exec = 0;
			isstarted = false;
			objWait.notifyAll();
		}
		
		if(wait_millis != 0 && objLatch != null){
			try {
				if(wait_millis == -1){
					//infinite
					objLatch.await();
				}else{
					objLatch.await(wait_millis, TimeUnit.MILLISECONDS);
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		synchronized (objWait) {
			if (thread != null) thread.interrupt();
			thread = null;
			objLatch = null;
			isrunning = false;
		}
	}

}
