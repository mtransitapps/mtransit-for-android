package org.mtransit.android.data;

import android.content.ContentValues;
import android.database.Cursor;

import androidx.annotation.ColorInt;
import androidx.annotation.Discouraged;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.CursorExtKt;
import org.mtransit.android.commons.data.DataSourceTypeId;
import org.mtransit.android.commons.data.DefaultPOI;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.provider.PlaceProvider;

import kotlinx.serialization.json.JsonObject;

public class Place extends DefaultPOI {

	protected static final String LOG_TAG = Place.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG + "-" + this.providerId;
	}

	@NonNull
	private final String providerId;

	@NonNull
	private final String lang;

	private final long readAtInMs;

	@Nullable
	private String subTitle = null;

	@Nullable
	private String iconUrl = null;
	@Nullable
	@ColorInt
	private Integer iconBgColorInt = null;

	public Place(
			@NonNull String authority,
			@NonNull String providerId,
			@NonNull String lang,
			long readAtInMs,
			double lat,
			double lng,
			@NonNull String name
	) {
		super(authority, -1, DataSourceTypeId.PLACE, POI.ITEM_VIEW_TYPE_PLACE, POI.ITEM_STATUS_TYPE_NONE, POI.ITEM_ACTION_TYPE_PLACE, lat, lng, name);
		this.providerId = providerId;
		this.lang = lang;
		this.readAtInMs = readAtInMs;
		resetUUID();
	}

	@Discouraged(message = "only useful for DB, use getProviderId() instead")
	@Override
	public int getId() {
		return super.getId();
	}

	@NonNull
	public String getProviderId() {
		return providerId;
	}

	@NonNull
	public String getLang() {
		return lang;
	}

	public long getReadAtInMs() {
		return readAtInMs;
	}

	public void setSubTitle(@Nullable String subTitle) {
		this.subTitle = subTitle;
	}

	@Nullable
	public String getSubTitle() {
		return subTitle;
	}

	public void setIconUrl(@Nullable String iconUrl) {
		this.iconUrl = iconUrl;
	}

	@Nullable
	public String getIconUrl() {
		return iconUrl;
	}

	public void setIconBgColor(@Nullable @ColorInt Integer iconBgColorInt) {
		this.iconBgColorInt = iconBgColorInt;
	}

	@ColorInt
	@Nullable
	public Integer getIconBgColor() {
		return iconBgColorInt;
	}

	@NonNull
	@Override
	public String toString() {
		return Place.class.getSimpleName() + ":[" +
				"authority:" + getAuthority() + ',' +
				"providerId:" + getProviderId() + ',' +
				"name:" + getName() + ',' +
				"subTitle: " + getSubTitle() + "," +
				"lat:" + getLat() + ',' +
				"lng:" + getLng() + ',' +
				"icon: " + getIconUrl() + " (" + getIconBgColor() + ")" + ',' +
				']';
	}

	@Override
	public boolean hasLocation() {
		return true; // required for distance sort
	}

	@Nullable
	private String uuid = null;

	@NonNull
	@Override
	public String getUUID() {
		if (this.uuid == null) {
			this.uuid = POI.POIUtils.makeUUID(getAuthority(), getProviderId());
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
		return PlaceExtKt.toJsonK(this);
	}

	/**
	 * {@link org.mtransit.android.provider.PlaceProvider.Companion#getPROJECTION_PLACE_POI()} =
	 * - {@link org.mtransit.android.commons.provider.poi.POIProviderContract.Companion#getPROJECTION_POI()}
	 * - {@link org.mtransit.android.provider.PlaceProvider.Companion#getPROJECTION_PLACE()}
	 */
	@NonNull
	public Object[] getCursorRow() {
		//noinspection DiscouragedApi
		final int id = getId();
		return new Object[]{
				// PROJECTION_POI:
				getUUID(),
				getDataSourceTypeId(),
				id,
				getName(),
				getLat(),
				getLng(),
				getAccessible(),
				getType(),
				getStatusType(),
				getActionsType(),
				// PROJECTION_PLACE:
				getScore(),
				getProviderId(),
				getLang(),
				getReadAtInMs(),
				getSubTitle(),
				getIconUrl(),
				getIconBgColor()
		};
	}

	@NonNull
	@Override
	public ContentValues toContentValues() {
		final ContentValues values = super.toContentValues();
		values.put(PlaceProvider.PlaceColumns.T_PLACE_K_PROVIDER_ID, getProviderId());
		values.put(PlaceProvider.PlaceColumns.T_PLACE_K_LANG, getLang());
		values.put(PlaceProvider.PlaceColumns.T_PLACE_K_READ_AT_IN_MS, getReadAtInMs());
		if (getSubTitle() != null) {
			values.put(PlaceProvider.PlaceColumns.T_PLACE_K_SUB_TITLE, getSubTitle());
		}
		if (getIconUrl() != null) {
			values.put(PlaceProvider.PlaceColumns.T_PLACE_K_ICON_URL, getIconUrl());
		}
		if (getIconBgColor() != null) {
			values.put(PlaceProvider.PlaceColumns.T_PLACE_K_ICON_BG_COLOR, getIconBgColor());
		}
		return values;
	}

	@NonNull
	@Override
	public POI fromCursor(@NonNull Cursor c, @NonNull String authority) {
		return fromCursorStatic(c, authority);
	}

	@NonNull
	public static Place fromCursorStatic(@NonNull Cursor c, @NonNull String authority) {
		final Place place = new Place(
				authority,
				c.getString(c.getColumnIndexOrThrow(PlaceProvider.PlaceColumns.T_PLACE_K_PROVIDER_ID)),
				c.getString(c.getColumnIndexOrThrow(PlaceProvider.PlaceColumns.T_PLACE_K_LANG)),
				c.getLong(c.getColumnIndexOrThrow(PlaceProvider.PlaceColumns.T_PLACE_K_READ_AT_IN_MS)),
				DefaultPOI.getLatFromCursor(c),
				DefaultPOI.getLngFromCursor(c),
				DefaultPOI.getNameFromCursor(c)
		);
		final String subTitle = CursorExtKt.optString(c, PlaceProvider.PlaceColumns.T_PLACE_K_SUB_TITLE, null);
		if (subTitle != null) {
			place.setSubTitle(subTitle);
		}
		final Integer iconColor = CursorExtKt.optInt(c, PlaceProvider.PlaceColumns.T_PLACE_K_ICON_BG_COLOR, null);
		if (iconColor != null) {
			place.setIconBgColor(iconColor);
		}
		final String iconUrl = CursorExtKt.optString(c, PlaceProvider.PlaceColumns.T_PLACE_K_ICON_URL, null);
		if (iconUrl != null) {
			place.setIconUrl(iconUrl);
		}
		DefaultPOI.fromCursor(c, place);
		return place;
	}
}
