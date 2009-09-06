package no.rehn.android.trafikanten;

import java.io.InputStream;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.net.Proxy.Type;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import uk.me.jstott.jcoord.LatLng;
import uk.me.jstott.jcoord.UTMRef;
import android.util.Log;

// maxproposals == 0 means no limit
public class RoutePlanner {
    public static final String TRANSPORT_SUBWAY = "T-bane";
    public static final String TRANSPORT_BUS = "Buss";
    public static final String TRANSPORT_WALK = "G\u00E5";

    private static final String LOG_CATEGORY = "ROUTEPLANNER";
    private static final int DEFAULT_MAX_PROPOSALS = 5;
    // walks about 80 meters per minute (~5km/h)
    private static final double DEFAULT_WALKING_SPEED = 5.0;
    private double mWalkingSpeed = DEFAULT_WALKING_SPEED;

    private final static String DEFAULT_URL = "http://www5.trafikanten.no/txml/"; 
    private Proxy mHttpProxy = Proxy.NO_PROXY;
    private String mBaseUrl = DEFAULT_URL;
    private final DocumentBuilderFactory mFactory = DocumentBuilderFactory.newInstance();
    private final TimeZone mServerTimeZone;
    private final SimpleDateFormat mDateFormat;
    private final SimpleDateFormat mTimeFormat;
    private long mStaticTime;

    public RoutePlanner() {
        mServerTimeZone = TimeZone.getTimeZone("Europe/Oslo");
        mDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        mDateFormat.setTimeZone(mServerTimeZone);
        mTimeFormat = new SimpleDateFormat("HH:mm");
        mTimeFormat.setTimeZone(mServerTimeZone);
    }
    
    public void setStaticTime(long staticTime) {
        mStaticTime = staticTime;
    }
    
    public void setProxyAddress(SocketAddress proxyAddress) {
        mHttpProxy = new Proxy(Type.HTTP, proxyAddress);
    }
    
    public void setWalkingSpeed(double speed) {
		this.mWalkingSpeed = speed;
	}
    
    public void setBaseUrl(String baseUrl) {
        this.mBaseUrl = baseUrl;
    }
    
    // Example:
    // http://www5.trafikanten.no/txml/?type=1&stopname=godlia&proposals=5
    public List<StopMatch> findStopByName(String stopName, int maxProposals) throws Exception {
        String url = String.format(mBaseUrl + "?type=1&stopname=%s&proposals=%d", URLEncoder.encode(stopName), maxProposals);
        return parseStopMatches(openUrl(url));
    }

    public List<StopMatch> findStopByName(String stopName) throws Exception {
        return findStopByName(stopName, 5);
    }

    // Example:
    // http://www5.trafikanten.no/txml/?type=2&x=602986&y=6642023&proposals=5
    public List<StopMatch> findStopByLatLon(double latitude, double longitude, int maxProposals) throws Exception {
        UTMRef umt = new LatLng(latitude, longitude).toUTMRef();
        String url = String.format(mBaseUrl + "?type=2&x=%.0f&y=%.0f&proposals=%d", umt.getEasting(), umt.getNorthing(),
                maxProposals);
        return parseStopMatches(openUrl(url));
    }

    public List<StopMatch> findStopByLatLon(double latitude, double longitude) throws Exception {
        return findStopByLatLon(latitude, longitude, 5);
    }

    // Example:
    // http://www5.trafikanten.no/txml/?type=3&fromid=3010012&depdate=2008-01-07&deptime=12:00&proposals=5
    public List<TravelProposal> findTravelsFrom(String stopName, Date departureAfter, int maxProposals) throws Exception {
        String url = String.format(mBaseUrl + "?type=3&fromid=%s&depdate=%s&deptime=%s&proposals=%d", stopName,
                mDateFormat.format(departureAfter), mTimeFormat.format(departureAfter), maxProposals);
        return parseTravelProposals(openUrl(url));
    }
    
    public List<TravelProposal> findTravelsFrom(String stopName) throws Exception {
        return findTravelsFrom(stopName, createCurrentDate(), DEFAULT_MAX_PROPOSALS);
    }
    
