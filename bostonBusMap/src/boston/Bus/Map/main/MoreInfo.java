package boston.Bus.Map.main;

import java.util.ArrayList;
import java.util.HashMap;

import boston.Bus.Map.R;
import boston.Bus.Map.data.Prediction;
import boston.Bus.Map.ui.TextViewBinder;
import android.app.Activity;
import android.app.ListActivity;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class MoreInfo extends ListActivity {
	public static final String predictionsKey = "predictions";
	public static final String routesKey = "routes";
	public static final String titlesKey = "titles";
	
	public static final String titleKey = "title";
	public static final String routeKey = "route";
	
	public static final String textKey = "text";
	
	private Parcelable[] predictions;
	private HashMap<String, String> routeKeysToTitles;
	private TextView title;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.moreinfo);
		
		Bundle extras = getIntent().getExtras();
		
		
		
		predictions = (Parcelable[])extras.getParcelableArray(predictionsKey);
		String[] keys = extras.getStringArray(routesKey);
		String[] titles = extras.getStringArray(titlesKey);
		
		HashMap<String, String> routeKeysToTitles = new HashMap<String, String>();
		for (int i = 0; i < keys.length; i++)
		{
			routeKeysToTitles.put(keys[i], titles[i]);
		}
		
		ArrayList<HashMap<String, Spanned>> data = new ArrayList<HashMap<String,Spanned>>();
		if (predictions != null)
		{
			for (Object predictionObj : predictions)
			{
				Prediction prediction = (Prediction)predictionObj;
				//data.add(prediction.generateMoreInfoMap());
				HashMap<String, Spanned> map = prediction.makeSnippetMap(routeKeysToTitles);
				data.add(map);
			}
		}
		SimpleAdapter adapter = new SimpleAdapter(this, data, R.layout.moreinfo_row,
				new String[]{textKey},
				new int[] {R.id.moreinfo_text});
		
		adapter.setViewBinder(new TextViewBinder());
		
		setListAdapter(adapter);
		
		title = (TextView)findViewById(R.id.moreinfo_title);
		
		String[] stopTitles = extras.getStringArray(titleKey);
		String routes = extras.getString(routeKey);
		
		StringBuilder titleText = new StringBuilder();
		for (int i = 0; i < stopTitles.length; i++)
		{
			titleText.append(stopTitles[i]);
			if (i != stopTitles.length - 1)
			{
				titleText.append("<br />");
			}
		}
		
		if (routes != null)
		{
			titleText.append("<br />Routes: ").append(routes);
			
		}
		
		title.setText(Html.fromHtml("<b>" + titleText + "</b>"));
	}
}