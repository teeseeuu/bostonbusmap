package boston.Bus.Map.data;

import com.google.common.collect.ImmutableCollection;

import boston.Bus.Map.database.Schema;
import android.graphics.drawable.Drawable;

public class CommuterTrainLocation extends BusLocation {

	public CommuterTrainLocation(float latitude, float longitude, String id,
			long lastFeedUpdateInMillis,
			String heading, boolean predictable, String dirTag, boolean isDirTag,
			String routeName) {
		super(latitude, longitude, id, lastFeedUpdateInMillis,
				heading, predictable, dirTag, isDirTag, routeName);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected String getBusNumberMessage() {
		return "Train number: " + busId + "<br />\n";
	}
	
	@Override
	public int getTransitSourceType() {
		return Schema.Routes.enumagencyidCommuterRail;
	}
	
	@Override
	protected ImmutableCollection<Alert> getAlerts(IAlerts alerts) {
		return alerts.getAlertsByCommuterRailTripId(busId, routeName);
	}	
}
