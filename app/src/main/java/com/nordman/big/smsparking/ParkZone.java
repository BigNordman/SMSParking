package com.nordman.big.smsparking;

import com.vividsolutions.jts.geom.Polygon;

/**
 * Created by s_vershinin on 11.01.2016.
 */
public class ParkZone {
    private Integer zoneNumber;
    private String zoneDesc;
    private Polygon zonePolygon;

    public ParkZone(Polygon zonePolygon, Integer zoneNumber, String zoneDesc) {
        this.zonePolygon = zonePolygon;
        this.zoneNumber = zoneNumber;
        this.zoneDesc = zoneDesc;
    }

    public Polygon getZonePolygon() {
        return zonePolygon;
    }

    public String getZoneDesc() {

        return zoneDesc;
    }


    public Integer getZoneNumber() {

        return zoneNumber;
    }

}