    // Example:
    // http://www5.trafikanten.no/txml/?type=4&fromid=3010032&toid=3010200
    public List<TravelProposal> findTravelBetween(String fromStop, String toStop, Date departureAfter, int maxProposals) throws Exception {
        String url = String.format(mBaseUrl + "?type=4&fromid=%s&toid=%s&depdate=%s&deptime=%s&proposals=%d", fromStop, toStop,
                mDateFormat.format(departureAfter), mTimeFormat.format(departureAfter), maxProposals);
        return parseTravelProposals(openUrl(url));
    }
    
    public List<TravelProposal> findTravelBetween(String fromStop, String toStop) throws Exception {
        return findTravelBetween(fromStop, toStop, createCurrentDate(), DEFAULT_MAX_PROPOSALS);
    }
    
    public Date createCurrentDate() {
        return new Date(currentTimeMillis());
    }
    
    public long currentTimeMillis() {
        if (mStaticTime > 0) {
            return mStaticTime;
        }
        return System.currentTimeMillis();
    }
    
    private InputStream openUrl(String urlStr) throws Exception {
        Log.i("fetch", urlStr);
        URL url = new URL(urlStr);
        return url.openConnection(mHttpProxy).getInputStream();
    }

    List<StopMatch> parseStopMatches(InputStream responseStream) throws Exception {
        List<StopMatch> matches = new LinkedList<StopMatch>();
        DocumentBuilder builder = mFactory.newDocumentBuilder();
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
            if (name.equals("fromid")) {
                stopMatch.fromId = property.getFirstChild().getNodeValue();
            } else if (name.equals("StopName")) {
                stopMatch.stopName = property.getFirstChild().getNodeValue();
            } else if (name.equals("XCoordinate")) {
                stopMatch.xCoordinate = Integer.parseInt(property.getFirstChild().getNodeValue());
            } else if (name.equals("YCoordinate")) {
                stopMatch.yCoordinate = Integer.parseInt(property.getFirstChild().getNodeValue());
            } else if (name.equals("AirDistance")) {
                stopMatch.airDistance = Integer.parseInt(property.getFirstChild().getNodeValue());
            }
        }
        return stopMatch;
    }

    List<TravelProposal> parseTravelProposals(InputStream responseStream) throws Exception {
        List<TravelProposal> matches = new LinkedList<TravelProposal>();
        DocumentBuilder builder = mFactory.newDocumentBuilder();
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
        Calendar departureDate = Calendar.getInstance(mServerTimeZone);
        departureDate.setTimeInMillis(currentTimeMillis());
        // these should not default to anything
        departureDate.clear(Calendar.SECOND);
        departureDate.clear(Calendar.MILLISECOND);
        for (int i = 0; i < properties.getLength(); i++) {
            Node property = properties.item(i);
            String name = property.getNodeName();
            if (name.equals("DepartureTime")) {
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
            if (stagePropertyName.equals("DepartureStopName")) {
                stage.departureStopName = stageProperty.getFirstChild().getNodeValue();
            } else if (stagePropertyName.equals("ArrivalStopName")) {
                stage.arrivalStopName = stageProperty.getFirstChild().getNodeValue();
            } else if (stagePropertyName.equals("Destination")) {
                stage.destination = stageProperty.getFirstChild().getNodeValue();
            } else if (stagePropertyName.equals("Line")) {
                stage.line = stageProperty.getFirstChild().getNodeValue();
            } else if (stagePropertyName.equals("TransportationName")) {
                stage.transportationName = stageProperty.getFirstChild().getNodeValue();
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

    int getDistanceInMinutes(double kilometers) {
        return (int) (kilometers / (mWalkingSpeed / 60));
    }

    public int getMinutesBetween(LatLng from, LatLng to) {
        double distance = from.distance(to);
		int minutes = getDistanceInMinutes(distance);
        Log.i(LOG_CATEGORY, distance + "km (" + minutes + "min) between " + from + " and " + to);
		return minutes;
    }

    public Calendar createCalendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentTimeMillis());
        return calendar;
    }
}
