package org.mtransit.android.provider;

import java.util.ArrayList;
import java.util.List;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.ToastUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.data.Favorite;
import org.mtransit.android.provider.FavoriteProvider.FavoriteColumns;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class FavoriteManager implements MTLog.Loggable {

	private static final String TAG = FavoriteManager.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static boolean isFavorite(Context context, String fkId) {
		return findFavorite(context, fkId) != null;
	}

	public static Favorite findFavorite(Context context, String fkId) {
		MTLog.v(TAG, "findFavorite(%s)", fkId);
		Favorite favorite = null;
		Cursor cursor = null;
		try {
			Uri uri = getFavoriteContentUri(context);
			String selection = new StringBuilder() //
					.append(FavoriteColumns.T_FAVORITE_K_FK_ID).append("='").append(fkId).append("'") //
					.toString();
			cursor = context.getContentResolver().query(uri, FavoriteProvider.PROJECTION_FAVORITE, selection, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					favorite = Favorite.fromCursor(cursor);
				}
			}
		} catch (Throwable t) {
			MTLog.w(TAG, t, "Error!");
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return favorite;
	}

	private static Favorite findFavorite(Context context, Uri uri, String selection) {
		Favorite cache = null;
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(uri, FavoriteProvider.PROJECTION_FAVORITE, selection, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					cache = Favorite.fromCursor(cursor);
				}
			}
		} catch (Throwable t) {
			MTLog.w(TAG, t, "Error!");
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return cache;
	}

	public static List<Favorite> findFavorites(Context context, Integer... types) {
		List<Favorite> result = new ArrayList<Favorite>();
		Cursor cursor = null;
		try {
			StringBuilder selectionSb = new StringBuilder();
			if (types != null && types.length > 0) {
				selectionSb.append(FavoriteColumns.T_FAVORITE_K_TYPE).append(" IN (");
				for (int i = 0; i < types.length; i++) {
					if (i > 0) {
						selectionSb.append(",");
					}
					selectionSb.append(types[i]);
				}
				selectionSb.append(")");
			}
			cursor = context.getContentResolver().query(getFavoriteContentUri(context), FavoriteProvider.PROJECTION_FAVORITE, selectionSb.toString(), null,
					null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					do {
						result.add(Favorite.fromCursor(cursor));
					} while (cursor.moveToNext());
				}
			}
		} catch (Throwable t) {
			MTLog.w(TAG, t, "Error!");
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return result;
	}

	public static void addOrDeleteFavorite(Context context, boolean isFavorite, String fkId) {
		if (isFavorite) { // WAS FAVORITE => TRY TO DELETE
			final Favorite findFavorite = findFavorite(context, fkId);
			boolean success = findFavorite == null ? true : // already deleted
					deleteFavorite(context, findFavorite.getId());
			if (success) {
				ToastUtils.makeTextAndShowCentered(context, R.string.favorite_removed);
			} else {
				MTLog.w(TAG, "Favorite not deleted!");
			}
		} else { // WAS NOT FAVORITE => TRY TO ADD
			Favorite newFavorite = new Favorite(fkId);
			boolean success = FavoriteManager.addFavorite(context, newFavorite) != null;
			if (success) {
				ToastUtils.makeTextAndShowCentered(context, R.string.favorite_added);
			} else {
				MTLog.w(TAG, "Favorite not added!");
			}
		}
	}

	public static Favorite addFavorite(Context context, Favorite newFavorite) {
		final Uri uri = context.getContentResolver().insert(getFavoriteContentUri(context), newFavorite.toContentValues());
		if (uri != null) {
			return findFavorite(context, uri, null);
		} else {
			return null;
		}
	}

	public static boolean deleteFavorite(Context context, int id) {
		String selection = new StringBuilder() //
				.append(FavoriteColumns.T_FAVORITE_K_ID).append("=").append(id) //
				.toString();
		int deletedRows = context.getContentResolver().delete(getFavoriteContentUri(context), selection, null);
		return deletedRows > 0;
	}

	public static final String FAVORITE_CONTENT_DIRECTORY = "favorite";

	public static Uri getFavoriteContentUri(Context context) {
		return Uri.withAppendedPath(getAuthorityUri(context), FAVORITE_CONTENT_DIRECTORY);
	}

	private static String authority;

	public static String getAUTHORITY(Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.favorite_authority);
		}
		return authority;
	}

	private static Uri authorityUri;

	private static Uri getAuthorityUri(Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	public interface FavoriteUpdateListener {
		public void onFavoriteUpdated();
	}

}
