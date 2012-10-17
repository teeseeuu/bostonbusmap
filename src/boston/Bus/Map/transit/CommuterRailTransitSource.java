package boston.Bus.Map.transit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.ClientProtocolException;
import org.xml.sax.SAXException;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;

import android.content.Context;
import android.content.OperationApplicationException;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.util.Log;
import boston.Bus.Map.data.AlertsMapping;
import boston.Bus.Map.data.BusLocation;
import boston.Bus.Map.data.CommuterRailStopLocation;
import boston.Bus.Map.data.Directions;
import boston.Bus.Map.data.Location;
import boston.Bus.Map.data.Locations;
import boston.Bus.Map.data.RouteConfig;
import boston.Bus.Map.data.RoutePool;
import boston.Bus.Map.data.RouteTitles;
import boston.Bus.Map.data.Selection;
import boston.Bus.Map.data.StopLocation;
import boston.Bus.Map.data.SubwayStopLocation;
import boston.Bus.Map.data.TransitDrawables;
import boston.Bus.Map.main.Main;
import boston.Bus.Map.main.UpdateAsyncTask;
import boston.Bus.Map.parser.AlertParser;
import boston.Bus.Map.parser.CommuterRailPredictionsFeedParser;
import boston.Bus.Map.parser.CommuterRailRouteConfigParser;
import boston.Bus.Map.parser.SubwayPredictionsFeedParser;
import boston.Bus.Map.parser.SubwayRouteConfigFeedParser;
import boston.Bus.Map.ui.ProgressMessage;
import boston.Bus.Map.util.DownloadHelper;
import boston.Bus.Map.util.SearchHelper;

public class CommuterRailTransitSource implements TransitSource {
	public static final String stopTagPrefix = "CRK-";
	private final RouteTitles routeKeysToTitles;
	private static final String predictionsUrlSuffix = ".csv";
	public static final String routeTagPrefix = "CR-";
	private static final String dataUrlPrefix = "http://developer.mbta.com/lib/RTCR/RailLine_";
	
	private final ImmutableMap<String, String> routeKeysToAlertUrls;
	private final TransitDrawables drawables;
	private final String commuterRailData;
	
	public CommuterRailTransitSource(TransitDrawables drawables, AlertsMapping alertsMapping, String commuterRailData)
	{
		this.drawables = drawables;
		
		String[] routeNames = new String[] {
				"Greenbush",
				"Kingston/Plymouth",
				"Middleborough/Lakeville",
				"Fairmount",
				"Providence/Stoughton",
				"Franklin",
				"Needham",
				"Framingham/Worcester",
				"Fitchburg/South Acton",
				"Lowell",
				"Haverhill",
				"Newburyport/Rockport"

		};
		
		//map alert keys to numbers
		ImmutableBiMap.Builder<String, String> routeBuilder = ImmutableBiMap.builder();
		for (int i = 0; i < routeNames.length; i++)
		{
			String key = routeTagPrefix + (i+1); 
			String title = routeNames[i];
			routeBuilder.put(key, title);
		}
		routeKeysToTitles = new RouteTitles(routeBuilder.build());

		ImmutableMap<String, Integer> alertNumbers = alertsMapping.getAlertNumbers(routeKeysToTitles);
		
		
		ImmutableMap.Builder<String, String> alertsBuilder = ImmutableMap.builder();
		for (int i = 0; i < routeNames.length; i++)
		{
			String routeTitle = routeNames[i];
			int alertKey = alertNumbers.get(routeTitle);
			String routeKey = routeTagPrefix + (i+1);
			alertsBuilder.put(routeKey, AlertsMapping.alertUrlPrefix + alertKey);
		}
		routeKeysToAlertUrls = alertsBuilder.build();
		this.commuterRailData = commuterRailData;
	}

	@Override
	public void populateStops(Context context, RoutePool routeMapping, String routeToUpdate,
			Directions directions,
			UpdateAsyncTask task, boolean silent) throws ClientProtocolException, IOException,
			ParserConfigurationException, SAXException, RemoteException, OperationApplicationException
	{
		/*
		//this will probably never be executed
		//final String urlString = getRouteConfigUrl();

		//DownloadHelper downloadHelper = new DownloadHelper(urlString);
		
		//downloadHelper.connect();
		//just initialize the route and then end for this round
		
		CommuterRailRouteConfigParser parser = new CommuterRailRouteConfigParser(directions, this);

		//parser.runParse(downloadHelper.getResponseData()); 
		parser.runParse(new StringReader(commuterRailData));

		parser.writeToDatabase(routeMapping, task, silent);*/
		//this space intentially left blank
	}

