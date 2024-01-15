package org.mtransit.android.util;

import static org.mtransit.commons.Constants.SPACE_;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.text.style.TypefaceSpan;

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

	private static final TypefaceSpan FONT_REGULAR = SpanUtils.getNewSansSerifTypefaceSpan();
	private static final TypefaceSpan FONT_CONDENSED = SpanUtils.getNewSansSerifCondensedTypefaceSpan();

	@NonNull
	public static CharSequence decorateDirection(@NonNull Context context,
												 @NonNull String direction,
												 boolean centered) {
		final int originalDirectionLength = direction.length();
		if (centered) {
			int spaceAdded = 0;
			while (direction.length() < 13 && spaceAdded <= 5) {
				//noinspection StringConcatenationInLoop
				direction += SPACE_;
				spaceAdded++;
			}
			return SpanUtils.setAll(direction,
					originalDirectionLength < 7 + 2 ? FONT_REGULAR : FONT_CONDENSED
			);
		}
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
