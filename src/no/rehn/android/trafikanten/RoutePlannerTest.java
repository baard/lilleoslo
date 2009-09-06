package no.rehn.android.trafikanten;

import java.util.List;

import junit.framework.TestCase;

public class RoutePlannerTest extends TestCase {
    RoutePlanner routePlanner = new RoutePlanner();
    public void testFindStopByLatLon() throws Exception {
        List<StopMatch> stops = routePlanner.findStopByLatLon(59.902849, 10.841138);
        assertFalse(stops.isEmpty());
        StopMatch skoeyenLia = stops.get(0);
        assertEquals("3011324", skoeyenLia.fromId);
        assertEquals("Skøyenlia", skoeyenLia.stopName);
        assertEquals(602986, skoeyenLia.xCoordinate);
        assertEquals(6641933, skoeyenLia.yCoordinate);
        assertEquals(91, skoeyenLia.airDistance);
    }
    
    public void testFindStopByName() throws Exception {
        List<StopMatch> stops = routePlanner.findStopByName("Godlia");
        assertFalse(stops.isEmpty());
        StopMatch godlia = stops.get(0);
        assertEquals("03011310", godlia.fromId);
        assertEquals("Godlia", godlia.stopName);
        assertEquals(602679, godlia.xCoordinate);
        assertEquals(6642472, godlia.yCoordinate);
        assertEquals(0, godlia.airDistance);
    }
    
    public void testFindTripFrom() throws Exception {
        List<TravelProposal> trips = routePlanner.findTravelsFrom("03011310");
        assertFalse(trips.isEmpty());
    }
    
    public void testFindTravelsBetween() throws Exception {
        List<TravelProposal> trips = routePlanner.findTravelBetween("3010032", "3010200");
        assertFalse(trips.isEmpty());
    }
    
    public void testParseStopMatches() throws Exception {
        List<StopMatch> stops = routePlanner.parseStopMatches(getClass().getResourceAsStream("stopmatch-sample.xml"));
        assertEquals(1, stops.size());
        StopMatch godlia = stops.get(0);
        assertEquals("03011310", godlia.fromId);
        assertEquals("Godlia", godlia.stopName);
        assertEquals(602679, godlia.xCoordinate);
        assertEquals(6642472, godlia.yCoordinate);
        assertEquals(0, godlia.airDistance);
    }
    
    
    public void testParseTravelProposal() throws Exception {
        List<TravelProposal> proposals = routePlanner.parseTravelProposals(getClass().getResourceAsStream("travelproposal-sample.xml"));
        assertEquals(1, proposals.size());
        TravelProposal route = proposals.get(0);
        assertEquals(3, route.stages.size());
        //TODO test rest of travel-proposals
    }
}
