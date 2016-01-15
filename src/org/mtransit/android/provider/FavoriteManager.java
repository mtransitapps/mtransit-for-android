package org.mtransit.android.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.mtransit.android.R;
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
import org.mtransit.android.provider.FavoriteProvider.FavoriteColumns;
import org.mtransit.android.provider.FavoriteProvider.FavoriteFolderColumns;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

public class FavoriteManager implements MTLog.Loggable {

	private static final String TAG = FavoriteManager.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static final int DEFAULT_FOLDER_ID = 0;

	private static FavoriteManager instance = null;

	public static FavoriteManager get(Context context) {
		if (instance == null) {
			initInstance(context);
		}
		return instance;
	}

	private static void initInstance(Context context) {
		instance = new FavoriteManager();
		instance.favoriteFolders = findFolders(context);
	}

	private HashMap<Integer, Favorite.Folder> favoriteFolders = null;

	public HashMap<Integer, Favorite.Folder> getFavoriteFolders() {
		return this.favoriteFolders;
	}

	public boolean hasFavoriteFolders() {
		return this.favoriteFolders != null && this.favoriteFolders.size() > 0;
	}

	public boolean hasFavoriteFolder(int favoriteFolderId) {
		return this.favoriteFolders != null && this.favoriteFolders.containsKey(favoriteFolderId);
	}

	public Favorite.Folder getFolder(int favoriteFolderId) {
		return this.favoriteFolders == null ? null : this.favoriteFolders.get(favoriteFolderId);
	}

	private void addFolder(Favorite.Folder favoriteFolder) {
		if (this.favoriteFolders == null) {
			MTLog.w(this, "Try to add folder to the list before init!");
			return;
		}
		this.favoriteFolders.put(favoriteFolder.getId(), favoriteFolder);
	}

	private void removeFolder(Favorite.Folder favoriteFolder) {
		removeFolder(favoriteFolder.getId());
	}

	private void removeFolder(int favoriteFolderId) {
		if (this.favoriteFolders == null) {
			MTLog.w(this, "Try to remove folder to the list before init!");
			return;
		}
		this.favoriteFolders.remove(favoriteFolderId);
	}

