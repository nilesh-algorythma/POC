//
//  Copyright © 2016 VXG Inc. All rights reserved.
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

package com.vxg.cnvrclient2.AccountProvider;

import com.vxg.cloudsdk.Helpers.MLog;

import org.json.JSONException;
import org.json.JSONObject;


public class AccountProviderUserProfile {
    private static final String TAG = AccountProviderUserProfile.class.getSimpleName();
    final public static int LOG_LEVEL = 2; //Log.VERBOSE;
    static MLog Log = new MLog(TAG, LOG_LEVEL);

    private String m_sEmail = "";
    private String m_sFirstName = "";
    private String m_sLastName = "";
    private String m_sCountry = "";
    private String m_sRegion = "";
    private String m_sCity = "";
    private String m_sAddress = "";
    private String m_sPostcode = "";
    private String m_sPhone = "";
    private String m_sContactWay = "";

    public AccountProviderUserProfile(){

    }

    public AccountProviderUserProfile(JSONObject obj){
        try {
            Log.i(obj.toString(1));
        }catch(JSONException e){
            // nothing
        }
        m_sEmail = getValue(obj, "email");
        m_sFirstName = getValue(obj, "first_name");
        m_sLastName = getValue(obj, "last_name");
        m_sCountry = getValue(obj, "country");
        m_sRegion = getValue(obj, "region");
        m_sCity = getValue(obj, "city");
        m_sAddress = getValue(obj, "address");
        m_sPostcode = getValue(obj, "postcode");
        m_sPhone = getValue(obj, "phone");
        m_sContactWay = getValue(obj, "contact_way");
    }

    public JSONObject toJson(){
        JSONObject data = new JSONObject();
        try {
            data.put("email", this.getEmail());
            data.put("first_name", this.getFirstName());
            data.put("last_name", this.getLastName());
            data.put("country", this.getCountry());
            data.put("region", this.getRegion());
            data.put("city", this.getCity());
            data.put("address", this.getAddress());
            data.put("postcode", this.getPostcode());
            data.put("phone", this.getPhone());
            data.put("contact_way", this.getContactWay());
        }catch(JSONException e){
            Log.e("Failed json: " + e.getMessage());
            e.printStackTrace();
        }
        return data;
    }

    private String getValue(JSONObject obj, String key){
        String val = "";
        if(obj.has(key)){
            try {
                val = obj.getString(key);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return val;
    }

    public String getEmail() { return m_sEmail; }
    public String getFirstName() { return m_sFirstName; }
    public String getLastName() { return m_sLastName; }
    public String getCountry() { return m_sCountry; }
    public String getRegion() { return m_sRegion; }
    public String getCity() { return m_sCity; }
    public String getAddress() { return m_sAddress; }
    public String getPostcode() { return m_sPostcode; }
    public String getPhone() { return m_sPhone; }
    public String getContactWay() { return m_sContactWay; }

    public void setEmail(String newVal) { m_sEmail = newVal; }
    public void setFirstName(String newVal) { m_sFirstName = newVal; }
    public void setLastName(String newVal) { m_sLastName = newVal; }
    public void setCountry(String newVal) { m_sCountry = newVal; }
    public void setRegion(String newVal) { m_sRegion = newVal; }
    public void setCity(String newVal) { m_sCity = newVal; }
    public void setAddress(String newVal) { m_sAddress = newVal; }
    public void setPostcode(String newVal) { m_sPostcode = newVal; }
    public void setPhone(String newVal) { m_sPhone = newVal; }
    public void setContactWay(String newVal) { m_sContactWay = newVal; }
}