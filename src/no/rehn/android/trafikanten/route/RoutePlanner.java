package no.rehn.android.trafikanten.route;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.util.Log;

import uk.me.jstott.jcoord.LatLng;
import uk.me.jstott.jcoord.UTMRef;

public class RoutePlanner {
    private static final int DEFAULT_MAX_PROPOSALS = 5;
    final String baseUrl = "http://www5.trafikanten.no/txml/";
    final HttpClient client = new DefaultHttpClient();
    final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    final TimeZone serverTimeZone;
    final SimpleDateFormat dateFormat;
    final SimpleDateFormat timeFormat;

    public RoutePlanner() {
        serverTimeZone = TimeZone.getTimeZone("Europe/Oslo");
        dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setTimeZone(serverTimeZone);
        timeFormat = new SimpleDateFormat("hh:mm");
        timeFormat.setTimeZone(serverTimeZone);
    }

    // Example:
    // http://www5.trafikanten.no/txml/?type=1&stopname=godlia&proposals=5
    public List<StopMatch> findStopByName(String stopName, int maxProposals) throws Exception {
        String url = String.format(baseUrl + "?type=1&stopname=%s&proposals=%d", stopName, maxProposals);
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
        return findTravelsFrom(stopName, new Date(), DEFAULT_MAX_PROPOSALS);
    }
    
    // Example:
    // http://www5.trafikanten.no/txml/?type=4&fromid=3010032&toid=3010200
    public List<TravelProposal> findTravelBetween(String fromStop, String toStop, Date departureAfter, int maxProposals) throws Exception {
        String url = String.format(baseUrl + "?type=4&fromid=%s&toid=%s&depdate=%s&deptime=%s&proposals=%d", fromStop, toStop,
                dateFormat.format(departureAfter), timeFormat.format(departureAfter), maxProposals);
        return parseTravelProposals(openUrl(url));
    }
    
    public List<TravelProposal> findTravelBetween(String fromStop, String toStop) throws Exception {
        return findTravelBetween(fromStop, toStop, new Date(), DEFAULT_MAX_PROPOSALS);
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
        return matches;
    }

