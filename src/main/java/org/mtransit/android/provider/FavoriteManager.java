package org.mtransit.android.provider;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.SparseArrayCompat;

import org.mtransit.android.R;
import org.mtransit.android.analytics.AnalyticsUserProperties;
import org.mtransit.android.analytics.IAnalyticsManager;
import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.ToastUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.DataSourceType;
import org.mtransit.android.data.Favorite;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.data.TextMessage;
import org.mtransit.android.di.Injection;
import org.mtransit.android.provider.FavoriteProvider.FavoriteColumns;
import org.mtransit.android.provider.FavoriteProvider.FavoriteFolderColumns;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@SuppressWarnings("unused")
public class FavoriteManager implements MTLog.Loggable {

	private static final String LOG_TAG = FavoriteManager.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public static final int DEFAULT_FOLDER_ID = 0;

	@Nullable
	private static FavoriteManager instance = null;

	@NonNull
	public static FavoriteManager get(@NonNull Context context) {
		if (instance == null) {
			initInstance(context);
		}
		return instance;
	}

	private static void initInstance(@NonNull Context context) {
		instance = new FavoriteManager();
		instance.favoriteFolders = findFolders(context);
		instance.analyticsManager.setUserProperty(AnalyticsUserProperties.FAVORITE_FOLDER_COUNT, instance.favoriteFolders.size());
	}

	@NonNull
	private final IAnalyticsManager analyticsManager;

	private FavoriteManager() {
		analyticsManager = Injection.providesAnalyticsManager();
	}

	@Nullable
	private SparseArrayCompat<Favorite.Folder> favoriteFolders = null;

	@Nullable
	public SparseArrayCompat<Favorite.Folder> getFavoriteFolders() {
		return this.favoriteFolders;
	}

	public boolean hasFavoriteFolders() {
		return this.favoriteFolders != null && this.favoriteFolders.size() > 0;
	}

	public boolean hasFavoriteFolder(int favoriteFolderId) {
		return ArrayUtils.containsKey(this.favoriteFolders, favoriteFolderId);
	}

	@Nullable
	public Favorite.Folder getFolder(int favoriteFolderId) {
		return this.favoriteFolders == null ? null : this.favoriteFolders.get(favoriteFolderId);
	}

	private void addFolder(@NonNull Favorite.Folder favoriteFolder) {
		if (this.favoriteFolders == null) {
			MTLog.w(this, "Try to add folder to the list before init!");
			return;
		}
		this.favoriteFolders.put(favoriteFolder.getId(), favoriteFolder);
		this.analyticsManager.setUserProperty(AnalyticsUserProperties.FAVORITE_FOLDER_COUNT, this.favoriteFolders.size());
	}

	private void removeFolder(@NonNull Favorite.Folder favoriteFolder) {
		removeFolder(favoriteFolder.getId());
	}

	private void removeFolder(int favoriteFolderId) {
		if (this.favoriteFolders == null) {
			MTLog.w(this, "Try to remove folder to the list before init!");
			return;
		}
		this.favoriteFolders.remove(favoriteFolderId);
		this.analyticsManager.setUserProperty(AnalyticsUserProperties.FAVORITE_FOLDER_COUNT, this.favoriteFolders.size());
	}

	public boolean isUsingFavoriteFolders() {
		if (this.favoriteFolders == null || this.favoriteFolders.size() == 0) {
			return false;
		}
		//noinspection RedundantIfStatement
		if (this.favoriteFolders.size() == 1 && this.favoriteFolders.get(DEFAULT_FOLDER_ID) != null) {
			return false;
		}
		return true;
	}

	public static boolean isFavoriteDataSourceId(int dataSourceId) {
		return dataSourceId > DataSourceType.MAX_ID;
	}

	public static int extractFavoriteFolderId(int dataSourceId) {
		return dataSourceId - DataSourceType.MAX_ID;
	}

	public static int generateFavoriteFolderId(int favoriteFolderId) {
		return favoriteFolderId + DataSourceType.MAX_ID;
	}

	public static int findFavoriteFolderId(@NonNull Context context, @NonNull String fkId) {
		Favorite favorite = findFavorite(context, fkId);
		if (favorite == null) {
			return -1;
		}
		return favorite.getFolderId();
	}

	public static boolean isFavorite(@NonNull Context context, @NonNull String fkId) {
		return findFavorite(context, fkId) != null;
	}

