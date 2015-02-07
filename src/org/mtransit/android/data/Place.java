package org.mtransit.android.data;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.data.DefaultPOI;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.provider.PlaceProvider;

import android.content.ContentValues;
import android.database.Cursor;

public class Place extends DefaultPOI {

	private static final String TAG = Place.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static final int DST_ID = DataSourceType.TYPE_PLACE.getId();

	private String providerId;

	private String lang;

	private long readAtInMs = -1l;

	public Place(String authority, String providerId, String lang, long readAtInMs) {
		super(authority, DST_ID, POI.ITEM_VIEW_TYPE_BASIC_POI, -1, POI.ITEM_ACTION_TYPE_PLACE);
		this.providerId = providerId;
		this.lang = lang;
		this.readAtInMs = readAtInMs;
	}

	public String getProviderId() {
		return providerId;
	}

	public String getLang() {
		return lang;
	}

	public long getReadAtInMs() {
		return readAtInMs;
	}

	@Override
	public String toString() {
		return new StringBuilder().append(Place.class.getSimpleName()).append(":[") //
				.append("authority:").append(getAuthority()).append(',') //
				.append("providerId:").append(this.providerId).append(',') //
				.append("id:").append(getId()).append(',') //
				.append("name:").append(getName()).append(',') //
				.append(']').toString();
	}

	@Override
	public int getType() {
		return POI.ITEM_VIEW_TYPE_BASIC_POI;
	}

	@Override
	public int getStatusType() {
		return -1;
	}

	@Override
	public int getActionsType() {
		return POI.ITEM_ACTION_TYPE_PLACE;
	}

	@Override
	public boolean hasLocation() {
		return true; // required for distance sort
	}

	@Override
	public String getUUID() {
		return POI.POIUtils.getUUID(getAuthority(), this.providerId);
	}

	private static final String JSON_PROVIDER_ID = "provider_id";
	private static final String JSON_LANG = "lang";
	private static final String JSON_READ_AT_IN_MS = "read_at_in_ms";

	@Override
	public JSONObject toJSON() {
		try {
			JSONObject json = new JSONObject();
			json.put(JSON_PROVIDER_ID, this.providerId);
			json.put(JSON_LANG, this.lang);
			json.put(JSON_READ_AT_IN_MS, this.readAtInMs);
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

	public static Place fromJSONStatic(JSONObject json) {
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
			MTLog.w(TAG, jsone, "Error while parsing JSON '%s'!", json);
			return null;
		}
	}

	@Override
	public ContentValues toContentValues() {
		ContentValues values = super.toContentValues();
		values.put(PlaceProvider.PlaceColumns.T_PLACE_K_PROVIDER_ID, this.providerId);
		values.put(PlaceProvider.PlaceColumns.T_PLACE_K_LANG, this.lang);
		values.put(PlaceProvider.PlaceColumns.T_PLACE_K_READ_AT_IN_MS, this.readAtInMs);
		return values;
	}

	@Override
	public POI fromCursor(Cursor c, String authority) {
		return fromCursorStatic(c, authority);
	}

	public static Place fromCursorStatic(Cursor c, String authority) {
		String providerId = c.getString(c.getColumnIndexOrThrow(PlaceProvider.PlaceColumns.T_PLACE_K_PROVIDER_ID));
		String lang = c.getString(c.getColumnIndexOrThrow(PlaceProvider.PlaceColumns.T_PLACE_K_LANG));
		long readAtInMs = c.getLong(c.getColumnIndexOrThrow(PlaceProvider.PlaceColumns.T_PLACE_K_READ_AT_IN_MS));
		Place place = new Place(authority, providerId, lang, readAtInMs);
		DefaultPOI.fromCursor(c, place);
		return place;
	}

}
