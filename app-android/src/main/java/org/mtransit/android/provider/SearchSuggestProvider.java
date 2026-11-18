package org.mtransit.android.provider;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.RuntimeUtils;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.provider.common.ContentProviderConstants;
import org.mtransit.android.commons.provider.common.MTSearchRecentSuggestionsProvider;
import org.mtransit.android.commons.task.MTCallable;
import org.mtransit.android.data.AgencyProperties;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.datasource.DataSourcesRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import dagger.hilt.EntryPoint;
import dagger.hilt.InstallIn;
import dagger.hilt.android.EntryPointAccessors;
import dagger.hilt.components.SingletonComponent;

@SuppressLint("Registered")
public class SearchSuggestProvider extends MTSearchRecentSuggestionsProvider {

	private static final String LOG_TAG = SearchSuggestProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private final static String AUTHORITY = "org.mtransit.android.provider.search.suggest";
	private final static int MODE = DATABASE_MODE_QUERIES;

	private static final UriMatcher URI_MATCHER = getNewUriMatcher(AUTHORITY);

	@NonNull
	private static UriMatcher getNewUriMatcher(@NonNull String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(authority, SearchManager.SUGGEST_URI_PATH_QUERY, ContentProviderConstants.SEARCH_SUGGEST_EMPTY);
		URI_MATCHER.addURI(authority, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", ContentProviderConstants.SEARCH_SUGGEST_QUERY);
		return URI_MATCHER;
	}

	private static final String[] SEARCH_SUGGEST_COLUMNS = new String[]{BaseColumns._ID, SearchManager.SUGGEST_COLUMN_ICON_1,
			SearchManager.SUGGEST_COLUMN_TEXT_1, SearchManager.SUGGEST_COLUMN_QUERY,};

	private static final String SEARCH_SUGGEST_ICON = "android.resource://system/" + android.R.drawable.ic_menu_search;
	private static final String RECENT_SEARCH_SUGGEST_ICON = "android.resource://system/" + android.R.drawable.ic_menu_recent_history;

	@EntryPoint
	@InstallIn(SingletonComponent.class)
	interface SearchSuggestProviderEntryPoint {
		DataSourcesRepository dataSourcesRepository();
	}

	@NonNull
	private SearchSuggestProviderEntryPoint getEntryPoint(@NonNull Context context) {
		return EntryPointAccessors.fromApplication(context.getApplicationContext(), SearchSuggestProviderEntryPoint.class);
	}

	public SearchSuggestProvider() {
		setupSuggestions(AUTHORITY, MODE);
	}

	@Nullable
	@Override
	public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
		Cursor cursor = null;
		String query;
		final Context context = requireContextCompat();
		try {
			switch (URI_MATCHER.match(uri)) {
			case ContentProviderConstants.SEARCH_SUGGEST_EMPTY:
				query = selectionArgs == null || selectionArgs.length == 0 ? null : selectionArgs[0];
				cursor = super.query(uri, projection, selection, selectionArgs, sortOrder);
				return getSearchSuggest(context, query, cursor);
			case ContentProviderConstants.SEARCH_SUGGEST_QUERY:
				query = uri.getLastPathSegment();
				cursor = super.query(uri, projection, selection, selectionArgs, sortOrder);
				return getSearchSuggest(context, query, cursor);
			default:
				throw new IllegalArgumentException(String.format("Unknown URI (query): '%s'", uri));
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while resolving query %s!", uri);
			return null;
		} finally {
			SqlUtils.close(cursor);
		}
	}

	private Cursor getSearchSuggest(@NonNull Context context, String query, Cursor recentSearchCursor) {
		clearAllTasks();
		HashSet<String> recentSearchSuggestions = DataSourceManager.getSearchSuggest(recentSearchCursor);
		HashSet<String> suggestions = new HashSet<>();
		if (!TextUtils.isEmpty(query)) {
			List<AgencyProperties> agencies = getEntryPoint(context).dataSourcesRepository().getAllAgencies();
			ArrayList<Future<HashSet<String>>> taskList = new ArrayList<>();
			for (AgencyProperties agency : agencies) {
				FindSearchSuggestTask task = new FindSearchSuggestTask(context, agency, query);
				taskList.add(getFetchSuggestExecutor().submit(task));
			}
			for (Future<HashSet<String>> future : taskList) {
				try {
					HashSet<String> agencySuggestions = future.get();
					if (agencySuggestions != null) {
						suggestions.addAll(agencySuggestions);
					}
				} catch (Exception e) {
					MTLog.w(this, e, "Error while loading in background!");
				}
			}
			clearAllTasks();
		}
		MatrixCursor cursor = new MatrixCursor(SEARCH_SUGGEST_COLUMNS);
		int autoIncId = 0;
		for (String suggestion : recentSearchSuggestions) {
			cursor.addRow(new Object[]{autoIncId++, RECENT_SEARCH_SUGGEST_ICON, suggestion, suggestion});
		}
		for (String suggestion : suggestions) {
			if (recentSearchSuggestions.contains(suggestion)) {
				continue; // skip
			}
			cursor.addRow(new Object[]{autoIncId++, SEARCH_SUGGEST_ICON, suggestion, suggestion});
		}
		return cursor;
	}

	private static final int CORE_POOL_SIZE = RuntimeUtils.NUMBER_OF_CORES;

	private static final int MAX_POOL_SIZE = RuntimeUtils.NUMBER_OF_CORES;

	@Nullable
	private ThreadPoolExecutor fetchSuggestExecutor;

	@NonNull
	private ThreadPoolExecutor getFetchSuggestExecutor() {
		if (this.fetchSuggestExecutor == null) {
			this.fetchSuggestExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, 0L, TimeUnit.MILLISECONDS,
					new LinkedBlockingDeque<>());
		}
		return fetchSuggestExecutor;
	}

	private void clearAllTasks() {
		if (this.fetchSuggestExecutor != null) {
			this.fetchSuggestExecutor.shutdown();
			this.fetchSuggestExecutor = null;
		}
	}

	private static class FindSearchSuggestTask extends MTCallable<HashSet<String>> {

		private static final String LOG_TAG = SearchSuggestProvider.class.getSimpleName() + ">" + FindSearchSuggestTask.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		@NonNull
		private final Context context;
		private final AgencyProperties agency;
		private final String query;

		FindSearchSuggestTask(@NonNull Context context, AgencyProperties agency, String query) {
			this.context = context;
			this.agency = agency;
			this.query = query;
		}

		@Override
		public HashSet<String> callMT() {
			return DataSourceManager.findSearchSuggest(this.context, this.agency.getAuthority(), this.query);
		}
	}
}
