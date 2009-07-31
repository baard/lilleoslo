package no.rehn.android.trafikanten.route;

import java.io.InputStream;
import java.io.Serializable;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import no.rehn.android.trafikanten.TimeUtils;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import uk.me.jstott.jcoord.LatLng;
import uk.me.jstott.jcoord.UTMRef;
import android.util.Log;

// maxproposals == 0 means no limit
public class RoutePlanner {
	static final String LOG_CATEGORY = "route-planner";
    private static final int DEFAULT_MAX_PROPOSALS = 5;
    // walks about 80 meters per minute (~5km/h)
    static final double WALKING_KM_PER_MINUTE = 5.0 / 60;

    final static String DEFAULT_URL = "http://www5.trafikanten.no/txml/"; 
    final String baseUrl = "http://192.168.0.201:8080/txml/";
    final HttpClient client;
    final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    final TimeZone serverTimeZone;
    final SimpleDateFormat dateFormat;
    final SimpleDateFormat timeFormat;
    public static final String TRANSPORT_SUBWAY = "T-bane";
    public static final String TRANSPORT_BUS = "Buss";
    public static final String TRANSPORT_WALK = "G\u00E5";

    public RoutePlanner() {
        serverTimeZone = TimeZone.getTimeZone("Europe/Oslo");
        dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setTimeZone(serverTimeZone);
        timeFormat = new SimpleDateFormat("HH:mm");
        timeFormat.setTimeZone(serverTimeZone);
        client = new DefaultHttpClient();
    }

    // Example:
    // http://www5.trafikanten.no/txml/?type=1&stopname=godlia&proposals=5
    public List<StopMatch> findStopByName(String stopName, int maxProposals) throws Exception {
        String url = String.format(baseUrl + "?type=1&stopname=%s&proposals=%d", URLEncoder.encode(stopName), maxProposals);
        return parseStopMatches(openUrl(url));
    }

    public List<StopMatch> findStopByName(String stopName) throws Exception {
        return findStopByName(stopName, 5);
    }

    // Example:
    // http://www5.trafikanten.no/txml/?type=2&x=602986&y=6642023&proposals=5
    public List<StopMatch> findStopByLatLon(double latitude, double longitude, int maxProposals) throws Exception {
        UTMRef umt = new LatLng(latitude, longitude).toUTMRef();
        String url = String.format(baseUrl + "?type=2&x=%.0f&y=%.0f&proposals=%d", umt.getEasting(), umt.getNorthing(),
                maxProposals);
        return parseStopMatches(openUrl(url));
    }

    public List<StopMatch> findStopByLatLon(double latitude, double longitude) throws Exception {
        return findStopByLatLon(latitude, longitude, 5);
    }

    // Example:
    // http://www5.trafikanten.no/txml/?type=3&fromid=3010012&depdate=2008-01-07&deptime=12:00&proposals=5
    public List<TravelProposal> findTravelsFrom(String stopName, Date departureAfter, int maxProposals) throws Exception {
        String url = String.format(baseUrl + "?type=3&fromid=%s&depdate=%s&deptime=%s&proposals=%d", stopName,
                dateFormat.format(departureAfter), timeFormat.format(departureAfter), maxProposals);
        return parseTravelProposals(openUrl(url));
    }
    
    public List<TravelProposal> findTravelsFrom(String stopName) throws Exception {
        return findTravelsFrom(stopName, TimeUtils.newDate(), DEFAULT_MAX_PROPOSALS);
    }
    
    // Example:
    // http://www5.trafikanten.no/txml/?type=4&fromid=3010032&toid=3010200
    public List<TravelProposal> findTravelBetween(String fromStop, String toStop, Date departureAfter, int maxProposals) throws Exception {
        String url = String.format(baseUrl + "?type=4&fromid=%s&toid=%s&depdate=%s&deptime=%s&proposals=%d", fromStop, toStop,
                dateFormat.format(departureAfter), timeFormat.format(departureAfter), maxProposals);
        return parseTravelProposals(openUrl(url));
    }
    
