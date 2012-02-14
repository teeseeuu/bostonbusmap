package boston.Bus.Map.provider;

import boston.Bus.Map.data.RoutePool;
import boston.Bus.Map.database.DatabaseHelper;
import boston.Bus.Map.transit.TransitSystem;
import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SearchRecentSuggestionsProvider;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class TransitContentProvider extends SearchRecentSuggestionsProvider {

	private UriMatcher matcher;
	private DatabaseHelper helper;

	public static final String AUTHORITY = "com.bostonbusmap.torontotransitprovider";
	public static final int MODE = SearchRecentSuggestionsProvider.DATABASE_MODE_QUERIES;
	
	
	private static final int SUGGESTIONS_CODE = 5;
	
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
	
	public TransitContentProvider()
	{
		matcher = new UriMatcher(UriMatcher.NO_MATCH);
		
		matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SUGGESTIONS_CODE);
		setupSuggestions(AUTHORITY, MODE);
	}
	
	@Override
	public boolean onCreate() {
		boolean create = super.onCreate();
		helper = new DatabaseHelper(this.getContext());
		return create;
	}
	
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder)
	{
		int code = matcher.match(uri);
		switch (code)
		{
		case SUGGESTIONS_CODE:
			if (selectionArgs == null || selectionArgs.length == 0 || selectionArgs[0].trim().length() == 0)
			{
				return super.query(uri, projection, selection, selectionArgs, sortOrder);
			}
			else
			{
				return helper.getCursorForSearch(selectionArgs != null && selectionArgs.length >= 1 ? selectionArgs[0] : null);
			}
		default:
			return super.query(uri, projection, selection, selectionArgs, sortOrder);
		}
	}
}