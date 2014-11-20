package org.mtransit.android.data;

import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;

import android.graphics.Paint;

public class JPaths implements MTLog.Loggable {

	private static final String TAG = JPaths.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private String id;

	private HashSet<JPath> paths = new HashSet<JPath>();

	public JPaths(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void addPath(JPath newPath) {
		this.paths.add(newPath);
	}

	public HashSet<JPath> getPaths() {
		return paths;
	}

	public JSONObject toJSON() {
		try {
			JSONObject json = new JSONObject();
			json.put("id", this.id);
			final JSONArray jPaths = new JSONArray();
			if (this.paths != null) {
				for (JPath path : this.paths) {
					JSONObject jPath = new JSONObject();
					jPath.put("paint", path.paint.toJSON());
					jPath.put("form", path.form.toJSON());
					if (path.rotation != null) {
						jPath.put("rotation", path.rotation.toJSON());
					}
					jPaths.put(jPath);
				}
			}
			json.put("paths", jPaths);
			return json;
		} catch (JSONException e) {
			MTLog.w(TAG, e, "Error while converting to JSON!");
			return null;
		}
	}

	public static JPaths fromJSONString(String jsonString) {
		try {
			return jsonString == null ? null : fromJSON(new JSONObject(jsonString));
		} catch (JSONException e) {
			MTLog.w(TAG, e, "Error while parsing JSON string '%s'!", jsonString);
			return null;
		}
	}

	public static JPaths fromJSON(JSONObject json) {
		try {
			final String id = json.getString("id");
			JPaths jPaths = new JPaths(id);
			JSONArray jsonPaths = json.optJSONArray("paths");
			if (jsonPaths != null && jsonPaths.length() > 0) {
				for (int i = 0; i < jsonPaths.length(); i++) {
					JSONObject jsonPath = jsonPaths.getJSONObject(i);
					JSONObject jPaint = jsonPath.getJSONObject("paint");
					JSONObject jForm = jsonPath.getJSONObject("form");
					JSONObject jRotation = jsonPath.optJSONObject("rotation");
					jPaths.addPath(new JPath(JPaint.fromJSON(jPaint), JForm.fromJSON(jForm), JRotation.fromJSON(jRotation)));
				}
			}
			return jPaths;
		} catch (JSONException e) {
			MTLog.w(TAG, e, "Error while parsing JSON!");
			return null;
		}
	}

	public static class JPath {

		public JPaint paint;
		public JForm form;
		public JRotation rotation;

		public JPath(JPaint paint, JForm form) {
			this(paint, form, null);
		}

		public JPath(JPaint paint, JForm form, JRotation rotation) {
			this.paint = paint;
			this.form = form;
			this.rotation = rotation;
		}
	}

	public static class JPaint {

		public Paint.Style style;
		public float strokeWidth = -1f;

		public JPaint(Paint.Style style) {
			this(style, -1f);
		}

		public JPaint(Paint.Style style, float strokeWidth) {
			this.style = style;
			this.strokeWidth = strokeWidth;
		}

		public JSONObject toJSON() {
			try {
				JSONObject json = new JSONObject();
				json.put("style", this.style.name());
				if (strokeWidth >= 0f) {
					json.put("strokeWidth", strokeWidth);
				}
				return json;
			} catch (JSONException e) {
				MTLog.w(TAG, e, "Error while converting to JSON!");
				return null;
			}
		}

		public static JPaint fromJSON(JSONObject json) {
			try {
				final Paint.Style style = Paint.Style.valueOf(json.getString("style"));
				JPaint jPaint = new JPaint(style);
				final float strokeWidth = (float) json.optDouble("strokeWidth", -1);
				if (strokeWidth >= 0f) {
					jPaint.strokeWidth = strokeWidth;
				}
				return jPaint;
			} catch (JSONException e) {
				MTLog.w(TAG, e, "Error while parsing JSON!");
				return null;
			}
		}
	}

	public static abstract class JForm {

		public static final int FORM_CIRCLE = 1;
		public static final int FORM_RECT = 2;

		public abstract int getFormType();

		public abstract JSONObject toJSON();

		public void toJSON(JSONObject json) throws JSONException {
			json.put("formType", getFormType());
		}

		public static JForm fromJSON(JSONObject json) {
			try {
				int formType = json.getInt("formType");
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

		public JSONObject toJSON() {
			try {
				JSONObject json = new JSONObject();
				json.put("x", this.x);
				json.put("y", this.y);
				json.put("radius", this.radius);
				super.toJSON(json);
				return json;
			} catch (JSONException e) {
				MTLog.w(TAG, e, "Error while converting to JSON!");
				return null;
			}
		}

		public static JCircle fromJSON(JSONObject json) {
			try {
				final float x = (float) json.getDouble("x");
				final float y = (float) json.getDouble("y");
				final float radius = (float) json.getDouble("radius");
				return new JCircle(x, y, radius);
			} catch (JSONException e) {
				MTLog.w(TAG, e, "Error while parsing JSON!");
				return null;
			}
		}
	}

	public static class JRect extends JForm {

		public float left;
		public float top;
		public float right;
		public float bottom;

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

		public JSONObject toJSON() {
			try {
				JSONObject json = new JSONObject();
				json.put("left", this.left);
				json.put("top", this.top);
				json.put("right", this.right);
				json.put("bottom", this.bottom);
				super.toJSON(json);
				return json;
			} catch (JSONException e) {
				MTLog.w(TAG, e, "Error while creating subway logo!");
				return null;
			}
		}

		public static JRect fromJSON(JSONObject json) {
			try {
				final float left = (float) json.getDouble("left");
				final float top = (float) json.getDouble("top");
				final float right = (float) json.getDouble("right");
				final float bottom = (float) json.getDouble("bottom");
				return new JRect(left, top, right, bottom);
			} catch (JSONException e) {
				MTLog.w(TAG, e, "Error while parsing JSON!");
				return null;
			}
		}
	}

	public static class JRotation implements MTLog.Loggable {

		private static final String TAG = JRotation.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		public float degrees;
		public float px;
		public float py;

		public JRotation(float degrees, float px, float py) {
			this.degrees = degrees;
			this.px = px;
			this.py = py;
		}

		public JSONObject toJSON() {
			try {
				JSONObject json = new JSONObject();
				json.put("degrees", this.degrees);
				json.put("px", this.px);
				json.put("py", this.py);
				return json;
			} catch (JSONException e) {
				MTLog.w(TAG, e, "Error while converting to JSON!");
				return null;
			}
		}

		public static JRotation fromJSON(JSONObject json) {
			try {
				if (json == null) {
					return null;
				}
				final float degrees = (float) json.getDouble("degrees");
				final float px = (float) json.getDouble("px");
				final float py = (float) json.getDouble("py");
				return new JRotation(degrees, px, py);
			} catch (JSONException e) {
				MTLog.w(TAG, e, "Error while parsing JSON!");
				return null;
			}
		}
	}
}
