package no.rehn.android.lilleoslo;

import java.net.InetSocketAddress;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import uk.me.jstott.jcoord.LatLng;

import no.rehn.android.trafikanten.R;
import no.rehn.android.trafikanten.RoutePlanner;
import no.rehn.android.trafikanten.StopMatch;
import no.rehn.android.trafikanten.TravelProposal;
import no.rehn.android.trafikanten.TravelStage;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

public class DirectionsActivity extends Activity {
    public static final String PARAMETER_TITLE = "title";
    public static final String PARAMETER_TO = "to";
    public static final String PARAMETER_FROM = "from";
    
    private static final String LOG_CATEGORY = "DIRECTIONS";
    private static final int NEAREST_STOP_COUNT = 8;
    private static final int MIN_PERMUTATIONS_TO_EVALUATE = 4;

    private static final int MSG_INFO = 1;
    private static final int MSG_PROGRESS = 2;
    private static final int MSG_PROPOSAL_UPDATE = 3;
    private static final int MSG_DEPARTURE_STOPS = 4;
    private static final int MSG_ARRIVAL_STOPS = 5;

    private static final int MENU_PREVIOUS = 1;
    private static final int MENU_NEXT = 2;

    private ProgressDialog mProgressDialog;
    private List<TravelProposal> mProposals = Collections.emptyList();
    private RoutePlanner mPlanner;
    private TravelStageAdapter mStageAdaptor;
    private Comparator<TravelProposal> mProposalComparator = new EarlyArrivalComparator();
    private int mProposalIndex = 0;
    private String mTitle;
    
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_ARRIVAL_STOPS:
                mProgressDialog.setMessage(getString(R.string.analyzing_arrivals));
                break;
            case MSG_DEPARTURE_STOPS:
                mProgressDialog.setMessage(getString(R.string.analyzing_departures));
                break;
            case MSG_INFO:
                Toast.makeText(DirectionsActivity.this, (String) msg.obj, Toast.LENGTH_LONG).show();
                break;
            case MSG_PROGRESS:
                // start i18n
                mProgressDialog.setMessage(getString(R.string.checking_route, msg.arg1 ,msg.arg2));
                // end i18n
                break;
            case MSG_PROPOSAL_UPDATE:
                mProposalIndex = 0;
                displayProposal();
                mProgressDialog.dismiss();
            }
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        
        // start receive intent
        Bundle b = getIntent().getExtras();
        final Location from = b.getParcelable(PARAMETER_FROM);
        final Location to = b.getParcelable(PARAMETER_TO);
        mTitle = b.getString(PARAMETER_TITLE);
        // end receive intent
        
        setTitle(mTitle);
        setContentView(R.layout.directions);
        mPlanner = createRoutePlanner(PreferenceManager.getDefaultSharedPreferences(this));
        ListView list = (ListView) findViewById(R.id.stages);
        mStageAdaptor = new TravelStageAdapter(this, R.layout.stage);
        list.setAdapter(mStageAdaptor);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // start send intent
                TravelStage stage = mStageAdaptor.getItem(position);
                Intent stageDetails = new Intent(DirectionsActivity.this, StageDetailsActivity.class);
                stageDetails.putExtra(StageDetailsActivity.PARAMETER_STAGE, stage);
                startActivity(stageDetails);
                // end send intent
            }
        });
        mProgressDialog = ProgressDialog.show(this, getString(R.string.calculating_routes), getString(R.string.please_wait), true, false);
        new Thread() {
            @Override
            public void run() {
                try {
                    mProposals = new TravelProposalRequest(from, to).getProposals();
                } catch (Exception e) {
                    mHandler.sendMessage(mHandler.obtainMessage(MSG_INFO, getString(R.string.unexpected_error)));
                    Log.e(LOG_CATEGORY, "failed to get directions", e);
                }
                mHandler.sendEmptyMessage(MSG_PROPOSAL_UPDATE);
            }
        }.start();
    }

    private void displayProposal() {
        if (mProposals != null && mProposals.size() > mProposalIndex) {
            mStageAdaptor.clear();
            TravelProposal proposal = mProposals.get(mProposalIndex);
            for (TravelStage stage : proposal.stages) {
                mStageAdaptor.add(stage);
            }
            long durationMillis = proposal.getArrival().getTime() - proposal.getDeparture().getTime();
            Log.i(LOG_CATEGORY, "total travel " + durationMillis);
            long seconds = durationMillis / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            minutes = minutes % 60;
            setTitle(getString(R.string.proposal_title, mTitle, hours, minutes));
        } else {
            Toast.makeText(DirectionsActivity.this, getString(R.string.no_route), Toast.LENGTH_LONG).show();
        }
    }

	private RoutePlanner createRoutePlanner(SharedPreferences prefs) {
        boolean useProxy = prefs.getBoolean("use_proxy", false);
        
        RoutePlanner planner = new RoutePlanner();
        boolean useMockTime = prefs.getBoolean("use_mock_time", false);
        if (useMockTime) {
            String timeString = prefs.getString("time", "0");
            planner.setStaticTime(Long.parseLong(timeString) * 1000);
        }
        if (useProxy) {
    		String host = prefs.getString("proxy_host", null);
    		String port = prefs.getString("proxy_port", null);
    		planner.setProxyAddress(new InetSocketAddress(host, Integer.parseInt(port)));
        }
        String walkingSpeed = prefs.getString("walking_speed", "5");
        planner.setWalkingSpeed(Double.parseDouble(walkingSpeed));
        return planner;
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
            mHandler.sendMessage(mHandler.obtainMessage(MSG_DEPARTURE_STOPS));
            List<StopMatch> departureStops = getNearestStops(mFrom);
            // get possible arrivals
            mHandler.sendMessage(mHandler.obtainMessage(MSG_ARRIVAL_STOPS));
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
            TravelStage walkingStage = proposal.createStage();
            walkingStage.departureStopName = getString(R.string.current_location);
            walkingStage.arrivalStopName = getString(R.string.final_destination);
            walkingStage.departureDate = mPlanner.createCalendar();
            Calendar arrival = (Calendar) walkingStage.departureDate.clone();
            int distance = mPlanner.getMinutesBetween(from, to);
            arrival.add(Calendar.MINUTE, distance);
            walkingStage.arrivalDate = arrival;
            walkingStage.transportationName = RoutePlanner.TRANSPORT_WALK;
            walkingStage.arrivalLocation = to;
            walkingStage.departureLocation = from;
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
                List<TravelProposal> travels = mPlanner.findTravelsFrom(stop.fromId, mPlanner.createCurrentDate(), 1);
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

                int distanceToSource = mPlanner.getMinutesBetween(toLatLng(mFrom), permutation.mFrom.getLocation());
                // adjust depart-time for distance to source
                Date earliestDeparture = new Date(mPlanner.currentTimeMillis() + distanceToSource * 60 * 1000);
                List<TravelProposal> travelProposals = mPlanner.findTravelBetween(permutation.mFrom.fromId,
                        permutation.mTo.fromId, earliestDeparture, 1);
                if (!travelProposals.isEmpty()) {
                    TravelProposal proposal = travelProposals.get(0);
                    proposal.addPreStage(getString(R.string.current_location), RoutePlanner.TRANSPORT_WALK, toLatLng(mFrom), permutation.mFrom, mPlanner.getMinutesBetween(toLatLng(mFrom), permutation.mFrom.getLocation()));
                    proposal.addPostStage(getString(R.string.final_destination), RoutePlanner.TRANSPORT_WALK, toLatLng(mTo), permutation.mTo, mPlanner.getMinutesBetween(toLatLng(mTo), permutation.mTo.getLocation()));
                    proposals.add(proposal);
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

    static LatLng toLatLng(Location location) {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }
    
    // start menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_PREVIOUS, Menu.NONE, R.string.previous_route).setIcon(
                android.R.drawable.ic_media_previous);
        menu.add(Menu.NONE, MENU_NEXT, Menu.NONE, R.string.next_route).setIcon(android.R.drawable.ic_media_next);
        return true;
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
            next();
            break;
        case MENU_PREVIOUS:
            previous();
            break;
        }
        return true;
    }
    // end menu

	private void next() {
        if (mProposals.size() > mProposalIndex + 1) {
        	mProposalIndex++;
        	displayProposal();
        }
	}

	private void previous() {
        if (mProposalIndex > 0) {
        	mProposalIndex--;
        	displayProposal();
        }
	}
}
