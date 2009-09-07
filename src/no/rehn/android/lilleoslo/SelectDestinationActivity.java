package no.rehn.android.lilleoslo;

import java.io.IOException;
import java.util.List;

import no.rehn.android.trafikanten.R;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Contacts;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class SelectDestinationActivity extends Activity {
    private static final String LOG_CATEGORY = "SDA";
    private static final int MENU_TRAVEL = 0;
    private static final int MENU_CONFIG = 1;
    
    private static final int REQUEST_CONFIG = 1;
    
    private DestinationsAdapter mDestinationsAdaptor;
    private Location mMyLocation;
    private MyLocationListener mLocationListener;
    private LocationManager mLocationService;
    private SharedPreferences mPrefs;
    DestinationDbHelper mStorage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setTitle(R.string.destinations);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mStorage = new DestinationDbHelper(this);
        
        ListView list = (ListView) findViewById(R.id.destinations);
        mDestinationsAdaptor = new DestinationsAdapter(this, R.layout.destination);
        //TODO fetch from database
        list.setAdapter(mDestinationsAdaptor);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                Destination destination = mDestinationsAdaptor
                        .getItem(position);

                findDirections(destination);
            }
        });
        registerForContextMenu(list);
        mLocationListener = new MyLocationListener();
        mLocationService = (LocationManager) getSystemService(LOCATION_SERVICE);
        initLocationListener();
        updateCurrentLocationFromPreferences();
    }

    private void findDirections(Destination destination) {
        Intent intent = new Intent(SelectDestinationActivity.this,
                DirectionsActivity.class);
        intent.putExtra(DirectionsActivity.PARAMETER_FROM, mMyLocation);
        intent.putExtra(DirectionsActivity.PARAMETER_TO, destination.location);
        intent.putExtra(DirectionsActivity.PARAMETER_TITLE, destination.name);
        startActivity(intent);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        menu.add("Find Route");
        menu.add("Show on map");
        menu.add("Delete Destination");
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if ("Delete Destination".equals(item.getTitle())) {
            AdapterView.AdapterContextMenuInfo menuInfo;
            try {
                menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            } catch (ClassCastException e) {
                return false;
            }
            Destination destination = mDestinationsAdaptor.getItem(menuInfo.position);
            mStorage.remove(destination);
            updateList();
            return true;
        } else if ("Find Route".equals(item.getTitle())) {
            AdapterView.AdapterContextMenuInfo menuInfo;
            try {
                menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            } catch (ClassCastException e) {
                return false;
            }
            Destination destination = mDestinationsAdaptor.getItem(menuInfo.position);
            findDirections(destination);
        } else if ("Show on map".equals(item.getTitle())) {
            AdapterView.AdapterContextMenuInfo menuInfo;
            try {
                menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            } catch (ClassCastException e) {
                return false;
            }
            Destination destination = mDestinationsAdaptor.getItem(menuInfo.position);
            Intent mapIntent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse("geo:"
                    + destination.location.getLatitude() + "," + destination.location.getLongitude()));
            startActivity(mapIntent);
        }
        return false;
        
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
                title.setText(o.name);
                TextView distance = (TextView) v
                        .findViewById(R.id.destinationDistance);
                if (mMyLocation != null) {
                    double km = o.location.distanceTo(mMyLocation) / 1000;
                    distance.setText(String.format("%.1f km", km));
                } else {
                    distance.setText("Waiting for location..");
                }
            }
            return v;
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        initLocationListener();
    }

    private void initLocationListener() {
        mLocationService.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 5, mLocationListener);        
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLocationService.removeUpdates(mLocationListener);
    }

    class MyLocationListener extends LocationAdapter {
        public void onLocationChanged(Location location) {
            mMyLocation = location;
            updateList();
        }
    }

    // start intent with result
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_TRAVEL, 0, "Add Contacts").setIcon(android.R.drawable.ic_menu_add);
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
        case MENU_TRAVEL:
            selectContactAddress();
            return true;
        }
        return false;
    }
    // end intent with result

    private void selectContactAddress() {
        final Cursor result = managedQuery(Contacts.ContactMethods.CONTENT_URI, null, Contacts.ContactMethods.KIND + "=" + Contacts.KIND_POSTAL, null, null);
        int[] layoutIds = new int[] { R.id.name, R.id.address};
        String[] viewColumns = new String[] {Contacts.ContactMethods.NAME, Contacts.ContactMethods.DATA};
        SimpleCursorAdapter simpleCursorAdapter = new SimpleCursorAdapter(this, R.layout.contact, result, viewColumns, layoutIds);
        final Dialog dialog = new Dialog(this);
        dialog.setTitle("Select destination");
        dialog.setContentView(R.layout.contacts);
        ListView contacts = (ListView) dialog.findViewById(R.id.contacts);
        contacts.setAdapter(simpleCursorAdapter);
        contacts.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                result.moveToPosition(position);
                String address = result.getString(result.getColumnIndexOrThrow(Contacts.ContactMethods.DATA));
                String name = result.getString(result.getColumnIndexOrThrow(Contacts.ContactMethods.NAME));
                int type = result.getInt(result.getColumnIndexOrThrow(Contacts.ContactMethods.TYPE));
                String typeName = getTypeName(type);
                Geocoder geocoder = new Geocoder(SelectDestinationActivity.this);
                try {
                    List<Address> addresses = geocoder.getFromLocationName(address, 1);
                    if (addresses.isEmpty()) {
                        Toast.makeText(SelectDestinationActivity.this, "No such address", Toast.LENGTH_LONG);
                    }
                    //TODO display list of possible locations
                    Address location = addresses.get(0);
                    Location destination = createLocation(location.getLatitude(), location.getLongitude());
                    Destination dest = new Destination(name + typeName, destination);
                    mStorage.save(dest);
                    updateList();
                    //findDirections(dest);
                    
                } catch (IOException e) {
                    Log.e(LOG_CATEGORY, "lookup failed", e);
                    Toast.makeText(SelectDestinationActivity.this, "Lookup failed", Toast.LENGTH_LONG);
                }
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private String getTypeName(int type) {
        switch (type) {
        case Contacts.ContactMethods.TYPE_HOME:
            return " at home";
        case Contacts.ContactMethods.TYPE_WORK:
            return " at work";
        default:
            return "";
        }
    }

    private Location createLocation(double lat, double lon) {
        Location location = new Location("MOCK");
        location.setLatitude(lat);
        location.setLongitude(lon);
        return location;
    }

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
        updateList();
    }

    private void updateList() {
        List<Destination> allEntries = mStorage.getAllEntries();
        mDestinationsAdaptor.clear();
        for (Destination destination : allEntries) {
            mDestinationsAdaptor.add(destination);
        }
        mDestinationsAdaptor.notifyDataSetInvalidated();
    }
}