package org.mtransit.android.provider;

import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.provider.MTSQLiteOpenHelper;
import org.mtransit.android.commons.provider.POIDbHelper;
import org.mtransit.android.commons.provider.StatusDbHelper;

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
	public static final int DB_VERSION = 16;

	/**
	 * Override if multiple {@link ModuleDbHelper} implementations in same app.
	 */
	protected static final String PREF_KEY_LAST_UPDATE_MS = "pModuleLastUpdate";

	public static final String T_MODULE = POIDbHelper.T_POI;
	public static final String T_MODULE_K_PKG = POIDbHelper.getFkColumnName("pkg");
	public static final String T_MODULE_K_TARGET_TYPE_ID = POIDbHelper.getFkColumnName("targetTypeId");
	public static final String T_MODULE_K_COLOR = POIDbHelper.getFkColumnName("color");
	public static final String T_MODULE_K_LOCATION = POIDbHelper.getFkColumnName("location");
	public static final String T_MODULE_K_NAME_FR = POIDbHelper.getFkColumnName("name_fr");
	private static final String T_MODULE_SQL_CREATE = POIDbHelper.getSqlCreate(T_MODULE, //
			T_MODULE_K_PKG + SqlUtils.TXT, //
			T_MODULE_K_TARGET_TYPE_ID + SqlUtils.INT, //
			T_MODULE_K_COLOR + SqlUtils.TXT, //
			T_MODULE_K_LOCATION + SqlUtils.TXT, //
			T_MODULE_K_NAME_FR + SqlUtils.TXT //
	);
	private static final String T_MODULE_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_MODULE);

	public static final String T_MODULE_STATUS = StatusDbHelper.T_STATUS;
	private static final String T_MODULE_STATUS_SQL_CREATE = StatusDbHelper.getSqlCreate(T_MODULE_STATUS);
	private static final String T_MODULE_STATUS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_MODULE_STATUS);

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
