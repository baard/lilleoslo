package no.rehn.android.lilleoslo;

import no.rehn.android.trafikanten.R;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
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
	private static final int MENU_CONFIG = 1;
	private static final int REQUEST_CONFIG = 1;
	
	private DestinationsAdapter mDestinationsAdaptor;
	private Location mMyLocation;
	private MyLocationListener mLocationListener;
	private LocationManager mLocationService;
	private SharedPreferences mPrefs;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		ListView list = (ListView) findViewById(R.id.destinations);
        mDestinationsAdaptor = new DestinationsAdapter(this, R.layout.destination);
        //TODO fetch from database
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
				intent.putExtra(DirectionsActivity.PARAMETER_FROM, mMyLocation);
				intent.putExtra(DirectionsActivity.PARAMETER_TO, destination.mLocation);
				intent.putExtra(DirectionsActivity.PARAMETER_TITLE, destination.mTitle);
				startActivity(intent);
			}
		});
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		updateCurrentLocationFromPreferences();
		
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

	class MyLocationListener extends LocationAdapter {
		public void onLocationChanged(Location location) {
			mMyLocation = location;
			mDestinationsAdaptor.notifyDataSetInvalidated();
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

	// start intent with result
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_CONFIG, 0, "Preferences").setIcon(android.R.drawable.ic_menu_preferences);
		return true;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CONFIG) {
			updateCurrentLocationFromPreferences();
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_CONFIG:
			Intent intent = new Intent(this, ConfigActivity.class);
			startActivityForResult(intent, REQUEST_CONFIG);
			return true;
		}
		return false;
	}
	// end intent with result

    private void updateCurrentLocationFromPreferences() {
        boolean useMockLocation = mPrefs.getBoolean("use_mock_location", false);
        if (useMockLocation) {
            String lat = mPrefs.getString("my_lat", "0.0");
            String lon = mPrefs.getString("my_lon", "0.0");
            Location location = new Location("MOCK");
            location.setLatitude(Float.parseFloat(lat));
            location.setLongitude(Float.parseFloat(lon));
            mMyLocation = location;
        }
        mDestinationsAdaptor.notifyDataSetInvalidated();
    }
}