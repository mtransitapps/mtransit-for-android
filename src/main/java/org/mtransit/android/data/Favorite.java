package org.mtransit.android.data;

import java.util.Comparator;

import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.provider.FavoriteManager;
import org.mtransit.android.provider.FavoriteProvider;
import org.mtransit.android.provider.FavoriteProvider.FavoriteColumns;

import android.content.ContentValues;
import android.database.Cursor;

import androidx.collection.SparseArrayCompat;

public class Favorite {

	private static final int KEY_TYPE_VALUE_AUTHORITY_POI = 1;

	private int id = -1;
	private String fkId;
	private int type;
	private int folder_id;

	private Favorite() {
	}

	public Favorite(String fkId, int folderId) {
		this.type = KEY_TYPE_VALUE_AUTHORITY_POI;
		this.fkId = fkId;
		this.folder_id = folderId;
	}

	public static Favorite fromCursor(Cursor c) {
		Favorite favorite = new Favorite();
		favorite.id = c.getInt(c.getColumnIndexOrThrow(FavoriteColumns.T_FAVORITE_K_ID));
		favorite.type = c.getInt(c.getColumnIndexOrThrow(FavoriteColumns.T_FAVORITE_K_TYPE));
		favorite.fkId = c.getString(c.getColumnIndexOrThrow(FavoriteColumns.T_FAVORITE_K_FK_ID));
		int folderIdColumnInx = c.getColumnIndex(FavoriteColumns.T_FAVORITE_K_FOLDER_ID);
		if (folderIdColumnInx >= 0) {
			favorite.folder_id = c.getInt(folderIdColumnInx);
		} else {
			favorite.folder_id = FavoriteManager.DEFAULT_FOLDER_ID;
		}
		return favorite;
	}

	public static ContentValues toContentValues(Favorite favorite) {
		ContentValues values = new ContentValues();
		if (favorite.getId() > 0) {
			values.put(FavoriteColumns.T_FAVORITE_K_ID, favorite.id);
		} // ELSE IF no ID yet, let SQLite choose the ID
		values.put(FavoriteColumns.T_FAVORITE_K_TYPE, favorite.type);
		values.put(FavoriteColumns.T_FAVORITE_K_FK_ID, favorite.fkId);
		values.put(FavoriteColumns.T_FAVORITE_K_FOLDER_ID, favorite.folder_id);
		return values;
	}

	public ContentValues toContentValues() {
		return toContentValues(this);
	}

	public int getId() {
		return id;
	}

	public String getFkId() {
		return fkId;
	}
	public int getFolderId() {
		return folder_id;
	}

	public static class FavoriteFolderNameComparator implements Comparator<POIManager> {

		private SparseArrayCompat<Favorite.Folder> favoriteFolders;

		public FavoriteFolderNameComparator(SparseArrayCompat<Folder> favoriteFolders) {
			this.favoriteFolders = favoriteFolders;
		}

		@Override
		public int compare(POIManager lPoim, POIManager rPoim) {
			String lFavoriteFolderName = StringUtils.EMPTY;
			if (FavoriteManager.isFavoriteDataSourceId(lPoim.poi.getDataSourceTypeId())) {
				int favoriteFolderId = FavoriteManager.extractFavoriteFolderId(lPoim.poi.getDataSourceTypeId());
				if (ArrayUtils.containsKey(this.favoriteFolders, favoriteFolderId)) {
					lFavoriteFolderName = this.favoriteFolders.get(favoriteFolderId).getName();
				}
			}
			String rFavoriteFolderName = StringUtils.EMPTY;
			if (FavoriteManager.isFavoriteDataSourceId(rPoim.poi.getDataSourceTypeId())) {
				int favoriteFolderId = FavoriteManager.extractFavoriteFolderId(rPoim.poi.getDataSourceTypeId());
				if (ArrayUtils.containsKey(this.favoriteFolders, favoriteFolderId)) {
					rFavoriteFolderName = this.favoriteFolders.get(favoriteFolderId).getName();
				}
			}
			return lFavoriteFolderName.compareTo(rFavoriteFolderName);
		}
	}

	@Override
	public String toString() {
		return Favorite.class.getSimpleName() + "[" + this.id + "," + this.fkId + "," + this.folder_id + "]";
	}

	public static class Folder {

		public static FavoriteFolderNameComparator NAME_COMPARATOR = new FavoriteFolderNameComparator();

		private int id = -1;
		private String name;

		private Folder() {
		}

		public Folder(String name) {
			this.name = name;
		}

		public Folder(int id, String name) {
			this.id = id;
			this.name = name;
		}

		public static Favorite.Folder fromCursor(Cursor c) {
			Favorite.Folder favoriteFolder = new Favorite.Folder();
			favoriteFolder.id = c.getInt(c.getColumnIndexOrThrow(FavoriteProvider.FavoriteFolderColumns.T_FAVORITE_FOLDER_K_ID));
			favoriteFolder.name = c.getString(c.getColumnIndexOrThrow(FavoriteProvider.FavoriteFolderColumns.T_FAVORITE_FOLDER_K_NAME));
			return favoriteFolder;
		}

		public static ContentValues toContentValues(Favorite.Folder favoriteFolder) {
			ContentValues values = new ContentValues();
			if (favoriteFolder.getId() >= 0) {
				values.put(FavoriteProvider.FavoriteFolderColumns.T_FAVORITE_FOLDER_K_ID, favoriteFolder.id);
			} // ELSE IF no ID yet, let SQLite choose the ID
			values.put(FavoriteProvider.FavoriteFolderColumns.T_FAVORITE_FOLDER_K_NAME, favoriteFolder.name);
			return values;
		}

		public ContentValues toContentValues() {
			return toContentValues(this);
		}

		public int getId() {
			return this.id;
		}

		public String getName() {
			return this.name;
		}

		@Override
		public String toString() {
			return "Favorite." + Favorite.Folder.class.getSimpleName() + "[" + this.id + "," + this.name + "]";
		}

		private static class FavoriteFolderNameComparator implements Comparator<Favorite.Folder> {

			@Override
			public int compare(Favorite.Folder lFolder, Favorite.Folder rFolder) {
				String lFolderName = lFolder == null ? StringUtils.EMPTY : lFolder.getName();
				String rFolderName = rFolder == null ? StringUtils.EMPTY : rFolder.getName();
				return lFolderName.compareTo(rFolderName);
			}
		}
	}
}
