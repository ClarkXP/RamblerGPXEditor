package pl.nwg.dev.rambler.gpx;

import android.Manifest;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.text.InputFilter;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.TilesOverlay;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.karambola.gpx.beans.RoutePoint;

import static pl.nwg.dev.rambler.gpx.R.id.osmmap;

/**
 * Route Creator activity created by piotr on 02.05.17.
 */
public class RouteEditorActivity extends Utils
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private final String TAG = "Creator";

    private Map<Marker,RoutePoint> markerToRoutePoint;

    private final int MAX_ZOOM_LEVEL = 19;
    private final int MIN_ZOOM_LEVEL = 4;

    Button locationButton;

    Button fitButton;
    Button zoomInButton;
    Button zoomOutButton;
    Button saveButton;

    TextView routePrompt;

    private MapView mMapView;
    private IMapController mapController;

    private MapEventsReceiver mapEventsReceiver;

    private MyLocationNewOverlay mLocationOverlay;

    private RotationGestureOverlay mRotationGestureOverlay;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#33ffffff")));
            actionBar.setStackedBackgroundDrawable(new ColorDrawable(Color.parseColor("#55ffffff")));
        }

        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_route_editor);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        if (Data.sCardinalGeoPoints == null) {
            Data.sCardinalGeoPoints = new ArrayList<>();
        }

        Data.sSelectedAlternative = null;

        setUpMap();

        refreshMap();
    }

    private void setUpMap() {

        mMapView = (MapView) findViewById(osmmap);

        mMapView.setTilesScaledToDpi(true);

        mMapView.setTileSource(TileSourceFactory.MAPNIK);

        TilesOverlay tilesOverlay = mMapView.getOverlayManager().getTilesOverlay();
        tilesOverlay.setOvershootTileCache(tilesOverlay.getOvershootTileCache() * 2);

        mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this),mMapView);
        mLocationOverlay.enableMyLocation();

        mRotationGestureOverlay = new RotationGestureOverlay(mMapView);
        mRotationGestureOverlay.setEnabled(true);

        mMapView.setMaxZoomLevel(MAX_ZOOM_LEVEL);
        mMapView.setMinZoomLevel(MIN_ZOOM_LEVEL);

        mMapView.setMultiTouchControls(true);

        mapController = mMapView.getController();

        mapEventsReceiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {

                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {

                RoutePoint routePoint = new RoutePoint();
                routePoint.setLatitude(p.getLatitude());
                routePoint.setLongitude(p.getLongitude());
                Data.sCopiedRoute.addRoutePoint(routePoint);

                refreshMap();
                return false;
            }
        };

        restoreMapPosition();

        mMapView.setMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                refreshMap();
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                return false;
            }
        });

        setUpButtons();
        setButtonsState();
    }

    private void restoreMapPosition() {

        if (Data.sLastZoom == null) {
            mapController.setZoom(3);
        } else {
            mapController.setZoom(Data.sLastZoom);
        }

        if (Data.sLastCenter == null) {
            mapController.setCenter(new GeoPoint(0d, 0d));
        } else {
            mapController.setCenter(Data.sLastCenter);
        }
    }

    private void refreshMap() {

        mMapView.getOverlays().clear();

        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(mapEventsReceiver);
        mMapView.getOverlays().add(0, mapEventsOverlay);

        mMapView.getOverlays().add(mLocationOverlay);

        mMapView.getOverlays().add(this.mRotationGestureOverlay);

        ScaleBarOverlay mScaleBarOverlay = new ScaleBarOverlay(mMapView);
        mMapView.getOverlays().add(mScaleBarOverlay);

        mScaleBarOverlay.setScaleBarOffset(
                (int) (getResources().getDisplayMetrics().widthPixels / 2 - getResources()
                        .getDisplayMetrics().xdpi / 2), 10);

        Polyline routeOverlay = new Polyline();
        routeOverlay.setColor(Color.parseColor("#0066ff"));

        Data.routeNodes = new ArrayList<>();
        markerToRoutePoint = new HashMap<>();

        List<RoutePoint> allRoutePointsList = Data.sCopiedRoute.getRoutePoints();

        /*
         * Let's limit the number of markers to draw to up to Data.POINTS_DISPLAY_LIMIT nearest to the center of the map
         */
        List<RoutePoint> nearestRoutePoints = Utils.getNearestRoutePoints(mMapView.getMapCenter(), Data.sCopiedRoute);

        for (RoutePoint routePoint : nearestRoutePoints) {

            GeoPoint markerPosition = new GeoPoint(routePoint.getLatitude(), routePoint.getLongitude());

            String displayName;
            if(routePoint.getName() != null && !routePoint.getName().isEmpty()) {
                displayName = routePoint.getName();
            } else {
                displayName = String.valueOf(allRoutePointsList.indexOf(routePoint));
            }

            Drawable icon = new BitmapDrawable(getResources(), makeMarkerBitmap(this, displayName));

            Marker marker = new Marker(mMapView);
            marker.setPosition(markerPosition);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setDraggable(true);
            marker.setIcon(icon);

            markerToRoutePoint.put(marker, routePoint);
            mMapView.getOverlays().add(marker);

            marker.setOnMarkerDragListener(new Marker.OnMarkerDragListener() {
                @Override
                public void onMarkerDrag(Marker marker) {}

                @Override
                public void onMarkerDragEnd(Marker marker) {

                    RoutePoint draggedPoint = markerToRoutePoint.get(marker);
                    draggedPoint.setLatitude(marker.getPosition().getLatitude());
                    draggedPoint.setLongitude(marker.getPosition().getLongitude());

                    refreshMap();
                }

                @Override
                public void onMarkerDragStart(Marker marker) {}
            });

            marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker, MapView mapView) {

                    RoutePoint clickedPoint = markerToRoutePoint.get(marker);
                    displayEditDialog(clickedPoint);

                    return false;
                }
            });
        }

        /*
         * And now we need all RoutePoints to draw the full path
         */
        for (int i = 0; i < allRoutePointsList.size(); i++) {

            RoutePoint routePoint = allRoutePointsList.get(i);
            GeoPoint node = new GeoPoint(routePoint.getLatitude(), routePoint.getLongitude());
            Data.routeNodes.add(node);
        }
        routeOverlay.setPoints(Data.routeNodes);
        mMapView.getOverlays().add(routeOverlay);

        routePrompt.setText(String.format(getResources().getString(R.string.map_prompt_route), Data.routeNodes.size()));

        mMapView.invalidate();
        setButtonsState();
    }

    private void setUpButtons() {

        locationButton = (Button) findViewById(R.id.location_button);
        locationButton.setEnabled(false);
        locationButton.getBackground().setAlpha(0);
        locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapController.setZoom(18);
                mapController.setCenter(Data.sCurrentPosition);
                setButtonsState();
            }
        });

        fitButton = (Button) findViewById(R.id.fit_button);
        fitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Data.routeNodes != null && Data.routeNodes.size() > 1) {
                    mMapView.zoomToBoundingBox(findBoundingBox(Data.routeNodes), true);
                }
                setButtonsState();
            }
        });
        zoomInButton = (Button) findViewById(R.id.zoom_in_button);
        zoomInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mapController.setZoom(mMapView.getProjection().getZoomLevel() +1);
                setButtonsState();
            }
        });
        zoomOutButton = (Button) findViewById(R.id.zoom_out_button);
        zoomOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mapController.setZoom(mMapView.getProjection().getZoomLevel() -1);
                setButtonsState();
            }
        });

        saveButton = (Button) findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent i = new Intent(RouteEditorActivity.this, RoutesBrowserActivity.class);

                if (Data.sSelectedRouteIdx != null) {

                    Data.sRoutesGpx.removeRoute(Data.sFilteredRoutes.get(Data.sSelectedRouteIdx));
                    Data.sRoutesGpx.addRoute(Data.sCopiedRoute);
                    Data.sSelectedRouteIdx = Data.sRoutesGpx.getRoutes().indexOf(Data.sCopiedRoute);

                } else {

                    Data.sRoutesGpx.addRoute(Data.sCopiedRoute);
                    Data.sSelectedRouteIdx = Data.sRoutesGpx.getRoutes().indexOf(Data.sCopiedRoute);
                    setResult(Data.NEW_ROUTE_ADDED, i);
                }
                finish();
            }
        });

        routePrompt = (TextView) findViewById(R.id.route_prompt);

        final TextView copyright = (TextView) findViewById(R.id.copyright);
        copyright.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void setButtonsState() {

        if (mMapView.getProjection().getZoomLevel() < MAX_ZOOM_LEVEL) {
            zoomInButton.setEnabled(true);
            zoomInButton.getBackground().setAlpha(255);
        } else {
            zoomInButton.setEnabled(false);
            zoomInButton.getBackground().setAlpha(100);
        }

        if (mMapView.getProjection().getZoomLevel() > MIN_ZOOM_LEVEL) {
            zoomOutButton.setEnabled(true);
            zoomOutButton.getBackground().setAlpha(255);
        } else {
            zoomOutButton.setEnabled(false);
            zoomOutButton.getBackground().setAlpha(100);
        }

        if (Data.sCopiedRoute.isChanged()) {
            saveButton.setEnabled(true);
            saveButton.getBackground().setAlpha(255);
        } else {
            saveButton.setEnabled(false);
            saveButton.getBackground().setAlpha(100);
        }

    }

    /**
     * Route Point edition
     */
    private void displayEditDialog(final RoutePoint routePoint) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();
        final View layout = inflater.inflate(R.layout.waypoint_edit_dialog, null);

        final EditText editName = (EditText) layout.findViewById(R.id.wp_edit_name);
        editName.setFilters(new InputFilter[] {
                new InputFilter.LengthFilter(19)
        });
        if (routePoint.getName() != null) {
            editName.setText(routePoint.getName());
        }

        String dialogTitle = getResources().getString(R.string.waypoint_edit_title);
        String okText = getResources().getString(R.string.dialog_apply);

        String deleteText = getResources().getString(R.string.dialog_delete);
        String insertText = getResources().getString(R.string.dialog_insert_b4);

        builder.setTitle(dialogTitle)
                .setView(layout)
                .setIcon(R.drawable.map_edit)
                .setCancelable(true)
                .setPositiveButton(okText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {


                        String name = editName.getText().toString().trim();
                        if (!name.isEmpty()) {
                            if (name.length() > 20) name = name.substring(0, 21);
                            routePoint.setName(name);

                        } else {
                            routePoint.setName(null);
                        }

                        refreshMap();
                    }
                })
                .setNegativeButton(insertText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        int idx = Data.sCopiedRoute.getRoutePoints().lastIndexOf(routePoint);
                        double this_point_lat = routePoint.getLatitude();
                        double this_point_lon = routePoint.getLongitude();

                        RoutePoint previousPoint = Data.sCopiedRoute.getRoutePoints().get(idx - 1);
                        double previous_point_lat = previousPoint.getLatitude();
                        double previous_point_lon = previousPoint.getLongitude();

                        RoutePoint halfway_point = new RoutePoint();
                        Data.sCopiedRoute.addRoutePoint(idx, halfway_point);
                        halfway_point.setLatitude((this_point_lat + previous_point_lat) * 0.5);
                        halfway_point.setLongitude((this_point_lon + previous_point_lon) * 0.5);

                        refreshMap();
                    }
                })
                .setNeutralButton(deleteText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        Data.sCopiedRoute.removeRoutePoint(routePoint);

                        refreshMap();
                    }
                });


        final AlertDialog alert = builder.create();

        alert.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {

                if (Data.sCopiedRoute.getRoutePoints().lastIndexOf(routePoint) == 0) {
                    ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
                }
            }
        });

        final Button instertStartButton = (Button) layout.findViewById(R.id.start_end_button);
        instertStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alert.dismiss();
                insertStartEnd(routePoint);
            }
        });

        final Button endHereButton = (Button) layout.findViewById(R.id.end_here_button);
        endHereButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alert.dismiss();
                endHere(routePoint);
            }
        });

        if (Data.sCopiedRoute.getRoutePoints().lastIndexOf(routePoint) == 0 ||
                Data.sCopiedRoute.getRoutePoints().lastIndexOf(routePoint) == Data.sCopiedRoute.getRoutePoints().size() -1) {
            instertStartButton.setEnabled(false);
            instertStartButton.setTextColor(Color.argb(80, 50, 50, 50));

            endHereButton.setEnabled(false);
            endHereButton.setTextColor(Color.argb(80, 50, 50, 50));
        }
        alert.show();
    }

    private void insertStartEnd (RoutePoint routePoint) {

        int idx = Data.sCopiedRoute.getRoutePoints().lastIndexOf(routePoint);

        List<RoutePoint> oldRoute = Data.sCopiedRoute.getRoutePoints();

        List<RoutePoint> newRoute = new ArrayList<>();

        for (int i = idx; i < oldRoute.size(); i++) {

            newRoute.add(oldRoute.get(i));

        }

        for (int i = 1; i < idx; i++) {

            newRoute.add(oldRoute.get(i));

        }

        RoutePoint lastPoint = new RoutePoint();
        lastPoint.setLatitude(newRoute.get(0).getLatitude() - 0.0002d);
        lastPoint.setLongitude(newRoute.get(0).getLongitude() - 0.0002d);

        newRoute.add(lastPoint);

        Data.sCopiedRoute.setRoutePoints(newRoute);

        refreshMap();
    }

    private void endHere(RoutePoint routePoint) {

        int idx = Data.sCopiedRoute.getRoutePoints().lastIndexOf(routePoint);

        List<RoutePoint> oldRoute = Data.sCopiedRoute.getRoutePoints();

        List<RoutePoint> newRoute = new ArrayList<>();

        for (int i = 0; i < idx+1; i++) {

            newRoute.add(oldRoute.get(i));

        }

        Data.sCopiedRoute.setRoutePoints(newRoute);

        refreshMap();
    }

    public void onResume(){
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));

        mGoogleApiClient.connect();
        restoreMapPosition();
    }

    @Override
    public void onLocationChanged(Location location) {

        try {

            Data.sCurrentPosition = new GeoPoint(location.getLatitude(), location.getLongitude());

            locationButton.setEnabled(true);
            locationButton.getBackground().setAlpha(255);

        } catch(Exception e) {

            locationButton.setEnabled(false);
            locationButton.getBackground().setAlpha(0);

            Log.d(TAG, "Error getting location: " + e);
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnected: " + connectionHint);
        }

        try {
            LocationRequest locationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(30000)
                    .setSmallestDisplacement(0);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
            }

        } catch (Exception e) {

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Error getting location: " + e);
            }
        }
    }

    @Override // GoogleApiClient.ConnectionCallbacks
    public void onConnectionSuspended(int cause) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnectionSuspended: " + cause);
        }
    }

    @Override // GoogleApiClient.OnConnectionFailedListener
    public void onConnectionFailed(ConnectionResult result) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnectionFailed: " + result);
        }
    }

    @Override
    protected void onPause() {

        super.onPause();

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        Data.sLastZoom = mMapView.getZoomLevel();
        Data.sLastCenter = new GeoPoint(mMapView.getMapCenter().getLatitude(), mMapView.getMapCenter().getLongitude());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        /*
         * Handle the back button
         */
        if(keyCode == KeyEvent.KEYCODE_BACK ) {

            /*
             * If data changed
             */
            if (Data.sCopiedRoute.isChanged() && Data.sCopiedRoute.getRoutePoints().size() > 1) {
                new AlertDialog.Builder(this)
                        .setIcon(R.drawable.map_warning)
                        .setTitle(R.string.dialog_save_changes_title)
                        .setMessage(R.string.dialog_route_changed_message)

                        .setPositiveButton(R.string.dialog_apply, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                if (!Data.sCopiedRoute.getRoutePoints().isEmpty()) {

                                    Data.sRoutesGpx.removeRoute(Data.sFilteredRoutes.get(Data.sSelectedRouteIdx));
                                    Data.sRoutesGpx.addRoute(Data.sCopiedRoute);

                                    Data.sSelectedRouteIdx = null; // index might have changed, clear selection
                                }
                                finish();
                            }
                        })
                        .setNegativeButton(R.string.dialog_discard, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                finish();
                            }

                        })
                        .show();
                return true;

            } else {

                finish();
                return true;
            }

        } else {
            return super.onKeyDown(keyCode, event);
        }
    }
}