	public boolean isUsingFavoriteFolders() {
		if (this.favoriteFolders == null || this.favoriteFolders.size() == 0) {
			return false;
		}
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

	public static POIManager getNewEmptyFolder(Context context, long textMessageId, int favoriteFolderId) {
		TextMessage textMessage = new TextMessage(textMessageId++, context.getString(R.string.favorite_folder_empty));
		textMessage.setDataSourceTypeId(generateFavoriteFolderId(favoriteFolderId));
		return new POIManager(textMessage);
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
		return extratFavoriteUUIDs(findFavorites(context));
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

	private int initialWhich;
	private int selectedWhich;

	public boolean addRemoveFavorite(final Activity activity, final String fkId, final FavoriteUpdateListener listener) {
		final Context context = activity;
		final int favoriteFolderId = findFavoriteFolderId(context, fkId);
		boolean isFavorite = favoriteFolderId >= 0;
		if (isUsingFavoriteFolders()) { // show folder selector
			int checkedItem = -1;
			ArrayList<String> itemsList = new ArrayList<String>();
			final ArrayList<Integer> itemsListId = new ArrayList<Integer>();
			int i = 0;
			itemsListId.add(DEFAULT_FOLDER_ID);
			itemsList.add(context.getString(R.string.favorite_folder_default));
			if (favoriteFolderId >= 0 && favoriteFolderId == DEFAULT_FOLDER_ID) {
				checkedItem = i;
			}
			i++;
			ArrayList<Favorite.Folder> favoriteFoldersList = new ArrayList<Favorite.Folder>(getFavoriteFolders().values());
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
			if (favoriteFolderId >= 0) {
				itemsListId.add(removeFavoriteId);
				itemsList.add(context.getString(R.string.favorite_remove));
				i++;
			}
			this.initialWhich = checkedItem;
			if (checkedItem < 0) {
				checkedItem = 0; // (default choice when adding favorite)
			}
			this.selectedWhich = checkedItem;
			new AlertDialog.Builder(context) //
					.setTitle(R.string.favorite_folder_pick) //
					.setSingleChoiceItems(itemsList.toArray(new String[] {}), checkedItem, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							FavoriteManager.this.selectedWhich = which;
						}
					}).setPositiveButton(R.string.favorite_folder_pick_ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							if (FavoriteManager.this.selectedWhich < 0 || FavoriteManager.this.selectedWhich == FavoriteManager.this.initialWhich) {
								return;
							}
							int selectedFavoriteFolderId = itemsListId.get(FavoriteManager.this.selectedWhich);
							if (selectedFavoriteFolderId == newFolderId) { // create new folder
								showAddFolderDialog(context, activity.getLayoutInflater(), listener, fkId);
							} else if (selectedFavoriteFolderId == removeFavoriteId) { // delete favorite
								deleteFavorite(context, fkId, favoriteFolderId);
							} else if (favoriteFolderId >= 0) { // move favorite
								updateFavoriteFolder(context, fkId, selectedFavoriteFolderId);
							} else { // add new favorite
								addFavorite(context, fkId, selectedFavoriteFolderId);
							}
							if (listener != null) {
								listener.onFavoriteUpdated();
							}
							if (dialog != null) {
								dialog.dismiss();
							}
						}
					}).setNegativeButton(R.string.favorite_folder_pick_cancel, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							if (dialog != null) {
								dialog.cancel();
							}
						}
					}).show();
		} else {
			addOrDeleteFavorite(context, isFavorite, fkId, DEFAULT_FOLDER_ID);
		}
		if (listener != null) {
			listener.onFavoriteUpdated();
		}
		return true; // HANDLED
	}

	private static void addOrDeleteFavorite(Context context, boolean isFavorite, String fkId, int folderId) {
		if (isFavorite) { // WAS FAVORITE => TRY TO DELETE
			deleteFavorite(context, fkId, folderId);
		} else { // WAS NOT FAVORITE => TRY TO ADD
			addFavorite(context, fkId, folderId);
		}
	}

	public static void addFavorite(Context context, String fkId, int folderId) {
		Favorite newFavorite = new Favorite(fkId, folderId);
		boolean success = addFavorite(context, newFavorite) != null;
		if (success) {
			Favorite.Folder favoriteFolder = folderId == DEFAULT_FOLDER_ID ? null : get(context).getFolder(folderId);
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

	public static void deleteFavorite(Context context, String fkId, int folderId) {
		Favorite findFavorite = findFavorite(context, fkId);
		boolean success = findFavorite == null || // already deleted
				deleteFavorite(context, findFavorite.getId());
		if (success) {
			Favorite.Folder favoriteFolder = folderId == DEFAULT_FOLDER_ID ? null : get(context).getFolder(folderId);
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

	public static void showAddFolderDialog(final Context context, LayoutInflater inflater, final FavoriteUpdateListener listener, final String optUpdatedFkId) {
		View view = inflater.inflate(R.layout.layout_favorites_folder_edit, null);
		final EditText newFolderNameTv = (EditText) view.findViewById(R.id.folder_name);
		new AlertDialog.Builder(context).setView(view).setPositiveButton(R.string.favorite_folder_new_create, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				String newFolderName = newFolderNameTv.getText().toString();
				Favorite.Folder createdFolder = addFolder(context, newFolderName, listener);
				if (createdFolder != null && !TextUtils.isEmpty(optUpdatedFkId)) {
					addFavorite(context, optUpdatedFkId, createdFolder.getId());
					if (listener != null) {
						listener.onFavoriteUpdated();
					}
				}
			}
		}).setNegativeButton(R.string.favorite_folder_new_cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				if (dialog != null) {
					dialog.cancel();
				}
			}
		}).show();
	}

	private static Favorite.Folder addFolder(Context context, String newFolderName, FavoriteUpdateListener listener) {
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

	public static void showDeleteFolderDialog(final Context context, final Favorite.Folder favoriteFolder, final FavoriteUpdateListener listener) {
		new AlertDialog.Builder(context) //
				.setTitle(context.getString(R.string.favorite_folder_deletion_confirmation_title_and_name, favoriteFolder.getName())) //
				.setMessage(context.getString(R.string.favorite_folder_deletion_confirmation_text_and_name, favoriteFolder.getName())) //
				.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						deleteFolder(context, favoriteFolder, listener);
					}
				}) //
				.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						if (dialog != null) {
							dialog.cancel();
						}
					}
				}) //
				.show();
	}

	private static boolean deleteFolder(Context context, Favorite.Folder favoriteFolder, FavoriteUpdateListener listener) {
		if (favoriteFolder.getId() == DEFAULT_FOLDER_ID) {
			MTLog.w(TAG, "Try to delete default favorite folder!");
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
		} else {
			ToastUtils.makeTextAndShowCentered(context, context.getString(R.string.favorite_folder_deletion_error_and_folder_name, favoriteFolder.getName()));
		}
		if (listener != null) {
			listener.onFavoriteUpdated();
		}
		get(context).removeFolder(favoriteFolder);
		return deletedRows > 0;
	}

	private static boolean updateFavoriteFolder(Context context, String favoriteFkId, int folderId) {
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
		} else {
			MTLog.w(TAG, "Favorite not moved!");
		}
		return updatedRows > 0;
	}

	public static void showUpdateFolderDialog(final Context context, LayoutInflater layoutInflater, final Favorite.Folder favoriteFolder,
			final FavoriteUpdateListener listener) {
		View editView = layoutInflater.inflate(R.layout.layout_favorites_folder_edit, null);
		final EditText newFolderNameTv = (EditText) editView.findViewById(R.id.folder_name);
		newFolderNameTv.setText(favoriteFolder.getName());
		new AlertDialog.Builder(context).setView(editView).setPositiveButton(R.string.favorite_folder_edit, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				String newFolderName = newFolderNameTv.getText().toString();
				updateFolder(context, favoriteFolder.getId(), newFolderName, listener);
			}
		}).setNegativeButton(R.string.favorite_folder_new_cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				if (dialog != null) {
					dialog.cancel();
				}
			}
		}).show();
	}

	private static boolean updateFolder(Context context, int folderId, String newFolderName, FavoriteUpdateListener listener) {
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
		} else {
			MTLog.w(TAG, "Favorite folder not updated!");
		}
		if (listener != null) {
			listener.onFavoriteUpdated();
		}
		get(context).removeFolder(folderId);
		get(context).addFolder(new Favorite.Folder(folderId, newFolderName));
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
