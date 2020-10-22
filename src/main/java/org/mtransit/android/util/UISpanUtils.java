package org.mtransit.android.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.style.ImageSpan;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import org.mtransit.android.BuildConfig;
import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.ThemeUtils;
import org.mtransit.android.ui.MTSuperscriptImageSpan;

public class UISpanUtils extends SpanUtils implements MTLog.Loggable {

	private static final String LOG_TAG = UISpanUtils.class.getSimpleName();

	static {
		if (BuildConfig.MIN_SDK_VERSION < 21) {
			AppCompatDelegate.setCompatVectorFromResourcesEnabled(true); // enable Vector Drawable for API Level < 21
		}
	}

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@Nullable
	public static ImageSpan getNewImage(@NonNull Context context, @DrawableRes int id, int verticalAlignment) {
		Drawable drawable = ContextCompat.getDrawable(context, id);
		if (drawable == null) {
			MTLog.w(LOG_TAG, "Cannot load new image span!");
			return null;
		}
		drawable = DrawableCompat.wrap(drawable); // tint
		int left = 0;
		int right = drawable.getIntrinsicWidth();
		int top = 0;
		int bottom = drawable.getIntrinsicHeight();
		drawable.setBounds(left, top, right, bottom);
		DrawableCompat.setTint(drawable, ThemeUtils.resolveColorAttribute(context, R.attr.colorOnSurface));
		return new MTSuperscriptImageSpan(drawable, verticalAlignment);
	}
}
