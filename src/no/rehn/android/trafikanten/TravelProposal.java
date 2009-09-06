package no.rehn.android.trafikanten;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;

import uk.me.jstott.jcoord.LatLng;

public class TravelProposal {
    public LinkedList<TravelStage> stages = new LinkedList<TravelStage>();

    public Date getDeparture() {
        return stages.getFirst().departureDate.getTime();
    }
    
    public Date getArrival() {
        return stages.getLast().arrivalDate.getTime();
    }

    public void addPreStage(String departureStopName, String transportationName, LatLng from, StopMatch arrivalStop, int minutesBetween) {
        TravelStage firstStage = stages.getFirst();
        TravelStage stage = new TravelStage();
        Calendar departure = (Calendar) firstStage.departureDate.clone();
        departure.add(Calendar.MINUTE, -minutesBetween);
        stage.departureDate = departure;
        stage.departureStopName = departureStopName;
        stage.arrivalStopName = firstStage.departureStopName;
        stage.arrivalDate = (Calendar) firstStage.departureDate.clone();
        stage.transportationName = transportationName;
        stage.arrivalLocation = arrivalStop.getLocation();
        stage.departureLocation= from;
        stages.addFirst(stage);
    }

    public void addPostStage(String arrivalStopName, String transportationName, LatLng to, StopMatch departureStop, int minutesBetween) {
        TravelStage lastStage = stages.getLast();
        TravelStage stage = new TravelStage();
        Calendar arrival = (Calendar) lastStage.arrivalDate.clone();
        arrival.add(Calendar.MINUTE, minutesBetween);
        stage.arrivalDate = arrival;
        stage.departureStopName = lastStage.departureStopName;
        stage.arrivalStopName = arrivalStopName;
        stage.departureDate = (Calendar) lastStage.arrivalDate.clone();
        stage.transportationName = transportationName;
        stage.arrivalLocation = to;
        stage.departureLocation= departureStop.getLocation();
        stages.addLast(stage);
    }

    public TravelStage createStage() {
        TravelStage stage = new TravelStage();
        stages.add(stage);
        return stage;
    }
}