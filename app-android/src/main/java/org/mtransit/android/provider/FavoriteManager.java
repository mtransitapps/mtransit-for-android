package org.mtransit.android.provider;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.collection.SparseArrayCompat;

import org.mtransit.android.R;
import org.mtransit.android.analytics.AnalyticsUserProperties;
import org.mtransit.android.analytics.IAnalyticsManager;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.Favorite;
import org.mtransit.android.data.FavoriteFolder;
import org.mtransit.android.dev.DemoModeManager;
import org.mtransit.android.provider.FavoriteProvider.FavoriteColumns;
import org.mtransit.android.provider.FavoriteProvider.FavoriteFolderColumns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;

public class FavoriteManager implements MTLog.Loggable {

	private static final String LOG_TAG = FavoriteManager.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private static final int DEFAULT_FOLDER_ID = FavoriteFolder.DEFAULT_FOLDER_ID;

	@NonNull
	private final DemoModeManager demoModeManager;

	@NonNull
	private final IAnalyticsManager analyticsManager;

	@Inject
	public FavoriteManager(@NonNull @ApplicationContext Context context,
						   @NonNull DemoModeManager demoModeManager,
						   @NonNull IAnalyticsManager analyticsManager) {
		this.demoModeManager = demoModeManager;
		this.analyticsManager = analyticsManager;
		//noinspection resource
		Executors.newSingleThreadExecutor().execute(() -> {
			final SparseArrayCompat<FavoriteFolder> newFavoriteFolders = findFolders(context);
			this.favoriteFolders.putAll(newFavoriteFolders);
			this.analyticsManager.setUserProperty(AnalyticsUserProperties.FAVORITE_FOLDER_COUNT, this.favoriteFolders.size());
		});
	}

	@NonNull
	private final SparseArrayCompat<FavoriteFolder> favoriteFolders = new SparseArrayCompat<>();

	private void addFolder(@NonNull FavoriteFolder favoriteFolder) {
		this.favoriteFolders.put(favoriteFolder.getId(), favoriteFolder);
		this.analyticsManager.setUserProperty(AnalyticsUserProperties.FAVORITE_FOLDER_COUNT, this.favoriteFolders.size());
	}

	private void removeFolder(@NonNull FavoriteFolder favoriteFolder) {
		removeFolder(favoriteFolder.getId());
	}

	private void removeFolder(int favoriteFolderId) {
		this.favoriteFolders.remove(favoriteFolderId);
		this.analyticsManager.setUserProperty(AnalyticsUserProperties.FAVORITE_FOLDER_COUNT, this.favoriteFolders.size());
	}

	@WorkerThread
	public boolean isFavorite(@NonNull Context context, @NonNull String fkId) {
		return findFavorite(context, fkId) != null;
	}

	@WorkerThread
	@Nullable
	public Favorite findFavorite(@NonNull Context context, @NonNull String fkId) {
		if (demoModeManager.getEnabled()) return null;
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
			MTLog.w(LOG_TAG, e, "Error!");
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
		return favorite;
	}

	@SuppressWarnings("SameParameterValue")
	@WorkerThread
	@Nullable
	private static Favorite findFavorite(@NonNull Context context, @NonNull Uri uri, @Nullable String selection) {
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
			MTLog.w(LOG_TAG, e, "Error!");
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
		return cache;
	}

	@WorkerThread
	public boolean hasFavorites(@NonNull Context context) {
		return extractHasFavorites(findFavorites(context));
	}

	private boolean extractHasFavorites(@Nullable List<Favorite> favorites) {
		return favorites != null && !favorites.isEmpty();
	}

	@WorkerThread
	@NonNull
	public HashSet<String> findFavoriteUUIDs(@NonNull Context context) {
		return extractFavoriteUUIDs(findFavorites(context));
	}

	@NonNull
	private HashSet<String> extractFavoriteUUIDs(@Nullable List<Favorite> favorites) {
		HashSet<String> favoriteUUIDs = new HashSet<>();
		if (favorites != null) {
			for (Favorite favorite : favorites) {
				favoriteUUIDs.add(favorite.getFkId());
			}
		}
		return favoriteUUIDs;
	}

