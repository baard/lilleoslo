package no.rehn.android.trafikanten;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class TrafikantenActivity extends Activity {
	private static final int MENU_MOCK = 0;
	private static final int MENU_CONFIG = 1;
	private static final int REQUEST_CONFIG = 1;
	DestinationsAdapter mDestinationsAdaptor;
	Location mMyLocation;
	MyLocationListener mLocationListener;
	LocationManager mLocationService;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle("Trafikanten");
		setContentView(R.layout.main);
		ListView list = (ListView) findViewById(R.id.destinations);
		mDestinationsAdaptor = new DestinationsAdapter(this,
				R.layout.destination);
		mDestinationsAdaptor.add(createDestination("Home", 59.9009047, 10.843648));
		mDestinationsAdaptor.add(createDestination("Work", 59.40, 10.94));
		list.setAdapter(mDestinationsAdaptor);
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Destination destination = mDestinationsAdaptor
						.getItem(position);

				Intent intent = new Intent(TrafikantenActivity.this,
						DirectionsActivity.class);
				intent.putExtra("from", mMyLocation);
				intent.putExtra("to", destination.mLocation);
				intent.putExtra("title", destination.mTitle);
				startActivity(intent);
			}
		});
		
		mLocationService = (LocationManager) getSystemService(LOCATION_SERVICE);
		mLocationListener = new MyLocationListener();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mLocationService.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 5, mLocationListener);	
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mLocationService.removeUpdates(mLocationListener);
	}
	
	private Destination createDestination(String title, double lat, double lon) {
		Location location = new Location("MOCK");
		location.setLatitude(lat);
		location.setLongitude(lon);
		return new Destination(title, location);
	}

	class MyLocationListener implements LocationListener {
		public void onLocationChanged(Location location) {
			mMyLocation = location;
			mDestinationsAdaptor.notifyDataSetInvalidated();
		}

		public void onProviderDisabled(String provider) {
		}

		public void onProviderEnabled(String provider) {
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	}

	class Destination {
		final String mTitle;
		final Location mLocation;
		
		Destination(String title, Location location) {
			mTitle = title;
			mLocation = location;
		}
	}

	class DestinationsAdapter extends ArrayAdapter<Destination> {
		DestinationsAdapter(Context context, int textViewResourceId) {
			// TODO why is the resource id required here?
			super(context, textViewResourceId);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			// TODO when is this null?
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.destination, null);
			}
			Destination o = getItem(position);
			// TODO when is this null?
			if (o != null) {
				TextView title = (TextView) v
						.findViewById(R.id.destinationTitle);
				title.setText(o.mTitle);
				TextView distance = (TextView) v
						.findViewById(R.id.destinationDistance);
				if (mMyLocation != null) {
					double km = o.mLocation.distanceTo(mMyLocation) / 1000;
					distance.setText(String.format("%.1fkm", km));
				} else {
					distance.setText("Waiting for location..");
				}
			}
			return v;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_CONFIG, 0, "Config").setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(0, MENU_MOCK, 0, "Mock location").setIcon(android.R.drawable.ic_menu_mylocation);
		return true;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CONFIG) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			String lat = prefs.getString("my_lat", "0.0");
			String lon = prefs.getString("my_lon", "0.0");
			Location location = new Location("MOCK");
			location.setLatitude(Float.parseFloat(lat));
			location.setLongitude(Float.parseFloat(lon));
			mMyLocation = location;
			mDestinationsAdaptor.notifyDataSetInvalidated();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_CONFIG:
			Intent intent = new Intent(this, ConfigActivity.class);
			startActivityForResult(intent, REQUEST_CONFIG);
			return true;
		case MENU_MOCK:
			return true;
		}
		return false;
	}
}