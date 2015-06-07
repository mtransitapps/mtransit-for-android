package org.mtransit.android.provider;

import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.provider.MTSQLiteOpenHelper;
import org.mtransit.android.commons.provider.POIProvider;
import org.mtransit.android.commons.provider.StatusProvider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class ModuleDbHelper extends MTSQLiteOpenHelper {

	private static final String TAG = ModuleDbHelper.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	/**
	 * Override if multiple {@link ModuleDbHelper} implementations in same app.
	 */
	protected static final String DB_NAME = "module.db";

	/**
	 * Override if multiple {@link ModuleDbHelper} in same app.
	 */
	public static final int DB_VERSION = 25;

	/**
	 * Override if multiple {@link ModuleDbHelper} implementations in same app.
	 */
	protected static final String PREF_KEY_LAST_UPDATE_MS = "pModuleLastUpdate";

	public static final String T_MODULE = POIProvider.POIDbHelper.T_POI;
	public static final String T_MODULE_K_PKG = POIProvider.POIDbHelper.getFkColumnName("pkg");
	public static final String T_MODULE_K_TARGET_TYPE_ID = POIProvider.POIDbHelper.getFkColumnName("targetTypeId");
	public static final String T_MODULE_K_COLOR = POIProvider.POIDbHelper.getFkColumnName("color");
	public static final String T_MODULE_K_LOCATION = POIProvider.POIDbHelper.getFkColumnName("location");
	public static final String T_MODULE_K_NAME_FR = POIProvider.POIDbHelper.getFkColumnName("name_fr");
	private static final String T_MODULE_SQL_CREATE = POIProvider.POIDbHelper.getSqlCreateBuilder(T_MODULE) //
			.appendColumn(T_MODULE_K_PKG, SqlUtils.TXT) //
			.appendColumn(T_MODULE_K_TARGET_TYPE_ID, SqlUtils.INT) //
			.appendColumn(T_MODULE_K_COLOR, SqlUtils.TXT) //
			.appendColumn(T_MODULE_K_LOCATION, SqlUtils.TXT) //
			.appendColumn(T_MODULE_K_NAME_FR, SqlUtils.TXT) //
			.build();
	private static final String T_MODULE_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_MODULE);

	public static final String T_MODULE_STATUS = StatusProvider.StatusDbHelper.T_STATUS;
	private static final String T_MODULE_STATUS_SQL_CREATE = StatusProvider.StatusDbHelper.getSqlCreateBuilder(T_MODULE_STATUS).build();
	private static final String T_MODULE_STATUS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_MODULE_STATUS);

	/**
	 * Override if multiple {@link ModuleDbHelper} in same app.
	 */
	public static int getDbVersion() {
		return DB_VERSION;
	}

	private Context context;

	public ModuleDbHelper(Context context) {
		super(context, DB_NAME, null, getDbVersion());
		this.context = context;
	}

	@Override
	public void onCreateMT(SQLiteDatabase db) {
		initAllDbTables(db);
	}

	@Override
	public void onUpgradeMT(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL(T_MODULE_SQL_DROP);
		db.execSQL(T_MODULE_STATUS_SQL_DROP);
		initAllDbTables(db);
	}

	public boolean isDbExist(Context context) {
		return SqlUtils.isDbExist(context, DB_NAME);
	}

	private void initAllDbTables(SQLiteDatabase db) {
		db.execSQL(T_MODULE_SQL_CREATE);
		db.execSQL(T_MODULE_STATUS_SQL_CREATE);
		PreferenceUtils.savePrefLcl(this.context, PREF_KEY_LAST_UPDATE_MS, 0l, true);
	}
}