	@WorkerThread
	@NonNull
	public List<Favorite> findFavorites(@NonNull Context context) {
		if (demoModeManager.getEnabled()) return Collections.emptyList();
		final List<Favorite> result = new ArrayList<>();
		Cursor cursor = null;
		try {
			cursor = DataSourceManager.queryContentResolver(
					context.getContentResolver(),
					getFavoriteContentUri(context),
					FavoriteProvider.PROJECTION_FAVORITE,
					null,
					null,
					null
			);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					do {
						result.add(Favorite.fromCursor(cursor));
					} while (cursor.moveToNext());
				}
			}
			analyticsManager.setUserProperty(AnalyticsUserProperties.FAVORITES_COUNT, result.size());
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error!");
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
		return result;
	}


	@WorkerThread
	public boolean addFavorite(@NonNull Context context, @NonNull String fkId, int folderId, @Nullable FavoriteUpdateListener listener) {
		final Favorite newFavorite = Favorite.makeFavorite(fkId, folderId);
		final boolean success = addFavorite(context, newFavorite) != null;
		if (success) {
			if (listener != null) {
				MTLog.d(LOG_TAG, "addFavorite() > onFavoriteUpdated()");
				listener.onFavoriteUpdated();
			}
		} else {
			MTLog.w(LOG_TAG, "Favorite not added!");
		}
		return success;
	}

	@Nullable
	private Favorite addFavorite(@NonNull Context context, @NonNull Favorite newFavorite) {
		final Uri uri = context.getContentResolver().insert(getFavoriteContentUri(context), newFavorite.toContentValues());
		if (uri == null) return null;
		return findFavorite(context, uri, null);
	}

	@WorkerThread
	public boolean deleteFavorite(@NonNull Context context, @NonNull String fkId, @Nullable FavoriteUpdateListener listener) {
		final Favorite findFavorite = findFavorite(context, fkId);
		if (findFavorite == null) return false; // already deleted
		final boolean success = deleteFavorite(context, findFavorite.getId());
		if (success) {
			if (listener != null) {
				listener.onFavoriteUpdated();
			}
		} else {
			MTLog.w(LOG_TAG, "Favorite not deleted!");
		}
		return success;
	}

	private static boolean deleteFavorite(@NonNull Context context, int id) {
		String selection = SqlUtils.getWhereEquals(FavoriteColumns.T_FAVORITE_K_ID, id);
		int deletedRows = context.getContentResolver().delete(getFavoriteContentUri(context), selection, null);
		return deletedRows > 0;
	}

	private static final String FAVORITE_CONTENT_DIRECTORY = "favorite";

	private static Uri getFavoriteContentUri(@NonNull Context context) {
		return Uri.withAppendedPath(getAuthorityUri(context), FAVORITE_CONTENT_DIRECTORY);
	}

	@WorkerThread
	@NonNull
	private static SparseArrayCompat<FavoriteFolder> findFolders(@NonNull Context context) {
		final SparseArrayCompat<FavoriteFolder> result = new SparseArrayCompat<>();
		Cursor cursor = null;
		try {
			cursor = DataSourceManager.queryContentResolver(
					context.getContentResolver(),
					getFolderContentUri(context),
					FavoriteProvider.PROJECTION_FOLDER,
					null,
					null,
					null
			);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					do {
						FavoriteFolder favoriteFolder = FavoriteFolder.fromCursor(cursor);
						result.put(favoriteFolder.getId(), favoriteFolder);
					} while (cursor.moveToNext());
				}
			}
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error!");
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
		return result;
	}

	@WorkerThread
	@NonNull
	public List<FavoriteFolder> findFoldersList(@NonNull Context context) {
		final List<FavoriteFolder> result = new ArrayList<>();
		Cursor cursor = null;
		try {
			cursor = DataSourceManager.queryContentResolver(
					context.getContentResolver(),
					getFolderContentUri(context),
					FavoriteProvider.PROJECTION_FOLDER,
					null,
					null,
					null
			);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					do {
						final FavoriteFolder favoriteFolder = FavoriteFolder.fromCursor(cursor);
						result.add(favoriteFolder);
					} while (cursor.moveToNext());
				}
			}
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error!");
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
		return result;
	}

	@WorkerThread
	@Nullable
	public FavoriteFolder addFolder(@NonNull Context context, @NonNull String newFolderName, @Nullable FavoriteUpdateListener listener) {
		final Uri uri = context.getContentResolver().insert(getFolderContentUri(context), new FavoriteFolder(newFolderName).toContentValues());
		if (uri == null) return null;
		final FavoriteFolder createdFolder = findFolder(context, uri, null);
		if (createdFolder == null) {
			return null;
		}
		if (listener != null) {
			listener.onFavoriteUpdated();
		}
		addFolder(createdFolder);
		return createdFolder;
	}

	@SuppressWarnings("SameParameterValue")
	@WorkerThread
	@Nullable
	private FavoriteFolder findFolder(@NonNull Context context, @NonNull Uri uri, @Nullable String selection) {
		FavoriteFolder cache = null;
		Cursor cursor = null;
		try {
			cursor = DataSourceManager.queryContentResolver(context.getContentResolver(), uri, FavoriteProvider.PROJECTION_FOLDER, selection, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					cache = FavoriteFolder.fromCursor(cursor);
				}
			}
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error!");
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
		return cache;
	}

	@WorkerThread
	public boolean deleteFolder(@NonNull Context context, @NonNull FavoriteFolder favoriteFolder, @Nullable FavoriteUpdateListener listener) {
		if (favoriteFolder.getId() == DEFAULT_FOLDER_ID) {
			MTLog.w(LOG_TAG, "Try to delete default favorite folder!");
			return false;
		}
		final String selectionF = SqlUtils.getWhereEquals(FavoriteColumns.T_FAVORITE_K_FOLDER_ID, favoriteFolder.getId());
		final ContentValues updateValues = new ContentValues();
		updateValues.put(FavoriteColumns.T_FAVORITE_K_FOLDER_ID, DEFAULT_FOLDER_ID);
		context.getContentResolver().update(getFavoriteContentUri(context), updateValues, selectionF, null);
		final String selection = SqlUtils.getWhereEquals(FavoriteFolderColumns.T_FAVORITE_FOLDER_K_ID, favoriteFolder.getId());
		final int deletedRows = context.getContentResolver().delete(getFolderContentUri(context), selection, null);
		if (deletedRows > 0) {
			if (listener != null) {
				listener.onFavoriteUpdated();
			}
			removeFolder(favoriteFolder);
		}
		return deletedRows > 0;
	}

	@WorkerThread
	public boolean updateFavoriteFolder(@NonNull Context context, @NonNull String favoriteFkId, int folderId, @Nullable FavoriteUpdateListener listener) {
		final String selectionF = SqlUtils.getWhereEqualsString(FavoriteColumns.T_FAVORITE_K_FK_ID, favoriteFkId);
		final ContentValues updateValues = new ContentValues();
		updateValues.put(FavoriteColumns.T_FAVORITE_K_FOLDER_ID, folderId);
		final int updatedRows = context.getContentResolver().update(getFavoriteContentUri(context), updateValues, selectionF, null);
		if (updatedRows > 0) {
			if (listener != null) {
				listener.onFavoriteUpdated();
			}
		} else {
			MTLog.w(LOG_TAG, "Favorite not moved!");
		}
		return updatedRows > 0;
	}


	@WorkerThread
	public boolean updateFolder(@NonNull Context context, int folderId, @NonNull String newFolderName, @Nullable FavoriteUpdateListener listener) {
		String selectionF = SqlUtils.getWhereEquals(FavoriteFolderColumns.T_FAVORITE_FOLDER_K_ID, folderId);
		ContentValues updateValues = new ContentValues();
		updateValues.put(FavoriteFolderColumns.T_FAVORITE_FOLDER_K_NAME, newFolderName);
		int updatedRows = context.getContentResolver().update(getFolderContentUri(context), updateValues, selectionF, null);
		if (updatedRows > 0) {
			if (listener != null) {
				listener.onFavoriteUpdated();
			}
			removeFolder(folderId);
			addFolder(new FavoriteFolder(folderId, newFolderName));
		} else {
			MTLog.w(LOG_TAG, "Favorite folder not updated!");
		}
		return updatedRows > 0;
	}

	private static final String FOLDER_CONTENT_DIRECTORY = "folder";

	private static Uri getFolderContentUri(@NonNull Context context) {
		return Uri.withAppendedPath(getFavoriteContentUri(context), FOLDER_CONTENT_DIRECTORY);
	}

	@Nullable
	private static String authority;

	@NonNull
	private static String getAUTHORITY(@NonNull Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.favorite_authority);
		}
		return authority;
	}

	@Nullable
	private static Uri authorityUri;

	@NonNull
	private static Uri getAuthorityUri(@NonNull Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	public interface FavoriteUpdateListener {
		@AnyThread
		void onFavoriteUpdated();
	}
}
