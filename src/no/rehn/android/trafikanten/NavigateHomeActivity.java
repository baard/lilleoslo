package no.rehn.android.trafikanten;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import no.rehn.android.trafikanten.route.RoutePlanner;
import no.rehn.android.trafikanten.route.RoutePlanner.StopMatch;
import no.rehn.android.trafikanten.route.RoutePlanner.TravelProposal;
import no.rehn.android.trafikanten.route.RoutePlanner.TravelStage;
import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class NavigateHomeActivity extends Activity {
    RoutePlanner planner = new RoutePlanner();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle b = this.getIntent().getExtras();
        Location from = b.getParcelable("from");
        Location to = b.getParcelable("to");
        setContentView(R.layout.navigate);
        ListView legs = (ListView) findViewById(R.id.legs);
        TravelProposal trip = null;
        try {
            trip = new TravelAgency(from, to).getBestProposal();
        } catch (Exception e) {
            Toast.makeText(this, "Error when finding route", Toast.LENGTH_LONG);
            e.printStackTrace();
        }
        if (trip != null) {
            legs.setAdapter(new TravelStageAdapter(this, R.layout.stage, trip.stages));
        }
    }
    
    class TravelAgency {
        Location from;
        Location to;
        
        public TravelAgency(Location from, Location to) throws Exception {
            this.from = from;
            this.to = to;
        }
        
        final int MIN_CHECK = 4;
        
        class Permutation implements Comparable<Permutation> {
            final int worstLevel;
            public Permutation(StopMatch departureStop, StopMatch arrivalStop, int worstLevel) { 
                this.from = departureStop;
                this.to = arrivalStop;
                this.worstLevel = worstLevel;
            }
            final StopMatch from;
            final StopMatch to;
            
            public int compareTo(Permutation another) {
                return worstLevel - another.worstLevel;
            }
        }
        
        List<Permutation> permutations = new LinkedList<Permutation>();

        public TravelProposal getBestProposal() throws Exception {
            // get possible departures
            List<StopMatch> departureStops = planner.findStopByLatLon(from.getLatitude(), from.getLongitude(), 10);
            // get possible arrivals
            List<StopMatch> arrivalStops = planner.findStopByLatLon(to.getLatitude(), to.getLongitude(), 10);
            
            if (departureStops.isEmpty()) {
                Toast.makeText(NavigateHomeActivity.this, "No stops found near your location", Toast.LENGTH_LONG).show();
                return null;
            }
            if (arrivalStops.isEmpty()) {
                Toast.makeText(NavigateHomeActivity.this, "No stops found near destination", Toast.LENGTH_LONG).show();
                return null;
            }
            if (departureStops.get(0).fromId.equals(arrivalStops.get(0))) {
                Toast.makeText(NavigateHomeActivity.this, "Just walk home", Toast.LENGTH_LONG).show();
                return null;
            }
            
            List<Permutation> permutations = createPermutations(departureStops, arrivalStops);
            
            // check all possible routes
            TravelProposal bestProposal = null;
            int checks = 0;
            for (Permutation permutation : permutations) {
                StopMatch departureStop = permutation.from;
                StopMatch arrivalStop = permutation.to;
                int departureOffsetMinutes = getWalkInMinutes(departureStop);
                Log.i("navigate", "Offset to departure is " + departureOffsetMinutes + " min");
                Date departureDate = new Date(System.currentTimeMillis() + departureOffsetMinutes * 60 * 1000);
                int arrivalOffsetMinutes = getWalkInMinutes(arrivalStop);
                Log.i("navigate", "Offset to arrival is " + arrivalOffsetMinutes + "min");
                Log.i("navigate", "Checking: " + departureStop.stopName + " to " + arrivalStop.stopName);
                List<TravelProposal> travelProposals = planner.findTravelBetween(departureStop.fromId, arrivalStop.fromId, departureDate, 1);
                for (TravelProposal proposal : travelProposals) {
                    proposal.addPreStage(createPreStage(departureOffsetMinutes, departureStop, proposal.stages.getFirst()));
                    proposal.addStage(createPostStage(arrivalOffsetMinutes, arrivalStop, proposal.stages.getLast()));
                    if (bestProposal == null || proposal.getArrival().before(bestProposal.getArrival())) {
                        bestProposal = proposal;
                    }
                }
                checks++;
                if (checks > MIN_CHECK && bestProposal != null) {
                    Toast.makeText(NavigateHomeActivity.this, "Checked " + checks + " routes", Toast.LENGTH_LONG).show();
                    Log.i("navigate", "Checked " + checks + " trips");
                    return bestProposal;
                }
            }
            if (bestProposal == null) {
                Toast.makeText(NavigateHomeActivity.this, "No route found", Toast.LENGTH_LONG).show();
                return null;
            }
            return bestProposal;
        }

        private List<Permutation> createPermutations(List<StopMatch> departureStops, List<StopMatch> arrivalStops) {
            List<Permutation> permutations = new LinkedList<Permutation>();
            for (int i = 0; i < departureStops.size(); i++) {
                StopMatch departureStop = departureStops.get(i);
                for (int j = 0; j < departureStops.size(); j++) {
                    StopMatch arrivalStop = arrivalStops.get(j);
                    permutations.add(new Permutation(departureStop, arrivalStop, Math.max(i, j)));
                }
            }
            Collections.sort(permutations);
            return permutations;
        }

        private TravelStage createPreStage(int departureOffSetMinutes, StopMatch departureStop, TravelStage first) {
            TravelStage stage = new TravelStage();
            Calendar departure = (Calendar) first.departureDate.clone();
            departure.add(Calendar.MINUTE, -departureOffSetMinutes);
            stage.departureDate = departure;
            stage.departureStopName = "Current location";
            stage.arrivalStopName = departureStop.stopName;
            stage.arrivalDate = first.departureDate;
            stage.transportationName = "Gå";
            return stage;
        }

        private TravelStage createPostStage(int arrivalOffSetMinutes, StopMatch arrivalStop, TravelStage last) {
            TravelStage stage = new TravelStage();
            Calendar arrival = (Calendar) last.arrivalDate.clone();
            arrival.add(Calendar.MINUTE, arrivalOffSetMinutes);
            stage.arrivalDate = arrival;
            stage.departureStopName = arrivalStop.stopName;
            stage.arrivalStopName = "Final destination";
            stage.departureDate = last.arrivalDate;
            stage.transportationName = "Gå";
            return stage;
        }
    }

    private int getWalkInMinutes(StopMatch departureStop) {
        return departureStop.airDistance / 80; // x(meter)/80(meter/min)
    }

    class TravelStageAdapter extends ArrayAdapter<TravelStage> {
        final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        public TravelStageAdapter(Context context, int textViewResourceId, List<TravelStage> items) {
            super(context, textViewResourceId, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.stage, null);
            }
            TravelStage o = getItem(position);
            if (o != null) {
                TextView route = (TextView) v.findViewById(R.id.route);
                int iconResource = R.drawable.unknown_small;
                if (o.transportationName.equals("Gå")) {
                    route.setText("Walk");
                    iconResource = R.drawable.walk_small;
                } else {
                    route.setText(String.format("%s %s - %s", o.transportationName, o.line, o.destination));
                    if (o.transportationName.equals("T-bane")) {
                        iconResource = R.drawable.subway_small;
                    } else if (o.transportationName.equals("Buss")) {
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
                fromTime.setText(sdf.format(o.departureDate.getTime()));
                TextView toTime = (TextView) v.findViewById(R.id.toTime);
                toTime.setText(sdf.format(o.arrivalDate.getTime()));
            }
            return v;
        }
    }
}
