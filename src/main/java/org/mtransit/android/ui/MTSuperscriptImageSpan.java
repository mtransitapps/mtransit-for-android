package org.mtransit.android.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.style.ImageSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MTSuperscriptImageSpan extends ImageSpan {

	@NonNull
	private final Drawable drawable;

	public MTSuperscriptImageSpan(@NonNull Drawable drawable, int verticalAlignment) {
		super(drawable, verticalAlignment);
		this.drawable = drawable;
	}

	// TOP
	// ASCENT
	// BASELINE
	// DESCENT
	// BOTTOM
	@Override
	public int getSize(@NonNull Paint paint, @Nullable CharSequence text, int start, int end, @Nullable Paint.FontMetricsInt fm) {
		return super.getSize(paint, text, start, end, fm);
	}

	@Override
	public void draw(@NonNull Canvas canvas, @Nullable CharSequence text,
					 int start, int end,
					 float x, int top,
					 int y, int bottom,
					 @NonNull Paint paint) {
		Rect rect = this.drawable.getBounds();
		canvas.save();
		int transY = (bottom - top) / 8 - rect.height() / 8;
		canvas.translate(x, transY);
		this.drawable.draw(canvas);
		canvas.restore();
	}
}
