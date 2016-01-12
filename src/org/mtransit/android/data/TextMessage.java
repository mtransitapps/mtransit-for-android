package org.mtransit.android.data;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.data.DefaultPOI;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.provider.POIProviderContract;

import android.content.ContentValues;
import android.database.Cursor;

public class TextMessage extends DefaultPOI {

	private static final String TAG = TextMessage.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static final String AUTHORITY = "org.mtransit.android.message";

	private long messageId;
	private String message;

	public TextMessage(long messageId, String message) {
		super(AUTHORITY, -1, POI.ITEM_VIEW_TYPE_TEXT_MESSAGE, POI.ITEM_STATUS_TYPE_NONE, POI.ITEM_ACTION_TYPE_NONE);
		setMessageId(messageId);
		setMessage(message);
	}

	public void setMessageId(long messageId) {
		this.messageId = messageId;
	}

	public long getMessageId() {
		return messageId;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getMessage() {
		return this.message;
	}

	@Override
	public String getName() {
		return getMessage();
	}

	@Override
	public String toString() {
		return new StringBuilder().append(TextMessage.class.getSimpleName()).append(":[") //
				.append("authority:").append(getAuthority()).append(',') //
				.append("messageId:").append(getMessageId()).append(',') //
				.append("message:").append(getMessage()).append(',') //
				.append("id:").append(getId()).append(',') //
				.append(']').toString();
	}

	@Override
	public int getType() {
		return POI.ITEM_VIEW_TYPE_TEXT_MESSAGE;
	}

	@Override
	public int getStatusType() {
		return POI.ITEM_STATUS_TYPE_NONE;
	}

	@Override
	public int getActionsType() {
		return POI.ITEM_ACTION_TYPE_NONE;
	}

	@Override
	public boolean hasLocation() {
		return false;
	}

	private String uuid = null;

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

	@Override
	public POI fromJSON(JSONObject json) {
		return fromJSONStatic(json);
	}

	public static TextMessage fromJSONStatic(JSONObject json) {
		try {
			TextMessage textMessage = new TextMessage( //
					json.getLong(JSON_MESSAGE_ID), //
					json.getString(JSON_MESSAGE) //
			);
			DefaultPOI.fromJSON(json, textMessage);
			return textMessage;
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while parsing JSON '%s'!", json);
			return null;
		}
	}

	public static TextMessage fromSimpleJSONStatic(JSONObject json, String authority) {
		try {
			TextMessage textMessage = new TextMessage( //
					json.getLong(JSON_MESSAGE_ID), //
					json.getString(JSON_MESSAGE) //
			);
			return textMessage;
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while parsing simple JSON '%s'!", json);
			return null;
		}
	}

	@Override
	public ContentValues toContentValues() {
		ContentValues values = super.toContentValues();
		values.put(TextMessageColumns.T_TEXT_MESSAGE_K_MESSAGE, getMessage());
		return values;
	}

	@Override
	public POI fromCursor(Cursor c, String authority) {
		return fromCursorStatic(c, authority);
	}

	public static TextMessage fromCursorStatic(Cursor c, String authority) {
		long messageId = c.getLong(c.getColumnIndexOrThrow(TextMessageColumns.T_TEXT_MESSAGE_K_MESSAGE_ID));
		String message = c.getString(c.getColumnIndexOrThrow(TextMessageColumns.T_TEXT_MESSAGE_K_MESSAGE));
		TextMessage textMessage = new TextMessage(messageId, message);
		DefaultPOI.fromCursor(c, textMessage);
		return textMessage;
	}

	private static class TextMessageColumns {
		public static final String T_TEXT_MESSAGE_K_MESSAGE_ID = POIProviderContract.Columns.getFkColumnName("messageId");
		public static final String T_TEXT_MESSAGE_K_MESSAGE = POIProviderContract.Columns.getFkColumnName("message");
	}
}
