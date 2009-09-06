package no.rehn.android.trafikanten;

import java.io.Serializable;
import java.util.Calendar;

import uk.me.jstott.jcoord.LatLng;

public class TravelStage implements Serializable {
    private static final long serialVersionUID = 1L;
    public String departureStopName;
    public Calendar departureDate;
    public String arrivalStopName;
    public Calendar arrivalDate;
    public String destination;
    public String line;
    public String transportationName;
    public LatLng departureLocation;
    public LatLng arrivalLocation;
}