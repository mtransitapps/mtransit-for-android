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
import org.mtransit.android.commons.provider.POIProviderContract;

@SuppressWarnings("WeakerAccess")
public class TextMessage extends DefaultPOI {

	private static final String LOG_TAG = TextMessage.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public static final String AUTHORITY = "org.mtransit.android.message";

	private final long messageId;

	public TextMessage(long messageId, @DataSourceTypeId.DataSourceType int dataSourceTypeId, @NonNull String message) {
		super(AUTHORITY, -1, dataSourceTypeId, POI.ITEM_VIEW_TYPE_TEXT_MESSAGE, POI.ITEM_STATUS_TYPE_NONE, POI.ITEM_ACTION_TYPE_NONE);
		this.messageId = messageId;
		setName(message);
	}

	/**
	 * Only useful when POI needs to be stored in DB like Modules (from JSON)
	 * use {@link #getMessageId()} instead
	 */
	@Override
	public int getId() {
		return super.getId();
	}

	public long getMessageId() {
		return messageId;
	}

	@NonNull
	public String getMessage() {
		return getName();
	}

	@NonNull
	@Override
	public String toString() {
		return TextMessage.class.getSimpleName() + ":[" + //
				"authority:" + getAuthority() + ',' + //
				"messageId:" + getMessageId() + ',' + //
				"message:" + getMessage() + ',' + //
				']';
	}

	@Override
	public boolean hasLocation() {
		return false;
	}

	@Nullable
	private String uuid = null;

	@NonNull
	@Override
	public String getUUID() {
		if (this.uuid == null) {
			this.uuid = POI.POIUtils.getUUID(getAuthority(), getMessageId());
		}
		return this.uuid;
	}

	@Override
	public void resetUUID() {
		this.uuid = null;
	}

	private static final String JSON_MESSAGE = "message";
	private static final String JSON_MESSAGE_ID = "messageId";

	@Nullable
	@Override
	public JSONObject toJSON() {
		try {
			JSONObject json = new JSONObject();
			json.put(JSON_MESSAGE_ID, getMessageId());
			json.put(JSON_MESSAGE, getMessage());
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
	public static TextMessage fromJSONStatic(@NonNull JSONObject json) {
		try {
			final TextMessage textMessage = new TextMessage(
					json.getLong(JSON_MESSAGE_ID),
					DefaultPOI.getDSTypeIdFromJSON(json),
					json.getString(JSON_MESSAGE)
			);
			DefaultPOI.fromJSON(json, textMessage);
			return textMessage;
		} catch (JSONException jsone) {
			MTLog.w(LOG_TAG, jsone, "Error while parsing JSON '%s'!", json);
			return null;
		}
	}

	@SuppressWarnings("unused")
	@Nullable
	public static TextMessage fromSimpleJSONStatic(@NonNull JSONObject json, @NonNull String authority) {
		try {
			return new TextMessage( //
					json.getLong(JSON_MESSAGE_ID),
					DefaultPOI.getDSTypeIdFromJSON(json),
					json.getString(JSON_MESSAGE)
			);
		} catch (JSONException jsone) {
			MTLog.w(LOG_TAG, jsone, "Error while parsing simple JSON '%s'!", json);
			return null;
		}
	}

	@NonNull
	@Override
	public ContentValues toContentValues() {
		ContentValues values = super.toContentValues();
		values.put(TextMessageColumns.T_TEXT_MESSAGE_K_MESSAGE_ID, getMessageId());
		values.put(TextMessageColumns.T_TEXT_MESSAGE_K_MESSAGE, getMessage());
		return values;
	}

	@NonNull
	@Override
	public POI fromCursor(@NonNull Cursor c, @NonNull String authority) {
		return fromCursorStatic(c, authority);
	}

	@NonNull
	public static TextMessage fromCursorStatic(@NonNull Cursor c,
											   @SuppressWarnings("unused") @NonNull String authority) {
		final long messageId = c.getLong(c.getColumnIndexOrThrow(TextMessageColumns.T_TEXT_MESSAGE_K_MESSAGE_ID));
		final String message = c.getString(c.getColumnIndexOrThrow(TextMessageColumns.T_TEXT_MESSAGE_K_MESSAGE));
		final int dataSourceTypeId = DefaultPOI.getDataSourceTypeIdFromCursor(c);
		final TextMessage textMessage = new TextMessage(messageId, dataSourceTypeId, message);
		DefaultPOI.fromCursor(c, textMessage);
		return textMessage;
	}

	private static class TextMessageColumns {
		public static final String T_TEXT_MESSAGE_K_MESSAGE_ID = POIProviderContract.Columns.getFkColumnName("messageId");
		public static final String T_TEXT_MESSAGE_K_MESSAGE = POIProviderContract.Columns.getFkColumnName("message");
	}
}