	@NonNull
	public static POIManager getNewEmptyFolder(@NonNull Context context, long textMessageId, int favoriteFolderId) {
		TextMessage textMessage = new TextMessage(textMessageId, context.getString(R.string.favorite_folder_empty));
		textMessage.setDataSourceTypeId(generateFavoriteFolderId(favoriteFolderId));
		return new POIManager(textMessage);
	}

	@Nullable
	private static Favorite findFavorite(@NonNull Context context, @NonNull String fkId) {
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

	@NonNull
	public static HashSet<String> findFavoriteUUIDs(@NonNull Context context) {
		return extractFavoriteUUIDs(findFavorites(context));
	}

	@NonNull
	private static HashSet<String> extractFavoriteUUIDs(@Nullable List<Favorite> favorites) {
		HashSet<String> favoriteUUIDs = new HashSet<>();
		if (favorites != null) {
			for (Favorite favorite : favorites) {
				favoriteUUIDs.add(favorite.getFkId());
			}
		}
		return favoriteUUIDs;
	}

	@NonNull
	public static ArrayList<Favorite> findFavorites(@NonNull Context context) {
		ArrayList<Favorite> result = new ArrayList<>();
		Cursor cursor = null;
		try {
			cursor = DataSourceManager.queryContentResolver( //
					context.getContentResolver(), //
					getFavoriteContentUri(context), //
					FavoriteProvider.PROJECTION_FAVORITE, //
					null, //
					null, //
					null //
			);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					do {
						result.add(Favorite.fromCursor(cursor));
					} while (cursor.moveToNext());
				}
			}
			get(context).analyticsManager.setUserProperty(AnalyticsUserProperties.FAVORITES_COUNT, result.size());
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error!");
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
		return result;
	}

	private int initialWhich;
	private int selectedWhich;

	public boolean addRemoveFavorite(final @NonNull Context context, final @NonNull String fkId, final @Nullable FavoriteUpdateListener listener) {
		final int favoriteFolderId = findFavoriteFolderId(context, fkId);
		boolean isFavorite = favoriteFolderId >= 0;
		if (isUsingFavoriteFolders()) { // show folder selector
			int checkedItem = -1;
			ArrayList<String> itemsList = new ArrayList<>();
			final ArrayList<Integer> itemsListId = new ArrayList<>();
			int i = 0;
			itemsListId.add(DEFAULT_FOLDER_ID);
			itemsList.add(context.getString(R.string.favorite_folder_default));
			//noinspection ConditionCoveredByFurtherCondition
			if (favoriteFolderId >= 0 && favoriteFolderId == DEFAULT_FOLDER_ID) {
				checkedItem = i;
			}
			i++;
			ArrayList<Favorite.Folder> favoriteFoldersList = ArrayUtils.asArrayList(getFavoriteFolders());
			CollectionUtils.sort(favoriteFoldersList, Favorite.Folder.NAME_COMPARATOR);
			for (Favorite.Folder favoriteFolder : favoriteFoldersList) {
				if (favoriteFolder.getId() == DEFAULT_FOLDER_ID) {
					continue;
				}
				itemsListId.add(favoriteFolder.getId());
				itemsList.add(favoriteFolder.getName());
				if (favoriteFolderId >= 0 && favoriteFolderId == favoriteFolder.getId()) {
					checkedItem = i;
				}
				i++;
			}
			final int newFolderId = Integer.MAX_VALUE;
			itemsListId.add(newFolderId);
			itemsList.add(context.getString(R.string.favorite_folder_new));
			i++;
			final int removeFavoriteId = newFolderId - 1;
			if (favoriteFolderId >= 0) {
				itemsListId.add(removeFavoriteId);
				itemsList.add(context.getString(R.string.favorite_remove));
				//noinspection UnusedAssignment // TODO ???
				i++;
			}
			this.initialWhich = checkedItem;
			if (checkedItem < 0) {
				checkedItem = 0; // (default choice when adding favorite)
			}
			this.selectedWhich = checkedItem;
			new AlertDialog.Builder(context) //
					.setTitle(R.string.favorite_folder_pick) //
					.setSingleChoiceItems( //
							itemsList.toArray(new String[]{}), //
							checkedItem, //
							(dialog, which) ->
									FavoriteManager.this.selectedWhich = which) //
					.setPositiveButton(R.string.favorite_folder_pick_ok, (dialog, id) -> {
						if (FavoriteManager.this.selectedWhich < 0 || FavoriteManager.this.selectedWhich == FavoriteManager.this.initialWhich) {
							return;
						}
						int selectedFavoriteFolderId = itemsListId.get(FavoriteManager.this.selectedWhich);
						if (selectedFavoriteFolderId == newFolderId) { // create new folder
							showAddFolderDialog(context, listener, fkId, favoriteFolderId);
						} else if (selectedFavoriteFolderId == removeFavoriteId) { // delete favorite
							deleteFavorite(context, fkId, favoriteFolderId, listener);
						} else if (favoriteFolderId >= 0) { // move favorite
							updateFavoriteFolder(context, fkId, selectedFavoriteFolderId, listener);
						} else { // add new favorite
							addFavorite(context, fkId, selectedFavoriteFolderId, listener);
						}
						if (dialog != null) {
							dialog.dismiss();
						}
					}) //
					.setNegativeButton(R.string.favorite_folder_pick_cancel, (dialog, id) -> {
						if (dialog != null) {
							dialog.cancel();
						}
					}).show();
		} else {
			addOrDeleteFavorite(context, isFavorite, fkId, DEFAULT_FOLDER_ID, listener);
		}
		return true; // HANDLED
	}