	@Override
	public void refreshData(RouteConfig routeConfig,
			Selection selection, int maxStops, double centerLatitude,
			double centerLongitude,
			ConcurrentHashMap<String, BusLocation> busMapping,
			RoutePool routePool, Directions directions,
			Locations locationsObj) throws IOException,
			ParserConfigurationException, SAXException
	{
		int selectedBusPredictions = selection.getMode();
		if (selectedBusPredictions == Selection.VEHICLE_LOCATIONS_ALL)
		{
			//for now I'm only refreshing data for buses if this is checked
			return;
		}
		
		ImmutableTable.Builder<Integer, Integer, String> table = ImmutableTable.builder();	
		
		List<RefreshData> outputData = Lists.newArrayList();
		switch (selectedBusPredictions)
		{
		case  Selection.BUS_PREDICTIONS_ONE:
		case Selection.VEHICLE_LOCATIONS_ONE:
		{

			List<Location> locations = locationsObj.getLocations(maxStops, centerLatitude, centerLongitude, false, selection);

			//ok, do predictions now
			getPredictionsUrl(locations, maxStops, routeConfig.getRouteName(), outputData, selectedBusPredictions);
			break;
		}
		case Selection.BUS_PREDICTIONS_ALL:
		case Selection.VEHICLE_LOCATIONS_ALL:
		case Selection.BUS_PREDICTIONS_STAR:
		case Selection.BUS_PREDICTIONS_INTERSECT:
		{
			List<Location> locations = locationsObj.getLocations(maxStops, centerLatitude, centerLongitude, false, selection);
			
			getPredictionsUrl(locations, maxStops, null, outputData, selectedBusPredictions);

		}
		break;

		}

		for (RefreshData outputRow : outputData)
		{
			String url = outputRow.url;
			DownloadHelper downloadHelper = new DownloadHelper(url);
			
			downloadHelper.connect();
			

			InputStream stream = downloadHelper.getResponseData();
			InputStreamReader data = new InputStreamReader(stream);
			//StringReader data = new StringReader(hardcodedData);

			//bus prediction

			String route = outputRow.route;
			RouteConfig railRouteConfig = routePool.get(route);
			CommuterRailPredictionsFeedParser parser = new CommuterRailPredictionsFeedParser(railRouteConfig, directions,
					drawables, busMapping, routeKeysToTitles);

			parser.runParse(data);
			data.close();
		}
		
		for (RefreshData outputRow : outputData)
		{
			String route = outputRow.route;
			RouteConfig railRouteConfig = routePool.get(route);

			if (railRouteConfig.obtainedAlerts() == false)
			{

				String url = outputRow.alertUrl;
				DownloadHelper downloadHelper = new DownloadHelper(url);
				downloadHelper.connect();

				InputStream stream = downloadHelper.getResponseData();
				InputStreamReader data = new InputStreamReader(stream);

				AlertParser parser = new AlertParser();
				parser.runParse(data);
				railRouteConfig.setAlerts(parser.getAlerts());
				data.close();

			}
		}
		
	}

	private static class RefreshData {
		private final String url;
		private final String alertUrl;
		private final String route;
		
		public RefreshData(String url, String alertUrl, String route) {
			this.url = url;
			this.alertUrl = alertUrl;
			this.route = route;
		}
	}
	
