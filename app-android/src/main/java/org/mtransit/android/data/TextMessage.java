package org.mtransit.android.data;

import static org.mtransit.android.data.TextMessageExtKt.makeTextMessage;

import android.content.ContentValues;
import android.database.Cursor;

import androidx.annotation.Discouraged;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.data.DataSourceTypeId;
import org.mtransit.android.commons.data.DefaultPOI;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.provider.poi.POIProviderContract;

import kotlinx.serialization.json.JsonObject;

@SuppressWarnings("WeakerAccess")
public class TextMessage extends DefaultPOI {

	protected static final String LOG_TAG = TextMessage.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG + "-" + this.messageId;
	}

	public static final String AUTHORITY = "org.mtransit.android.message";

	private final long messageId;

	protected TextMessage(long messageId, @DataSourceTypeId.DataSourceType int dataSourceTypeId, @NonNull String message) {
		super(AUTHORITY, -1, dataSourceTypeId, POI.ITEM_VIEW_TYPE_TEXT_MESSAGE, POI.ITEM_STATUS_TYPE_NONE, POI.ITEM_ACTION_TYPE_NONE, 0.0d, 0.0d, message);
		this.messageId = messageId;
	}

	@Discouraged(message = "only useful for DB, use getMessageId() instead")
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
			this.uuid = POI.POIUtils.makeUUID(getAuthority(), getMessageId());
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
		return TextMessageExtKt.toJsonK(this);
	}

	@NonNull
	@Override
	public ContentValues toContentValues() {
		final ContentValues values = super.toContentValues();
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
		return makeTextMessage(
				c.getLong(c.getColumnIndexOrThrow(TextMessageColumns.T_TEXT_MESSAGE_K_MESSAGE_ID)),
				DefaultPOI.getDataSourceTypeIdFromCursor(c),
				c.getString(c.getColumnIndexOrThrow(TextMessageColumns.T_TEXT_MESSAGE_K_MESSAGE)),
				DefaultPOI.getScoreFromCursor(c),
				DefaultPOI.getAccessibleFromCursor(c)
		);
	}

	private static class TextMessageColumns {
		public static final String T_TEXT_MESSAGE_K_MESSAGE_ID = POIProviderContract.Columns.getFkColumnName("messageId");
		public static final String T_TEXT_MESSAGE_K_MESSAGE = POIProviderContract.Columns.getFkColumnName("message");
	}
}
