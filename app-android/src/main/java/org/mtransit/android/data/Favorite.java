package org.mtransit.android.data;

import android.content.ContentValues;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.SparseArrayCompat;

import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.provider.FavoriteManager;
import org.mtransit.android.provider.FavoriteProvider;
import org.mtransit.android.provider.FavoriteProvider.FavoriteColumns;

import java.util.Comparator;

public class Favorite {

	private static final int KEY_TYPE_VALUE_AUTHORITY_POI = 1;

	private final int id;
	@NonNull
	private final String fkId;
	private final int type;
	private final int folder_id;

	public Favorite(@NonNull String fkId, int folderId) {
		this(-1, fkId, KEY_TYPE_VALUE_AUTHORITY_POI, folderId);
	}

	@SuppressWarnings("WeakerAccess")
	public Favorite(int id, @NonNull String fkId, int type, int folderId) {
		this.id = id;
		this.fkId = fkId;
		this.type = type;
		this.folder_id = folderId;
	}

	@NonNull
	public static Favorite fromCursor(@NonNull Cursor c) {
		final int folderId;
		final int folderIdColumnInx = c.getColumnIndex(FavoriteColumns.T_FAVORITE_K_FOLDER_ID);
		if (folderIdColumnInx > FavoriteManager.DEFAULT_FOLDER_ID) {
			folderId = c.getInt(folderIdColumnInx);
		} else {
			folderId = FavoriteManager.DEFAULT_FOLDER_ID;
		}
		return new Favorite(
				c.getInt(c.getColumnIndexOrThrow(FavoriteColumns.T_FAVORITE_K_ID)),
				c.getString(c.getColumnIndexOrThrow(FavoriteColumns.T_FAVORITE_K_FK_ID)),
				c.getInt(c.getColumnIndexOrThrow(FavoriteColumns.T_FAVORITE_K_TYPE)),
				folderId
		);
	}

	@SuppressWarnings("WeakerAccess")
	@NonNull
	public static ContentValues toContentValues(@NonNull Favorite favorite) {
		ContentValues values = new ContentValues();
		if (favorite.getId() >= 0) {
			values.put(FavoriteColumns.T_FAVORITE_K_ID, favorite.id);
		} // ELSE IF no ID yet, let SQLite choose the ID
		values.put(FavoriteColumns.T_FAVORITE_K_TYPE, favorite.type);
		values.put(FavoriteColumns.T_FAVORITE_K_FK_ID, favorite.fkId);
		values.put(FavoriteColumns.T_FAVORITE_K_FOLDER_ID, favorite.folder_id);
		return values;
	}

	@NonNull
	public ContentValues toContentValues() {
		return toContentValues(this);
	}

	public int getId() {
		return id;
	}

	@NonNull
	public String getFkId() {
		return fkId;
	}

	public int getFolderId() {
		return folder_id;
	}

	public static class FavoriteFolderNameComparator implements Comparator<POIManager> {

		@NonNull
		private final FavoriteManager favoriteManager;
		@NonNull
		private final SparseArrayCompat<Favorite.Folder> favoriteFolders;

		public FavoriteFolderNameComparator(@NonNull FavoriteManager favoriteManager,
											@NonNull SparseArrayCompat<Folder> favoriteFolders) {
			this.favoriteManager = favoriteManager;
			this.favoriteFolders = favoriteFolders;
		}

		@Override
		public int compare(@NonNull POIManager lPoim, @NonNull POIManager rPoim) {
			String lFavoriteFolderName = StringUtils.EMPTY;
			if (this.favoriteManager.isFavoriteDataSourceId(lPoim.poi.getDataSourceTypeId())) {
				final int favoriteFolderId = this.favoriteManager.extractFavoriteFolderId(lPoim.poi.getDataSourceTypeId());
				final Folder favFolder = this.favoriteFolders.get(favoriteFolderId);
				if (favFolder != null) {
					lFavoriteFolderName = favFolder.getName();
				}
			}
			String rFavoriteFolderName = StringUtils.EMPTY;
			if (this.favoriteManager.isFavoriteDataSourceId(rPoim.poi.getDataSourceTypeId())) {
				final int favoriteFolderId = this.favoriteManager.extractFavoriteFolderId(rPoim.poi.getDataSourceTypeId());
				final Folder favFolder = this.favoriteFolders.get(favoriteFolderId);
				if (favFolder != null) {
					rFavoriteFolderName = favFolder.getName();
				}
			}
			return lFavoriteFolderName.compareTo(rFavoriteFolderName);
		}
	}

	@NonNull
	@Override
	public String toString() {
		return Favorite.class.getSimpleName() + "[" + this.id + "," + this.fkId + "," + this.folder_id + "]";
	}

	public static class Folder {

		@NonNull
		public static FavoriteFolderNameComparator NAME_COMPARATOR = new FavoriteFolderNameComparator();

		private final int id;
		@NonNull
		private final String name;

		public Folder(@NonNull String name) {
			this(-1, name);
		}

		public Folder(int id, @NonNull String name) {
			this.id = id;
			this.name = name;
		}

		@NonNull
		public static Favorite.Folder fromCursor(@NonNull Cursor c) {
			return new Favorite.Folder(
					c.getInt(c.getColumnIndexOrThrow(FavoriteProvider.FavoriteFolderColumns.T_FAVORITE_FOLDER_K_ID)),
					c.getString(c.getColumnIndexOrThrow(FavoriteProvider.FavoriteFolderColumns.T_FAVORITE_FOLDER_K_NAME))
			);
		}

		@SuppressWarnings("WeakerAccess")
		@NonNull
		public static ContentValues toContentValues(@NonNull Favorite.Folder favoriteFolder) {
			ContentValues values = new ContentValues();
			if (favoriteFolder.getId() >= 0) {
				values.put(FavoriteProvider.FavoriteFolderColumns.T_FAVORITE_FOLDER_K_ID, favoriteFolder.id);
			} // ELSE IF no ID yet, let SQLite choose the ID
			values.put(FavoriteProvider.FavoriteFolderColumns.T_FAVORITE_FOLDER_K_NAME, favoriteFolder.name);
			return values;
		}

		@NonNull
		public ContentValues toContentValues() {
			return toContentValues(this);
		}

		public int getId() {
			return this.id;
		}

		@NonNull
		public String getName() {
			return this.name;
		}

		@NonNull
		@Override
		public String toString() {
			return "Favorite." + Favorite.Folder.class.getSimpleName() + "[" + this.id + "," + this.name + "]";
		}

		private static class FavoriteFolderNameComparator implements Comparator<Favorite.Folder> {

			@Override
			public int compare(@Nullable Favorite.Folder lFolder, @Nullable Favorite.Folder rFolder) {
				String lFolderName = lFolder == null ? StringUtils.EMPTY : lFolder.getName();
				String rFolderName = rFolder == null ? StringUtils.EMPTY : rFolder.getName();
				return lFolderName.compareTo(rFolderName);
			}
		}
	}
}