    private TravelProposal parseTravelProposal(Node node) {
        NodeList properties = node.getChildNodes();
        TravelProposal travelProposal = new TravelProposal();
        // defaults to today if not set
        travelProposal.departureDate = Calendar.getInstance(serverTimeZone);
        // these should not default to anything
        travelProposal.departureDate.clear(Calendar.SECOND);
        travelProposal.departureDate.clear(Calendar.MILLISECOND);
        travelProposal.arrivalDate = createEmptyCalendar();
        for (int i = 0; i < properties.getLength(); i++) {
            Node property = properties.item(i);
            String name = property.getNodeName();
            if (name.equals("ID")) {
                travelProposal.id = Integer.parseInt(property.getFirstChild().getNodeValue());
            } else if (name.equals("DepartureTime")) {
                String[] hourMinutes = property.getFirstChild().getNodeValue().split(":");
                travelProposal.departureDate.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hourMinutes[0]));
                travelProposal.departureDate.set(Calendar.MINUTE, Integer.parseInt(hourMinutes[1]));
            } else if (name.equals("DepartureDay")) {
                travelProposal.departureDate.set(Calendar.DATE, Integer.parseInt(property.getFirstChild()
                        .getNodeValue()));
            } else if (name.equals("DepartureMonth")) {
                travelProposal.departureDate.set(Calendar.MONTH, Integer.parseInt(property.getFirstChild()
                        .getNodeValue()) - 1);
            } else if (name.equals("DepartureYear")) {
                travelProposal.departureDate.set(Calendar.YEAR, Integer.parseInt(property.getFirstChild()
                        .getNodeValue()));
            } else if (name.equals("ArrivalTime")) {
                String[] hourMinutes = property.getFirstChild().getNodeValue().split(":");
                travelProposal.arrivalDate.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hourMinutes[0]));
                travelProposal.arrivalDate.set(Calendar.MINUTE, Integer.parseInt(hourMinutes[1]));
            } else if (name.equals("TravelStages")) {
                Element element = (Element) property;
                NodeList stages = element.getElementsByTagName("TravelStage");
                for (int j = 0; j < stages.getLength(); j++) {
                    TravelStage stage = parseTravelStage(stages.item(j));
                    travelProposal.addStage(stage);
                }
            }
        }
        travelProposal.propagateDepartureDate();
        return travelProposal;
    }

    private TravelStage parseTravelStage(Node property) {
        NodeList stageProperties = property.getChildNodes();
        TravelStage stage = new TravelStage();
        stage.departureDate = createEmptyCalendar();
        stage.arrivalDate = createEmptyCalendar();
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
            } else if (stagePropertyName.equals("DepartureTime")) {
                String[] hourMinutes = stageProperty.getFirstChild().getNodeValue().split(":");
                stage.departureDate.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hourMinutes[0]));
                stage.departureDate.set(Calendar.MINUTE, Integer.parseInt(hourMinutes[1]));
            } else if (stagePropertyName.equals("ArrivalStopId")) {
                stage.arrivalStopId = stageProperty.getFirstChild().getNodeValue();
            } else if (stagePropertyName.equals("ArrivalStopName")) {
                stage.arrivalStopName = stageProperty.getFirstChild().getNodeValue();
            } else if (stagePropertyName.equals("ArrivalStopWalkingDistance")) {
                stage.arrivalStopWalkingDistance = Integer.parseInt(stageProperty.getFirstChild().getNodeValue());
            } else if (stagePropertyName.equals("ArrivalTime")) {
                String[] hourMinutes = stageProperty.getFirstChild().getNodeValue().split(":");
                stage.arrivalDate.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hourMinutes[0]));
                stage.arrivalDate.set(Calendar.MINUTE, Integer.parseInt(hourMinutes[1]));
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
            } else if (stagePropertyName.equals("WaitingTime")) {
                String[] hourMinutes = stageProperty.getFirstChild().getNodeValue().split(":");
                int minutes = 0;
                minutes += 60 * Integer.parseInt(hourMinutes[0]);
                minutes += Integer.parseInt(hourMinutes[1]);
                stage.waitingMinutes = minutes;
            }
        }
        return stage;
    }

    private Calendar createEmptyCalendar() {
        Calendar calendar = Calendar.getInstance(serverTimeZone);
        calendar.clear();
        return calendar;
    }

    public static class StopMatch {
        public String id;
        public String fromId;
        public String stopName;
        public String district;
        public int xCoordinate;
        public int yCoordinate;
        public int airDistance;
    }

    public static class TravelProposal {
        static final int[] PROPAGATED_FIELDS = new int[] { Calendar.YEAR, Calendar.MONTH, Calendar.DATE, Calendar.HOUR,
            Calendar.SECOND };
        public int id;
        public Calendar departureDate;
        public Calendar arrivalDate;
        public List<TravelStage> stages = new LinkedList<TravelStage>();

        void addStage(TravelStage stage) {
            stages.add(stage);
        }

        void propagateDepartureDate() {
            copyUnsetFields(arrivalDate, departureDate);
            for (TravelStage stage : stages) {
                copyUnsetFields(stage.arrivalDate, departureDate);
                copyUnsetFields(stage.departureDate, departureDate);
            }
        }
        
        private void copyUnsetFields(Calendar copyTo, Calendar copyFrom) {
            for (int field : PROPAGATED_FIELDS) {
                if (!copyTo.isSet(field)) {
                    copyTo.set(field, copyFrom.get(field));
                }
            }
        }
    }

    public static class TravelStage {
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
        public long waitingMinutes;
        
        @Override
        public String toString() {
            return departureStopName + " to " + arrivalStopName;
        }
    }
}
