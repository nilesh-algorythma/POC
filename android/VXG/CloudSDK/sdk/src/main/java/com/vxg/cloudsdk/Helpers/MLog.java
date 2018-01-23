package com.vxg.cloudsdk.Helpers;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.util.Log;

/**
 * Created by bleikher on 26.07.2017.
 */

public class MLog {
	
	String tag="";
	int level = Log.ASSERT; //ASSERT = 7; DEBUG = 3; ERROR = 6; INFO = 4; VERBOSE = 2; WARN = 5;
	
	public static Context app = null;
	public static Boolean isSignedWithDebugKey = null;
	public static int globalLevel = Log.ASSERT;

	private static final X500Principal DEBUG_DN = new X500Principal("CN=Android Debug,O=Android,C=US");
	protected boolean signedWithDebug()
	{
		//force debug
        //isSignedWithDebugKey = true;
		
        if(app != null && isSignedWithDebugKey == null) {
    	    try
    	    {
    	        PackageInfo pinfo = app.getPackageManager().getPackageInfo(app.getPackageName(),PackageManager.GET_SIGNATURES);
    	        Signature signatures[] = pinfo.signatures;
    	         
    	        for ( int i = 0; i < signatures.length;i++)
    	        {
    	            CertificateFactory cf = CertificateFactory.getInstance("X.509");
    	            ByteArrayInputStream stream = new ByteArrayInputStream(signatures[i].toByteArray());
    	            X509Certificate cert = (X509Certificate) cf.generateCertificate(stream);       
    	            isSignedWithDebugKey = cert.getSubjectX500Principal().equals(DEBUG_DN);
    	            if (isSignedWithDebugKey)
    	                break;
    	        }
    	 
    	    }
    	    catch (NameNotFoundException e)
    	    {
    	        //debuggable variable will remain false
    	    }
    	    catch (CertificateException e)
    	    {
    	        //debuggable variable will remain false
    	    }
		}
		if(isSignedWithDebugKey == null){
			return false;
		}
	     
        return isSignedWithDebugKey;     
	}
	
    /*protected boolean signedWithDebug() {         
        if(app != null && isSignedWithDebugKey == null) { 
        	
            PackageManager pm = app.getPackageManager();             
            try {                
             PackageInfo pi = pm.getPackageInfo(app.getPackageName(), 0);                 
             isSignedWithDebugKey = (pi.applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;             
            }             
            catch(NameNotFoundException nnfe) {                 
                nnfe.printStackTrace();                 
                isSignedWithDebugKey = false;             
            }         
            //force debug
            isSignedWithDebugKey = true;
        }         
         return isSignedWithDebugKey;     
    }*/
	
	public MLog(String tag, int level){
		this.tag = tag;
		
		if(signedWithDebug() == true) {
			this.level = Math.min(globalLevel, level);
		}
	}
	
	public void v(String msg){
		if(level <= Log.VERBOSE){
			Log.v(tag, msg);
		}
	}
	
	public void d(String msg){
		if(level <= Log.DEBUG){
			Log.d(tag, msg);
		}
	}

	public void i(String msg){
		if(level <= Log.INFO){
			Log.i(tag, msg);
		}
	}

	public void w(String msg){
		if(level <= Log.WARN){
			Log.w(tag, msg);
		}
	}

	public void e(String msg){
		if(level <= Log.ERROR){
			Log.e(tag, msg);
		}
	}

}
