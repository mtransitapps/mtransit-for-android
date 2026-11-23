package org.mtransit.android.data;

import android.content.ContentValues;
import android.database.Cursor;

import androidx.annotation.ColorInt;
import androidx.annotation.Discouraged;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.BuildConfig;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.HtmlUtils;
import org.mtransit.android.commons.LocaleUtils;
import org.mtransit.android.commons.data.Accessibility;
import org.mtransit.android.commons.data.DataSourceTypeId;
import org.mtransit.android.commons.data.DefaultPOI;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.provider.ModuleProvider;

import kotlinx.serialization.json.JsonObject;

public class Module extends DefaultPOI {

	protected static final String LOG_TAG = Module.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG + "-" + this.pkg;
	}

	@NonNull
	private final String pkg;

	@DataSourceTypeId.DataSourceType
	private final int dstId;

	@Nullable
	private String color = null;

	@Nullable
	protected String location = null;

	@Nullable
	private String nameFr = null;

	@Nullable
	private String repo = null;

	@Nullable
	private String store = null;

	/**
	 * @param id useful to store in DB
	 */
	public Module(
			@NonNull String authority,
			int id,
			@NonNull String pkg,
			@DataSourceTypeId.DataSourceType int dstId,
			double lat,
			double lng,
			@NonNull String name
	) {
		super(authority, id, DataSourceTypeId.MODULE, POI.ITEM_VIEW_TYPE_MODULE, POI.ITEM_STATUS_TYPE_APP, POI.ITEM_ACTION_TYPE_APP, lat, lng, name);
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

	protected void setColor(@Nullable String color) {
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

	protected void setNameFr(@Nullable String nameFr) {
		this.nameFr = nameFr;
	}

	@SuppressWarnings("unused")
	@Nullable
	public String getRepo() {
		return repo;
	}

	protected void setRepo(@Nullable String repo) {
		this.repo = repo;
	}

	protected static final String STORE_PRODUCTION = "production";
	@SuppressWarnings("unused")
	protected static final String STORE_BETA_PRIVATE = "beta-private";

	@Nullable
	public String getStore() {
		return store;
	}

	protected void setStore(@Nullable String store) {
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

	@NonNull
	@Override
	public JsonObject toJson() {
		return ModuleExtKt.toJsonK(this);
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
		final Module module = new Module(
				authority,
				DefaultPOI.getIdFromCursor(c),
				c.getString(c.getColumnIndexOrThrow(ModuleProvider.ModuleColumns.T_MODULE_K_PKG)),
				c.getInt(c.getColumnIndexOrThrow(ModuleProvider.ModuleColumns.T_MODULE_K_TARGET_TYPE_ID)),
				DefaultPOI.getLatFromCursor(c),
				DefaultPOI.getLngFromCursor(c),
				DefaultPOI.getNameFromCursor(c)
		);
		module.setColor(c.getString(c.getColumnIndexOrThrow(ModuleProvider.ModuleColumns.T_MODULE_K_COLOR)));
		module.setLocation(c.getString(c.getColumnIndexOrThrow(ModuleProvider.ModuleColumns.T_MODULE_K_LOCATION)));
		module.setNameFr(c.getString(c.getColumnIndexOrThrow(ModuleProvider.ModuleColumns.T_MODULE_K_NAME_FR)));
		module.setRepo(c.getString(c.getColumnIndexOrThrow(ModuleProvider.ModuleColumns.T_MODULE_K_REPO)));
		module.setStore(c.getString(c.getColumnIndexOrThrow(ModuleProvider.ModuleColumns.T_MODULE_K_STORE)));
		DefaultPOI.fromCursor(c, module);
		return module;
	}
}
