package org.mtransit.android.provider;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.provider.MTContentProvider;

public class FavoriteProvider extends MTContentProvider {

	private static final String LOG_TAG = FavoriteProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private static final int FAVORITE = 100;
	private static final int FAVORITE_ID = 101;

	private static final int FAVORITE_FOLDER = 200;
	private static final int FAVORITE_FOLDER_ID = 201;

	private static final ArrayMap<String, String> FAVORITE_PROJECTION_MAP = SqlUtils.ProjectionMapBuilder.getNew() //
			.appendTableColumn(FavoriteDbHelper.T_FAVORITE, FavoriteDbHelper.T_FAVORITE_K_ID, FavoriteColumns.T_FAVORITE_K_ID) //
			.appendTableColumn(FavoriteDbHelper.T_FAVORITE, FavoriteDbHelper.T_FAVORITE_K_TYPE, FavoriteColumns.T_FAVORITE_K_TYPE) //
			.appendTableColumn(FavoriteDbHelper.T_FAVORITE, FavoriteDbHelper.T_FAVORITE_K_FK_ID, FavoriteColumns.T_FAVORITE_K_FK_ID) //
			.appendTableColumn(FavoriteDbHelper.T_FAVORITE, FavoriteDbHelper.T_FAVORITE_K_FOLDER_ID, FavoriteColumns.T_FAVORITE_K_FOLDER_ID) //
			.build();

	public static final String[] PROJECTION_FAVORITE = new String[]{FavoriteColumns.T_FAVORITE_K_ID, FavoriteColumns.T_FAVORITE_K_TYPE,
			FavoriteColumns.T_FAVORITE_K_FK_ID, FavoriteColumns.T_FAVORITE_K_FOLDER_ID};

	private static final ArrayMap<String, String> FAVORITE_FOLDER_PROJECTION_MAP = SqlUtils.ProjectionMapBuilder.getNew() //
			.appendTableColumn(FavoriteDbHelper.T_FAVORITE_FOLDER, FavoriteDbHelper.T_FAVORITE_FOLDER_K_ID, FavoriteFolderColumns.T_FAVORITE_FOLDER_K_ID) //
			.appendTableColumn(FavoriteDbHelper.T_FAVORITE_FOLDER, FavoriteDbHelper.T_FAVORITE_FOLDER_K_NAME, FavoriteFolderColumns.T_FAVORITE_FOLDER_K_NAME) //
			.build();

	public static final String[] PROJECTION_FOLDER = new String[]{FavoriteFolderColumns.T_FAVORITE_FOLDER_K_ID,
			FavoriteFolderColumns.T_FAVORITE_FOLDER_K_NAME};

	@NonNull
	public static UriMatcher getNewUriMatcher(@NonNull String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(authority, "favorite", FAVORITE);
		URI_MATCHER.addURI(authority, "favorite/#", FAVORITE_ID);
		URI_MATCHER.addURI(authority, "favorite/folder", FAVORITE_FOLDER);
		URI_MATCHER.addURI(authority, "favorite/folder/#", FAVORITE_FOLDER_ID);
		return URI_MATCHER;
	}

	@Nullable
	private static String authority;

	@Nullable
	private static Uri authorityUri;

	@Nullable
	private static UriMatcher uriMatcher;

	@Nullable
	private FavoriteDbHelper dbHelper;

	private static int currentDbVersion = -1;

	@Override
	public boolean onCreateMT() {
		return true;
	}

	@NonNull
	private FavoriteDbHelper getDBHelper(@NonNull Context context) {
		if (dbHelper == null) { // initialize
			dbHelper = getNewDbHelper(context);
			currentDbVersion = getCurrentDbVersion();
		} else { // reset
			try {
				if (currentDbVersion != getCurrentDbVersion()) {
					dbHelper.close();
					dbHelper = null;
					return getDBHelper(context);
				}
			} catch (Exception e) { // fail if locked, will try again later
				MTLog.w(this, e, "Can't check DB version!");
			}
		}
		return dbHelper;
	}

	@NonNull
	private FavoriteDbHelper getNewDbHelper(@NonNull Context context) {
		return new FavoriteDbHelper(context.getApplicationContext());
	}

	private int getCurrentDbVersion() {
		return FavoriteDbHelper.getDbVersion();
	}

