package org.mtransit.android.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.ThemeUtils;
import org.mtransit.android.ui.MTSuperscriptImageSpan;

// Simple SVG editor: https://yqnn.github.io/svg-path-editor/
public class UISpanUtils extends SpanUtils implements MTLog.Loggable {

	private static final String LOG_TAG = UISpanUtils.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@Nullable
	public static ImageSpan getNewImage(@NonNull Context context,
										@DrawableRes int id,
										boolean tint,
										boolean bounds,
										boolean superscript,
										int superscriptVerticalAlign) {
		return getNewImage(
				context,
				id,
				tint ? ThemeUtils.resolveColorAttribute(context, R.attr.colorOnSurface) : null,
				bounds,
				superscript,
				superscriptVerticalAlign == -1 ? null : superscriptVerticalAlign
		);
	}

	@Nullable
	public static ImageSpan getNewImage(@NonNull Context context,
										@DrawableRes int id,
										@Nullable Integer tintColor,
										boolean bounds,
										boolean superscript,
										@Nullable Integer verticalAlign) {
		Drawable drawable = ContextCompat.getDrawable(context, id);
		if (drawable == null) {
			MTLog.w(LOG_TAG, "Cannot load new image span!");
			return null;
		}
		if (tintColor != null) {
			drawable = DrawableCompat.wrap(drawable); // tint
		}
		if (bounds) {
			int left = 0;
			int right = drawable.getIntrinsicWidth();
			int top = 0;
			int bottom = drawable.getIntrinsicHeight();
			drawable.setBounds(left, top, right, bottom);
		}
		if (tintColor != null) {
			DrawableCompat.setTint(drawable, tintColor);
		}
		if (superscript && verticalAlign != null) {
			return new MTSuperscriptImageSpan(drawable, verticalAlign);
		}
		return new ImageSpan(drawable, verticalAlign == null ? DynamicDrawableSpan.ALIGN_BASELINE : verticalAlign);
	}
}
