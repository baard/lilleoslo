package uk.me.jstott.jcoord;

import android.util.Log;
import junit.framework.TestCase;

public class LatLngTest extends TestCase {
	public void testLogConversion() {
		logLatLng(new UTMRef(594930, 6648176, 'V', 32));
		logLatLng(new UTMRef(603133, 6641811, 'V', 32));
	}

	private void logLatLng(UTMRef utm) {
		LatLng latLng = utm.toLatLng();
		Log.i("LAT_LNG", String.format("easting: %s, northing: %s -> lat: %s, lng: %s", utm.getEasting(), utm.getNorthing(), latLng.getLat(), latLng.getLng()));
	}
}
