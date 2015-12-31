/**
 * Created by s_vershinin on 30.12.2015.
 * GPS operations
 */

package com.nordman.big.smsparking;

import android.content.Context;
import android.location.Location;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

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

    public Point getCurrentPoint(GoogleApiClient mGoogleApiClient) {
        Point result = null;
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            result = factory.createPoint(new Coordinate(mLastLocation.getLatitude(),mLastLocation.getLongitude()));
        }
        return result;
    }

    public ArrayList<Polygon> getPolygonList(){
        ArrayList<Polygon> result = new ArrayList<Polygon>();
        ArrayList<Point> points;

        // парсим xml с координатами полигонов
        try {
            XmlPullParser xpp = context.getResources().getXml(R.xml.park_zones);
            while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
                switch (xpp.getEventType()) {
                    // начало документа
                    case XmlPullParser.START_DOCUMENT:
                        Log.d("LOG", "START_DOCUMENT");
                        break;
                    // начало тэга
                    case XmlPullParser.START_TAG:
                        if (xpp.getName().equals("zone")) {
                            // Создал пустой массив точек полигона
                            points = new ArrayList<Point>();
                            Log.d("LOG", "START_ZONE");
                        }
                        Log.d("LOG", "START_TAG: name = " + xpp.getName()
                                + ", depth = " + xpp.getDepth() + ", attrCount = "
                                + xpp.getAttributeCount());
                        String tmp = "";
                        for (int i = 0; i < xpp.getAttributeCount(); i++) {
                            tmp = tmp + xpp.getAttributeName(i) + " = "
                                    + xpp.getAttributeValue(i) + ", ";
                        }
                        if (!TextUtils.isEmpty(tmp))
                            Log.d("LOG", "Attributes: " + tmp);
                        break;
                    // конец тэга
                    case XmlPullParser.END_TAG:
                        if (xpp.getName().equals("zone")) {
                            //TODO добавить в result объект типа polygon
                            Log.d("LOG", "END_ZONE");
                        }

                        Log.d("LOG", "END_TAG: name = " + xpp.getName());
                        break;
                    // содержимое тэга
                    case XmlPullParser.TEXT:
                        Log.d("LOG", "text = " + xpp.getText());
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
}
