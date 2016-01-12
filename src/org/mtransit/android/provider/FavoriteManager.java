package org.mtransit.android.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.ToastUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.DataSourceType;
import org.mtransit.android.data.Favorite;
import org.mtransit.android.provider.FavoriteProvider.FavoriteColumns;
import org.mtransit.android.provider.FavoriteProvider.FavoriteFolderColumns;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class FavoriteManager implements MTLog.Loggable {

	private static final String TAG = FavoriteManager.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static final int DEFAULT_FOLDER_ID = 0;

	public static boolean isFavoriteDataSourceId(int dataSourceId) {
		return dataSourceId > DataSourceType.MAX_ID;
	}

	public static int extractFavoriteFolderId(int dataSourceId) {
		return dataSourceId - DataSourceType.MAX_ID;
	}

	public static int generateFavoriteFolderId(int favoriteFolderId) {
		return favoriteFolderId + DataSourceType.MAX_ID;
	}

	public static int findFavoriteFolderId(Context context, String fkId) {
		Favorite favorite = findFavorite(context, fkId);
		if (favorite == null) {
			return -1;
		}
		return favorite.getFolderId();
	}

	public static boolean isFavorite(Context context, String fkId) {
		return findFavorite(context, fkId) != null;
	}

	public static boolean hasFavoriteFolders(HashMap<Integer, Favorite.Folder> favoriteFolders) {
		if (favoriteFolders == null || favoriteFolders.size() == 0) {
			return false;
		}
		if (favoriteFolders.size() == 1 && favoriteFolders.get(FavoriteManager.DEFAULT_FOLDER_ID) != null) {
			return false;
		}
		return true;
	}

	public static Favorite findFavorite(Context context, String fkId) {
		Favorite favorite = null;
		Cursor cursor = null;
		try {
			Uri uri = getFavoriteContentUri(context);
			String selection = SqlUtils.getWhereEqualsString(FavoriteColumns.T_FAVORITE_K_FK_ID, fkId);
			cursor = DataSourceManager.queryContentResolver(context.getContentResolver(), uri, FavoriteProvider.PROJECTION_FAVORITE, selection, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					favorite = Favorite.fromCursor(cursor);
				}
			}
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error!");
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
		return favorite;
	}

	private static Favorite findFavorite(Context context, Uri uri, String selection) {
		Favorite cache = null;
		Cursor cursor = null;
		try {
			cursor = DataSourceManager.queryContentResolver(context.getContentResolver(), uri, FavoriteProvider.PROJECTION_FAVORITE, selection, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					cache = Favorite.fromCursor(cursor);
				}
			}
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error!");
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
		return cache;
	}

	public static HashSet<String> findFavoriteUUIDs(Context context) {
		return extratFavoriteUUIDs(FavoriteManager.findFavorites(context));
	}

	public static HashSet<String> extratFavoriteUUIDs(ArrayList<Favorite> favorites) {
		HashSet<String> favoriteUUIDs = new HashSet<String>();
		if (favorites != null) {
			for (Favorite favorite : favorites) {
				favoriteUUIDs.add(favorite.getFkId());
			}
		}
		return favoriteUUIDs;
	}

	public static ArrayList<Favorite> findFavorites(Context context) {
		ArrayList<Favorite> result = new ArrayList<Favorite>();
		Cursor cursor = null;
		try {
			String selection = null;
			cursor = DataSourceManager.queryContentResolver(context.getContentResolver(), getFavoriteContentUri(context), FavoriteProvider.PROJECTION_FAVORITE,
					selection, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					do {
						result.add(Favorite.fromCursor(cursor));
					} while (cursor.moveToNext());
				}
			}
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error!");
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
		return result;
	}

	public static void addOrDeleteFavorite(Context context, boolean isFavorite, String fkId, int folderId, HashMap<Integer, Favorite.Folder> optFavoriteFolders) {
		if (isFavorite) { // WAS FAVORITE => TRY TO DELETE
			deleteFavorite(context, fkId, folderId, optFavoriteFolders);
		} else { // WAS NOT FAVORITE => TRY TO ADD
			addFavorite(context, fkId, folderId, optFavoriteFolders);
		}
	}

	public static void addFavorite(Context context, String fkId, int folderId, HashMap<Integer, Favorite.Folder> optFavoriteFolders) {
		Favorite newFavorite = new Favorite(fkId, folderId);
		boolean success = FavoriteManager.addFavorite(context, newFavorite) != null;
		if (success) {
			Favorite.Folder favoriteFolder = folderId != DEFAULT_FOLDER_ID && optFavoriteFolders != null && optFavoriteFolders.containsKey(folderId) ? optFavoriteFolders
					.get(folderId) : null;
			if (favoriteFolder == null) {
				ToastUtils.makeTextAndShowCentered(context, R.string.favorite_added);
			} else {
				ToastUtils.makeTextAndShowCentered(context, context.getString(R.string.favorite_added_to_folder_and_folder, favoriteFolder.getName()));
			}
		} else {
			MTLog.w(TAG, "Favorite not added!");
		}
	}

	public static Favorite addFavorite(Context context, Favorite newFavorite) {
		Uri uri = context.getContentResolver().insert(getFavoriteContentUri(context), newFavorite.toContentValues());
		if (uri == null) {
			return null;
		}
		return findFavorite(context, uri, null);
	}

	public static void deleteFavorite(Context context, String fkId, int folderId, HashMap<Integer, Favorite.Folder> optFavoriteFolders) {
		Favorite findFavorite = findFavorite(context, fkId);
		boolean success = findFavorite == null || // already deleted
				deleteFavorite(context, findFavorite.getId());
		if (success) {
			Favorite.Folder favoriteFolder = folderId != DEFAULT_FOLDER_ID && optFavoriteFolders != null && optFavoriteFolders.containsKey(folderId) ? optFavoriteFolders
					.get(folderId) : null;
			if (favoriteFolder == null) {
				ToastUtils.makeTextAndShowCentered(context, R.string.favorite_removed);
			} else {
				ToastUtils.makeTextAndShowCentered(context, context.getString(R.string.favorite_removed_to_folder_and_folder, favoriteFolder.getName()));
			}
		} else {
			MTLog.w(TAG, "Favorite not deleted!");
		}
	}

	public static boolean deleteFavorite(Context context, int id) {
		String selection = SqlUtils.getWhereEquals(FavoriteColumns.T_FAVORITE_K_ID, id);
		int deletedRows = context.getContentResolver().delete(getFavoriteContentUri(context), selection, null);
		return deletedRows > 0;
	}

	public static final String FAVORITE_CONTENT_DIRECTORY = "favorite";

	public static Uri getFavoriteContentUri(Context context) {
		return Uri.withAppendedPath(getAuthorityUri(context), FAVORITE_CONTENT_DIRECTORY);
	}

	public static HashMap<Integer, Favorite.Folder> findFolders(Context context) {
		HashMap<Integer, Favorite.Folder> result = new HashMap<Integer, Favorite.Folder>();
		Cursor cursor = null;
		try {
			String selection = null;
			cursor = DataSourceManager.queryContentResolver(context.getContentResolver(), getFolderContentUri(context), FavoriteProvider.PROJECTION_FOLDER,
					selection, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					do {
						Favorite.Folder favoriteFolder = Favorite.Folder.fromCursor(cursor);
						result.put(favoriteFolder.getId(), favoriteFolder);
					} while (cursor.moveToNext());
				}
			}
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error!");
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
		return result;
	}

	public static Favorite.Folder addFolder(Context context, Favorite.Folder newFolder) {
		Uri uri = context.getContentResolver().insert(getFolderContentUri(context), newFolder.toContentValues());
		if (uri == null) {
			return null;
		}
		return findFolder(context, uri, null);
	}

	private static Favorite.Folder findFolder(Context context, Uri uri, String selection) {
		Favorite.Folder cache = null;
		Cursor cursor = null;
		try {
			cursor = DataSourceManager.queryContentResolver(context.getContentResolver(), uri, FavoriteProvider.PROJECTION_FOLDER, selection, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					cache = Favorite.Folder.fromCursor(cursor);
				}
			}
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error!");
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
		return cache;
	}

	public static boolean deleteFolder(Context context, int folderId) {
		if (folderId == DEFAULT_FOLDER_ID) {
			MTLog.w(TAG, "Try to delete default favorite folder!");
			return false;
		}
		String selectionF = SqlUtils.getWhereEquals(FavoriteColumns.T_FAVORITE_K_FOLDER_ID, folderId);
		ContentValues updateValues = new ContentValues();
		updateValues.put(FavoriteColumns.T_FAVORITE_K_FOLDER_ID, DEFAULT_FOLDER_ID);
		int updatedRows = context.getContentResolver().update(getFavoriteContentUri(context), updateValues, selectionF, null);
		String selection = SqlUtils.getWhereEquals(FavoriteFolderColumns.T_FAVORITE_FOLDER_K_ID, folderId);
		int deletedRows = context.getContentResolver().delete(getFolderContentUri(context), selection, null);
		return deletedRows > 0;
	}

	public static boolean updateFavoriteFolder(Context context, String favoriteFkId, int folderId, HashMap<Integer, Favorite.Folder> optFavoriteFolders) {
		String selectionF = SqlUtils.getWhereEqualsString(FavoriteColumns.T_FAVORITE_K_FK_ID, favoriteFkId);
		ContentValues updateValues = new ContentValues();
		updateValues.put(FavoriteColumns.T_FAVORITE_K_FOLDER_ID, folderId);
		int updatedRows = context.getContentResolver().update(getFavoriteContentUri(context), updateValues, selectionF, null);
		if (updatedRows > 0) {
			Favorite.Folder favoriteFolder = folderId != DEFAULT_FOLDER_ID && optFavoriteFolders != null && optFavoriteFolders.containsKey(folderId) ? optFavoriteFolders
					.get(folderId) : null;
			if (favoriteFolder == null) {
				ToastUtils.makeTextAndShowCentered(context, R.string.favorite_moved);
			} else {
				ToastUtils.makeTextAndShowCentered(context, context.getString(R.string.favorite_moved_to_folder_and_folder, favoriteFolder.getName()));
			}
		} else {
			MTLog.w(TAG, "Favorite not moved!");
		}
		return updatedRows > 0;
	}

	public static boolean updateFolder(Context context, int folderId, String newFolderName) {
		String selectionF = SqlUtils.getWhereEquals(FavoriteFolderColumns.T_FAVORITE_FOLDER_K_ID, folderId);
		ContentValues updateValues = new ContentValues();
		updateValues.put(FavoriteFolderColumns.T_FAVORITE_FOLDER_K_NAME, newFolderName);
		int updatedRows = context.getContentResolver().update(getFolderContentUri(context), updateValues, selectionF, null);
		if (updatedRows > 0) {
			ToastUtils.makeTextAndShowCentered(context, context.getString(R.string.favorite_folder_edited_and_folder, newFolderName));
		} else {
			MTLog.w(TAG, "Favorite folder not updated!");
		}
		return updatedRows > 0;
	}

	public static final String FOLDER_CONTENT_DIRECTORY = "folder";

	public static Uri getFolderContentUri(Context context) {
		return Uri.withAppendedPath(Uri.withAppendedPath(getAuthorityUri(context), FAVORITE_CONTENT_DIRECTORY), FOLDER_CONTENT_DIRECTORY);
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
		void onFavoriteUpdated();
	}
}
