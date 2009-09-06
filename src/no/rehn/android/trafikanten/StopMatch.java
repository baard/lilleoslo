package no.rehn.android.trafikanten;

import uk.me.jstott.jcoord.LatLng;
import uk.me.jstott.jcoord.UTMRef;

public class StopMatch {
    public String fromId;
    public String stopName;
    public UTMRef utmRef;
    public int xCoordinate;
    public int yCoordinate;

    public LatLng getLocation() {
        // 32 is special for norway, 'V' is (64 > latitude) && (latitude >= 56)
        return new UTMRef(xCoordinate, yCoordinate, 'V', 32).toLatLng();
    }

    public int airDistance;
}