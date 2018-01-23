package com.vxg.cloud.core.CloudCommon;

public class CloudUri {
    private String mSource;
    private String mProtocol;
    public CloudUri(String str){
        mSource = str;
        String[] arr = str.split("/");
        // TODO 
        // parse protocol
        mProtocol = arr[0];

        /*result.protocol = arr[0];
        result.protocol = mProtocol.slice(0, mProtocol.protocol.length-1);
        result.protocol = result.protocol.toLowerCase();

        // parse user/password/host/port
        var uphp = arr[2];
        // console.log(uphp);
        result.path = "/" + arr.slice(3).join("/");

        // parse port
        var reg_port = new RegExp(".*:(\\d+)$", "g");
        var port = reg_port.exec(uphp);
        if(port && port.length > 1){
            result.port = parseInt(port[1],10);
            uphp = uphp.slice(0, uphp.length - port[1].length - 1);
        }

        // parse host
        if(uphp.indexOf(":") == -1 && uphp.indexOf("@") == -1){
            result.host = uphp;
            uphp = "";
        }else{
            var host = uphp.split("@");
            result.host = host[host.length-1];
            uphp = uphp.slice(0, uphp.length - result.host.length - 1);
        }

        // parse user/password
        if(uphp != ""){
            if(uphp.indexOf(":") != -1){
                var a = uphp.split(":");
                result.user = a[0];
                result.password = a[1];
            }else{
                result.user = uphp;
                uphp = "";
            }
        }

        return result;*/
    }

    public String getSource(String src){
        return mSource;
    }
}
