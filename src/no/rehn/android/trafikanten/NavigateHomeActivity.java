package no.rehn.android.trafikanten;

import no.rehn.android.trafikanten.route.RoutePlanner;
import no.rehn.android.trafikanten.route.RoutePlanner.TravelProposal;
import no.rehn.android.trafikanten.route.RoutePlanner.TravelStage;
import android.app.Activity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class NavigateHomeActivity extends Activity {
    RoutePlanner planner = new RoutePlanner();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navigate);
        ListView legs = (ListView) findViewById(R.id.legs);
        ArrayAdapter<TravelStage> adapter = new ArrayAdapter<TravelStage>(this, R.layout.stage);
        TravelProposal trip = findBestTravelProposal();
        for (TravelStage stage : trip.stages) {
            adapter.add(stage);
        }
        legs.setAdapter(adapter);
    }
    private TravelProposal findBestTravelProposal() {
        // http://www5.trafikanten.no/txml/?type=4&fromid=3010032&toid=3010200&proposals=2&
        try {
            return planner.findTravelBetween("3010032", "3010200").get(0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
