package com.vxg.cloudsdk.Enums;

/**
 * Created by bleikher on 30.10.2017.
 */

public enum PS_Privacy {
        ps_owner_not_public, 	//owner=true, public=false
        ps_owner, 			//owner=true, public=null
        ps_public_not_owners, 	//owner=false, public=true
        ps_public,			//owner=null, public=true
        ps_owners_public,		//owner=true, public=true
        ps_all				//owner=null, public=null
}
