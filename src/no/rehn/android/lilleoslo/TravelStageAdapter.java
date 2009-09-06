package no.rehn.android.lilleoslo;

import java.text.SimpleDateFormat;

import no.rehn.android.trafikanten.R;
import no.rehn.android.trafikanten.RoutePlanner;
import no.rehn.android.trafikanten.TravelStage;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

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
            LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.stage, null);
        }
        TravelStage o = getItem(position);
        // TODO when is this null?
        if (o != null) {
            TextView route = (TextView) v.findViewById(R.id.route);
            int iconResource = getTransportationSymbol(o);
            if (o.transportationName.equals(RoutePlanner.TRANSPORT_WALK)) {
            	if (o.departureLocation != null && o.arrivalLocation != null) {
            		route.setText(getContext().getString(R.string.walk_km_estimated, o.departureLocation.distance(o.arrivalLocation)));
            	} else {
            		route.setText(getContext().getString(R.string.walk_estimated));
            	}
            } else {
                route.setText(String.format("%s #%s - %s", getTransportationName(o), o.line, o.destination));
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
    private String getTransportationName(TravelStage stage) {
        String transportationName = stage.transportationName;
        if (transportationName.equals(RoutePlanner.TRANSPORT_SUBWAY)) {
            return getContext().getString(R.string.subway);
        } else if (transportationName.equals(RoutePlanner.TRANSPORT_BUS)) {
            return getContext().getString(R.string.bus);
        }
        return transportationName;
    }
    private int getTransportationSymbol(TravelStage stage) {
        String transportationName = stage.transportationName;
        if (transportationName.equals(RoutePlanner.TRANSPORT_WALK)) {
            return R.drawable.walk_small;
        } else if (transportationName.equals(RoutePlanner.TRANSPORT_SUBWAY)) {
            return R.drawable.subway_small;
        } else if (transportationName.equals(RoutePlanner.TRANSPORT_BUS)) {
            return R.drawable.bus_small;
        }
        return R.drawable.unknown_small;
    }
}