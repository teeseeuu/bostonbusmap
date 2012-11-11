package boston.Bus.Map.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArraySet;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import boston.Bus.Map.annotations.KeepSorted;
import boston.Bus.Map.math.Geometry;
import boston.Bus.Map.transit.TransitSource;
import boston.Bus.Map.transit.TransitSystem;
import android.content.Context;
import android.graphics.drawable.Drawable;

public class StopLocation implements Location
{
	private final float latitude;
	private final float longitude;
	private final float latitudeAsDegrees;
	private final float longitudeAsDegrees;
	private final TransitDrawables drawables;
	
	private final String tag;
	
	private final String title;
	
	private Predictions predictions;
	
	private boolean isFavorite;
	private boolean recentlyUpdated;
	
	/**
	 * A set of routes the stop belongs to
	 */
	@KeepSorted
	@IsGuardedBy("this")
	private final RouteSet routes = new RouteSet();

	private static final int LOCATIONTYPE = 3;
	
	protected StopLocation(Builder builder)
	{
		this.latitudeAsDegrees = builder.latitudeAsDegrees;
		this.longitudeAsDegrees = builder.longitudeAsDegrees;
		this.latitude = (float) (latitudeAsDegrees * Geometry.degreesToRadians);
		this.longitude = (float) (longitudeAsDegrees * Geometry.degreesToRadians);
		this.drawables = builder.drawables;
		this.tag = builder.tag;
		this.title = builder.title;
	}

	public static class Builder {
		private final float latitudeAsDegrees;
		private final float longitudeAsDegrees;
		private final TransitDrawables drawables;
		private final String tag;
		private final String title;

		public Builder(float latitudeAsDegrees, float longitudeAsDegrees,
			TransitDrawables drawables, String tag, String title) {
			this.latitudeAsDegrees = latitudeAsDegrees;
			this.longitudeAsDegrees = longitudeAsDegrees;
			this.drawables = drawables;
			this.tag = tag;
			this.title = title;
		}
		
		public float getLatitudeAsDegrees() {
			return latitudeAsDegrees;
		}
		
		public float getLongitudeAsDegrees() {
			return longitudeAsDegrees;
		}
		
		public StopLocation build() {
			return new StopLocation(this);
		}
	}
	
	@Override
	public float distanceFrom(double centerLatitude, double centerLongitude)
	{
		return Geometry.computeCompareDistance(latitude, longitude, centerLatitude, centerLongitude);
	}

	public float distanceFromInMiles(double latitudeAsRads,
			double longitudeAsRads) {
		return Geometry.computeDistanceInMiles(latitude, longitude, latitudeAsRads, longitudeAsRads);
	}

	@Override
	public Drawable getDrawable(Context context, boolean shadow,
			boolean isSelected) {
		return recentlyUpdated ? drawables.getStopUpdated() : drawables.getStop();
	}

	public void clearRecentlyUpdated()
	{
		recentlyUpdated = false;
	}
	
	@Override
	public int getHeading() {
		return 0;
	}

	@Override
	public int getId() {
		return (tag.hashCode() & 0xffffff) | LOCATIONTYPE << 24;
	}

	@Override
	public float getLatitudeAsDegrees() {
		return latitudeAsDegrees;
	}

	@Override
	public float getLongitudeAsDegrees() {
		return longitudeAsDegrees;
	}

	@Override
	public boolean hasHeading() {
		return false;
	}

	public Predictions getPredictions()
	{
		return predictions;
	}
	
	@Override
	public void makeSnippetAndTitle(RouteConfig routeConfig, RouteTitles routeKeysToTitles, 
			Locations locations, Context context)
	{
		if (predictions == null)
		{
			predictions = new Predictions();
		}
		
		Set<Alert> alerts;
		if (routeConfig != null) {
			alerts = routeConfig.getAlerts();
		}
		else
		{
			alerts = ImmutableSet.of();
		}
		
		predictions.makeSnippetAndTitle(routeConfig, routeKeysToTitles, context, routes, this, alerts, locations);
	}
	
