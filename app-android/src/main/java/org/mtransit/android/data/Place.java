package org.mtransit.android.data;

import android.content.ContentValues;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.data.DataSourceTypeId;
import org.mtransit.android.commons.data.DefaultPOI;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.provider.PlaceProvider;
import org.mtransit.commons.FeatureFlags;

public class Place extends DefaultPOI {

	private static final String LOG_TAG = Place.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	private final String providerId;

	@NonNull
	private final String lang;

	private final long readAtInMs;

	public Place(@NonNull String authority, @NonNull String providerId, @NonNull String lang, long readAtInMs) {
		super(authority, DataSourceTypeId.PLACE, POI.ITEM_VIEW_TYPE_BASIC_POI, POI.ITEM_STATUS_TYPE_NONE, POI.ITEM_ACTION_TYPE_PLACE);
		this.providerId = providerId;
		this.lang = lang;
		this.readAtInMs = readAtInMs;
		resetUUID();
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

	@NonNull
	@Override
	public String toString() {
		return Place.class.getSimpleName() + ":[" + //
				"authority:" + getAuthority() + ',' + //
				"providerId:" + getProviderId() + ',' + //
				"id:" + getId() + ',' + //
				"name:" + getName() + ',' + //
				']';
	}

	@ItemViewType
	@Override
	public int getType() {
		return POI.ITEM_VIEW_TYPE_BASIC_POI;
	}

	@ItemStatusType
	@Override
	public int getStatusType() {
		return POI.ITEM_STATUS_TYPE_NONE;
	}

	@ItemActionType
	@Override
	public int getActionsType() {
		return POI.ITEM_ACTION_TYPE_PLACE;
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
			this.uuid = POI.POIUtils.getUUID(getAuthority(), getProviderId());
		}
		return this.uuid;
	}

	@Override
	public void resetUUID() {
		this.uuid = null;
	}

	private static final String JSON_PROVIDER_ID = "provider_id";
	private static final String JSON_LANG = "lang";
	private static final String JSON_READ_AT_IN_MS = "read_at_in_ms";

	@Nullable
	@Override
	public JSONObject toJSON() {
		try {
			JSONObject json = new JSONObject();
			json.put(JSON_PROVIDER_ID, getProviderId());
			json.put(JSON_LANG, getLang());
			json.put(JSON_READ_AT_IN_MS, getReadAtInMs());
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
	public static Place fromJSONStatic(@NonNull JSONObject json) {
		try {
			Place module = new Place( //
					DefaultPOI.getAuthorityFromJSON(json), //
					json.getString(JSON_PROVIDER_ID), //
					json.getString(JSON_LANG), //
					json.getLong(JSON_READ_AT_IN_MS) //
			);
			DefaultPOI.fromJSON(json, module);
			return module;
		} catch (JSONException jsone) {
			MTLog.w(LOG_TAG, jsone, "Error while parsing JSON '%s'!", json);
			return null;
		}
	}

	@NonNull
	public Object[] getCursorRow() {
		if (FeatureFlags.F_ACCESSIBILITY_PRODUCER) {
			return new Object[]{ //
					getUUID(), //
					getDataSourceTypeId(), //
					getId(),//
					getName(), //
					getLat(),//
					getLng(), //
					getAccessible(), //
					getType(), getStatusType(), getActionsType(), //
					getScore(), //
					getProviderId(), getLang(), getReadAtInMs() //
			};
		}
		return new Object[]{ //
				getUUID(), //
				getDataSourceTypeId(), //
				getId(),//
				getName(), //
				getLat(),//
				getLng(), //
				getType(), getStatusType(), getActionsType(), //
				getScore(), //
				getProviderId(), getLang(), getReadAtInMs() //
		};
	}

	@NonNull
	@Override
	public ContentValues toContentValues() {
		ContentValues values = super.toContentValues();
		values.put(PlaceProvider.PlaceColumns.T_PLACE_K_PROVIDER_ID, getProviderId());
		values.put(PlaceProvider.PlaceColumns.T_PLACE_K_LANG, getLang());
		values.put(PlaceProvider.PlaceColumns.T_PLACE_K_READ_AT_IN_MS, getReadAtInMs());
		return values;
	}

	@NonNull
	@Override
	public POI fromCursor(@NonNull Cursor c, @NonNull String authority) {
		return fromCursorStatic(c, authority);
	}

	@NonNull
	public static Place fromCursorStatic(@NonNull Cursor c, @NonNull String authority) {
		String providerId = c.getString(c.getColumnIndexOrThrow(PlaceProvider.PlaceColumns.T_PLACE_K_PROVIDER_ID));
		String lang = c.getString(c.getColumnIndexOrThrow(PlaceProvider.PlaceColumns.T_PLACE_K_LANG));
		long readAtInMs = c.getLong(c.getColumnIndexOrThrow(PlaceProvider.PlaceColumns.T_PLACE_K_READ_AT_IN_MS));
		Place place = new Place(authority, providerId, lang, readAtInMs);
		DefaultPOI.fromCursor(c, place);
		return place;
	}
}
