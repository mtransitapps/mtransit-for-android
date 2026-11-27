package org.mtransit.android.data;

import android.graphics.Paint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.MTLog;

import java.util.HashSet;
import java.util.Objects;

public class JPaths implements MTLog.Loggable {

	protected static final String LOG_TAG = JPaths.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	private final String id;

	@NonNull
	protected final HashSet<JPath> paths = new HashSet<>();

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

	@Nullable
	public static JPaths fromJsonString(@Nullable String jsonString) {
		return JPathsExtKt.toJPathsOrNull(jsonString);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		JPaths jPaths = (JPaths) o;

		if (!id.equals(jPaths.id)) return false;
		return paths.equals(jPaths.paths);
	}

	@Override
	public int hashCode() {
		int result = id.hashCode();
		result = 31 * result + paths.hashCode();
		return result;
	}

	@NonNull
	@Override
	public String toString() {
		return JPaths.class.getSimpleName() + "{" +
				"id='" + id + '\'' +
				", paths=" + paths.size() +
				'}';
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

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			JPath jPath = (JPath) o;

			if (!paint.equals(jPath.paint)) return false;
			if (!form.equals(jPath.form)) return false;
			return Objects.equals(rotation, jPath.rotation);
		}

		@Override
		public int hashCode() {
			int result = paint.hashCode();
			result = 31 * result + form.hashCode();
			result = 31 * result + (rotation != null ? rotation.hashCode() : 0);
			return result;
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

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			JPaint jPaint = (JPaint) o;

			if (Float.compare(jPaint.strokeWidth, strokeWidth) != 0) return false;
			return style == jPaint.style;
		}

		@Override
		public int hashCode() {
			int result = style.hashCode();
			result = 31 * result + (strokeWidth != +0.0f ? Float.floatToIntBits(strokeWidth) : 0);
			return result;
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

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			JCircle jCircle = (JCircle) o;

			if (Float.compare(jCircle.x, x) != 0) return false;
			if (Float.compare(jCircle.y, y) != 0) return false;
			return Float.compare(jCircle.radius, radius) == 0;
		}

		@Override
		public int hashCode() {
			int result = (x != +0.0f ? Float.floatToIntBits(x) : 0);
			result = 31 * result + (y != +0.0f ? Float.floatToIntBits(y) : 0);
			result = 31 * result + (radius != +0.0f ? Float.floatToIntBits(radius) : 0);
			return result;
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

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			JRect jRect = (JRect) o;

			if (Float.compare(jRect.left, left) != 0) return false;
			if (Float.compare(jRect.top, top) != 0) return false;
			if (Float.compare(jRect.right, right) != 0) return false;
			return Float.compare(jRect.bottom, bottom) == 0;
		}

		@Override
		public int hashCode() {
			int result = (left != +0.0f ? Float.floatToIntBits(left) : 0);
			result = 31 * result + (top != +0.0f ? Float.floatToIntBits(top) : 0);
			result = 31 * result + (right != +0.0f ? Float.floatToIntBits(right) : 0);
			result = 31 * result + (bottom != +0.0f ? Float.floatToIntBits(bottom) : 0);
			return result;
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

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			JRotation jRotation = (JRotation) o;

			if (Float.compare(jRotation.degrees, degrees) != 0) return false;
			if (Float.compare(jRotation.px, px) != 0) return false;
			return Float.compare(jRotation.py, py) == 0;
		}

		@Override
		public int hashCode() {
			int result = (degrees != +0.0f ? Float.floatToIntBits(degrees) : 0);
			result = 31 * result + (px != +0.0f ? Float.floatToIntBits(px) : 0);
			result = 31 * result + (py != +0.0f ? Float.floatToIntBits(py) : 0);
			return result;
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
