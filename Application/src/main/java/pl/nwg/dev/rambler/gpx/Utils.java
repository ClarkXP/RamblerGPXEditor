package pl.nwg.dev.rambler.gpx;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.karambola.gpx.beans.Gpx;
import pt.karambola.gpx.beans.Point;
import pt.karambola.gpx.beans.Route;
import pt.karambola.gpx.beans.RoutePoint;
import pt.karambola.gpx.beans.Track;
import pt.karambola.gpx.beans.TrackPoint;
import pt.karambola.gpx.util.GpxUtils;

import static android.graphics.Bitmap.Config.ARGB_8888;

/**
 * Created by piotr on 30.01.16.
 */
public class Utils extends Activity {

    private static final String TAG = "Utils";

    protected GoogleApiClient mGoogleApiClient;

    protected String responseString;

    protected String sdRootTxt = "";

    protected File defaultRamblerFile;

    protected File defaultPoisFile;
    protected File defaultRoutesFile;
    protected File defaultTracksFile;

    protected String externalGpxFile;

    protected final int REQUEST_CODE_PICK_DIR = 1;
    protected final int REQUEST_CODE_PICK_FILE = 2;

    protected int fileActionRequested;
    protected final int OPEN_GPX_FILE = 0;
    protected final int SAVE_ALL_POIS = 1;
    protected final int SAVE_VISIBLE_POIS = 2;
    protected final int IMPORT_FROM_GPX = 3;

    protected String fileFullPath = "";
    protected String sdRoot = "";
    protected String fileName = "myfile";
    protected String fileFolderAndName = "";

    /**
     * Calculate bounding box for given List of GeoPoints
     * Based on the osmdroid code by Nicolas Gramlich, released under the Apache License 2.0
     * https://github.com/osmdroid/osmdroid/blob/master/osmdroid-android/src/main/java/org/osmdroid/util/BoundingBox.java
     */
    protected BoundingBox findBoundingBox(List<GeoPoint> geoPoints) {

        double minLat = Double.MAX_VALUE;
        double maxLat = Double.MIN_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLon = Double.MIN_VALUE;

        for (GeoPoint geoPoint : geoPoints) {

            final double latitude = geoPoint.getLatitude();
            final double longitude = geoPoint.getLongitude();

            minLat = Math.min(minLat, latitude);
            minLon = Math.min(minLon, longitude);
            maxLat = Math.max(maxLat, latitude);
            maxLon = Math.max(maxLon, longitude);

        }
        return new BoundingBox(maxLat, maxLon, minLat, minLon);
    }

    protected List<GeoPoint> pointsToGeoPoints(List<Point> source) {

        List<GeoPoint> geopoints = new ArrayList<>();

        for(Point point : source) {
            GeoPoint geoPoint = new GeoPoint(point.getLatitude(), point.getLongitude());
            geopoints.add(geoPoint);
        }
        return geopoints;
    }

    protected String shortenDouble(double dblValue) {

        String string = String.valueOf(dblValue);
        if (string.length() > 9) {
            string = string.substring(0, 9);
        }
        return string;
    }

    /**
     * This will return a marker bitmap.
     * @param hoverText - is the name of the route point or POI displayed above;
     * @param color - if passed, we create a bitmap for POI, it should be coloured by the POI type
     */
    protected Bitmap makeMarkerBitmap(Context context, String hoverText, Integer color) {

        Resources resources = context.getResources();
        float scale = resources.getDisplayMetrics().density;

        Bitmap bitmap;
        if (color == null) {
            bitmap = BitmapFactory.decodeResource(resources, R.drawable.wp);
        } else {
            bitmap = BitmapFactory.decodeResource(resources, R.drawable.poi_grey);
        }

        bitmap = bitmap.copy(ARGB_8888, true);

        Canvas canvas = new Canvas(bitmap);

        if (color != null) {
            Paint p = new Paint();
            ColorFilter filter = new LightingColorFilter(color, 1);
            p.setColorFilter(filter);
            canvas.drawBitmap(bitmap, 0, 0, p);
        }

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.DKGRAY);
        paint.setTextSize(14 * scale);
        paint.setShadowLayer(1f, 0f, 1f, Color.WHITE);

        Rect bounds = new Rect();
        paint.getTextBounds(hoverText, 0, hoverText.length(), bounds);

        int x = (bitmap.getWidth() - bounds.width()) / 2;
        if (x < 0) x = 0;
        int y = bounds.height();
        canvas.drawText(hoverText, x, y, paint);

