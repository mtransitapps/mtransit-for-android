package org.mtransit.android.data;

import android.graphics.Paint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;

import java.util.HashSet;
import java.util.Objects;

@SuppressWarnings({"WeakerAccess", "unused"})
public class JPaths implements MTLog.Loggable {

	private static final String TAG = JPaths.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG;
	}

	@NonNull
	private final String id;

	private final HashSet<JPath> paths = new HashSet<>();

	public JPaths(@NonNull String id) {
		this.id = id;
	}

	@NonNull
	public String getId() {
		return id;
	}

	public void addPath(@NonNull JPath newPath) {
		this.paths.add(newPath);
	}

	@NonNull
	public HashSet<JPath> getPaths() {
		return paths;
	}

	private static final String JSON_ID = "id";
	private static final String JSON_PAINT = "paint";
	private static final String JSON_FORM = "form";
	private static final String JSON_ROTATION = "rotation";
	private static final String JSON_PATHS = "paths";

	@Nullable
	public JSONObject toJSON() {
		try {
			JSONObject json = new JSONObject();
			json.put(JSON_ID, this.id);
			JSONArray jPaths = new JSONArray();
			for (JPath path : this.paths) {
				JSONObject jPath = toJSONPath(path);
				if (jPath != null) {
					jPaths.put(jPath);
				}
			}
			json.put(JSON_PATHS, jPaths);
			return json;
		} catch (JSONException e) {
			MTLog.w(TAG, e, "Error while converting to JSON!");
			return null;
		}
	}

	@Nullable
	private JSONObject toJSONPath(@NonNull JPath path) {
		try {
			JSONObject jPath = new JSONObject();
			jPath.put(JSON_PAINT, path.paint.toJSON());
			jPath.put(JSON_FORM, path.form.toJSON());
			if (path.rotation != null) {
				jPath.put(JSON_ROTATION, path.rotation.toJSON());
			}
			return jPath;
		} catch (JSONException e) {
			MTLog.w(TAG, e, "Error while converting to JSON!");
			return null;
		}
	}

	@Nullable
	public static JPaths fromJSONString(@Nullable String jsonString) {
		try {
			return jsonString == null ? null : fromJSON(new JSONObject(jsonString));
		} catch (JSONException e) {
			MTLog.w(TAG, e, "Error while parsing JSON string '%s'!", jsonString);
			return null;
		}
	}

	@Nullable
	public static JPaths fromJSON(@NonNull JSONObject json) {
		try {
			String id = json.getString(JSON_ID);
			JPaths jPaths = new JPaths(id);
			JSONArray jsonPaths = json.optJSONArray(JSON_PATHS);
			if (jsonPaths != null && jsonPaths.length() > 0) {
				for (int i = 0; i < jsonPaths.length(); i++) {
					JPath newPath = fromJSONPath(jsonPaths.getJSONObject(i));
					if (newPath != null) {
						jPaths.addPath(newPath);
					}
				}
			}
			return jPaths;
		} catch (JSONException e) {
			MTLog.w(TAG, e, "Error while parsing JSON!");
			return null;
		}
	}

	@Nullable
	private static JPath fromJSONPath(@NonNull JSONObject json) {
		try {
			JSONObject jPaint = json.getJSONObject(JSON_PAINT);
			JSONObject jForm = json.getJSONObject(JSON_FORM);
			JSONObject jRotation = json.optJSONObject(JSON_ROTATION);
			return new JPath(
					Objects.requireNonNull(JPaint.fromJSON(jPaint)),
					Objects.requireNonNull(JForm.fromJSON(jForm)),
					JRotation.fromJSON(jRotation)
			);
		} catch (JSONException e) {
			MTLog.w(TAG, e, "Error while parsing JSON!");
			return null;
		}
	}

	public static class JPath {

		@NonNull
		public final JPaint paint;
		@NonNull
		public final JForm form;
		@Nullable
		public final JRotation rotation;

		public JPath(@NonNull JPaint paint, @NonNull JForm form) {
			this(paint, form, null);
		}

		public JPath(@NonNull JPaint paint, @NonNull JForm form, @Nullable JRotation rotation) {
			this.paint = paint;
			this.form = form;
			this.rotation = rotation;
		}

		@NonNull
		@Override
		public String toString() {
			return JPath.class.getSimpleName() + '[' + //
					"paint:" + paint + ',' + //
					"form:" + form + ',' + //
					"rotation:" + rotation + //
					']'; //
		}
	}

	public static class JPaint {

		@NonNull
		public final Paint.Style style;

		public final float strokeWidth;

		public JPaint(@NonNull Paint.Style style) {
			this(style, -1f);
		}

		public JPaint(@NonNull Paint.Style style, float strokeWidth) {
			this.style = style;
			this.strokeWidth = strokeWidth;
		}

		private static final String JSON_STYLE = "style";
		private static final String JSON_STROKE_WIDTH = "strokeWidth";

		@Nullable
		public JSONObject toJSON() {
			try {
				JSONObject json = new JSONObject();
				json.put(JSON_STYLE, this.style.name());
				if (strokeWidth >= 0f) {
					json.put(JSON_STROKE_WIDTH, strokeWidth);
				}
				return json;
			} catch (JSONException e) {
				MTLog.w(TAG, e, "Error while converting to JSON!");
				return null;
			}
		}

		@Nullable
		public static JPaint fromJSON(@NonNull JSONObject json) {
			try {
				Paint.Style style = Paint.Style.valueOf(json.getString(JSON_STYLE));
				JPaint jPaint;
				float strokeWidth = (float) json.optDouble(JSON_STROKE_WIDTH, -1);
				if (strokeWidth >= 0f) {
					jPaint = new JPaint(style, strokeWidth);
				} else {
					jPaint = new JPaint(style);
				}
				return jPaint;
			} catch (JSONException e) {
				MTLog.w(TAG, e, "Error while parsing JSON!");
				return null;
			}
		}

		@NonNull
		@Override
		public String toString() {
			return JPaint.class.getSimpleName() + '[' + //
					"style:" + style + ',' + //
					"strokeWidth:" + strokeWidth + //
					']'; //
		}
	}

	public static abstract class JForm {

		public static final int FORM_CIRCLE = 1;
		public static final int FORM_RECT = 2;

		public abstract int getFormType();

		@Nullable
		public abstract JSONObject toJSON();

		private static final String JSON_FORM_TYPE = "formType";

		public void toJSON(@NonNull JSONObject json) throws JSONException {
			json.put(JSON_FORM_TYPE, getFormType());
		}

		@Nullable
		public static JForm fromJSON(@NonNull JSONObject json) {
			try {
				int formType = json.getInt(JSON_FORM_TYPE);
				switch (formType) {
				case FORM_CIRCLE:
					return JCircle.fromJSON(json);
				case FORM_RECT:
					return JRect.fromJSON(json);
				default:
					MTLog.w(TAG, "Unexpected form type ID '%s'!", formType);
					return null;
				}
			} catch (JSONException e) {
				MTLog.w(TAG, e, "Error while parsing JSON!");
				return null;
			}
		}

		@NonNull
		@Override
		public String toString() {
			return getClass().getSimpleName() + '[' + //
					"getFormType():" + getFormType() + //
					']'; //
		}
	}

	public static class JCircle extends JForm {

		public float x;
		public float y;
		public float radius;

		public JCircle(float x, float y, float radius) {
			this.x = x;
			this.y = y;
			this.radius = radius;
		}

		@Override
		public int getFormType() {
			return JForm.FORM_CIRCLE;
		}

		private static final String JSON_X = "x";
		private static final String JSON_Y = "y";
		private static final String JSON_RADIUS = "radius";

		@Nullable
		@Override
		public JSONObject toJSON() {
			try {
				JSONObject json = new JSONObject();
				json.put(JSON_X, this.x);
				json.put(JSON_Y, this.y);
				json.put(JSON_RADIUS, this.radius);
				super.toJSON(json);
				return json;
			} catch (JSONException e) {
				MTLog.w(TAG, e, "Error while converting to JSON!");
				return null;
			}
		}

		@Nullable
		public static JCircle fromJSON(@NonNull JSONObject json) {
			try {
				float x = (float) json.getDouble(JSON_X);
				float y = (float) json.getDouble(JSON_Y);
				float radius = (float) json.getDouble(JSON_RADIUS);
				return new JCircle(x, y, radius);
			} catch (JSONException e) {
				MTLog.w(TAG, e, "Error while parsing JSON!");
				return null;
			}
		}
	}

	public static class JRect extends JForm {

		public final float left;
		public final float top;
		public final float right;
		public final float bottom;

		public JRect(float left, float top, float right, float bottom) {
			this.left = left;
			this.top = top;
			this.right = right;
			this.bottom = bottom;
		}

		@Override
		public int getFormType() {
			return JForm.FORM_RECT;
		}

		private static final String JSON_LEFT = "left";
		private static final String JSON_TOP = "top";
		private static final String JSON_RIGHT = "right";
		private static final String JSON_BOTTOM = "bottom";

		@Nullable
		@Override
		public JSONObject toJSON() {
			try {
				JSONObject json = new JSONObject();
				json.put(JSON_LEFT, this.left);
				json.put(JSON_TOP, this.top);
				json.put(JSON_RIGHT, this.right);
				json.put(JSON_BOTTOM, this.bottom);
				super.toJSON(json);
				return json;
			} catch (JSONException e) {
				MTLog.w(TAG, e, "Error while creating subway logo!");
				return null;
			}
		}

		@Nullable
		public static JRect fromJSON(@NonNull JSONObject json) {
			try {
				float left = (float) json.getDouble(JSON_LEFT);
				float top = (float) json.getDouble(JSON_TOP);
				float right = (float) json.getDouble(JSON_RIGHT);
				float bottom = (float) json.getDouble(JSON_BOTTOM);
				return new JRect(left, top, right, bottom);
			} catch (JSONException e) {
				MTLog.w(TAG, e, "Error while parsing JSON!");
				return null;
			}
		}
	}

	public static class JRotation implements MTLog.Loggable {

		private static final String LOG_TAG = JRotation.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		public final float degrees;
		public final float px;
		public final float py;

		public JRotation(float degrees, float px, float py) {
			this.degrees = degrees;
			this.px = px;
			this.py = py;
		}

		private static final String JSON_DEGREES = "degrees";
		private static final String JSON_PX = "px";
		private static final String JSON_PY = "py";

		@Nullable
		public JSONObject toJSON() {
			try {
				JSONObject json = new JSONObject();
				json.put(JSON_DEGREES, this.degrees);
				json.put(JSON_PX, this.px);
				json.put(JSON_PY, this.py);
				return json;
			} catch (JSONException e) {
				MTLog.w(LOG_TAG, e, "Error while converting to JSON!");
				return null;
			}
		}

		@Nullable
		public static JRotation fromJSON(@Nullable JSONObject json) {
			try {
				if (json == null) {
					return null;
				}
				float degrees = (float) json.getDouble(JSON_DEGREES);
				float px = (float) json.getDouble(JSON_PX);
				float py = (float) json.getDouble(JSON_PY);
				return new JRotation(degrees, px, py);
			} catch (JSONException e) {
				MTLog.w(LOG_TAG, e, "Error while parsing JSON!");
				return null;
			}
		}

		@NonNull
		@Override
		public String toString() {
			return JRotation.class.getSimpleName() + '[' + //
					"degrees:" + degrees + ',' + //
					"px:" + px + ',' + //
					"py:" + py + //
					']'; //
		}
	}
}
