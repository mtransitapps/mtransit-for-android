package org.mtransit.android.data;

import android.content.ContentValues;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.data.DefaultPOI;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.provider.POIProviderContract;

@SuppressWarnings("WeakerAccess")
public class TextMessage extends DefaultPOI {

	private static final String TAG = TextMessage.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG;
	}

	public static final String AUTHORITY = "org.mtransit.android.message";

	private long messageId;
	@SuppressWarnings("NotNullFieldNotInitialized")
	@NonNull
	private String message;

	public TextMessage(long messageId, @NonNull String message) {
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

	public void setMessage(@NonNull String message) {
		this.message = message;
	}

	@NonNull
	public String getMessage() {
		return this.message;
	}

	@NonNull
	@Override
	public String getName() {
		return getMessage();
	}

	@NonNull
	@Override
	public String toString() {
		return TextMessage.class.getSimpleName() + ":[" + //
				"authority:" + getAuthority() + ',' + //
				"messageId:" + getMessageId() + ',' + //
				"message:" + getMessage() + ',' + //
				"id:" + getId() + ',' + //
				']';
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

	@SuppressWarnings("unused")
	@Nullable
	public static TextMessage fromSimpleJSONStatic(@NonNull JSONObject json, @NonNull String authority) {
		try {
			return new TextMessage( //
					json.getLong(JSON_MESSAGE_ID), //
					json.getString(JSON_MESSAGE) //
			);
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while parsing simple JSON '%s'!", json);
			return null;
		}
	}

	@NonNull
	@Override
	public ContentValues toContentValues() {
		ContentValues values = super.toContentValues();
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
