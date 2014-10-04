package org.mtransit.android.data;

import org.mtransit.android.provider.FavoriteColumns;

import android.content.ContentValues;
import android.database.Cursor;

public class Favorite {

	private static final int KEY_TYPE_VALUE_AUTHORITY_POI = 1;

	private int id = -1;
	private String fkId;
	private int type;

	public Favorite() {
	}

	public Favorite(/* int type, */String fkId) {
		this.type = KEY_TYPE_VALUE_AUTHORITY_POI;// type;
		this.fkId = fkId;
	}

	public static Favorite fromCursor(Cursor c) {
		final Favorite favorite = new Favorite();
		favorite.id = c.getInt(c.getColumnIndexOrThrow(FavoriteColumns.T_FAVORITE_K_ID));
		favorite.type = c.getInt(c.getColumnIndexOrThrow(FavoriteColumns.T_FAVORITE_K_TYPE));
		favorite.fkId = c.getString(c.getColumnIndexOrThrow(FavoriteColumns.T_FAVORITE_K_FK_ID));
		return favorite;
	}

	public static ContentValues toContentValues(Favorite favorite) {
		final ContentValues values = new ContentValues();
		if (favorite.getId() > 0) {
			values.put(FavoriteColumns.T_FAVORITE_K_ID, favorite.id);
		} // ELSE IF no ID yet, let SQLite choose the ID
		values.put(FavoriteColumns.T_FAVORITE_K_TYPE, favorite.type);
		values.put(FavoriteColumns.T_FAVORITE_K_FK_ID, favorite.fkId);
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

}
