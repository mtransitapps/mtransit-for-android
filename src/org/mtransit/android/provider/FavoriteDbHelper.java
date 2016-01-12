package org.mtransit.android.provider;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.provider.MTSQLiteOpenHelper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

public class FavoriteDbHelper extends MTSQLiteOpenHelper {

	private static final String TAG = FavoriteDbHelper.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static final String DB_NAME = "favorite.db";

	public static final String T_FAVORITE = "favorite";
	public static final String T_FAVORITE_K_ID = BaseColumns._ID;
	public static final String T_FAVORITE_K_TYPE = "type";
	public static final String T_FAVORITE_K_FK_ID = "fk_id";
	public static final String T_FAVORITE_K_FOLDER_ID = "folder_id";

	public static final String T_FAVORITE_FOLDER = "favorite_folder";
	public static final String T_FAVORITE_FOLDER_K_ID = BaseColumns._ID;
	public static final String T_FAVORITE_FOLDER_K_NAME = "name";

	public static final String T_FAVORITE_SQL_CREATE = SqlUtils.SQLCreateBuilder.getNew(T_FAVORITE) //
			.appendColumn(T_FAVORITE_K_ID, SqlUtils.INT_PK) //
			.appendColumn(T_FAVORITE_K_TYPE, SqlUtils.INT) //
			.appendColumn(T_FAVORITE_K_FK_ID, SqlUtils.TXT) //
			.appendColumn(T_FAVORITE_K_FOLDER_ID, SqlUtils.INT) //
			.appendForeignKey(T_FAVORITE_K_FOLDER_ID, T_FAVORITE_FOLDER, T_FAVORITE_FOLDER_K_ID) //
			.build();
	public static final String T_FAVORITE_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_FAVORITE);

	private static final String T_FAVORITE_SQL_UPGRADE_BEFORE_2 = "ALTER TABLE " + T_FAVORITE + " ADD " + T_FAVORITE_K_FOLDER_ID + " " + SqlUtils.INT
			+ " NOT NULL DEFAULT(" + FavoriteManager.DEFAULT_FOLDER_ID + ")";

	public static final String T_FAVORITE_FOLDER_SQL_CREATE = SqlUtils.SQLCreateBuilder.getNew(T_FAVORITE_FOLDER) //
			.appendColumn(T_FAVORITE_FOLDER_K_ID, SqlUtils.INT_PK) //
			.appendColumn(T_FAVORITE_FOLDER_K_NAME, SqlUtils.TXT) //
			.build();
	private static final String T_FAVORITE_FOLDER_SQL_INIT = String.format(
			SqlUtils.SQLInsertBuilder.getNew(T_FAVORITE_FOLDER).appendColumns(T_FAVORITE_FOLDER_K_ID, T_FAVORITE_FOLDER_K_NAME).build(), //
			FavoriteManager.DEFAULT_FOLDER_ID + "," + SqlUtils.escapeString(StringUtils.EMPTY));
	public static final String T_FAVORITE_FOLDER_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_FAVORITE_FOLDER);

	public static final int DB_VERSION = 2;

	public FavoriteDbHelper(Context context) {
		super(context, getDbName(), null, getDbVersion());
	}

	@Override
	public void onCreateMT(SQLiteDatabase db) {
		initAllDbTables(db);
	}

	@Override
	public void onUpgradeMT(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion < 2) {
			try {
				db.execSQL(T_FAVORITE_FOLDER_SQL_CREATE);
				db.execSQL(T_FAVORITE_FOLDER_SQL_INIT);
				db.execSQL(T_FAVORITE_SQL_UPGRADE_BEFORE_2);
				return;
			} catch (Exception e) {
				MTLog.w(this, e, "Error while upgrading DB from %s to %s! (reset)", oldVersion, newVersion);
			}
		}
		db.execSQL(T_FAVORITE_SQL_DROP);
		db.execSQL(T_FAVORITE_FOLDER_SQL_DROP);
		initAllDbTables(db);
	}

	private void initAllDbTables(SQLiteDatabase db) {
		db.execSQL(T_FAVORITE_SQL_CREATE);
		db.execSQL(T_FAVORITE_FOLDER_SQL_CREATE);
		db.execSQL(T_FAVORITE_FOLDER_SQL_INIT);
	}

	public static int getDbVersion() {
		return DB_VERSION;
	}

	public static String getDbName() {
		return DB_NAME;
	}
}
