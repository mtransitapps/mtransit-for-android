package org.mtransit.android.ui;

import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

import androidx.annotation.NonNull;

import org.mtransit.android.commons.MTLog;

/**
 * Font metrics explained:
 * @see <a href="https://proandroiddev.com/android-and-typography-101-5f06722dd611">Android 101: Typography</a>
 */
public class MTTopSuperscriptSpan extends MetricAffectingSpan implements MTLog.Loggable {

	private static final String LOG_TAG = MTTopSuperscriptSpan.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private double ratio = 1.0;

	public MTTopSuperscriptSpan() {
	}

	public MTTopSuperscriptSpan(double ratio) {
		this.ratio = ratio;
	}

	@Override
	public void updateDrawState(@NonNull TextPaint paint) {
		updateBaselineShift(paint);
	}

	@Override
	public void updateMeasureState(@NonNull TextPaint paint) {
		updateBaselineShift(paint);
	}

	private void updateBaselineShift(@NonNull TextPaint paint) {
		paint.baselineShift += (int) (paint.getFontMetrics().top * ratio);
	}
}