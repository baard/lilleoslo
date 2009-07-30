package no.rehn.android.trafikanten;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import uk.me.jstott.jcoord.LatLng;

import no.rehn.android.trafikanten.route.RoutePlanner;
import no.rehn.android.trafikanten.route.RoutePlanner.StopMatch;
import no.rehn.android.trafikanten.route.RoutePlanner.TravelProposal;
import no.rehn.android.trafikanten.route.RoutePlanner.TravelStage;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class DirectionsActivity extends Activity {
    static final String LOG_CATEGORY = "directions";
    // revert to 10 for prod
    static final int NEAREST_STOP_COUNT = 8;
    static final int MIN_PERMUTATIONS_TO_EVALUATE = 4;

    static final int MSG_INFO = 1;
    static final int MSG_PROGRESS = 2;
    static final int MSG_ROUTE_FOUND = 3;
    static final int MSG_PROPOSAL_UPDATE = 4;

    static final int MENU_PREVIOUS = 1;
    static final int MENU_NEXT = 2;

    ProgressDialog mProgressDialog;
    List<TravelProposal> mProposals = Collections.emptyList();
    RoutePlanner mPlanner = new RoutePlanner();
    TravelStageAdapter mStageAdaptor;
    EarlyArrivalComparator mProposalComparator = new EarlyArrivalComparator();
    int mProposalIndex = 0;
    
    static {
    	// mock the time
    	GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("Europe/Oslo"));
    	calendar.set(2009, Calendar.JULY, 30, 23, 0);
    	Log.i(LOG_CATEGORY, "Setting time to: " + calendar);
    	TimeUtils.setStaticTime(calendar.getTimeInMillis());
    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_INFO:
                Toast.makeText(DirectionsActivity.this, (String) msg.obj, Toast.LENGTH_LONG).show();
                break;
            case MSG_PROGRESS:
                mProgressDialog.setMessage("Checking route " + msg.arg1 + " of " + msg.arg2);
                break;
            case MSG_ROUTE_FOUND:
                Toast.makeText(DirectionsActivity.this, "Found " + msg.arg1 + " route(s)", Toast.LENGTH_SHORT)
                        .show();
                break;
            case MSG_PROPOSAL_UPDATE:
                mProposalIndex = 0;
                displayProposal();
                mProgressDialog.dismiss();
            }
            super.handleMessage(msg);
        }
    };

    void displayProposal() {
        if (mProposals != null && mProposals.size() > mProposalIndex) {
            mStageAdaptor.clear();
            for (TravelStage stage : mProposals.get(mProposalIndex).stages) {
                mStageAdaptor.add(stage);
            }
        } else {
            Toast.makeText(DirectionsActivity.this, "No route was found", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle b = getIntent().getExtras();
        final Location from = b.getParcelable("from");
        final Location to = b.getParcelable("to");
        setContentView(R.layout.directions);
        ListView list = (ListView) findViewById(R.id.stages);
        mStageAdaptor = new TravelStageAdapter(this, R.layout.stage);
        list.setAdapter(mStageAdaptor);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TravelStage stage = mStageAdaptor.getItem(position);
                Intent stageDetails = new Intent(DirectionsActivity.this, StageDetailsActivity.class);
                stageDetails.putExtra("stage", stage);
                startActivity(stageDetails);
            }
        });
        mProgressDialog = ProgressDialog.show(this, "Calculating routes", "Locating stops...", true, false);
        new Thread() {
            @Override
            public void run() {
                try {
                    mProposals = new TravelProposalRequest(from, to).getProposals();
                } catch (Exception e) {
                    mHandler.sendMessage(mHandler.obtainMessage(MSG_INFO, "Unexpected error"));
                    Log.e(LOG_CATEGORY, "failed to get directions", e);
                }
                mHandler.sendEmptyMessage(MSG_PROPOSAL_UPDATE);
            }
        }.start();
    }

    class TravelProposalRequest {
        final Location mFrom;
        final Location mTo;

        TravelProposalRequest(Location from, Location to) {
            mFrom = from;
            mTo = to;
        }

        List<TravelProposal> getProposals() throws Exception {
            // get possible departures
            List<StopMatch> departureStops = getNearestStops(mFrom);
            // get possible arrivals
            List<StopMatch> arrivalStops = getNearestStops(mTo);

            List<StopMatchPermutation> permutations = createPermutations(departureStops, arrivalStops);
            Log.i(LOG_CATEGORY, "Found " + permutations.size() + " permutations");

            // check all possible routes
            List<TravelProposal> proposals = fetchProposals(permutations);

            // add "just walk home" proposal
            proposals.add(createWalkingProposal());

            Collections.sort(proposals, mProposalComparator);
            return proposals;
        }

        TravelProposal createWalkingProposal() {
            LatLng from = toLatLng(mFrom);
            LatLng to = toLatLng(mTo);
            TravelProposal proposal = new TravelProposal();
            TravelStage walkingStage = new TravelStage();
            walkingStage.departureStopName = "Current location";
            walkingStage.arrivalStopName = "Final destination";
            walkingStage.departureDate = TimeUtils.newCalendar();
            Calendar arrival = (Calendar) walkingStage.departureDate.clone();
            int distance = RoutePlanner.getMinutesBetween(from, to);
            arrival.add(Calendar.MINUTE, distance);
            walkingStage.arrivalDate = arrival;
            walkingStage.transportationName = RoutePlanner.TRANSPORT_WALK;
            walkingStage.arrivalLocation = to;
            walkingStage.departureLocation = from;
            proposal.stages.add(walkingStage);
            return proposal;
        }

        List<StopMatch> getNearestStops(Location from) throws Exception {
            List<StopMatch> stops = mPlanner.findStopByLatLon(from.getLatitude(), from.getLongitude(),
                    NEAREST_STOP_COUNT);
            Log.i(LOG_CATEGORY, "Stops near: " + from.getLatitude() + "," + from.getLongitude() + " was " + stops);
            // filter dead stops (no need to include them in the permutations)
            Iterator<StopMatch> it = stops.iterator();
            while (it.hasNext()) {
                StopMatch stop = it.next();
                // only get one proposal to check if the stop is dead
                List<TravelProposal> travels = mPlanner.findTravelsFrom(stop.fromId, new Date(TimeUtils.currentTimeMillis()), 1);
                if (travels.isEmpty()) {
                    Log.i(LOG_CATEGORY, "Removing dead stop: " + stop.fromId);
                    it.remove();
                } else {
                    Log.i(LOG_CATEGORY, "Stop seems alive: " + stop.fromId);
                }
            }
            return stops;
        }

        List<TravelProposal> fetchProposals(List<StopMatchPermutation> permutations) throws Exception {
            List<TravelProposal> proposals = new LinkedList<TravelProposal>();
            int permutationCount = 0;
            int totalPermutationCount = permutations.size();
            for (StopMatchPermutation permutation : permutations) {
                permutationCount++;
                Log.i(LOG_CATEGORY, "Checking: " + permutation.mFrom.stopName + " to " + permutation.mTo.stopName);
                mHandler.sendMessage(mHandler.obtainMessage(MSG_PROGRESS, permutationCount, totalPermutationCount));

                int distanceToSource = RoutePlanner.getMinutesBetween(toLatLng(mFrom), permutation.mFrom.getLocation());
                // adjust depart-time for distance to source
                Date earliestDeparture = new Date(TimeUtils.currentTimeMillis() + distanceToSource * 60 * 1000);
                List<TravelProposal> travelProposals = mPlanner.findTravelBetween(permutation.mFrom.fromId,
                        permutation.mTo.fromId, earliestDeparture, 1);
                if (!travelProposals.isEmpty()) {
                    TravelProposal proposal = travelProposals.get(0);
                    proposal.addPreStage("Current location", RoutePlanner.TRANSPORT_WALK, toLatLng(mFrom), permutation.mFrom);
                    proposal.addPostStage("Final destination", RoutePlanner.TRANSPORT_WALK, toLatLng(mTo), permutation.mTo);
                    proposals.add(proposal);
                    // last argument is not used
                    mHandler.sendMessage(mHandler.obtainMessage(MSG_ROUTE_FOUND, proposals.size(), 0));
                }
                if (permutationCount > MIN_PERMUTATIONS_TO_EVALUATE && !proposals.isEmpty()) {
                    Log.i(LOG_CATEGORY, "Breaking iteration after " + permutationCount + " permutations");
                    return proposals;
                }
            }
            return proposals;
        }

        List<StopMatchPermutation> createPermutations(List<StopMatch> departures, List<StopMatch> arrivals) {
            List<StopMatchPermutation> permutations = new LinkedList<StopMatchPermutation>();
            for (int i = 0; i < departures.size(); i++) {
                StopMatch departure = departures.get(i);
                for (int j = 0; j < arrivals.size(); j++) {
                    StopMatch arrival = arrivals.get(j);
                    permutations.add(new StopMatchPermutation(departure, arrival));
                }
            }
            Collections.sort(permutations);
            return permutations;
        }

    }

    static class StopMatchPermutation implements Comparable<StopMatchPermutation> {
        final int mTotalAirDistance;
        final StopMatch mFrom;
        final StopMatch mTo;

        public StopMatchPermutation(StopMatch from, StopMatch to) {
            mFrom = from;
            mTo = to;
            // long walking distance is not preferred
            mTotalAirDistance = mFrom.airDistance + mTo.airDistance;
        }

        public int compareTo(StopMatchPermutation another) {
            return mTotalAirDistance - another.mTotalAirDistance;
        }
    }

    static class EarlyArrivalComparator implements Comparator<TravelProposal> {
        public int compare(TravelProposal object1, TravelProposal object2) {
            return (int) (object1.getArrival().getTime() - object2.getArrival().getTime());
        }
    }

    class TravelStageAdapter extends ArrayAdapter<TravelStage> {
        final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

        TravelStageAdapter(Context context, int textViewResourceId) {
            // TODO why is the resource id required here?
            super(context, textViewResourceId);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            // TODO when is this null?
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.stage, null);
            }
            TravelStage o = getItem(position);
            // TODO when is this null?
            if (o != null) {
                TextView route = (TextView) v.findViewById(R.id.route);
                int iconResource = R.drawable.unknown_small;
                if (o.transportationName.equals(RoutePlanner.TRANSPORT_WALK)) {
                	if (o.departureLocation != null && o.arrivalLocation != null) {
                		route.setText(String.format("Walk %.1fkm (estimated)", o.departureLocation.distance(o.arrivalLocation)));
                	} else {
                		route.setText("Walk (estimated)");
                	}
                    iconResource = R.drawable.walk_small;
                } else {
                    route.setText(String.format("%s %s - %s", o.transportationName, o.line, o.destination));
                    if (o.transportationName.equals(RoutePlanner.TRANSPORT_SUBWAY)) {
                        iconResource = R.drawable.subway_small;
                    } else if (o.transportationName.equals(RoutePlanner.TRANSPORT_BUS)) {
                        iconResource = R.drawable.bus_small;
                    }
                }
                ImageView icon = (ImageView) v.findViewById(R.id.icon);
                icon.setImageResource(iconResource);
                TextView fromStop = (TextView) v.findViewById(R.id.fromStop);
                fromStop.setText(o.departureStopName);
                TextView toStop = (TextView) v.findViewById(R.id.toStop);
                toStop.setText(o.arrivalStopName);
                TextView fromTime = (TextView) v.findViewById(R.id.fromTime);
                fromTime.setText(timeFormat.format(o.departureDate.getTime()));
                TextView toTime = (TextView) v.findViewById(R.id.toTime);
                toTime.setText(timeFormat.format(o.arrivalDate.getTime()));
            }
            return v;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_PREVIOUS, Menu.NONE, "Previous route").setIcon(android.R.drawable.ic_media_previous);
        menu.add(Menu.NONE, MENU_NEXT, Menu.NONE, "Next route").setIcon(android.R.drawable.ic_media_next);
        return true;
    }

    static LatLng toLatLng(Location location) {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean enablePrevious = mProposalIndex > 0;
        menu.findItem(MENU_PREVIOUS).setEnabled(enablePrevious);
        boolean enableNext = mProposals.size() > mProposalIndex + 1;
        menu.findItem(MENU_NEXT).setEnabled(enableNext);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_NEXT:
            mProposalIndex++;
            break;
        case MENU_PREVIOUS:
            mProposalIndex--;
            break;
        }
        displayProposal();
        return true;
    }
}
