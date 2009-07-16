package no.rehn.android.trafikanten;

import no.rehn.android.trafikanten.route.RoutePlanner.TravelStage;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class StageDetailsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        TravelStage stage = (TravelStage) extras.getSerializable("stage");
        Log.i("details", "" + stage);
    }
}