	@NonNull
	private static Uri getAuthorityUri(@NonNull Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	@NonNull
	private static UriMatcher getURI_MATCHER(@NonNull Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	/**
	 * Override if multiple {@link FavoriteProvider} implementations in same app.
	 */
	@NonNull
	private static String getAUTHORITY(@NonNull Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.favorite_authority);
		}
		return authority;
	}

	@Nullable
	@Override
	public Cursor queryMT(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
		try {
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			final Context context = getContext();
			if (context == null) {
				return null;
			}
			switch (getURI_MATCHER(context).match(uri)) {
			case FAVORITE:
				qb.setTables(FavoriteDbHelper.T_FAVORITE);
				qb.setProjectionMap(FAVORITE_PROJECTION_MAP);
				break;
			case FAVORITE_ID:
				qb.setTables(FavoriteDbHelper.T_FAVORITE);
				qb.setProjectionMap(FAVORITE_PROJECTION_MAP);
				selection = SqlUtils.getWhereEquals( //
						SqlUtils.getTableColumn(FavoriteDbHelper.T_FAVORITE, FavoriteDbHelper.T_FAVORITE_K_ID), //
						uri.getPathSegments().get(1));
				break;
			case FAVORITE_FOLDER:
				qb.setTables(FavoriteDbHelper.T_FAVORITE_FOLDER);
				qb.setProjectionMap(FAVORITE_FOLDER_PROJECTION_MAP);
				break;
			case FAVORITE_FOLDER_ID:
				qb.setTables(FavoriteDbHelper.T_FAVORITE_FOLDER);
				qb.setProjectionMap(FAVORITE_FOLDER_PROJECTION_MAP);
				selection = SqlUtils.getWhereEquals( //
						SqlUtils.getTableColumn(FavoriteDbHelper.T_FAVORITE_FOLDER, FavoriteDbHelper.T_FAVORITE_FOLDER_K_ID), //
						uri.getPathSegments().get(2));
				break;
			default:
				throw new IllegalArgumentException(String.format("Unknown URI (query): '%s'", uri));
			}
			if (TextUtils.isEmpty(sortOrder)) {
				sortOrder = getSortOrder(context, uri);
			}
			Cursor cursor = qb.query(getDBHelper(context).getReadableDatabase(), projection, selection, selectionArgs, null, null, sortOrder, null);
			if (cursor != null) {
				cursor.setNotificationUri(context.getContentResolver(), uri);
			}
			return cursor;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while resolving query '%s'!", uri);
			return null;
		}
	}

	@Nullable
	public String getSortOrder(@NonNull Context context, @NonNull Uri uri) {
		switch (getURI_MATCHER(context).match(uri)) {
		case FAVORITE:
		case FAVORITE_ID:
		case FAVORITE_FOLDER:
		case FAVORITE_FOLDER_ID:
			return null;
		default:
			throw new IllegalArgumentException(String.format("Unknown URI (order): '%s'", uri));
		}
	}

	@Nullable
	@Override
	public String getTypeMT(@NonNull Uri uri) {
		final Context context = getContext();
		if (context == null) {
			return null;
		}
		switch (getURI_MATCHER(context).match(uri)) {
		case FAVORITE:
		case FAVORITE_ID:
		case FAVORITE_FOLDER:
		case FAVORITE_FOLDER_ID:
			return null;
		default:
			throw new IllegalArgumentException(String.format("Unknown URI (type): '%s'", uri));
		}
	}

