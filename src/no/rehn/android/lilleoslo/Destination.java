package no.rehn.android.lilleoslo;

import android.location.Location;

public class Destination {
    int id;
    String name;
    Location location;

    public Destination(String name, Location location) {
        this.name = name;
        this.location = location;
    }

    public Destination(int id2, String name2, float lat, float lon) {
        this.id = id2;
        this.name = name2;
        this.location = new Location("foo");
        this.location.setLatitude(lat);
        this.location.setLongitude(lon);
    }
}
