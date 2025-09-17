package com.hexedrealms.components;

import com.hexedrealms.components.bulletbodies.OcclusionBody;
import com.hexedrealms.components.bulletbodies.ZoneEntry;

public class ZoneConnection {
    public ZoneEntry fromZone;
    public ZoneEntry toZone;
    public OcclusionBody portal;

    public ZoneConnection(ZoneEntry fromZone, ZoneEntry toZone, OcclusionBody occlusionBody){
        this.fromZone = fromZone;
        this.toZone = toZone;
        this.portal = occlusionBody;
    }
}