	@SuppressWarnings("SameParameterValue")
	private static void addOrDeleteFavorite(@NonNull Context context, boolean isFavorite, String fkId, int folderId, @Nullable FavoriteUpdateListener listener) {
		if (isFavorite) { // WAS FAVORITE => TRY TO DELETE
			deleteFavorite(context, fkId, folderId, listener);
		} else { // WAS NOT FAVORITE => TRY TO ADD
			addFavorite(context, fkId, folderId, listener);
		}
	}

	private static void addFavorite(@NonNull Context context, @NonNull String fkId, int folderId, @Nullable FavoriteUpdateListener listener) {
		Favorite newFavorite = new Favorite(fkId, folderId);
		boolean success = addFavorite(context, newFavorite) != null;
		if (success) {
			Favorite.Folder favoriteFolder = folderId == DEFAULT_FOLDER_ID ? null : get(context).getFolder(folderId);
			if (favoriteFolder == null) {
				ToastUtils.makeTextAndShowCentered(context, R.string.favorite_added);
			} else {
				ToastUtils.makeTextAndShowCentered(context, context.getString(R.string.favorite_added_to_folder_and_folder, favoriteFolder.getName()));
			}
			if (listener != null) {
				MTLog.d(LOG_TAG, "addFavorite() > onFavoriteUpdated()");
				listener.onFavoriteUpdated();
			}
		} else {
			MTLog.w(LOG_TAG, "Favorite not added!");
		}
	}

	@Nullable
	private static Favorite addFavorite(@NonNull Context context, @NonNull Favorite newFavorite) {
		Uri uri = context.getContentResolver().insert(getFavoriteContentUri(context), newFavorite.toContentValues());
		if (uri == null) {
			return null;
		}
		return findFavorite(context, uri, null);
	}

