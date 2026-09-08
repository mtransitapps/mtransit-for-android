package org.mtransit.android.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import androidx.annotation.ColorInt;
import androidx.annotation.Discouraged;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.BuildConfig;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.HtmlUtils;
import org.mtransit.android.commons.LocaleUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.data.Accessibility;
import org.mtransit.android.commons.data.DataSourceTypeId;
import org.mtransit.android.commons.data.DefaultPOI;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.provider.ModuleProvider;

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
	private final int dstId;

	@Nullable
	private String color = null;

	@Nullable
	private String location = null;

	@Nullable
	private String nameFr = null;

	@Nullable
	private String repo = null;

	@Nullable
	private String store = null;

	/**
	 * @param id useful to store in DB
	 */
	public Module(@NonNull String authority, int id, @NonNull String pkg, @DataSourceTypeId.DataSourceType int dstId) {
		super(authority, id, DataSourceTypeId.MODULE, POI.ITEM_VIEW_TYPE_MODULE, POI.ITEM_STATUS_TYPE_APP, POI.ITEM_ACTION_TYPE_APP);
		this.pkg = pkg;
		resetUUID();
		this.dstId = dstId;
	}

	@Discouraged(message = "only useful for DB, use getPkg() instead")
	@Override
	public int getId() {
		return super.getId();
	}

	@NonNull
	public String getPkg() {
		if (BuildConfig.DEBUG) {
			return pkg + ".debug";
		}
		return pkg;
	}

	@NonNull
	String getStorePkg() {
		return pkg; // no "debug" pkg on Store
	}

	@DataSourceTypeId.DataSourceType
	public int getDstId() {
		return dstId;
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

	@SuppressWarnings("WeakerAccess")
	@Nullable
	public String getLocationString() {
		return this.location;
	}

	private void setLocation(@Nullable String location) {
		this.location = location;
	}

	@SuppressWarnings("WeakerAccess")
	@Nullable
	public String getNameFr() {
		return nameFr;
	}

	private void setNameFr(@Nullable String nameFr) {
		this.nameFr = nameFr;
	}

	@SuppressWarnings("unused")
	@Nullable
	public String getRepo() {
		return repo;
	}

	private void setRepo(@Nullable String repo) {
		this.repo = repo;
	}

	private static final String STORE_PRODUCTION = "production";
	@SuppressWarnings("unused")
	private static final String STORE_BETA_PRIVATE = "beta-private";

	@Nullable
	public String getStore() {
		return store;
	}

	private void setStore(@Nullable String store) {
		this.store = store;
	}

	@NonNull
	@Override
	public String getName() {
		return LocaleUtils.isFR() && getNameFr() != null ? getNameFr() : super.getName();
	}

	@NonNull
	@Override
	public CharSequence getLabel() {
		return HtmlUtils.fromHtmlCompact(
				Accessibility.decorate(getName(), getAccessible(), false)
		);
	}

	@NonNull
	@Override
	public String toString() {
		return Module.class.getSimpleName() + "{" +
				"pkg='" + pkg + '\'' +
				", dstId=" + dstId +
				", color='" + color + '\'' +
				", location='" + location + '\'' +
				", nameFr='" + nameFr + '\'' +
				", repo='" + repo + '\'' +
				", store='" + store + '\'' +
				", colorInt=" + colorInt +
				", uuid='" + uuid + '\'' +
				'}';
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
			this.uuid = POI.POIUtils.makeUUID(getAuthority(), this.pkg);
		}
		return this.uuid;
	}

	@Override
	public void resetUUID() {
		this.uuid = null;
	}

	private static final String JSON_PKG = "pkg";
	private static final String JSON_TYPE_ID = "type_id";
	private static final String JSON_COLOR = "color";
	private static final String JSON_LOCATION = "location";
	private static final String JSON_NAME_FR = "name_fr";
	private static final String JSON_REPO = "repo";
	private static final String JSON_STORE = "store";

	@Nullable
	@Override
	public JSONObject toJSON() {
		try {
			final JSONObject json = new JSONObject();
			json.put(JSON_PKG, this.pkg);
			json.put(JSON_TYPE_ID, this.dstId);
			if (!TextUtils.isEmpty(this.color)) {
				json.put(JSON_COLOR, this.color);
			}
			if (!TextUtils.isEmpty(this.location)) {
				json.put(JSON_LOCATION, this.location);
			}
			if (!TextUtils.isEmpty(this.nameFr)) {
				json.put(JSON_NAME_FR, this.nameFr);
			}
			if (!TextUtils.isEmpty(this.repo)) {
				json.put(JSON_REPO, this.repo);
			}
			if (!TextUtils.isEmpty(this.store)) {
				json.put(JSON_STORE, this.store);
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
			final String optStore = json.optString(JSON_STORE);
			if (!BuildConfig.DEBUG && !STORE_PRODUCTION.equals(optStore)) return null; // ignore non-production agency modules in release builds
			final Module module = new Module(
					DefaultPOI.getAuthorityFromJSON(json),
					DefaultPOI.getIdFromJSON(json),
					json.getString(JSON_PKG),
					json.getInt(JSON_TYPE_ID)
			);
			final String optColor = json.optString(JSON_COLOR);
			if (!TextUtils.isEmpty(optColor)) {
				module.setColor(optColor);
			}
			final String optLocation = json.optString(JSON_LOCATION);
			if (!TextUtils.isEmpty(optLocation)) {
				module.setLocation(optLocation);
			}
			final String optNameFr = json.optString(JSON_NAME_FR);
			if (!TextUtils.isEmpty(optNameFr)) {
				module.setNameFr(optNameFr);
			}
			final String optRepo = json.optString(JSON_REPO);
			if (!TextUtils.isEmpty(optRepo)) {
				module.setRepo(optRepo);
			}
			if (!TextUtils.isEmpty(optStore)) {
				module.setStore(optStore);
			}
			DefaultPOI.fromJSON(json, module);
			return module;
		} catch (JSONException jsone) {
			MTLog.w(LOG_TAG, jsone, "Error while parsing JSON '%s'!", json);
			return null;
		}
	}

	@Nullable
	public static Module fromSimpleJSONStatic(@NonNull JSONObject json, @NonNull String authority, int id) {
		try {
			final String optStore = json.optString(JSON_STORE);
			if (!BuildConfig.DEBUG && !STORE_PRODUCTION.equals(optStore)) return null; // ignore non-production agency modules in release builds
			final Module module = new Module(
					authority,
					id,
					json.getString(JSON_PKG),
					Integer.parseInt(json.getString(JSON_TYPE_ID))
			);
			module.setName(json.getString(JSON_NAME));
			module.setLat(json.getDouble(JSON_LAT));
			module.setLng(json.getDouble(JSON_LNG));
			final String optColor = json.optString(JSON_COLOR);
			if (!TextUtils.isEmpty(optColor)) {
				module.setColor(optColor);
			}
			final String optLocation = json.optString(JSON_LOCATION);
			if (!TextUtils.isEmpty(optLocation)) {
				module.setLocation(optLocation);
			}
			final String optNameFr = json.optString(JSON_NAME_FR);
			if (!TextUtils.isEmpty(optNameFr)) {
				module.setNameFr(optNameFr);
			}
			final String optRepo = json.optString(JSON_REPO);
			if (!TextUtils.isEmpty(optRepo)) {
				module.setRepo(optRepo);
			}
			if (!TextUtils.isEmpty(optStore)) {
				module.setStore(optStore);
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
		final ContentValues values = super.toContentValues();
		values.put(ModuleProvider.ModuleColumns.T_MODULE_K_PKG, this.pkg);
		values.put(ModuleProvider.ModuleColumns.T_MODULE_K_TARGET_TYPE_ID, this.dstId);
		values.put(ModuleProvider.ModuleColumns.T_MODULE_K_COLOR, this.color);
		values.put(ModuleProvider.ModuleColumns.T_MODULE_K_LOCATION, this.location);
		values.put(ModuleProvider.ModuleColumns.T_MODULE_K_NAME_FR, this.nameFr);
		values.put(ModuleProvider.ModuleColumns.T_MODULE_K_REPO, this.repo);
		values.put(ModuleProvider.ModuleColumns.T_MODULE_K_STORE, this.store);
		return values;
	}

	@NonNull
	@Override
	public POI fromCursor(@NonNull Cursor c, @NonNull String authority) {
		return fromCursorStatic(c, authority);
	}

	@NonNull
	public static Module fromCursorStatic(@NonNull Cursor c, @NonNull String authority) {
		final String pkg = c.getString(c.getColumnIndexOrThrow(ModuleProvider.ModuleColumns.T_MODULE_K_PKG));
		final int targetTypeId = c.getInt(c.getColumnIndexOrThrow(ModuleProvider.ModuleColumns.T_MODULE_K_TARGET_TYPE_ID));
		final int id = DefaultPOI.getIdFromCursor(c);
		final Module module = new Module(authority, id, pkg, targetTypeId);
		module.setColor(c.getString(c.getColumnIndexOrThrow(ModuleProvider.ModuleColumns.T_MODULE_K_COLOR)));
		module.setLocation(c.getString(c.getColumnIndexOrThrow(ModuleProvider.ModuleColumns.T_MODULE_K_LOCATION)));
		module.setNameFr(c.getString(c.getColumnIndexOrThrow(ModuleProvider.ModuleColumns.T_MODULE_K_NAME_FR)));
		module.setRepo(c.getString(c.getColumnIndexOrThrow(ModuleProvider.ModuleColumns.T_MODULE_K_REPO)));
		module.setStore(c.getString(c.getColumnIndexOrThrow(ModuleProvider.ModuleColumns.T_MODULE_K_STORE)));
		DefaultPOI.fromCursor(c, module);
		return module;
	}
}