	private void getPredictionsUrl(List<Location> locations, int maxStops,
			String routeName, List<RefreshData> outputData, int mode)
	{
		//http://developer.mbta.com/lib/RTCR/RailLine_1.csv
		
		//BUS_PREDICTIONS_ONE or VEHICLE_LOCATIONS_ONE
		if (routeName != null)
		{
			//we know we're updating only one route
			if (isCommuterRail(routeName))
			{
				String index = routeName.substring(routeTagPrefix.length()); //snip off beginning "CR-"
				String url = dataUrlPrefix + index + predictionsUrlSuffix;
				String alertUrl = routeKeysToAlertUrls.get(routeName);
				
				outputData.add(new RefreshData(url, alertUrl, routeName));
				return;
			}
		}
		else
		{
			if (mode == Selection.BUS_PREDICTIONS_STAR || mode == Selection.BUS_PREDICTIONS_INTERSECT)
			{
				//ok, let's look at the locations and see what we can get
				for (Location location : locations)
				{
					if (location instanceof StopLocation)
					{
						StopLocation stopLocation = (StopLocation)location;


						for (String route : stopLocation.getRoutes())
						{
							if (isCommuterRail(route) && containsRoute(route, outputData) == false)
							{
								String index = route.substring(routeTagPrefix.length());
								String url = dataUrlPrefix + index + predictionsUrlSuffix;
								String alertUrl = routeKeysToAlertUrls.get(route);
								outputData.add(new RefreshData(url, alertUrl, route));
							}
						}
					}
					else
					{
						//bus location
						BusLocation busLocation = (BusLocation)location;
						String route = busLocation.getRouteId();

						if (isCommuterRail(route) && containsRoute(route, outputData) == false)
						{
							String index = route.substring(3);
							String url = dataUrlPrefix + index + predictionsUrlSuffix;
							String alertUrl = routeKeysToAlertUrls.get(route);
							outputData.add(new RefreshData(url, alertUrl, route));
						}
					}
				}
			}
			else
			{
				//add all 12 of them
				
				for (int i = 1; i <= 12; i++)
				{
					String url = dataUrlPrefix + i + predictionsUrlSuffix;
					String routeKey = routeTagPrefix + i;
					String alertUrl = routeKeysToAlertUrls.get(routeKey);
					
					outputData.add(new RefreshData(url, alertUrl, routeKey));
				}
			}
		}
	}

	private static boolean containsRoute(String route, List<RefreshData> outputData) {
		boolean containsRoute = false;
		for (RefreshData row : outputData) {
			if (row.route.equals(route)) {
				containsRoute = true;
				break;
			}
		}
		return containsRoute;
	}

	private boolean isCommuterRail(String routeName) {
		for (String route : routeKeysToTitles.routeTags())
		{
			if (route.equals(routeName))
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean hasPaths() {
		return false;
	}

	@Override
	public void initializeAllRoutes(UpdateAsyncTask task, Context context,
			Directions directions, RoutePool routeMapping) throws IOException,
			ParserConfigurationException, SAXException, RemoteException, OperationApplicationException {
/*		task.publish(new ProgressMessage(ProgressMessage.PROGRESS_DIALOG_ON, "Downloading commuter info", null));
		//final String subwayUrl = getRouteConfigUrl();
		//URL url = new URL(subwayUrl);
		//InputStream in = Locations.downloadStream(url, task);
		
		CommuterRailRouteConfigParser subwayParser = new CommuterRailRouteConfigParser(directions, this);
		
		subwayParser.runParse(new StringReader(commuterRailData));
		
		subwayParser.writeToDatabase(routeMapping, task, false);*/
		//this space intentially left blank
		
		
	}

	@Override
	public RouteTitles getRouteKeysToTitles() {
		return routeKeysToTitles;
	}

	@Override
	public TransitDrawables getDrawables() {
		return drawables;
	}

	@Override
	public String searchForRoute(String indexingQuery, String lowercaseQuery)
	{
		//try splitting up the route keys along the diagonal and see if they match one piece of it
		for (String route : routeKeysToTitles.getKeys())
		{
			String title = routeKeysToTitles.getTitle(route);
			if (title.contains("/"))
			{
				String[] pieces = title.split("/");
				for (int i = 0; i < pieces.length; i++)
				{
					if (lowercaseQuery.equals(pieces[i].toLowerCase()))
					{
						return route;
					}
				}
			}
		}
		
		return SearchHelper.naiveSearch(indexingQuery, lowercaseQuery, routeKeysToTitles);
		
	}

	@Override
	public CommuterRailStopLocation createStop(float latitude, float longitude,
			String stopTag, String stopTitle, int platformOrder, String branch,
			String route) {
		CommuterRailStopLocation stop = new CommuterRailStopLocation.CommuterRailBuilder(
				latitude, longitude, drawables, stopTag, stopTitle, platformOrder, branch).build();
		stop.addRoute(route);
		return stop;
	}

	@Override
	public int getLoadOrder() {
		return 3;
	}
}
