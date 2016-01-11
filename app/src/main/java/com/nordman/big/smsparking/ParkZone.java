package com.nordman.big.smsparking;

import com.vividsolutions.jts.geom.Polygon;

/**
 * Created by s_vershinin on 11.01.2016.
 */
public class ParkZone {
    private Integer zoneNumber;
    private String zoneDesc;
    private Polygon zonePolygon;

    public Polygon getZonePolygon() {
        return zonePolygon;
    }

    public void setZonePolygon(Polygon zonePolygon) {
        this.zonePolygon = zonePolygon;
    }

    public String getZoneDesc() {

        return zoneDesc;
    }

    public void setZoneDesc(String zoneDesc) {
        this.zoneDesc = zoneDesc;
    }

    public Integer getZoneNumber() {

        return zoneNumber;
    }

    public void setZoneNumber(Integer zoneNumber) {
        this.zoneNumber = zoneNumber;
    }

}
