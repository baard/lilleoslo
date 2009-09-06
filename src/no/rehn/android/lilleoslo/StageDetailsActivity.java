package no.rehn.android.lilleoslo;

import java.text.SimpleDateFormat;
import java.util.List;

import no.rehn.android.trafikanten.R;
import no.rehn.android.trafikanten.RoutePlanner;
import no.rehn.android.trafikanten.StopMatch;
import no.rehn.android.trafikanten.TravelStage;
import uk.me.jstott.jcoord.LatLng;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class StageDetailsActivity extends MapActivity {
	public static final String PARAMETER_STAGE = "stage";
    static final String LOG_CATEGORY = "stage-detail";
    RoutePlanner mPlanner = new RoutePlanner();
	final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    private TravelStage mStage;
    private MyLocationOverlay mLocationOverlay;
    
    @Override
    protected void onPause() {
        super.onPause();
        if (mLocationOverlay != null) {
            mLocationOverlay.disableMyLocation();
            mLocationOverlay.disableCompass();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (mLocationOverlay != null) {
            enableMyLocationOverlay();
        }
    }
	
    private void enableMyLocationOverlay() {
        mLocationOverlay.enableCompass();
        mLocationOverlay.enableMyLocation();        
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        mStage = (TravelStage) extras.getSerializable(PARAMETER_STAGE);
        fillInMissingLocations();
        setTitle(getString(R.string.from_to, mStage.departureStopName, mStage.arrivalStopName));
        setContentView(R.layout.stagedetail);
        if (isMissingAnyLocations()) {
            Toast.makeText(this, R.string.unknown_location, Toast.LENGTH_LONG).show();
        } else {
            displayMap();
        }
    }

    private boolean isMissingAnyLocations() {
        return mStage.arrivalLocation == null || mStage.departureLocation == null;
    }

    private void displayMap() {
        GeoPoint from = toGeoPoint(mStage.departureLocation);
        GeoPoint to = toGeoPoint(mStage.arrivalLocation);
        
        // begin map
        MapView mapView = (MapView) findViewById(R.id.stageMap);
        
        LinearLayout zoomLayout = (LinearLayout)findViewById(R.id.zoom);  
        View zoomView = mapView.getZoomControls(); 
    
        zoomLayout.addView(zoomView, 
            new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, 
                LayoutParams.WRAP_CONTENT)); 
        mapView.displayZoomControls(true);
        
        mapView.setSatellite(true);
        List<Overlay> overlays = mapView.getOverlays();
        overlays.add(createItemOverlay(R.drawable.red_pin, from, mStage.departureStopName));
        overlays.add(createItemOverlay(R.drawable.green_pin, to, mStage.arrivalStopName));
        mLocationOverlay = new MyLocationOverlay(this, mapView);
        enableMyLocationOverlay();
        overlays.add(mLocationOverlay);
        
        int spanLat = Math.abs(from.getLatitudeE6() - to.getLatitudeE6());
        int spanLng = Math.abs(from.getLongitudeE6() - to.getLongitudeE6());
        MapController mapController = mapView.getController();
        mapController.zoomToSpan(spanLat + spanLat / 2, spanLng + spanLng / 2);
        mapController.animateTo(getMidPoint(from, to));
        
        // end map
    }
    
    SingleItemOverlay createItemOverlay(int itemResource, GeoPoint location, String popUpText) {
        Drawable symbol = getResources().getDrawable(itemResource);
        OverlayItem fromItem = new OverlayItem(location, popUpText, null);
        return new SingleItemOverlay(this, symbol, fromItem);
    }
    
    //TODO do async
    void fillInMissingLocations() {
        if (mStage.arrivalLocation == null) {
            mStage.arrivalLocation = findLocationForName(mStage.arrivalStopName);
        }
        if (mStage.departureLocation == null) {
            mStage.departureLocation = findLocationForName(mStage.departureStopName);
        }
    }
    
    LatLng findLocationForName(String stopName) {
        try {
            List<StopMatch> stops = mPlanner.findStopByName(stopName, 1);
            if (stops.isEmpty()) {
            	Log.w(LOG_CATEGORY, "zero stops found for: " + stopName);
                return null;
            }
            return stops.get(0).getLocation();
        } catch (Exception e) {
            Log.e(LOG_CATEGORY, "failed to find location: " + stopName, e);
            return null;
        }
    }
    
    private static GeoPoint toGeoPoint(LatLng location) {
        return new GeoPoint((int) (location.getLat() * 1E6), (int) (location.getLng() * 1E6));
    }
    
    private static GeoPoint getMidPoint(GeoPoint a, GeoPoint b) {
		int avgLat = (a.getLatitudeE6() + b.getLatitudeE6()) / 2;
		int avgLng = (a.getLongitudeE6() + b.getLongitudeE6()) / 2;
		return new GeoPoint(avgLat, avgLng);
	}

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }
}
