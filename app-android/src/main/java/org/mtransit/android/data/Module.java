package org.mtransit.android.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.BuildConfig;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.LocaleUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.data.DataSourceTypeId;
import org.mtransit.android.commons.data.DefaultPOI;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.provider.ModuleProvider;

@SuppressWarnings("WeakerAccess")
public class Module extends DefaultPOI {

	private static final String LOG_TAG = Module.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	private final String pkg;

	@DataSourceTypeId.DataSourceType
	private final int targetTypeId;

	@Nullable
	private String color = null;

	@Nullable
	private String location = null;

	@Nullable
	private String nameFr = null;

	public Module(@NonNull String authority, @NonNull String pkg, @DataSourceTypeId.DataSourceType int targetTypeId) {
		super(authority, DataSourceTypeId.MODULE, POI.ITEM_VIEW_TYPE_MODULE, POI.ITEM_STATUS_TYPE_APP, POI.ITEM_ACTION_TYPE_APP);
		this.pkg = pkg;
		resetUUID();
		this.targetTypeId = targetTypeId;
	}

	@NonNull
	public String getPkg() {
		if (BuildConfig.DEBUG) {
			return pkg + ".debug";
		}
		return pkg;
	}

	@DataSourceTypeId.DataSourceType
	public int getTargetTypeId() {
		return targetTypeId;
	}

	private void setColor(@Nullable String color) {
		this.color = color;
		this.colorInt = null; // reset
	}

	@Nullable
	public String getColor() {
		return this.color;
	}

	@ColorInt
	@Nullable
	private Integer colorInt = null;

	@ColorInt
	public int getColorInt() {
		if (this.colorInt == null) {
			if (getColor() != null) {
				this.colorInt = ColorUtils.parseColor(getColor());
			}
		}
		return this.colorInt;
	}

	@Nullable
	public String getLocation() {
		return this.location;
	}

	private void setLocation(@Nullable String location) {
		this.location = location;
	}

	@Nullable
	public String getNameFr() {
		return nameFr;
	}

	private void setNameFr(@Nullable String nameFr) {
		this.nameFr = nameFr;
	}

	@NonNull
	@Override
	public String getName() {
		if (LocaleUtils.isFR()
				&& getNameFr() != null) {
			return getNameFr();
		}
		return super.getName();
	}

	@NonNull
	@Override
	public String toString() {
		return Module.class.getSimpleName() + "{" +
				"pkg='" + pkg + '\'' +
				", targetTypeId=" + targetTypeId +
				", color='" + color + '\'' +
				", location='" + location + '\'' +
				", nameFr='" + nameFr + '\'' +
				", colorInt=" + colorInt +
				", uuid='" + uuid + '\'' +
				'}';
	}

	@ItemViewType
	@Override
	public int getType() {
		return POI.ITEM_VIEW_TYPE_MODULE;
	}

	@ItemStatusType
	@Override
	public int getStatusType() {
		return POI.ITEM_STATUS_TYPE_APP;
	}

	@ItemActionType
	@Override
	public int getActionsType() {
		return POI.ITEM_ACTION_TYPE_APP;
	}

	@Override
	public boolean hasLocation() {
		return true; // required for distance sort
	}

	private String uuid = null;

	@NonNull
	@Override
	public String getUUID() {
		if (this.uuid == null) {
			this.uuid = POI.POIUtils.getUUID(getAuthority(), this.pkg);
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

	@Nullable
	@Override
	public JSONObject toJSON() {
		try {
			JSONObject json = new JSONObject();
			json.put(JSON_PKG, this.pkg);
			json.put(JSON_TARGET_TYPE_ID, this.targetTypeId);
			if (!TextUtils.isEmpty(this.color)) {
				json.put(JSON_COLOR, this.color);
			}
			if (!TextUtils.isEmpty(this.location)) {
				json.put(JSON_LOCATION, this.location);
			}
			if (!TextUtils.isEmpty(this.nameFr)) {
				json.put(JSON_NAME_FR, this.nameFr);
			}
			DefaultPOI.toJSON(this, json);
			return json;
		} catch (JSONException jsone) {
			MTLog.w(this, jsone, "Error while converting to JSON (%s)!", this);
			return null;
		}
	}

	@Nullable
	@Override
	public POI fromJSON(@NonNull JSONObject json) {
		return fromJSONStatic(json);
	}

	@Nullable
	public static Module fromJSONStatic(@NonNull JSONObject json) {
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
			MTLog.w(LOG_TAG, jsone, "Error while parsing JSON '%s'!", json);
			return null;
		}
	}

	@Nullable
	public static Module fromSimpleJSONStatic(@NonNull JSONObject json, @NonNull String authority) {
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
			MTLog.w(LOG_TAG, jsone, "Error while parsing simple JSON '%s'!", json);
			return null;
		}
	}

	@NonNull
	@Override
	public ContentValues toContentValues() {
		ContentValues values = super.toContentValues();
		values.put(ModuleProvider.ModuleColumns.T_MODULE_K_PKG, this.pkg);
		values.put(ModuleProvider.ModuleColumns.T_MODULE_K_TARGET_TYPE_ID, this.targetTypeId);
		values.put(ModuleProvider.ModuleColumns.T_MODULE_K_COLOR, this.color);
		values.put(ModuleProvider.ModuleColumns.T_MODULE_K_LOCATION, this.location);
		values.put(ModuleProvider.ModuleColumns.T_MODULE_K_NAME_FR, this.nameFr);
		return values;
	}

	@NonNull
	@Override
	public POI fromCursor(@NonNull Cursor c, @NonNull String authority) {
		return fromCursorStatic(c, authority);
	}

	@NonNull
	public static Module fromCursorStatic(@NonNull Cursor c, @NonNull String authority) {
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
