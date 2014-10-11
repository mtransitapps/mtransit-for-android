package org.mtransit.android.provider;

import java.util.HashMap;
import java.util.Map;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.provider.MTContentProvider;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class FavoriteProvider extends MTContentProvider {

	private static final String TAG = FavoriteProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final int FAVORITE = 100;
	private static final int FAVORITE_ID = 101;

	private static final Map<String, String> FAVORITE_PROJECTION_MAP;
	static {
		HashMap<String, String> map;
		map = new HashMap<String, String>();
		map.put(FavoriteColumns.T_FAVORITE_K_ID, FavoriteDbHelper.T_FAVORITE + "." + FavoriteDbHelper.T_FAVORITE_K_ID + " AS "
				+ FavoriteColumns.T_FAVORITE_K_ID);
		map.put(FavoriteColumns.T_FAVORITE_K_TYPE, FavoriteDbHelper.T_FAVORITE + "." + FavoriteDbHelper.T_FAVORITE_K_TYPE + " AS "
				+ FavoriteColumns.T_FAVORITE_K_TYPE);
		map.put(FavoriteColumns.T_FAVORITE_K_FK_ID, FavoriteDbHelper.T_FAVORITE + "." + FavoriteDbHelper.T_FAVORITE_K_FK_ID + " AS "
				+ FavoriteColumns.T_FAVORITE_K_FK_ID);
		FAVORITE_PROJECTION_MAP = map;
	}

	public static UriMatcher getNewUriMatcher(String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(authority, "favorite", FAVORITE);
		URI_MATCHER.addURI(authority, "favorite/#", FAVORITE_ID);
		return URI_MATCHER;
	}

	private static String authority;
	private static Uri authorityUri;
	private static UriMatcher uriMatcher;

	private static FavoriteDbHelper dbHelper;

	private static int currentDbVersion = -1;

	@Override
	public boolean onCreateMT() {
		return true;
	}

	private FavoriteDbHelper getDBHelper(Context context) {
		if (dbHelper == null) {
			dbHelper = getNewDbHelper(context);
			currentDbVersion = getCurrentDbVersion();
		} else {
			try {
				if (currentDbVersion != getCurrentDbVersion()) {
					dbHelper.close();
					dbHelper = null;
					return getDBHelper(context);
				}
			} catch (Throwable t) {
				MTLog.d(this, t, "Can't check DB version!");
			}
		}
		return dbHelper;
	}

	public FavoriteDbHelper getNewDbHelper(Context context) {
		return new FavoriteDbHelper(context.getApplicationContext());
	}

	public int getCurrentDbVersion() {
		return FavoriteDbHelper.getDbVersion();
	}

	private static Uri getAuthorityUri(Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	public static UriMatcher getURIMATCHER(Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	public static String getAUTHORITY(Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.favorite_authority);
		}
		return authority;
	}

	@Override
	public Cursor queryMT(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		try {
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			String limit = null;
			switch (getURIMATCHER(getContext()).match(uri)) {
			case FAVORITE:
				MTLog.v(this, "query>FAVORITE");
				qb.setTables(FavoriteDbHelper.T_FAVORITE);
				qb.setProjectionMap(FAVORITE_PROJECTION_MAP);
				break;
			case FAVORITE_ID:
				MTLog.v(this, "query>FAVORITE_ID");
				qb.setTables(FavoriteDbHelper.T_FAVORITE);
				qb.setProjectionMap(FAVORITE_PROJECTION_MAP);
				selection = FavoriteDbHelper.T_FAVORITE + "." + FavoriteDbHelper.T_FAVORITE_K_ID + "=" + uri.getPathSegments().get(1);
				break;
			default:
				throw new IllegalArgumentException(String.format("Unknown URI (query): '%s'", uri));
			}
			if (TextUtils.isEmpty(sortOrder)) {
				sortOrder = getSortOrder(uri);
			}
			Cursor cursor = qb.query(getDBHelper(getContext()).getReadableDatabase(), projection, selection, selectionArgs, null, null, sortOrder, limit);
			if (cursor != null) {
				cursor.setNotificationUri(getContext().getContentResolver(), uri);
			}
			return cursor;
		} catch (Throwable t) {
			MTLog.w(this, t, "Error while resolving query '%s'!", uri);
			return null;
		}
	}

	public String getSortOrder(Uri uri) {
		switch (getURIMATCHER(getContext()).match(uri)) {
		case FAVORITE:
		case FAVORITE_ID:
			return null;
		default:
			throw new IllegalArgumentException(String.format("Unknown URI (order): '%s'", uri));
		}
	}

	@Override
	public String getTypeMT(Uri uri) {
		switch (getURIMATCHER(getContext()).match(uri)) {
		case FAVORITE:
		case FAVORITE_ID:
			return null;
		default:
			throw new IllegalArgumentException(String.format("Unknown URI (type): '%s'", uri));
		}
	}

	@Override
	public int deleteMT(Uri uri, String selection, String[] selectionArgs) {
		int affectedRows = 0;
		SQLiteDatabase db = null;
		try {
			switch (getURIMATCHER(getContext()).match(uri)) {
			case FAVORITE:
				db = getDBHelper(getContext()).getWritableDatabase();
				affectedRows = db.delete(FavoriteDbHelper.T_FAVORITE, selection, selectionArgs);
				break;
			case FAVORITE_ID:
				selection = FavoriteDbHelper.T_FAVORITE + "." + FavoriteDbHelper.T_FAVORITE_K_ID + "=" + uri.getPathSegments().get(1);
				db = getDBHelper(getContext()).getWritableDatabase();
				affectedRows = db.delete(FavoriteDbHelper.T_FAVORITE, selection, null);
				break;
			default:
				throw new IllegalArgumentException(String.format("Unknown URI (delete): '%s'", uri));
			}
			if (affectedRows > 0) {
				getContext().getContentResolver().notifyChange(uri, null);
			}
		} catch (Throwable t) {
			MTLog.w(this, t, "Error while processing delete query %s!", uri);
		}
		return affectedRows;
	}

	@Override
	public int updateMT(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		MTLog.w(this, "The update method is not available.");
		return 0; // no row affected
	}

	public static final String FAVORITE_CONTENT_DIRECTORY = "favorite";

	public static Uri getFavoriteContentUri(Context context) {
		return Uri.withAppendedPath(getAuthorityUri(context), FAVORITE_CONTENT_DIRECTORY);
	}

	@Override
	public Uri insertMT(Uri uri, ContentValues values) {
		try {
			Uri insertUri = null;
			long newRowId = -1;
			switch (getURIMATCHER(getContext()).match(uri)) {
			case FAVORITE:
				MTLog.v(this, "insert>FAVORITE");
				newRowId = getDBHelper(getContext()).getWritableDatabase().insert(FavoriteDbHelper.T_FAVORITE, FavoriteDbHelper.T_FAVORITE_K_FK_ID, values);
				if (newRowId > 0) {
					insertUri = ContentUris.withAppendedId(getFavoriteContentUri(getContext()), newRowId);
				}
				break;
			default:
				throw new IllegalArgumentException(String.format("Unknown URI (insert): '%s'", uri));
			}
			if (insertUri == null) {
				throw new SQLException(String.format("Failed to insert row into %s", uri));
			} else {
				getContext().getContentResolver().notifyChange(insertUri, null);
				return insertUri;
			}
		} catch (Throwable t) {
			MTLog.w(this, t, "Error while resolving insert query '%s'!", uri);
			return null;
		}
	}

}
