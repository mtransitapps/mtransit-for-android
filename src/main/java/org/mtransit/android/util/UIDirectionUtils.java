package org.mtransit.android.util;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SpanUtils;

public final class UIDirectionUtils implements MTLog.Loggable {

	private static final String LOG_TAG = UIDirectionUtils.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private static final boolean USE_DRAWABLE = false;

	@Nullable
	private static ImageSpan directionImage = null;

	@Nullable
	private static ImageSpan getDirectionImage(@NonNull Context context) {
		if (directionImage == null) {
			directionImage = getNewDirectionImage(context);
		}
		return directionImage;
	}

	@Nullable
	private static ImageSpan getNewDirectionImage(@NonNull Context context) {
		return UISpanUtils.getNewImage(context,
				R.drawable.ic_baseline_arrow_forward_12_trim,
				false,
				true,
				false,
				-1
		);
	}

	@NonNull
	public static CharSequence decorateDirection(@NonNull Context context,
												 @NonNull String direction) {
		if (!USE_DRAWABLE) {
			return direction;
		}
		final String character = context.getString(org.mtransit.android.commons.R.string.trip_direction_character);
		if (direction.startsWith(character)) {
			final int start = 0;
			final int end = start + character.length();
			return SpanUtils.setNN(
					new SpannableStringBuilder(direction),
					start,
					end,
					getDirectionImage(context)
			);
		} else {
			return direction;
		}
	}

	@SuppressWarnings("WeakerAccess")
	public static void resetColorCache() {
		directionImage = null;
	}
}