	private static void deleteFavorite(@NonNull Context context, String fkId, int folderId, @Nullable FavoriteUpdateListener listener) {
		Favorite findFavorite = findFavorite(context, fkId);
		if (findFavorite == null) {
			return; // already deleted
		}
		boolean success = deleteFavorite(context, findFavorite.getId());
		if (success) {
			Favorite.Folder favoriteFolder = folderId == DEFAULT_FOLDER_ID ? null : get(context).getFolder(folderId);
			if (favoriteFolder == null) {
				ToastUtils.makeTextAndShowCentered(context, R.string.favorite_removed);
			} else {
				ToastUtils.makeTextAndShowCentered(context, context.getString(R.string.favorite_removed_to_folder_and_folder, favoriteFolder.getName()));
			}
			if (listener != null) {
				listener.onFavoriteUpdated();
			}
		} else {
			MTLog.w(LOG_TAG, "Favorite not deleted!");
		}
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

	@NonNull
	public static SparseArrayCompat<Favorite.Folder> findFolders(@NonNull Context context) {
		SparseArrayCompat<Favorite.Folder> result = new SparseArrayCompat<>();
		Cursor cursor = null;
		try {
			cursor = DataSourceManager.queryContentResolver( //
					context.getContentResolver(), //
					getFolderContentUri(context), //
					FavoriteProvider.PROJECTION_FOLDER, //
					null, //
					null, //
					null //
			);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					do {
						Favorite.Folder favoriteFolder = Favorite.Folder.fromCursor(cursor);
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

	public static void showAddFolderDialog(final @NonNull Context context,
										   final @Nullable FavoriteUpdateListener listener,
										   final @Nullable String optUpdatedFkId,
										   final @Nullable Integer optFavoriteFolderId) {
		@SuppressLint("InflateParams") // dialog
				View view = LayoutInflater.from(context).inflate(R.layout.layout_favorites_folder_edit, null);
		final EditText newFolderNameTv = view.findViewById(R.id.folder_name);
		new AlertDialog.Builder(context) //
				.setView(view) //
				.setPositiveButton(R.string.favorite_folder_new_create, (dialog, id) -> {
					String newFolderName = newFolderNameTv.getText().toString();
					Favorite.Folder createdFolder = addFolder(context, newFolderName, TextUtils.isEmpty(optUpdatedFkId) ? listener : null);
					if (createdFolder != null && optUpdatedFkId != null && !optUpdatedFkId.isEmpty()) {
						if (optFavoriteFolderId != null && optFavoriteFolderId >= 0) { // move favorite
							updateFavoriteFolder(context, optUpdatedFkId, createdFolder.getId(), listener);
						} else { // add new favorite
							addFavorite(context, optUpdatedFkId, createdFolder.getId(), listener);
						}
					}
				}) //
				.setNegativeButton(R.string.favorite_folder_new_cancel, (dialog, id) -> {
					if (dialog != null) {
						dialog.cancel();
					}
				}) //
				.show();
	}

	@Nullable
	private static Favorite.Folder addFolder(@NonNull Context context, @NonNull String newFolderName, @Nullable FavoriteUpdateListener listener) {
		if (TextUtils.isEmpty(newFolderName)) {
			ToastUtils.makeTextAndShowCentered(context, R.string.favorite_folder_new_invalid_name);
			return null;
		}
		Uri uri = context.getContentResolver().insert(getFolderContentUri(context), new Favorite.Folder(newFolderName).toContentValues());
		if (uri == null) {
			return null;
		}
		Favorite.Folder createdFolder = findFolder(context, uri, null);
		if (createdFolder == null) {
			ToastUtils.makeTextAndShowCentered(context, context.getString(R.string.favorite_folder_new_creation_error_and_folder_name, newFolderName));
			return null;
		}
		ToastUtils.makeTextAndShowCentered(context, context.getString(R.string.favorite_folder_new_created_and_folder_name, createdFolder.getName()));
		if (listener != null) {
			listener.onFavoriteUpdated();
		}
		get(context).addFolder(createdFolder);
		return createdFolder;
	}

	@SuppressWarnings("SameParameterValue")
	@Nullable
	private static Favorite.Folder findFolder(@NonNull Context context, @NonNull Uri uri, @Nullable String selection) {
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
			MTLog.w(LOG_TAG, e, "Error!");
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
		return cache;
	}

	public static void showDeleteFolderDialog(final @NonNull Context context, final @NonNull Favorite.Folder favoriteFolder, final @Nullable FavoriteUpdateListener listener) {
		new AlertDialog.Builder(context) //
				.setTitle(context.getString(R.string.favorite_folder_deletion_confirmation_title_and_name, favoriteFolder.getName())) //
				.setMessage(context.getString(R.string.favorite_folder_deletion_confirmation_text_and_name, favoriteFolder.getName())) //
				.setPositiveButton(R.string.delete, (dialog, which) ->
						deleteFolder(context, favoriteFolder, listener)
				) //
				.setNegativeButton(R.string.cancel, (dialog, id) -> {
					if (dialog != null) {
						dialog.cancel();
					}
				}) //
				.show();
	}

	@SuppressWarnings("UnusedReturnValue")
	private static boolean deleteFolder(@NonNull Context context, @NonNull Favorite.Folder favoriteFolder, @Nullable FavoriteUpdateListener listener) {
		if (favoriteFolder.getId() == DEFAULT_FOLDER_ID) {
			MTLog.w(LOG_TAG, "Try to delete default favorite folder!");
			return false;
		}
		String selectionF = SqlUtils.getWhereEquals(FavoriteColumns.T_FAVORITE_K_FOLDER_ID, favoriteFolder.getId());
		ContentValues updateValues = new ContentValues();
		updateValues.put(FavoriteColumns.T_FAVORITE_K_FOLDER_ID, DEFAULT_FOLDER_ID);
		int updatedRows = context.getContentResolver().update(getFavoriteContentUri(context), updateValues, selectionF, null);
		String selection = SqlUtils.getWhereEquals(FavoriteFolderColumns.T_FAVORITE_FOLDER_K_ID, favoriteFolder.getId());
		int deletedRows = context.getContentResolver().delete(getFolderContentUri(context), selection, null);
		if (deletedRows > 0) {
			ToastUtils.makeTextAndShowCentered(context, context.getString(R.string.favorite_folder_deleted_and_folder_name, favoriteFolder.getName()));
			if (listener != null) {
				listener.onFavoriteUpdated();
			}
			get(context).removeFolder(favoriteFolder);
		} else {
			ToastUtils.makeTextAndShowCentered(context, context.getString(R.string.favorite_folder_deletion_error_and_folder_name, favoriteFolder.getName()));
		}
		return deletedRows > 0;
	}

	@SuppressWarnings("UnusedReturnValue")
	private static boolean updateFavoriteFolder(@NonNull Context context, @NonNull String favoriteFkId, int folderId, @Nullable FavoriteUpdateListener listener) {
		String selectionF = SqlUtils.getWhereEqualsString(FavoriteColumns.T_FAVORITE_K_FK_ID, favoriteFkId);
		ContentValues updateValues = new ContentValues();
		updateValues.put(FavoriteColumns.T_FAVORITE_K_FOLDER_ID, folderId);
		int updatedRows = context.getContentResolver().update(getFavoriteContentUri(context), updateValues, selectionF, null);
		if (updatedRows > 0) {
			Favorite.Folder favoriteFolder = folderId == DEFAULT_FOLDER_ID ? null : get(context).getFolder(folderId);
			if (favoriteFolder == null) {
				ToastUtils.makeTextAndShowCentered(context, R.string.favorite_moved);
			} else {
				ToastUtils.makeTextAndShowCentered(context, context.getString(R.string.favorite_moved_to_folder_and_folder, favoriteFolder.getName()));
			}
			if (listener != null) {
				listener.onFavoriteUpdated();
			}
		} else {
			MTLog.w(LOG_TAG, "Favorite not moved!");
		}
		return updatedRows > 0;
	}

	public static void showUpdateFolderDialog(final @NonNull Context context, @NonNull LayoutInflater layoutInflater, final @NonNull Favorite.Folder favoriteFolder,
											  final @Nullable FavoriteUpdateListener listener) {
		@SuppressLint("InflateParams") // dialog
				View editView = layoutInflater.inflate(R.layout.layout_favorites_folder_edit, null);
		final EditText newFolderNameTv = editView.findViewById(R.id.folder_name);
		newFolderNameTv.setText(favoriteFolder.getName());
		new AlertDialog.Builder(context) //
				.setView(editView) //
				.setPositiveButton(R.string.favorite_folder_edit, (dialog, id) -> {
					String newFolderName = newFolderNameTv.getText().toString();
					updateFolder(context, favoriteFolder.getId(), newFolderName, listener);
				}) //
				.setNegativeButton(R.string.favorite_folder_new_cancel, (dialog, id) -> {
					if (dialog != null) {
						dialog.cancel();
					}
				}) //
				.show();
	}

	@SuppressWarnings("UnusedReturnValue")
	private static boolean updateFolder(@NonNull Context context, int folderId, @NonNull String newFolderName, @Nullable FavoriteUpdateListener listener) {
		if (TextUtils.isEmpty(newFolderName)) {
			ToastUtils.makeTextAndShowCentered(context, R.string.favorite_folder_new_invalid_name);
			return false;
		}
		String selectionF = SqlUtils.getWhereEquals(FavoriteFolderColumns.T_FAVORITE_FOLDER_K_ID, folderId);
		ContentValues updateValues = new ContentValues();
		updateValues.put(FavoriteFolderColumns.T_FAVORITE_FOLDER_K_NAME, newFolderName);
		int updatedRows = context.getContentResolver().update(getFolderContentUri(context), updateValues, selectionF, null);
		if (updatedRows > 0) {
			ToastUtils.makeTextAndShowCentered(context, context.getString(R.string.favorite_folder_edited_and_folder, newFolderName));
			if (listener != null) {
				listener.onFavoriteUpdated();
			}
			get(context).removeFolder(folderId);
			get(context).addFolder(new Favorite.Folder(folderId, newFolderName));
		} else {
			MTLog.w(LOG_TAG, "Favorite folder not updated!");
		}
		return updatedRows > 0;
	}

	private static final String FOLDER_CONTENT_DIRECTORY = "folder";

	private static Uri getFolderContentUri(@NonNull Context context) {
		return Uri.withAppendedPath(Uri.withAppendedPath(getAuthorityUri(context), FAVORITE_CONTENT_DIRECTORY), FOLDER_CONTENT_DIRECTORY);
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
		void onFavoriteUpdated();
	}
}