	@Override
	public void addToSnippetAndTitle(RouteConfig routeConfig, Location location, RouteTitles routeKeysToTitles,
			Locations locations, Context context)
	{
		if (predictions == null)
		{
			predictions = new Predictions();
		}
		
		StopLocation stopLocation = (StopLocation)location;
		
		// TODO: support mixing multiple alerts
		ImmutableSet<Alert> alerts = ImmutableSet.of();
		
		predictions.addToSnippetAndTitle(routeConfig, stopLocation, routeKeysToTitles, context, title, routes, alerts, locations);
	}
	
	public String getStopTag()
	{
		return tag;
	}

	public void clearPredictions(RouteConfig routeConfig)
	{
		if (predictions != null)
		{
			predictions.clearPredictions(routeConfig.getRouteName());
		}
		
		recentlyUpdated = true;
	}
	
	public void addPrediction(Prediction prediction)
	{
		if (predictions == null)
		{
			predictions = new Predictions();
		}
		
		predictions.addPredictionIfNotExists(prediction);
	}
	
	public void addPrediction(int minutes, long epochTime, String vehicleId,
			String direction, RouteConfig route, Directions directions, boolean affectedByLayover, boolean isDelayed, int lateness)
	{
		if (predictions == null)
		{
			predictions = new Predictions();
		}
		
		Prediction prediction = new Prediction(minutes, vehicleId, 
				directions.getTitleAndName(direction), route.getRouteName(),
				route.getRouteTitle(), affectedByLayover, isDelayed, lateness);
		predictions.addPredictionIfNotExists(prediction);
	}

	public String getTitle()
	{
		return title;
	}

	public void setFavorite(boolean b)
	{
		isFavorite = b;
	}
	
	@Override
	public boolean isFavorite() {
		return isFavorite;
	}
	/**
	 * The list of routes that owns the StopLocation. NOTE: this is not in any particular order
	 * @return
	 */
	public Collection<String> getRoutes() {
		return routes.getRoutes();
	}

	public String getFirstRoute() {
		return routes.getFirstRoute();
	}
	
	@Override
	public boolean containsId(int selectedBusId) {
		if (getId() == selectedBusId)
		{
			return true;
		}
		else if (predictions != null)
		{
			return predictions.containsId(selectedBusId);
		}
		else
		{
			return false;
		}
	}

	/**
	 * Only list stops once if they share the same location
	 * @param stops
	 * @return
	 */
	public static StopLocation[] consolidateStops(StopLocation[] stops) {
		if (stops.length < 2)
		{
			return stops;
		}
		
		ArrayList<StopLocation> ret = new ArrayList<StopLocation>();
		for (int i = 0; i < stops.length; i++)
		{
			ret.add(stops[i]);
		}
		
		//make sure stops sharing a location are touching each other
		final StopLocation firstStop = stops[0];
		Collections.sort(ret, new LocationComparator(firstStop.getLatitudeAsDegrees(), firstStop.getLongitudeAsDegrees()));
		
		ArrayList<StopLocation> ret2 = new ArrayList<StopLocation>(stops.length);
		StopLocation prev = null;
		for (StopLocation stop : ret)
		{
			if (prev != null && prev.getLatitudeAsDegrees() == stop.getLatitudeAsDegrees() &&
					prev.getLongitudeAsDegrees() == stop.getLongitudeAsDegrees())
			{
				//skip
			}
			else
			{
				ret2.add(stop);
			}
			
			prev = stop;
		}
		
		return ret2.toArray(new StopLocation[0]);
	}
	
	@Override
	public String toString() {
		return Objects.toStringHelper(this).add("id", tag).toString();
	}

	/**
	 * Are these predictions experimental?
	 * @return
	 */
	public boolean isBeta()
	{
		return false;
	}

	@Override
	public PredictionView getPredictionView() {
		if (predictions != null) {
			return predictions.getPredictionView();
		}
		else
		{
			return StopPredictionViewImpl.empty();
		}
	}

	public void addRoute(String route) {
		routes.addRoute(route);
	}

	@Override
	public boolean hasMoreInfo() {
		return true;
	}

	@Override
	public boolean hasFavorite() {
		return true;
	}

	@Override
	public boolean hasReportProblem() {
		return true;
	}
	
	@Override
	public boolean isIntersection() {
		return false;
	}

	public boolean hasRoute(String route) {
		return routes.hasRoute(route);
	}
}
