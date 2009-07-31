package no.rehn.android.trafikanten;

import java.text.SimpleDateFormat;
import java.util.List;

import no.rehn.android.trafikanten.route.RoutePlanner;
import no.rehn.android.trafikanten.route.RoutePlanner.StopMatch;
import no.rehn.android.trafikanten.route.RoutePlanner.TravelStage;
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
	static final String LOG_CATEGORY = "stage-detail";
    RoutePlanner mPlanner = new RoutePlanner();
    TravelStage mStage;
	final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        mStage = (TravelStage) extras.getSerializable("stage");
        findMissingLocations(mStage);
        setTitle(String.format("%s to %s", mStage.departureStopName, mStage.arrivalStopName));
        setContentView(R.layout.stagedetail);
        if (mStage.arrivalLocation == null || mStage.departureLocation == null) {
            Toast.makeText(this, "Unknown locations", Toast.LENGTH_LONG).show();
        } else {
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
            Drawable mFromSymbol = getResources().getDrawable(R.drawable.red_pin);
            Drawable mToSymbol = getResources().getDrawable(R.drawable.green_pin);
            GeoPoint from = toGeoPoint(mStage.departureLocation);
			OverlayItem fromItem = new OverlayItem(from, mStage.departureStopName, null);
            GeoPoint to = toGeoPoint(mStage.arrivalLocation);
			OverlayItem toItem = new OverlayItem(to, mStage.arrivalStopName, null);
            overlays.add(new SingleItemOverlay(this, mFromSymbol, fromItem));
            overlays.add(new SingleItemOverlay(this, mToSymbol, toItem));
            MyLocationOverlay myLocationOverlay = new MyLocationOverlay(this, mapView);
            myLocationOverlay.enableMyLocation();
			overlays.add(myLocationOverlay);
			
			int spanLat = Math.abs(from.getLatitudeE6() - to.getLatitudeE6());
			int spanLng = Math.abs(from.getLongitudeE6() - to.getLongitudeE6());
			MapController mapController = mapView.getController();
			mapController.zoomToSpan(spanLat, spanLng);
			mapController.animateTo(getMidPoint(from, to));
        }
    }
    
    //TODO do async
    void findMissingLocations(TravelStage stage) {
        if (stage.arrivalLocation == null) {
            stage.arrivalLocation = findLocationForName(stage.arrivalStopName);
        }
        if (stage.departureLocation == null) {
            stage.departureLocation = findLocationForName(stage.departureStopName);
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
