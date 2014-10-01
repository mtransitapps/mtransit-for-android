package org.mtransit.android.provider;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SqlUtils;
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

	public static final String T_FAVORITE_SQL_CREATE = SqlUtils.CREATE_TABLE_IF_NOT_EXIST + T_FAVORITE + " (" //
			+ T_FAVORITE_K_ID + SqlUtils.INT_PK + ", "//
			+ T_FAVORITE_K_TYPE + SqlUtils.INT + ", " //
			+ T_FAVORITE_K_FK_ID + SqlUtils.TXT + ")";
	public static final String T_FAVORITE_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_FAVORITE);

	public static final int DB_VERSION = 1;

	public FavoriteDbHelper(Context context) {
		super(context, getDbName(), null, getDbVersion());
	}

	@Override
	public void onCreateMT(SQLiteDatabase db) {
		initAllDbTables(db);
	}

	@Override
	public void onUpgradeMT(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL(T_FAVORITE_SQL_DROP);
		initAllDbTables(db);
	}

	private void initAllDbTables(SQLiteDatabase db) {
		db.execSQL(T_FAVORITE_SQL_CREATE);
	}

	public static int getDbVersion() {
		return DB_VERSION;
	}

	public static String getDbName() {
		return DB_NAME;
	}

}