        return  bitmap;
    }

    /**
     * Color value not passed: create the route point bitmap (w/o a color filter).
     */
    protected Bitmap makeMarkerBitmap(Context context, String hoverText) {
        return makeMarkerBitmap(context, hoverText, null);
    }

    protected Bitmap makeRouteNameBitmap(Context context, String name) {

        Resources resources = context.getResources();
        float scale = resources.getDisplayMetrics().density;

        Bitmap bitmap = BitmapFactory.decodeResource(resources, R.drawable.path_label);

        bitmap = bitmap.copy(ARGB_8888, true);

        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.DKGRAY);
        paint.setTextSize(12 * scale);
        paint.setShadowLayer(1f, 0f, 1f, Color.WHITE);

        Rect bounds = new Rect();
        paint.getTextBounds(name, 0, name.length(), bounds);

        int x = (bitmap.getWidth() - bounds.width()) / 2;
        if (x < 0) x = 0;
        int y = bounds.height();
        canvas.drawText(name, x, y, paint);

        return  bitmap;
    }

    protected static String convertStreamToString(InputStream inputStream) throws IOException {
        if (inputStream != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"),1024);
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                inputStream.close();
            }
            return writer.toString();
        } else {
            return "";
        }
    }

    /* The Polyline encode/decode code below was adapted from Polyline utils
     * Copyright 2014 Google Inc. All rights reserved.
     *
     * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
     * file except in compliance with the License. You may obtain a copy of the License at
     *
     *     http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software distributed under
     * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF
     * ANY KIND, either express or implied. See the License for the specific language governing
     * permissions and limitations under the License.
     */

    /**
     * Decodes an encoded path string into a sequence of LatLngs.
     */
    public static List<GeoPoint> decode(final String encodedPath) {

        int len = encodedPath.length();

        final List<GeoPoint> path = new ArrayList<>(len / 2);
        int index = 0;
        int lat = 0;
        int lng = 0;

        while (index < len) {
            int result = 1;
            int shift = 0;
            int b;
            do {
                b = encodedPath.charAt(index++) - 63 - 1;
                result += b << shift;
                shift += 5;
            } while (b >= 0x1f);
            lat += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            result = 1;
            shift = 0;
            do {
                b = encodedPath.charAt(index++) - 63 - 1;
                result += b << shift;
                shift += 5;
            } while (b >= 0x1f);
            lng += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            path.add(new GeoPoint(lat * 1e-5, lng * 1e-5));
        }

        return path;
    }

    /**
     * Decodes an encoded path string into a sequence of RoutePoints.
     */
    public static List<RoutePoint> decodeToRoutePoints(final String encodedPath) {

        int len = encodedPath.length();

        final List<RoutePoint> path = new ArrayList<>(len / 2);
        int index = 0;
        int lat = 0;
        int lng = 0;

        while (index < len) {
            int result = 1;
            int shift = 0;
            int b;
            do {
                b = encodedPath.charAt(index++) - 63 - 1;
                result += b << shift;
                shift += 5;
            } while (b >= 0x1f);
            lat += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            result = 1;
            shift = 0;
            do {
                b = encodedPath.charAt(index++) - 63 - 1;
                result += b << shift;
                shift += 5;
            } while (b >= 0x1f);
            lng += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            RoutePoint routePoint = new RoutePoint();
            routePoint.setLatitude(lat * 1e-5);
            routePoint.setLongitude(lng * 1e-5);
            path.add(routePoint);
        }

        return path;
    }

    /**
     * Encodes a sequence of GeoPoints into an encoded path string.
     */
    public static String encode(final List<GeoPoint> path) {
        long lastLat = 0;
        long lastLng = 0;

        final StringBuffer result = new StringBuffer();

        for (final GeoPoint point : path) {
            long lat = Math.round(point.getLatitude() * 1e5);
            long lng = Math.round(point.getLongitude() * 1e5);

            long dLat = lat - lastLat;
            long dLng = lng - lastLng;

            encode(dLat, result);
            encode(dLng, result);

            lastLat = lat;
            lastLng = lng;
        }
        return result.toString();
    }

    private static void encode(long v, StringBuffer result) {
        v = v < 0 ? ~(v << 1) : v << 1;
        while (v >= 0x20) {
            result.append(Character.toChars((int) ((0x20 | (v & 0x1f)) + 63)));
            v >>= 5;
        }
        result.append(Character.toChars((int) (v + 63)));
    }

    /**
     * Encodes an array of GeoPoints into an encoded path string.
     */
    public static String encode(GeoPoint[] path) {
        return encode(Arrays.asList(path));
    }

    /**
     * Parses received OSRM JSON string and stores result in List<Route> Data.osrmRoutes
     */
    protected void parseOsrmResponse(final String json_string){

        Log.d(TAG, "Parsing OSRM response");

        Data.osrmRoutes = new ArrayList<>();

        try {
            JSONObject receivedJson = new JSONObject(json_string);

            if (receivedJson.getString("code").toUpperCase().equals("OK")) {

                JSONArray osrmRoutes = receivedJson.getJSONArray("routes");

                Data.sAlternativesNumber = osrmRoutes.length();
                Log.d(TAG, "Found routes# = " + Data.sAlternativesNumber);

                for (int i = 0; i < osrmRoutes.length(); i++) {

                    Route route = new Route();
                    route.setType("OSRM");

                    JSONObject osrmRoute = osrmRoutes.getJSONObject(i);
                    String encodedPolyline = osrmRoute.getString("geometry");
                    //Log.d(TAG, "EncPolyline: " + encodedPolyline);
                    route.setRoutePoints(decodeToRoutePoints(encodedPolyline));

                    Log.d(TAG, "Route: " + route);

                    float distance = Float.valueOf(osrmRoute.getString("distance"));

                    GpxUtils.simplifyRoute(route, (int)distance / 100, 6d);

                    Log.d(TAG, "Simplified: " + route);

                    /*
                     * Let's use the last route point name (if received) as a temporary route name
                     * In case we received nothing, use a generic, timestamp-based name
                     */
                    String tmpName = "OSMR_" + String.valueOf(System.currentTimeMillis()).substring(7);

                    JSONArray osrmWaypoints = receivedJson.getJSONArray("waypoints");
                    JSONObject lastWaypoint = osrmWaypoints.getJSONObject(osrmWaypoints.length() -1);
                    if (!lastWaypoint.getString("name").isEmpty()) {
                        tmpName = lastWaypoint.getString("name");
                    }
                    route.setName(tmpName);
                    Log.d(TAG, "Named: " + tmpName);

                    Data.osrmRoutes.add(route);
                }

            } else {

                Toast.makeText(getApplicationContext(), receivedJson.getString("code") + ": "
                        + receivedJson.getString("message"), Toast.LENGTH_LONG).show();
            }

        } catch(JSONException e){
            e.printStackTrace();
        }
    }

    protected static Integer[] typeColors = {
            Color.parseColor("#99006600"),
            Color.parseColor("#99b8860b"),
            Color.parseColor("#998b008b"),
            Color.parseColor("#99b22222"),
            Color.parseColor("#99d02090"),
            Color.parseColor("#99d2691e"),
            Color.parseColor("#99a52a2a"),
            Color.parseColor("#99ff8c00"),
            Color.parseColor("#996b8e23"),
            Color.parseColor("#9900bfff"),
            Color.parseColor("#992e8b57"),
            Color.parseColor("#99ff6347"),
            Color.parseColor("#99ff00ff"),
            Color.parseColor("#99f4a460"),
            Color.parseColor("#993cb371"),
            Color.parseColor("#99ffa500")
    };
    protected static int N_COLOURS = typeColors.length;

    protected static Route copyRoute(Route source) {

        Route copy = new Route();

        copy.setName(source.getName());
        copy.setType(source.getType());
        copy.setComment(source.getComment());
        copy.setDescription(source.getDescription());
        copy.setNumber(source.getNumber());
        copy.setSrc(source.getSrc());

        if (!source.getRoutePoints().isEmpty()) {

            for (RoutePoint routePoint : source.getRoutePoints()) {

                RoutePoint newRoutePoint = new RoutePoint();

                newRoutePoint.setDescription(routePoint.getDescription());
                newRoutePoint.setLongitude(routePoint.getLongitude());
                newRoutePoint.setLatitude(routePoint.getLatitude());
                newRoutePoint.setSrc(routePoint.getSrc());
                newRoutePoint.setType(routePoint.getType());
                newRoutePoint.setComment(routePoint.getComment());
                newRoutePoint.setAgeOfGpsData(routePoint.getAgeOfGpsData());
                newRoutePoint.setDgpsid(routePoint.getDgpsid());
                newRoutePoint.setElevation(routePoint.getElevation());
                newRoutePoint.setGeoidHeight(routePoint.getGeoidHeight());
                newRoutePoint.setMagneticDeclination(routePoint.getMagneticDeclination());
                newRoutePoint.setHdop(routePoint.getHdop());
                newRoutePoint.setPdop(routePoint.getPdop());
                newRoutePoint.setSat(routePoint.getSat());
                newRoutePoint.setName(routePoint.getName());
                newRoutePoint.setSym(routePoint.getSym());
                newRoutePoint.setTime(routePoint.getTime());
                newRoutePoint.setVdop(routePoint.getVdop());

                copy.addRoutePoint(newRoutePoint);
            }
        }
        return copy;
    }

    protected static Gpx copyPoiGpx(Gpx source) {

        Gpx copy = new Gpx();

        List<Point> sourcePoints = source.getPoints();

        List<Point> copiedPoints = new ArrayList<>();

        for (Point point : sourcePoints) {

            Point copiedPoint = new Point();

            copiedPoint.setName(point.getName());
            copiedPoint.setLatitude(point.getLatitude());
            copiedPoint.setLongitude(point.getLongitude());
            copiedPoint.setElevation(point.getElevation());
            copiedPoint.setDescription(point.getDescription());
            copiedPoint.setComment(point.getComment());
            copiedPoint.setTime(point.getTime());
            copiedPoint.setType(point.getType());
            copiedPoint.setAgeOfGpsData(point.getAgeOfGpsData());
            copiedPoint.setDgpsid(point.getDgpsid());
            copiedPoint.setGeoidHeight(point.getGeoidHeight());
            copiedPoint.setMagneticDeclination(point.getMagneticDeclination());
            copiedPoint.setSrc(point.getSrc());
            copiedPoint.setSat(point.getSat());
            copiedPoint.setSym(point.getSym());
            copiedPoint.setHdop(point.getHdop());
            copiedPoint.setVdop(point.getVdop());
            copiedPoint.setPdop(point.getPdop());

            copiedPoints.add(copiedPoint);
        }
        copy.setPoints(copiedPoints);
        return copy;
    }

    /*
     * In case user attempted to edit a multi-pointed route (e.g. imported GPX track)
     * we may draw plenty of (useless) route point markers at a time, which will surely
     * slow the device down. To avoid this, let's sort route points by their distance
     * to the map center, and draw first int Data.POINTS_DISPLAY_LIMIT.
     */
    public static List<RoutePoint> getNearestRoutePoints(IGeoPoint mapCenter, Route route) {

        Location mapCenterLoc = new Location("dummy");
        mapCenterLoc.setLatitude(mapCenter.getLatitude());
        mapCenterLoc.setLongitude(mapCenter.getLongitude());

        List<RoutePoint> allRoutePoints = route.getRoutePoints();

        Map<Float,RoutePoint> distanceToRoutePoint = new HashMap<>();

        List<RoutePoint> limitedRoutePoints = new ArrayList<>();

        for (RoutePoint routePoint : allRoutePoints) {

            Location pointLoc = new Location("dummy");
            pointLoc.setLatitude(routePoint.getLatitude());
            pointLoc.setLongitude(routePoint.getLongitude());
            distanceToRoutePoint.put(mapCenterLoc.distanceTo(pointLoc), routePoint);
        }
        List<Float> mapKeys = new ArrayList<>(distanceToRoutePoint.keySet());
        Collections.sort(mapKeys);

        int counter = 0;
        for (Float distance : mapKeys) {

            if (counter == Data.POINTS_DISPLAY_LIMIT) {
                break;
            }

            RoutePoint routePoint = distanceToRoutePoint.get(distance);
            limitedRoutePoints.add(routePoint);

            counter++;
        }
        return limitedRoutePoints;
    }

    public static Route convertTrackToRoute(Track track) {

        Route route = new Route();

        route.setName(track.getName());
        route.setDescription(track.getDescription());
        route.setType(track.getType());
        route.setComment(track.getComment());
        route.setSrc(track.getSrc());
        route.setNumber(track.getNumber());

        for (TrackPoint trackPoint : track.getTrackPoints()) {

            RoutePoint routePoint = new RoutePoint();

            routePoint.setName(trackPoint.getName());
            routePoint.setType(trackPoint.getType());
            routePoint.setLatitude(trackPoint.getLatitude());
            routePoint.setLongitude(trackPoint.getLongitude());
            routePoint.setElevation(trackPoint.getElevation());
            routePoint.setTime(trackPoint.getTime());
            routePoint.setMagneticDeclination(trackPoint.getMagneticDeclination());
            routePoint.setGeoidHeight(trackPoint.getGeoidHeight());
            routePoint.setComment(trackPoint.getComment());
            routePoint.setDescription(trackPoint.getDescription());
            routePoint.setSrc(trackPoint.getSrc());
            routePoint.setSym(trackPoint.getSym());
            routePoint.setFix(trackPoint.getFix());
            routePoint.setSat(trackPoint.getSat());
            routePoint.setHdop(trackPoint.getHdop());
            routePoint.setVdop(trackPoint.getVdop());
            routePoint.setPdop(trackPoint.getPdop());
            routePoint.setAgeOfGpsData(trackPoint.getAgeOfGpsData());
            routePoint.setDgpsid(trackPoint.getDgpsid());

            route.addRoutePoint(routePoint);
        }
        return route;
    }
}