	@Override
	public int deleteMT(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
		int affectedRows = 0;
		try {
			final Context context = getContext();
			if (context == null) {
				return affectedRows;
			}
			switch (getURI_MATCHER(context).match(uri)) {
			case FAVORITE:
				affectedRows = getDBHelper(context).getWritableDatabase().delete(FavoriteDbHelper.T_FAVORITE, selection, selectionArgs);
				break;
			case FAVORITE_ID:
				selection = SqlUtils.getWhereEquals( //
						SqlUtils.getTableColumn(FavoriteDbHelper.T_FAVORITE, FavoriteDbHelper.T_FAVORITE_K_ID), //
						uri.getPathSegments().get(1));
				affectedRows = getDBHelper(context).getWritableDatabase().delete(FavoriteDbHelper.T_FAVORITE, selection, null);
				break;
			case FAVORITE_FOLDER:
				affectedRows = getDBHelper(context).getWritableDatabase().delete(FavoriteDbHelper.T_FAVORITE_FOLDER, selection, selectionArgs);
				break;
			case FAVORITE_FOLDER_ID:
				selection = SqlUtils.getWhereEquals( //
						SqlUtils.getTableColumn(FavoriteDbHelper.T_FAVORITE_FOLDER, FavoriteDbHelper.T_FAVORITE_FOLDER_K_ID), //
						uri.getPathSegments().get(2));
				affectedRows = getDBHelper(context).getWritableDatabase().delete(FavoriteDbHelper.T_FAVORITE_FOLDER, selection, null);
				break;
			default:
				throw new IllegalArgumentException(String.format("Unknown URI (delete): '%s'", uri));
			}
			if (affectedRows > 0) {
				context.getContentResolver().notifyChange(uri, null);
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while processing delete query %s!", uri);
		}
		return affectedRows;
	}

	@Override
	public int updateMT(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
		int affectedRows = 0;
		try {
			final Context context = getContext();
			if (context == null) {
				return affectedRows;
			}
			switch (getURI_MATCHER(context).match(uri)) {
			case FAVORITE:
				affectedRows = getDBHelper(context).getWritableDatabase().update(FavoriteDbHelper.T_FAVORITE, values, selection, selectionArgs);
				break;
			case FAVORITE_FOLDER:
				affectedRows = getDBHelper(context).getWritableDatabase().update(FavoriteDbHelper.T_FAVORITE_FOLDER, values, selection, selectionArgs);
				break;
			default:
				throw new IllegalArgumentException(String.format("Unknown URI (delete): '%s'", uri));
			}
			if (affectedRows > 0) {
				context.getContentResolver().notifyChange(uri, null);
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while processing delete query %s!", uri);
		}
		return affectedRows;
	}

	public static final String FAVORITE_CONTENT_DIRECTORY = "favorite";

	@NonNull
	public static Uri getFavoriteContentUri(@NonNull Context context) {
		return Uri.withAppendedPath(getAuthorityUri(context), FAVORITE_CONTENT_DIRECTORY);
	}

	public static final String FAVORITE_FOLDER_CONTENT_DIRECTORY = "folder";

	@NonNull
	public static Uri getFavoriteFolderContentUri(@NonNull Context context) {
		return Uri.withAppendedPath(Uri.withAppendedPath(getAuthorityUri(context), FAVORITE_CONTENT_DIRECTORY), FAVORITE_FOLDER_CONTENT_DIRECTORY);
	}

	@Nullable
	@Override
	public Uri insertMT(@NonNull Uri uri, @Nullable ContentValues values) {
		try {
			Uri insertUri = null;
			long newRowId;
			final Context context = getContext();
			if (context == null) {
				return null;
			}
			switch (getURI_MATCHER(context).match(uri)) {
			case FAVORITE:
				newRowId = getDBHelper(context).getWritableDatabase().insert(FavoriteDbHelper.T_FAVORITE, FavoriteDbHelper.T_FAVORITE_K_FK_ID, values);
				if (newRowId > 0) {
					insertUri = ContentUris.withAppendedId(getFavoriteContentUri(context), newRowId);
				}
				break;
			case FAVORITE_FOLDER:
				newRowId = getDBHelper(context).getWritableDatabase().insert(FavoriteDbHelper.T_FAVORITE_FOLDER,
						FavoriteDbHelper.T_FAVORITE_FOLDER_K_NAME, values);
				if (newRowId > 0) {
					insertUri = ContentUris.withAppendedId(getFavoriteFolderContentUri(context), newRowId);
				}
				break;
			default:
				throw new IllegalArgumentException(String.format("Unknown URI (insert): '%s'", uri));
			}
			if (insertUri == null) {
				throw new SQLException(String.format("Failed to insert row into %s", uri));
			} else {
				context.getContentResolver().notifyChange(insertUri, null);
				return insertUri;
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while resolving insert query '%s'!", uri);
			return null;
		}
	}

	public static class FavoriteColumns {
		public static final String T_FAVORITE_K_ID = BaseColumns._ID;
		public static final String T_FAVORITE_K_FK_ID = "fk_id";
		public static final String T_FAVORITE_K_TYPE = "type";
		public static final String T_FAVORITE_K_FOLDER_ID = "folder_id";
	}

	public static class FavoriteFolderColumns {
		public static final String T_FAVORITE_FOLDER_K_ID = BaseColumns._ID;
		public static final String T_FAVORITE_FOLDER_K_NAME = "name";
	}
}