    public List<TravelProposal> findTravelBetween(String fromStop, String toStop) throws Exception {
        return findTravelBetween(fromStop, toStop, TimeUtils.newDate(), DEFAULT_MAX_PROPOSALS);
    }

    private InputStream openUrl(String url) throws Exception {
        HttpGet request = new HttpGet(url);
        Log.i("fetch", url);
        HttpResponse response = client.execute(request);
        InputStream inputStream = response.getEntity().getContent();
        return inputStream;
    }

    List<StopMatch> parseStopMatches(InputStream responseStream) throws Exception {
        List<StopMatch> matches = new LinkedList<StopMatch>();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document dom = builder.parse(responseStream);
        Element root = dom.getDocumentElement();
        NodeList items = root.getElementsByTagName("StopMatch");
        for (int i = 0; i < items.getLength(); i++) {
            matches.add(parseStopMatch(items.item(i)));
        }
        Log.i("fetch", "Parsed stops: " + matches.size());
        return matches;
    }

    private StopMatch parseStopMatch(Node item) {
        StopMatch stopMatch = new StopMatch();
        NodeList properties = item.getChildNodes();
        for (int i = 0; i < properties.getLength(); i++) {
            Node property = properties.item(i);
            String name = property.getNodeName();
            if (name.equals("ID")) {
                stopMatch.id = property.getFirstChild().getNodeValue();
            } else if (name.equals("fromid")) {
                stopMatch.fromId = property.getFirstChild().getNodeValue();
            } else if (name.equals("StopName")) {
                stopMatch.stopName = property.getFirstChild().getNodeValue();
            } else if (name.equals("XCoordinate")) {
                stopMatch.xCoordinate = Integer.parseInt(property.getFirstChild().getNodeValue());
            } else if (name.equals("YCoordinate")) {
                stopMatch.yCoordinate = Integer.parseInt(property.getFirstChild().getNodeValue());
            } else if (name.equals("AirDistance")) {
                stopMatch.airDistance = Integer.parseInt(property.getFirstChild().getNodeValue());
            } else if (name.equals("District")) {
                stopMatch.district = property.getFirstChild().getNodeValue();
            }
        }
        return stopMatch;
    }

    List<TravelProposal> parseTravelProposals(InputStream responseStream) throws Exception {
        List<TravelProposal> matches = new LinkedList<TravelProposal>();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document dom = builder.parse(responseStream);
        Element root = dom.getDocumentElement();
        NodeList items = root.getElementsByTagName("TravelProposal");
        for (int i = 0; i < items.getLength(); i++) {
            matches.add(parseTravelProposal(items.item(i)));
        }
        Log.i("fetch", "Parsed travels: " + matches.size());
        return matches;
    }

