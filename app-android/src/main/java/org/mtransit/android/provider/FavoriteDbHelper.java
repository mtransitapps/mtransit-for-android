package org.mtransit.android.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import androidx.annotation.NonNull;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.provider.MTSQLiteOpenHelper;
import org.mtransit.commons.sql.SQLCreateBuilder;
import org.mtransit.commons.sql.SQLInsertBuilder;

public class FavoriteDbHelper extends MTSQLiteOpenHelper {

	private static final String LOG_TAG = FavoriteDbHelper.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private static final String DB_NAME = "favorite.db";

	static final String T_FAVORITE = "favorite";
	static final String T_FAVORITE_K_ID = BaseColumns._ID;
	static final String T_FAVORITE_K_TYPE = "type";
	static final String T_FAVORITE_K_FK_ID = "fk_id";
	static final String T_FAVORITE_K_FOLDER_ID = "folder_id";

	static final String T_FAVORITE_FOLDER = "favorite_folder";
	static final String T_FAVORITE_FOLDER_K_ID = BaseColumns._ID;
	static final String T_FAVORITE_FOLDER_K_NAME = "name";

	private static final String T_FAVORITE_SQL_CREATE = SQLCreateBuilder.getNew(T_FAVORITE) //
			.appendColumn(T_FAVORITE_K_ID, SqlUtils.INT_PK) //
			.appendColumn(T_FAVORITE_K_TYPE, SqlUtils.INT) //
			.appendColumn(T_FAVORITE_K_FK_ID, SqlUtils.TXT) //
			.appendColumn(T_FAVORITE_K_FOLDER_ID, SqlUtils.INT) //
			.appendForeignKey(T_FAVORITE_K_FOLDER_ID, T_FAVORITE_FOLDER, T_FAVORITE_FOLDER_K_ID) //
			.build();
	private static final String T_FAVORITE_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_FAVORITE);

	private static final String T_FAVORITE_SQL_UPGRADE_BEFORE_2 = "ALTER TABLE " + T_FAVORITE + " ADD " + T_FAVORITE_K_FOLDER_ID + " " + SqlUtils.INT
			+ " NOT NULL DEFAULT(" + FavoriteManager.DEFAULT_FOLDER_ID + ")";

	private static final String T_FAVORITE_FOLDER_SQL_CREATE = SQLCreateBuilder.getNew(T_FAVORITE_FOLDER) //
			.appendColumn(T_FAVORITE_FOLDER_K_ID, SqlUtils.INT_PK) //
			.appendColumn(T_FAVORITE_FOLDER_K_NAME, SqlUtils.TXT) //
			.build();
	private static final String T_FAVORITE_FOLDER_SQL_INIT = String.format(
			SQLInsertBuilder.getNew(T_FAVORITE_FOLDER).appendColumns(T_FAVORITE_FOLDER_K_ID, T_FAVORITE_FOLDER_K_NAME).build(), //
			FavoriteManager.DEFAULT_FOLDER_ID + "," + SqlUtils.escapeString(StringUtils.EMPTY));
	private static final String T_FAVORITE_FOLDER_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_FAVORITE_FOLDER);

	private static final int DB_VERSION = 2;

	FavoriteDbHelper(@NonNull Context context) {
		super(context, getDbName(), null, getDbVersion());
	}

	@Override
	public void onCreateMT(@NonNull SQLiteDatabase db) {
		initAllDbTables(db);
	}

	@Override
	public void onUpgradeMT(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
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

	private void initAllDbTables(@NonNull SQLiteDatabase db) {
		db.execSQL(T_FAVORITE_SQL_CREATE);
		db.execSQL(T_FAVORITE_FOLDER_SQL_CREATE);
		db.execSQL(T_FAVORITE_FOLDER_SQL_INIT);
	}

	static int getDbVersion() {
		return DB_VERSION;
	}

	private static String getDbName() {
		return DB_NAME;
	}
}
