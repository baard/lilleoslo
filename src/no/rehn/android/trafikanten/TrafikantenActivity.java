package no.rehn.android.trafikanten;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

public class TrafikantenActivity extends MapActivity {
    GoHomeOverlay goHomeOverlay;
    MapView mapView;
    TextView banner;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        banner = (TextView) findViewById(R.id.banner);
        mapView = (MapView) findViewById(R.id.mapView);
        goHomeOverlay = new GoHomeOverlay(this);
        mapView.getOverlays().add(goHomeOverlay);
    }

    class GoHomeOverlay extends Overlay implements LocationListener {
        Location myLocation;
        Location home;
        Paint myLocationPaint = new Paint();
        Paint homePaint = new Paint();

        public GoHomeOverlay(Context context) {
            myLocationPaint.setColor(Color.BLUE);
            homePaint.setColor(Color.GREEN);
            fixMockHomeLocation();
            LocationManager locationService = (LocationManager) context.getSystemService(LOCATION_SERVICE);
            locationService.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 5, this);
        }

        private void fixMockHomeLocation() {
            // TODO get from prefs
            // 59.900909,10.843645 == oppsal
            home = new Location(LocationManager.GPS_PROVIDER);
            home.setLatitude(59.900909);
            home.setLongitude(10.843645);
        }
        
        public void onLocationChanged(Location location) {
            myLocation = location;
            mapView.getController().animateTo(getMidPoint(toGeoPoint(myLocation), toGeoPoint(home)));
            mapView.getController().zoomToSpan(
                    Math.abs(toGeoPoint(home).getLatitudeE6() - toGeoPoint(myLocation).getLatitudeE6()),
                    Math.abs(toGeoPoint(home).getLongitudeE6() - toGeoPoint(myLocation).getLongitudeE6()));
            Toast.makeText(TrafikantenActivity.this, "Found your location!", Toast.LENGTH_SHORT).show();
        }
        
        public void onProviderDisabled(String provider) {
        }
        
        public void onProviderEnabled(String provider) {
        }
        
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        private GeoPoint toGeoPoint(Location location) {
            return new GeoPoint((int) (location.getLatitude() * 1E6), (int) (location.getLongitude() * 1E6));
        }

        private GeoPoint getMidPoint(GeoPoint a, GeoPoint b) {
            return new GeoPoint((a.getLatitudeE6() + b.getLatitudeE6()) / 2,
                    (a.getLongitudeE6() + b.getLongitudeE6()) / 2);
        }

        @Override
        public void draw(Canvas canvas, MapView mapView, boolean shadow) {
            Projection projection = mapView.getProjection();
            if (myLocation != null) {
                Point myLocationPoint = new Point();
                projection.toPixels(toGeoPoint(myLocation), myLocationPoint);
                canvas.drawCircle(myLocationPoint.x, myLocationPoint.y, 5.0f, myLocationPaint);
                banner.setText(String.format("You are %.1fkm from home!", myLocation.distanceTo(home) / 1000));
            }

            Point homePoint = new Point();
            projection.toPixels(toGeoPoint(home), homePoint);
            canvas.drawCircle(homePoint.x, homePoint.y, 5.0f, homePaint);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add("I wanna go home!").setIcon(android.R.drawable.ic_menu_directions);
        menu.add("Config").setIcon(android.R.drawable.ic_menu_preferences);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        CharSequence menuItem = item.getTitle();
        if (menuItem.equals("I wanna go home!")) {
            if (goHomeOverlay.myLocation == null) {
                Toast.makeText(this, "Your location is not found", Toast.LENGTH_LONG).show();
            } else {
                Intent intent = new Intent(this, DirectionsActivity.class);
                intent.putExtra("from", goHomeOverlay.myLocation);
                intent.putExtra("to", goHomeOverlay.home);
                startActivity(intent);
            }
        } else if (menuItem.equals("Config")) {
            Intent intent = new Intent(this, ConfigActivity.class);
            startActivity(intent);
        }
        return true;
    }

    @Override
    protected boolean isRouteDisplayed() {
        return true;
    }
}