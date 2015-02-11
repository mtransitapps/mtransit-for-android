package org.mtransit.android.data;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.LocaleUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.data.DefaultPOI;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.provider.ModuleProvider;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

public class Module extends DefaultPOI {

	private static final String TAG = Module.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static final int DST_ID = DataSourceType.TYPE_MODULE.getId();

	private String pkg;

	private int targetTypeId;

	private String color = null;

	private String location = null;

	private String nameFr = null;

	public Module(String authority, String pkg, int targetTypeId) {
		super(authority, DST_ID, POI.ITEM_VIEW_TYPE_MODULE, POI.ITEM_STATUS_TYPE_APP, POI.ITEM_ACTION_TYPE_APP);
		setPkg(pkg);
		setTargetTypeId(targetTypeId);
	}

	public String getPkg() {
		return pkg;
	}

	private void setPkg(String pkg) {
		this.pkg = pkg;
		resetUUID();
	}

	public int getTargetTypeId() {
		return targetTypeId;
	}

	private void setTargetTypeId(int targetTypeId) {
		this.targetTypeId = targetTypeId;
	}

	private void setColor(String color) {
		this.color = color;
		this.colorInt = null; // reset
	}

	public String getColor() {
		return this.color;
	}

	private Integer colorInt = null;

	public int getColorInt() {
		if (this.colorInt == null) {
			this.colorInt = ColorUtils.parseColor(getColor());
		}
		return this.colorInt;
	}

	public String getLocation() {
		return this.location;
	}

	private void setLocation(String location) {
		this.location = location;
	}

	public String getNameFr() {
		return nameFr;
	}

	private void setNameFr(String nameFr) {
		this.nameFr = nameFr;
	}

	@Override
	public String getName() {
		if (LocaleUtils.isFR() && !TextUtils.isEmpty(getNameFr())) {
			return getNameFr();
		}
		return super.getName();
	}

	@Override
	public String toString() {
		return new StringBuilder().append(Module.class.getSimpleName()).append(":[") //
				.append("authority:").append(getAuthority()).append(',') //
				.append("pkg:").append(getPkg()).append(',') //
				.append("id:").append(getId()).append(',') //
				.append(']').toString();
	}

	@Override
	public int getType() {
		return POI.ITEM_VIEW_TYPE_MODULE;
	}

	@Override
	public int getStatusType() {
		return POI.ITEM_STATUS_TYPE_APP;
	}

	@Override
	public int getActionsType() {
		return POI.ITEM_ACTION_TYPE_APP;
	}

	@Override
	public boolean hasLocation() {
		return true; // required for distance sort
	}

	private String uuid = null;

	@Override
	public String getUUID() {
		if (this.uuid == null) {
			this.uuid = POI.POIUtils.getUUID(getAuthority(), getPkg());
		}
		return this.uuid;
	}

	@Override
	public void resetUUID() {
		this.uuid = null;
	}

	private static final String JSON_PKG = "pkg";
	private static final String JSON_TARGET_TYPE_ID = "targetTypeId";
	private static final String JSON_COLOR = "color";
	private static final String JSON_LOCATION = "location";
	private static final String JSON_NAME_FR = "name_fr";

	@Override
	public JSONObject toJSON() {
		try {
			JSONObject json = new JSONObject();
			json.put(JSON_PKG, getPkg());
			json.put(JSON_TARGET_TYPE_ID, getTargetTypeId());
			if (!TextUtils.isEmpty(getColor())) {
				json.put(JSON_COLOR, getColor());
			}
			if (!TextUtils.isEmpty(getLocation())) {
				json.put(JSON_LOCATION, getLocation());
			}
			if (!TextUtils.isEmpty(getNameFr())) {
				json.put(JSON_NAME_FR, getNameFr());
			}
			DefaultPOI.toJSON(this, json);
			return json;
		} catch (JSONException jsone) {
			MTLog.w(this, jsone, "Error while converting to JSON (%s)!", this);
			return null;
		}
	}

	@Override
	public POI fromJSON(JSONObject json) {
		return fromJSONStatic(json);
	}

	public static Module fromJSONStatic(JSONObject json) {
		try {
			Module module = new Module( //
					DefaultPOI.getAuthorityFromJSON(json), //
					json.getString(JSON_PKG), //
					json.getInt(JSON_TARGET_TYPE_ID) //
			);
			String optColor = json.optString(JSON_COLOR);
			if (!TextUtils.isEmpty(optColor)) {
				module.setColor(optColor);
			}
			String optLocation = json.optString(JSON_LOCATION);
			if (!TextUtils.isEmpty(optLocation)) {
				module.setLocation(optLocation);
			}
			String optNameFr = json.optString(JSON_NAME_FR);
			if (!TextUtils.isEmpty(optNameFr)) {
				module.setNameFr(optNameFr);
			}
			DefaultPOI.fromJSON(json, module);
			return module;
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while parsing JSON '%s'!", json);
			return null;
		}
	}

	public static Module fromSimpleJSONStatic(JSONObject json, String authority) {
		try {
			Module module = new Module( //
					authority, //
					json.getString(JSON_PKG), //
					json.getInt(JSON_TARGET_TYPE_ID) //
			);
			module.setName(json.getString(JSON_NAME));
			module.setLat(json.getDouble(JSON_LAT));
			module.setLng(json.getDouble(JSON_LNG));
			String optColor = json.optString(JSON_COLOR);
			if (!TextUtils.isEmpty(optColor)) {
				module.setColor(optColor);
			}
			String optLocation = json.optString(JSON_LOCATION);
			if (!TextUtils.isEmpty(optLocation)) {
				module.setLocation(optLocation);
			}
			String optNameFr = json.optString(JSON_NAME_FR);
			if (!TextUtils.isEmpty(optNameFr)) {
				module.setNameFr(optNameFr);
			}
			return module;
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while parsing simple JSON '%s'!", json);
			return null;
		}
	}

	@Override
	public ContentValues toContentValues() {
		ContentValues values = super.toContentValues();
		values.put(ModuleProvider.ModuleColumns.T_MODULE_K_PKG, getPkg());
		values.put(ModuleProvider.ModuleColumns.T_MODULE_K_TARGET_TYPE_ID, getTargetTypeId());
		values.put(ModuleProvider.ModuleColumns.T_MODULE_K_COLOR, getColor());
		values.put(ModuleProvider.ModuleColumns.T_MODULE_K_LOCATION, getLocation());
		values.put(ModuleProvider.ModuleColumns.T_MODULE_K_NAME_FR, getNameFr());
		return values;
	}

	@Override
	public POI fromCursor(Cursor c, String authority) {
		return fromCursorStatic(c, authority);
	}

	public static Module fromCursorStatic(Cursor c, String authority) {
		String pkg = c.getString(c.getColumnIndexOrThrow(ModuleProvider.ModuleColumns.T_MODULE_K_PKG));
		int targetTypeId = c.getInt(c.getColumnIndexOrThrow(ModuleProvider.ModuleColumns.T_MODULE_K_TARGET_TYPE_ID));
		Module module = new Module(authority, pkg, targetTypeId);
		module.setColor(c.getString(c.getColumnIndexOrThrow(ModuleProvider.ModuleColumns.T_MODULE_K_COLOR)));
		module.setLocation(c.getString(c.getColumnIndexOrThrow(ModuleProvider.ModuleColumns.T_MODULE_K_LOCATION)));
		module.setNameFr(c.getString(c.getColumnIndexOrThrow(ModuleProvider.ModuleColumns.T_MODULE_K_NAME_FR)));
		DefaultPOI.fromCursor(c, module);
		return module;
	}
}
