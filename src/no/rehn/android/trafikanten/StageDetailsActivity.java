package no.rehn.android.trafikanten;

import java.util.List;

import no.rehn.android.trafikanten.route.RoutePlanner;
import no.rehn.android.trafikanten.route.RoutePlanner.StopMatch;
import no.rehn.android.trafikanten.route.RoutePlanner.TravelStage;
import uk.me.jstott.jcoord.LatLng;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

public class StageDetailsActivity extends MapActivity {
    RoutePlanner mPlanner = new RoutePlanner();
    TravelStage mStage;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        mStage = (TravelStage) extras.getSerializable("stage");
        findMissingLocations(mStage);
        setContentView(R.layout.stagedetail);
        if (mStage.arrivalLocation == null || mStage.departureLocation == null) {
            Toast.makeText(this, "Unknown locations", Toast.LENGTH_LONG).show();
        } else {
            MapView mapView = (MapView) findViewById(R.id.stageMap);
            mapView.getOverlays().add(new StageOverlay(this, mapView));
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
                return null;
            }
            return stops.get(0).getLocation();
        } catch (Exception e) {
            Log.e("detail", "failed to find location: " + stopName, e);
            return null;
        }
    }
    
    private static GeoPoint toGeoPoint(LatLng location) {
        return new GeoPoint((int) (location.getLat() * 1E6), (int) (location.getLng() * 1E6));
    }
    
    class StageOverlay extends Overlay {
        final GeoPoint mFrom;
        final GeoPoint mTo;
        final Paint mPaint = new Paint();
        public StageOverlay(Context context, MapView mapView) {
            MapController mapController = mapView.getController();
            mFrom = toGeoPoint(mStage.departureLocation);
            mTo = toGeoPoint(mStage.arrivalLocation);
            int spanLat = Math.abs(mFrom.getLatitudeE6() - mTo.getLatitudeE6());
            int spanLng = Math.abs(mFrom.getLongitudeE6() - mTo.getLongitudeE6());
            mapController.zoomToSpan(spanLat, spanLng);
            mapController.animateTo(getMidPoint(mFrom, mTo));
        }
        
        private GeoPoint getMidPoint(GeoPoint a, GeoPoint b) {
            int avgLat = (a.getLatitudeE6() + b.getLatitudeE6()) / 2;
            int avgLng = (a.getLongitudeE6() + b.getLongitudeE6()) / 2;
            return new GeoPoint(avgLat, avgLng);
        }

        @Override
        public void draw(Canvas canvas, MapView mapView, boolean shadow) {
            Projection projection = mapView.getProjection();
            markPosition(projection, canvas, mFrom);
            markPosition(projection, canvas, mTo);
        }

        void markPosition(Projection projection, Canvas canvas, GeoPoint point) {
            Point projectedPoint = new Point();
            projection.toPixels(point, projectedPoint);
            canvas.drawCircle(projectedPoint.x, projectedPoint.y, 7.0f, mPaint);
        }
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }
}
