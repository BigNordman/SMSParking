/**
 * Created by s_vershinin on 30.12.2015.
 * GPS operations
 */

package com.nordman.big.smsparking;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import com.vividsolutions.jts.awt.PointShapeFactory;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;


public class GeoManager {
    Context context;
    GeometryFactory factory;

    public GeoManager(Context context) {
        this.context = context;
        factory = new GeometryFactory();
    }

    public String getCoordinates(GoogleApiClient mGoogleApiClient)
    {
        return this.getCurrentPoint(mGoogleApiClient).toString();
    }

    private Point getCurrentPoint(GoogleApiClient mGoogleApiClient) {
        Point result = null;
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            result = factory.createPoint(new Coordinate(mLastLocation.getLatitude(),mLastLocation.getLongitude()));
        }
        return result;
    }

    private ArrayList<ParkZone> getParkZoneList(){
        ArrayList<ParkZone> result = new ArrayList<ParkZone>();
        ArrayList<Coordinate> coords = null;
        Polygon polygon = null;
        Integer zoneNumber = null;
        String zoneDesc = null;

        // парсим xml с координатами полигонов
        try {
            XmlPullParser xpp = context.getResources().getXml(R.xml.park_zones);
            while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
                switch (xpp.getEventType()) {
                    // начало тэга
                    case XmlPullParser.START_TAG:
                        switch (xpp.getName()) {
                            case "zone":
                                // массив точек полигона. Пустой
                                coords = new ArrayList<Coordinate>();
                                zoneNumber = Integer.parseInt(xpp.getAttributeValue(null,"zone_number")) ;
                                zoneDesc = xpp.getAttributeValue(null,"zone_desc");
                                break;
                            case "point":
                                // точка полигона
                                Double lat = Double.parseDouble(xpp.getAttributeValue(0));
                                Double lon = Double.parseDouble(xpp.getAttributeValue(1));
                                if (coords!=null) {
                                    coords.add(new Coordinate(lat, lon));
                                }
                                break;
                            default:
                                break;
                        }

                        break;
                    // конец тэга
                    case XmlPullParser.END_TAG:
                        if (xpp.getName().equals("zone")) {
                            // Зона определена - создаем объект ParkZone и зохраняем в результате
                            if (coords!=null) {
                                polygon = factory.createPolygon(coords.toArray(new Coordinate[coords.size()]));
                                Log.d("LOG", polygon.toString());
                                ParkZone zone = new ParkZone(polygon,zoneNumber,zoneDesc);
                                result.add(zone);
                            }
                        }
                        break;

                    default:
                        break;
                }
                xpp.next();
            }
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    public String getParkZoneNumber(GoogleApiClient mGoogleApiClient){
        ArrayList<ParkZone> zones = this.getParkZoneList();
        Point currentPoint = this.getCurrentPoint(mGoogleApiClient);

        for(ParkZone zone : zones){
            if (zone.getZonePolygon().contains(currentPoint)){
                return zone.getZoneNumber().toString();
            }
        }

        return "___";
    }
}