    private TravelProposal parseTravelProposal(Node node) {
        NodeList properties = node.getChildNodes();
        TravelProposal travelProposal = new TravelProposal();
        // defaults to today if not set
        Calendar departureDate = Calendar.getInstance(serverTimeZone);
        departureDate.setTimeInMillis(TimeUtils.currentTimeMillis());
        // these should not default to anything
        departureDate.clear(Calendar.SECOND);
        departureDate.clear(Calendar.MILLISECOND);
        for (int i = 0; i < properties.getLength(); i++) {
            Node property = properties.item(i);
            String name = property.getNodeName();
            if (name.equals("ID")) {
                travelProposal.id = Integer.parseInt(property.getFirstChild().getNodeValue());
            } else if (name.equals("DepartureTime")) {
                String[] hourMinutes = property.getFirstChild().getNodeValue().split(":");
                departureDate.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hourMinutes[0]));
                departureDate.set(Calendar.MINUTE, Integer.parseInt(hourMinutes[1]));
            } else if (name.equals("DepartureDay")) {
                departureDate.set(Calendar.DATE, Integer.parseInt(property.getFirstChild()
                        .getNodeValue()));
            } else if (name.equals("DepartureMonth")) {
                departureDate.set(Calendar.MONTH, Integer.parseInt(property.getFirstChild()
                        .getNodeValue()) - 1);
            } else if (name.equals("DepartureYear")) {
                departureDate.set(Calendar.YEAR, Integer.parseInt(property.getFirstChild()
                        .getNodeValue()));
            } else if (name.equals("TravelStages")) {
                Element element = (Element) property;
                NodeList stages = element.getElementsByTagName("TravelStage");
                for (int j = 0; j < stages.getLength(); j++) {
                    TravelStage stage = parseTravelStage(travelProposal, departureDate, stages.item(j));
                    travelProposal.stages.addLast(stage);
                }
            }
        }
        //travelProposal.propagateDepartureDate();
        return travelProposal;
    }

    private TravelStage parseTravelStage(TravelProposal travelProposal, Calendar departureDate, Node property) {
        NodeList stageProperties = property.getChildNodes();
        TravelStage stage = new TravelStage();
        if (travelProposal.stages.isEmpty()) {
        	// first stage
        	stage.departureDate = (Calendar) departureDate.clone();
        } else {
        	stage.departureDate = (Calendar) travelProposal.stages.getLast().arrivalDate.clone();
        }
    	stage.arrivalDate = (Calendar) stage.departureDate.clone();
        for (int l = 0; l < stageProperties.getLength(); l++) {
            Node stageProperty = stageProperties.item(l);
            String stagePropertyName = stageProperty.getNodeName();
            if (stagePropertyName.equals("ID")) {
                stage.id = Integer.parseInt(stageProperty.getFirstChild().getNodeValue());
            } else if (stagePropertyName.equals("DepartureStopId")) {
                stage.departureStopId = stageProperty.getFirstChild().getNodeValue();
            } else if (stagePropertyName.equals("DepartureStopName")) {
                stage.departureStopName = stageProperty.getFirstChild().getNodeValue();
            } else if (stagePropertyName.equals("DepartureStopWalkingDistance")) {
                stage.departureStopWalkingDistance = Integer.parseInt(stageProperty.getFirstChild().getNodeValue());
            } else if (stagePropertyName.equals("ArrivalStopId")) {
                stage.arrivalStopId = stageProperty.getFirstChild().getNodeValue();
            } else if (stagePropertyName.equals("ArrivalStopName")) {
                stage.arrivalStopName = stageProperty.getFirstChild().getNodeValue();
            } else if (stagePropertyName.equals("ArrivalStopWalkingDistance")) {
                stage.arrivalStopWalkingDistance = Integer.parseInt(stageProperty.getFirstChild().getNodeValue());
            } else if (stagePropertyName.equals("Destination")) {
                stage.destination = stageProperty.getFirstChild().getNodeValue();
            } else if (stagePropertyName.equals("Line")) {
                stage.line = stageProperty.getFirstChild().getNodeValue();
            } else if (stagePropertyName.equals("TourID")) {
                stage.tourId = stageProperty.getFirstChild().getNodeValue();
            } else if (stagePropertyName.equals("TransportationID")) {
                stage.transportationId = stageProperty.getFirstChild().getNodeValue();
            } else if (stagePropertyName.equals("TransportationName")) {
                stage.transportationName = stageProperty.getFirstChild().getNodeValue();
            } else if (stagePropertyName.equals("TransportationValid")) {
                stage.transportationValid = Boolean.parseBoolean(stageProperty.getFirstChild().getNodeValue());
            } else if (stagePropertyName.equals("TravelTime")) {
            	String[] hourMinutes = stageProperty.getFirstChild().getNodeValue().split(":");
                int minutes = 0;
                minutes += 60 * Integer.parseInt(hourMinutes[0]);
                minutes += Integer.parseInt(hourMinutes[1]);
                stage.arrivalDate.add(Calendar.MINUTE, minutes);
            } else if (stagePropertyName.equals("WaitingTime")) {
                String[] hourMinutes = stageProperty.getFirstChild().getNodeValue().split(":");
                int minutes = 0;
                minutes += 60 * Integer.parseInt(hourMinutes[0]);
                minutes += Integer.parseInt(hourMinutes[1]);
                stage.departureDate.add(Calendar.MINUTE, minutes);
                stage.arrivalDate.add(Calendar.MINUTE, minutes);
            }
        }
        return stage;
    }

    public static class StopMatch {
        public String id;
        public String fromId;
        public String stopName;
        public String district;
        public UTMRef utmRef;
        public int xCoordinate;
        public int yCoordinate;
        
        public LatLng getLocation() {
            // 32 is special for norway, 'V' is (64 > latitude) && (latitude >= 56)
            return new UTMRef(xCoordinate, yCoordinate, 'V', 32).toLatLng();
        }
        public int airDistance;
    }

    public static class TravelProposal {
        public int id;
        public LinkedList<TravelStage> stages = new LinkedList<TravelStage>();

        public Date getDeparture() {
            return stages.getFirst().departureDate.getTime();
        }
        
        public Date getArrival() {
            return stages.getLast().arrivalDate.getTime();
        }

        public void addPreStage(String departureStopName, String transportationName, LatLng from, StopMatch arrivalStop) {
            TravelStage firstStage = stages.getFirst();
            TravelStage stage = new TravelStage();
            Calendar departure = (Calendar) firstStage.departureDate.clone();
            departure.add(Calendar.MINUTE, -getMinutesBetween(from, arrivalStop.getLocation()));
            stage.departureDate = departure;
            stage.departureStopName = departureStopName;
            stage.arrivalStopName = firstStage.departureStopName;
            stage.arrivalDate = (Calendar) firstStage.departureDate.clone();
            stage.transportationName = transportationName;
            stage.arrivalLocation = arrivalStop.getLocation();
            stage.departureLocation= from;
            stages.addFirst(stage);
        }

        public void addPostStage(String arrivalStopName, String transportationName, LatLng to, StopMatch departureStop) {
            TravelStage lastStage = stages.getLast();
            TravelStage stage = new TravelStage();
            Calendar arrival = (Calendar) lastStage.arrivalDate.clone();
            arrival.add(Calendar.MINUTE, getMinutesBetween(departureStop.getLocation(), to));
            stage.arrivalDate = arrival;
            stage.departureStopName = lastStage.departureStopName;
            stage.arrivalStopName = arrivalStopName;
            stage.departureDate = (Calendar) lastStage.arrivalDate.clone();
            stage.transportationName = transportationName;
            stage.arrivalLocation = to;
            stage.departureLocation= departureStop.getLocation();
            stages.addLast(stage);
        }
    }
    
    static int getDistanceInMinutes(double kilometers) {
        return (int) (kilometers / WALKING_KM_PER_MINUTE);
    }

    public static class TravelStage implements Serializable {
        private static final long serialVersionUID = 1L;
        public int id;
        public String departureStopId;
        public String departureStopName;
        public int departureStopWalkingDistance;
        public Calendar departureDate;
        public String arrivalStopId;
        public String arrivalStopName;
        public int arrivalStopWalkingDistance;
        public Calendar arrivalDate;
        public String destination;
        public String line;
        public String tourId;
        public String transportationId;
        public String transportationName;
        public boolean transportationValid;
        
        public LatLng departureLocation;
        public LatLng arrivalLocation;
    }

    public static int getMinutesBetween(LatLng from, LatLng to) {
        double distance = from.distance(to);
		int minutes = getDistanceInMinutes(distance);
        Log.i(LOG_CATEGORY, distance + "km (" + minutes + "min) between " + format(from) + " and " + format(to));
		return minutes;
    }

	private static String format(LatLng from) {
		String frm = from.getLat() + ", " + from.getLng();
		return frm;
	}
}
