package org.mtransit.android.provider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.RuntimeUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.provider.ContentProviderConstants;
import org.mtransit.android.commons.provider.MTSearchRecentSuggestionsProvider;
import org.mtransit.android.commons.task.MTCallable;
import org.mtransit.android.data.AgencyProperties;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.DataSourceProvider;

import android.app.SearchManager;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

public class SearchSuggestProvider extends MTSearchRecentSuggestionsProvider {

	private static final String TAG = SearchSuggestProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public final static String AUTHORITY = "org.mtransit.android.provider.search.suggest";
	public final static int MODE = DATABASE_MODE_QUERIES;

	private static final UriMatcher URI_MATCHER = getNewUriMatcher(AUTHORITY);

	public static UriMatcher getNewUriMatcher(String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(authority, SearchManager.SUGGEST_URI_PATH_QUERY, ContentProviderConstants.SEARCH_SUGGEST_EMPTY);
		URI_MATCHER.addURI(authority, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", ContentProviderConstants.SEARCH_SUGGEST_QUERY);
		return URI_MATCHER;
	}

	private static final String[] SEARCH_SUGGEST_COLUMNS = new String[] { BaseColumns._ID, SearchManager.SUGGEST_COLUMN_ICON_1,
			SearchManager.SUGGEST_COLUMN_TEXT_1, SearchManager.SUGGEST_COLUMN_QUERY, };

	private static final String SEARCH_SUGGEST_ICON = "android.resource://system/" + android.R.drawable.ic_menu_search;
	private static final String RECENT_SEARCH_SUGGEST_ICON = "android.resource://system/" + android.R.drawable.ic_menu_recent_history;

	public SearchSuggestProvider() {
		setupSuggestions(AUTHORITY, MODE);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		try {
			switch (URI_MATCHER.match(uri)) {
			case ContentProviderConstants.SEARCH_SUGGEST_EMPTY:
				final String query;
				if (selectionArgs != null && selectionArgs.length > 0) {
					query = selectionArgs[0];
				} else {
					query = null;
				}
				final Cursor recentSearchCursor = super.query(uri, projection, selection, selectionArgs, sortOrder);
				return getSearchSuggest(query, recentSearchCursor);
			case ContentProviderConstants.SEARCH_SUGGEST_QUERY:
				final Cursor recentSearchCursor2 = super.query(uri, projection, selection, selectionArgs, sortOrder);
				final String query2 = uri.getLastPathSegment();
				return getSearchSuggest(query2, recentSearchCursor2);
			default:
				throw new IllegalArgumentException(String.format("Unknown URI (query): '%s'", uri));
			}
		} catch (Throwable t) {
			MTLog.w(this, t, "Error while resolving query %s!", uri);
			return null;
		}
	}

	private Cursor getSearchSuggest(String query, Cursor recentSearchCursor) {
		clearAllTasks();
		Set<String> recentSearchSuggestions = DataSourceManager.getSearchSuggest(recentSearchCursor);
		Set<String> suggestions = new HashSet<String>();
		if (!TextUtils.isEmpty(query)) {
			List<AgencyProperties> agencies = DataSourceProvider.get().getAllAgencies(getContext());
			List<Future<Set<String>>> taskList = new ArrayList<Future<Set<String>>>();
			for (AgencyProperties agency : agencies) {
				final FindSearchSuggestTask task = new FindSearchSuggestTask(getContext(), agency, query);
				taskList.add(getFetchSuggestExecutor().submit(task));
			}
			for (Future<Set<String>> future : taskList) {
				try {
					Set<String> agencySuggestions = future.get();
					suggestions.addAll(agencySuggestions);
				} catch (Exception e) {
					MTLog.w(this, e, "Error while loading in background!");
				}
			}
			clearAllTasks();
		}
		MatrixCursor cursor = new MatrixCursor(SEARCH_SUGGEST_COLUMNS);
		int autoIncId = 0;
		if (recentSearchSuggestions != null) {
			for (String suggestion : recentSearchSuggestions) {
				cursor.addRow(new Object[] { autoIncId++, RECENT_SEARCH_SUGGEST_ICON, suggestion, suggestion });
			}
		}
		if (suggestions != null) {
			for (String suggestion : suggestions) {
				if (recentSearchSuggestions != null && recentSearchSuggestions.contains(suggestion)) {
					continue; // skip
				}
				cursor.addRow(new Object[] { autoIncId++, SEARCH_SUGGEST_ICON, suggestion, suggestion });
			}
		}
		return cursor;
	}

	private static final int CORE_POOL_SIZE = RuntimeUtils.NUMBER_OF_CORES;

	private static final int MAX_POOL_SIZE = RuntimeUtils.NUMBER_OF_CORES;

	private ThreadPoolExecutor fetchSuggestExecutor;

	public ThreadPoolExecutor getFetchSuggestExecutor() {
		if (this.fetchSuggestExecutor == null) {
			this.fetchSuggestExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, 0L, TimeUnit.MILLISECONDS,
					new LinkedBlockingDeque<Runnable>());
		}
		return fetchSuggestExecutor;
	}

	public void clearAllTasks() {
		if (this.fetchSuggestExecutor != null) {
			this.fetchSuggestExecutor.shutdown();
			this.fetchSuggestExecutor = null;
		}
	}

	private static class FindSearchSuggestTask extends MTCallable<Set<String>> {

		private static final String TAG = SearchSuggestProvider.class.getSimpleName() + ">" + FindSearchSuggestTask.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private Context context;
		private AgencyProperties agency;
		private String query;

		public FindSearchSuggestTask(Context context, AgencyProperties agency, String query) {
			this.context = context;
			this.agency = agency;
			this.query = query;
		}

		@Override
		public Set<String> callMT() throws Exception {
			final Uri agencyUri = UriUtils.newContentUri(this.agency.getAuthority());
			return DataSourceManager.findSearchSuggest(this.context, agencyUri, this.query);
		}
	}

}
